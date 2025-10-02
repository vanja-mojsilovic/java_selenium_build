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
import java.util.Collections;



public class BuildPage extends AbstractClass{



    // Constructor
    public BuildPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    // Methods
    public JSONObject fetchSpotSampleLinks(String email, String apiToken,String jql) throws Exception {
        JSONObject result = new JSONObject();
        System.out.println(jql);
        String apiUrl = "https://spothopper.atlassian.net/rest/api/3/search/jql";
        String jqlPayload = String.format("""
           {
             "jql": "%s",
             "fields": ["key", "customfield_10053", "comment"]
           }
        """, jql.replace("\"", "\\\""));
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



    public List<String> fetchIssueKeys(String email, String apiToken, String jql) throws Exception {
        String apiUrl = "https://spothopper.atlassian.net/rest/api/3/search/jql";

        String jqlPayload = String.format("""
        {
         "jql": "%s",
         "fields": ["key"]
       }
    """, jql.replace("\"", "\\\""));

        String auth = Base64.getEncoder().encodeToString((email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

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

        List<String> keys = new ArrayList<>();
        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            keys.add(issue.getString("key"));
        }

        return keys;
    }

    public String getFilteredTasksCsv(List<String> allTasks, List<String> suppressionList) {
        if (allTasks == null || allTasks.isEmpty()) {
            return "";
        }
        if (suppressionList == null) {
            suppressionList = Collections.emptyList();
        }
        List<String> filtered = new ArrayList<>();
        for (String task : allTasks) {
            if (!suppressionList.contains(task)) {
                filtered.add(task);
            }
        }
        if (filtered.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < filtered.size(); i++) {
            result.append(filtered.get(i));
            if (i < filtered.size() - 1) {
                result.append(",");
            }
        }
        return result.toString();
    }

    public JSONObject fetchWebsiteFields(int spotId) throws Exception {
        String cookie = getSpothopperCookie();
        if (cookie == null) return null;
        String urlStr = "https://www.spothopperapp.com/api/spots/" + spotId + "/websites";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Cookie", cookie);
        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String response = reader.lines().collect(Collectors.joining("\n"));
                if (response.trim().startsWith("{")) {
                    JSONObject json = new JSONObject(response);
                    JSONObject result = new JSONObject();
                    result.put("is_ada", json.optBoolean("is_ada", false));
                    result.put("is_wcache", json.optBoolean("is_wcache", false));
                    result.put("test_site_number", json.optString("test_site_number", "null"));
                    result.put("need_website_feedback", json.optBoolean("need_website_feedback", false));
                    result.put("is_real_website", json.optBoolean("is_real_website", false));
                    result.put("is_wcache_test_location", json.optBoolean("is_wcache_test_location", false));
                    return result;
                } else {
                    System.err.println("Response for spot " + spotId + " is not valid JSON.");
                    return null;
                }
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String errorResponse = errorReader.lines().collect(Collectors.joining("\n"));
                System.err.println("Request failed for spot " + spotId + " with status " + status + ": " + errorResponse);
                return null;
            }
        }
    }



    public void updateSpotField(int spotId, String fieldName, String newValue) {
        String cookie = getSpothopperCookie();
        if (cookie == null) return;
        try {
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
                    System.out.println("Spot updated successfully!");
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errorResponse = errorReader.lines().collect(Collectors.joining("\n"));
                    System.err.println(" Failed to update spot: " + responseCode + " - " + errorResponse);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getSpothopperCookie() {
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
            return null;
        }
        return cookie;
    }

    public void updateWebsiteBooleanField(int spotId, String fieldName, boolean newValue) {
        String cookie = getSpothopperCookie();
        if (cookie == null) {
            System.err.println("No cookie available — cannot proceed.");
            return;
        }
        try {
            String apiUrl = "https://www.spothopperapp.com/api/spots/" + spotId + "/websites";
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Cookie", cookie);
            con.setDoOutput(true);
            String jsonInputString = String.format("{ \"%s\": %s }", fieldName, newValue);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = reader.lines().collect(Collectors.joining("\n"));
                    System.out.println("✅ Spot " + spotId + " updated: " + fieldName + " = " + newValue);
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errorResponse = errorReader.lines().collect(Collectors.joining("\n"));
                    System.err.println("❌ Failed to update spot " + spotId + ": " + responseCode + " - " + errorResponse);
                }
            }
        } catch (IOException e) {
            System.err.println("Exception while updating spot " + spotId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


}


