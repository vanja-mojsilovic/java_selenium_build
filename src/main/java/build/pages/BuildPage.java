package build.pages;

import build.tests.BaseTest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import io.github.cdimascio.dotenv.Dotenv;


public class BuildPage extends AbstractClass{



    // Constructor
    public BuildPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    // Methods
    public JSONObject fetchSpotSampleLinks(String email, String apiToken) throws Exception {
        JSONObject result = new JSONObject();

        String apiUrl = "https://spothopper.atlassian.net/rest/api/3/search/jql";
        String jqlPayload = """
       {
         "jql": "issuetype in (Epic, LandingAG, Redesign) AND status = QA ORDER BY statusCategoryChangedDate ASC",
         "fields": ["key", "customfield_10053", "comment"]
       }
    """;

        String auth = Base64.getEncoder().encodeToString((email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        System.out.println("Request URL: " + apiUrl);



        try (OutputStream os = conn.getOutputStream()) {
            os.write(jqlPayload.getBytes(StandardCharsets.UTF_8));
        }

        String response;
        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                response = reader.lines().collect(Collectors.joining("\n"));
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String errorResponse = errorReader.lines().collect(Collectors.joining("\n"));
                throw new IOException("Request failed with status " + status + ": " + errorResponse);
            }
        }

        JSONObject json = new JSONObject(response);
        JSONArray issues = json.getJSONArray("issues");

        for (int i = 0; i < issues.length(); i++) {
            List<String> testSiteUrls = new ArrayList<>();
            JSONObject issue = issues.getJSONObject(i);
            String key = issue.getString("key");
            String spotId = issue.getJSONObject("fields").optString("customfield_10053", "null");
            //System.out.println("Issue: " + key);
            //System.out.println("Spot ID: " + spotId);

            JSONArray comments = issue.getJSONObject("fields")
                    .optJSONObject("comment")
                    .optJSONArray("comments");

            if (comments != null) {
                for (int j = 0; j < comments.length(); j++) {
                    JSONObject body = comments.getJSONObject(j).optJSONObject("body");
                    if (body != null && body.has("content")) {
                        JSONArray blocks = body.getJSONArray("content");
                        for (int b = 0; b < blocks.length(); b++) {
                            JSONArray parts = blocks.getJSONObject(b).optJSONArray("content");
                            if (parts != null) {
                                for (int p = 0; p < parts.length(); p++) {
                                    JSONObject part = parts.getJSONObject(p);

                                    // 🔹 Case 1: inlineCard with URL
                                    if ("inlineCard".equals(part.optString("type"))) {
                                        JSONObject attrs = part.optJSONObject("attrs");
                                        if (attrs != null) {
                                            String url = attrs.optString("url", "");
                                            url = trimAndRemoveForwardSlash(url);
                                            if (url.contains("spot-sample")) {
                                                testSiteUrls.add(url);
                                                //System.out.println("spot-sample link: " + url);
                                            }
                                        }
                                    }

                                    // 🔹 Case 2: text block with link mark
                                    if ("text".equals(part.optString("type"))) {
                                        JSONArray marks = part.optJSONArray("marks");
                                        if (marks != null) {
                                            for (int m = 0; m < marks.length(); m++) {
                                                JSONObject mark = marks.getJSONObject(m);
                                                if ("link".equals(mark.optString("type"))) {
                                                    JSONObject attrs = mark.optJSONObject("attrs");
                                                    if (attrs != null) {
                                                        String href = attrs.optString("href", "");
                                                        href = trimAndRemoveForwardSlash(href);
                                                        if (href.contains("spot-sample")) {
                                                            testSiteUrls.add(href);
                                                            //System.out.println("spot-sample link: " + href);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            String firstUrl = testSiteUrls.isEmpty() ? "null" : testSiteUrls.get(0);
            JSONObject entry = new JSONObject();
            entry.put("issue_key", key);
            entry.put("spot_id", spotId);
            entry.put("test_site_url", firstUrl);
            result.put(key, entry);
        }
        return result;
    }

    public void changeCityOnApp() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir")) // ensures correct path
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            String cookie = dotenv.get("SPOTHOPPER_COOKIES");
            if (cookie == null || cookie.isBlank()) {
                System.err.println("SPOTHOPPER_COOKIES not loaded. Check .env file format and location.");
                return;
            }
            int spotId = 321387;
            String apiUrl = "https://www.spothopperapp.com/api/spots/" + spotId + "/";
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Cookie", cookie);
            con.setDoOutput(true);
            String jsonInputString = "{ \"city\": \"Austin\" }";
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = reader.lines().collect(Collectors.joining("\n"));
                    System.out.println("City updated successfully! ");
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errorResponse = errorReader.lines().collect(Collectors.joining("\n"));
                    System.err.println(" Failed to update city: " + responseCode + " - " + errorResponse);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateSpotField(int spotId, String fieldName, String newValue) {
        try {
            String cookie = System.getenv("SPOTHOPPER_COOKIES");
            if (cookie == null || cookie.isBlank()) {
                Dotenv dotenv = Dotenv.configure()
                        .directory(System.getProperty("user.dir"))
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
                cookie = dotenv.get("SPOTHOPPER_COOKIES");
            }
            if (cookie == null || cookie.isBlank()) {
                System.err.println("SPOTHOPPER_COOKIES not provided or empty.");
                return;
            }
            String apiUrl = "https://www.spothopperapp.com/api/spots/" + spotId + "/";
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Cookie", cookie);
            con.setDoOutput(true);
            String jsonInputString = String.format("{ \"%s\": \"%s\" }", fieldName, newValue);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = reader.lines().collect(Collectors.joining("\n"));
                    System.out.println("Spot updated successfully! ");
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errorResponse = errorReader.lines().collect(Collectors.joining("\n"));
                    System.err.println("Failed to update spot: " + responseCode + " - " + errorResponse);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}


