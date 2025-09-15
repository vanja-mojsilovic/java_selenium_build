package build.tests;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.net.URL;
import org.openqa.selenium.WebDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import build.pages.BuildPage;
import build.pages.VariablesPage;

public class TaskTest extends BaseTest{
    // Variables
    private int numberOfTasks;
    private final List<String> issueKeyCollection = new ArrayList<>();
    private final List<String> parentIssueKeyCollection = new ArrayList<>();
    private String logFileMessage = "";
    private int counter = 0;


    public void searchTasks() throws IOException {
        VariablesPage vars = new VariablesPage(driver);
        BuildPage buildPage = new BuildPage(driver);

        System.out.println("******* Starting Task Validation: 'Go Live' in QA *******");

        // 🔹 Step 1: Get all "Go Live" tasks currently in QA
        String jqlGoLiveInQA = "project = WEB AND summary ~ 'Go Live' AND status = QA";
        System.out.println("Fetching issues with JQL: " + jqlGoLiveInQA);

        String allKeyIssues = buildPage.getKeyIssuesByApiPost(jqlGoLiveInQA, vars.emailGoogle, vars.jiraApiKey);
        if (allKeyIssues.isEmpty()) {
            System.out.println("No 'Go Live' tasks found in QA.");
            return;
        }

        String issueInClause = "issue in (" + allKeyIssues + ")";
        System.out.println("Found tasks: " + allKeyIssues);

        // 🔹 Step 2: Find tasks that already have our comment to exclude them
        String jqlAlreadyCommented = jqlGoLiveInQA +
                " AND comment ~ 'Please check if there is a Web editor task on this branch'";
        System.out.println("Checking for already commented tasks with JQL: " + jqlAlreadyCommented);

        String alreadyCommentedIssues = buildPage.getKeyIssuesByApiPost(jqlAlreadyCommented, vars.emailGoogle, vars.jiraApiKey);

        String finalJql = issueInClause;
        if (!alreadyCommentedIssues.isEmpty()) {
            finalJql += " AND issue not in (" + alreadyCommentedIssues + ")";
            System.out.println("Excluding already-commented tasks: " + alreadyCommentedIssues);
        } else {
            System.out.println("No tasks have been commented on yet.");
        }

        System.out.println("Final JQL for processing: " + finalJql);

        // 🔹 Step 3: Get parent issues for remaining tasks
        String encodedJql = URLEncoder.encode(finalJql, StandardCharsets.UTF_8);
        String apiUrl = vars.jiraUrl + "/rest/api/3/search?jql=" + encodedJql + "&maxResults=1000";

        numberOfTasks = buildPage.getIssueKeyParentIsssueKeyFromApi(driver, apiUrl, issueKeyCollection, parentIssueKeyCollection);
        System.out.println("Number of tasks to process: " + numberOfTasks);

        if (numberOfTasks == 0) {
            System.out.println("No tasks to process. Exiting.");
            return;
        }

        // 🔹 Step 4: For each task, check child issues of parent for "Website Editor" work
        for (int i = 0; i < numberOfTasks; i++) {
            String issueKey = issueKeyCollection.get(i);
            String parentKey = parentIssueKeyCollection.get(i);

            System.out.println("\n--- Processing Task " + (i + 1) + ": " + issueKey + " (Parent: " + parentKey + ") ---");

            // Fetch child tasks using API
            List<String> summaries = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            List<String> statuses = new ArrayList<>();

            boolean hasWebsiteEditorInProgress = getChildTasksViaApi(
                    buildPage, vars.emailGoogle, vars.jiraApiKey, parentKey,
                    summaries, keys, statuses
            );

            // Log all child tasks
            for (int j = 0; j < summaries.size(); j++) {
                System.out.println("  " + (j + 1) + ". " + summaries.get(j) +
                        " [" + keys.get(j) + "] - " + statuses.get(j).toLowerCase());
            }

            // 🔹 Step 5: Comment if active Website Editor task exists
            if (hasWebsiteEditorInProgress) {
                String comment = "Please check if there is a Web editor task on this branch!";
                try {
                    buildPage.postJiraComment(issueKey, comment, vars.emailGoogle, vars.jiraApiKey);
                    System.out.println("Comment posted on: " + issueKey);
                } catch (IOException e) {
                    System.err.println("Failed to post comment on " + issueKey + ": " + e.getMessage());
                }
            } else {
                System.out.println("No in-progress Website Editor task found for parent " + parentKey);
            }

            logFileMessage = (counter + 1) + ". " + issueKey;
            if (hasWebsiteEditorInProgress) {
                logFileMessage += " >>>>> Issue found! <<<<< " + issueKey;
            }
            System.out.println("Log: " + logFileMessage);
            System.out.println(" -- end of the task --");

            counter++;
        }
    }

    /**
     * Fetches child issues of a parent using Jira API.
     * Returns true if any child has "website editor" in summary and is not Done/Closed.
     */
    private boolean getChildTasksViaApi(BuildPage buildPage, String email, String apiToken,
                                        String parentKey,
                                        List<String> summaries, List<String> keys, List<String> statuses) throws IOException {
        String childJql = "parent = " + parentKey;
        String children = buildPage.getKeyIssuesByApiPost(childJql, email, apiToken);

        if (children.isEmpty()) {
            return false;
        }

        String[] issueKeys = children.split(",");
        boolean hasActiveWebsiteEditor = false;

        for (String key : issueKeys) {
            key = key.trim();
            try {
                // Reuse existing method to get full issue details via API
                String apiUrl = "https://spothopper.atlassian.net/rest/api/3/issue/" + key +
                        "?fields=summary,status";
                String jsonResponse = fetchUrlContent(driver, apiUrl, email, apiToken); // You may need to add this helper

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonResponse);
                String summary = root.get("fields").get("summary").asText().toLowerCase();
                String status = root.get("fields").get("status").get("name").asText().toLowerCase();

                summaries.add(summary);
                keys.add(key);
                statuses.add(status);

                if (summary.contains("website editor")) {
                    System.out.println("  > Found Website Editor task: " + key);
                    if (!status.equals("done") && !status.equals("closed")) {
                        hasActiveWebsiteEditor = true;
                        System.out.println("  >>> Website Editor task is IN PROGRESS: " + key);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching details for " + key + ": " + e.getMessage());
            }
        }

        return hasActiveWebsiteEditor;
    }

    public String fetchUrlContent(WebDriver driver, String url, String email, String apiToken) throws IOException {
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
}
