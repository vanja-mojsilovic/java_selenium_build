package build.pages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import build.tests.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openqa.selenium.JavascriptExecutor;
import java.io.InputStream;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;





public class BuildPage extends AbstractClass{

    // Locators
    @FindBy(xpath = "//tr[contains(@data-testid,'native-issue-table.ui.issue-row')]/td[2]//a")
    private List<WebElement> issueKeyLocator;

    @FindBy(xpath = "//div[@data-testid='jql-editor-input']")
    private WebElement jqlEditorLocator;

    @FindBy(xpath = "//button[@data-testid='jql-editor-search']")
    private WebElement jqlEditorSearchButtonLocator;

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
            System.out.println("Issue: " + key);
            System.out.println("Spot ID: " + spotId);

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
                                                System.out.println("spot-sample link: " + url);
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
                                                            System.out.println("spot-sample link: " + href);
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

}


