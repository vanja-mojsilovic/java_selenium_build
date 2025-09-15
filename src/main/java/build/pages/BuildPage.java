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

    public void enterJql(String jql) {
        WebElement element = waitForVisibilityOfElement(driver, jqlEditorLocator, 15);
        element.clear();
        element.sendKeys(jql);
        System.out.println("JQL entered: " + jql);
    }

    public void clickSearchJql() {
        WebElement element = waitForClickabilityOfElement(driver, jqlEditorSearchButtonLocator, 15);
        element.click();
        System.out.println("Search clicked!");
    }

    public String getAllKeyIssues() {
        List<WebElement> elements = waitForVisibilityOfElements(driver, issueKeyLocator, 10);
        List<String> keys = new ArrayList<>();
        for (WebElement el : elements) {
            String text = el.getText().trim();
            if (!text.isEmpty()) {
                keys.add(text);
            }
        }
        String result = String.join(",", keys);
        System.out.println("Found issues (UI): " + result);
        return result;
    }

    public String getKeyIssuesByApiPost(String jql, String email, String apiToken) throws IOException {
        String apiUrl = "https://spothopper.atlassian.net/rest/api/3/search";

        String credentials = email + ":" + apiToken;
        String encodedCreds = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("jql", jql);
        body.put("maxResults", 1000);
        String jsonBody = mapper.writeValueAsString(body);

        System.out.println("API Request Body: " + jsonBody);

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + encodedCreds);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            String error = readStream(conn.getErrorStream());
            System.err.println("Jira API Error (" + responseCode + "): " + error);
            throw new IOException("Jira API error: " + responseCode + " - " + error);
        }

        String response = readStream(conn.getInputStream());
        JsonNode root = mapper.readTree(response);
        JsonNode issues = root.get("issues");

        List<String> keys = new ArrayList<>();
        for (JsonNode issue : issues) {
            keys.add(issue.get("key").asText());
        }

        String result = String.join(",", keys);
        System.out.println("API Found Issues: " + result);
        return result;
    }

    /**
     * Post comment on Jira issue via REST API
     */
    public void postJiraComment(String issueKey, String commentText, String email, String apiToken) throws IOException {
        String apiUrl = "https://spothopper.atlassian.net/rest/api/3/issue/" + issueKey + "/comment";
        String credentials = email + ":" + apiToken;
        String encodedCreds = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        String jsonBody = "{"
                + "\"body\": {"
                + "  \"version\": 1,"
                + "  \"type\": \"doc\","
                + "  \"content\": ["
                + "    {"
                + "      \"type\": \"paragraph\","
                + "      \"content\": ["
                + "        {"
                + "          \"type\": \"text\","
                + "          \"text\": \"" + commentText.replace("\"", "\\\"") + "\""
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}"
                + "}";

        System.out.println("📤 Request Body: " + jsonBody);

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + encodedCreds);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        if (responseCode >= 400) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) error.append(line);
                System.err.println("❌ Failed to post comment on: " + issueKey);
                System.err.println("❌ Status: " + responseCode);
                System.err.println("❌ Response: " + error);
                throw new IOException("Jira API error: " + responseCode + " - " + error);
            }
        } else {
            System.out.println("✅ Comment posted on " + issueKey);
        }
    }

    public int getIssueKeyParentIsssueKeyFromApi(WebDriver driver, String apiUrl, List<String> issueKeys, List<String> parentKeys) throws IOException {
        VariablesPage vars = new VariablesPage(driver);
        return fetchAndParseIssuesDirect(apiUrl, vars.emailGoogle, vars.jiraApiKey, issueKeys, parentKeys);
    }

    private int fetchAndParseIssuesDirect(String apiUrl, String email, String apiToken, List<String> issueKeys, List<String> parentKeys) throws IOException {
        String credentials = email + ":" + apiToken;
        String encodedCreds = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + encodedCreds);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) err.append(line);
                throw new IOException("HTTP " + responseCode + ": " + err);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());
            JsonNode issues = root.get("issues");

            int count = 0;
            if (issues != null && issues.isArray()) {
                for (JsonNode issue : issues) {
                    String key = issue.get("key").asText();
                    JsonNode parent = issue.get("fields").get("parent");
                    String parentKey = parent != null ? parent.get("key").asText() : "N/A";

                    issueKeys.add(key);
                    parentKeys.add(parentKey);
                    System.out.println(count + ". Issue: " + key + " | Parent: " + parentKey);
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Check if parent has an in-progress "Website Editor" child task (via API)
     */
    public boolean hasInProgressWebsiteEditorTask(String parentKey, String email, String apiToken) throws IOException {
        String jql = "parent='" + parentKey + "' AND summary ~ 'Website Editor'";
        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);
        String apiUrl = "https://spothopper.atlassian.net/rest/api/3/search?jql=" + encodedJql +
                "&fields=summary,status&maxResults=50";

        String jsonResponse = makeGetRequest(apiUrl, email, apiToken);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode issues = root.get("issues");

        if (issues == null || !issues.isArray()) return false;

        for (JsonNode issue : issues) {
            String summary = issue.get("fields").get("summary").asText().toLowerCase();
            String status = issue.get("fields").get("status").get("name").asText().toLowerCase();
            if (summary.contains("website editor") && !status.equals("done") && !status.equals("closed")) {
                System.out.println(">>> In-progress Website Editor task found: " + issue.get("key").asText());
                return true;
            }
        }
        return false;
    }

    // --- Utility ---

    private String readStream(java.io.InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }

    private String makeGetRequest(String url, String email, String apiToken) throws IOException {
        String credentials = email + ":" + apiToken;
        String encodedCreds = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + encodedCreds);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                throw new IOException("HTTP " + responseCode + ": " + response);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            return response.toString();
        }
    }

    public void testMyself(String email, String apiToken) throws IOException {
        // ✅ Fixed: Removed trailing spaces in URL
        String url = "https://spothopper.atlassian.net/rest/api/3/myself";

        String credentials = email + ":" + apiToken;
        String encodedCreds = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "Basic " + encodedCreds);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        System.out.println("✅ Status Code: " + responseCode);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            System.out.println("✅ Response: " + response);
        } catch (IOException e) {
            // If error stream has data (e.g., 401), read that
            if (conn.getErrorStream() != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) error.append(line);
                    System.err.println("❌ Error Response: " + error);
                }
            }
            throw e;
        }
    }
}
