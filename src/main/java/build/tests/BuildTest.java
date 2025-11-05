package build.tests;

import java.io.IOException;
import build.pages.BuildPage;
import build.pages.VariablesPage;
import com.google.gson.JsonObject;
import org.openqa.selenium.WebDriver;
import java.util.List;
import java.util.ArrayList;
import org.openqa.selenium.JavascriptExecutor;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Base64;
import java.util.stream.Collectors;






public class BuildTest extends BaseTest{
    public static void main(String[] args) throws Exception {
        // Set up
        BuildTest buildTest = new BuildTest();
        buildTest.setUp();
        WebDriver driver = buildTest.driver;
        BuildPage buildPage = new BuildPage(driver);
        VariablesPage variablesPage = new VariablesPage(driver);
        boolean isCi = System.getenv("CI") != null;
        String email = VariablesPage.get("VANJA_EMAIL");
        String apiToken = VariablesPage.get("JIRA_API_KEY");
        //System.out.println("Loaded Email: '" + email + "'");
        //System.out.println("Loaded API Token Length: " + (apiToken != null ? apiToken.length() : "null"));
        if (email == null || email.trim().isEmpty()) {
            System.err.println("ERROR: VANJA_EMAIL environment variable is not set or is empty!");
            System.exit(1);
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            System.err.println(" ERROR: JIRA_API_KEY environment variable is not set or is empty!");
            System.exit(1);
        }

        // Fetch all the tasks from Jira
        String initialJql = "labels NOT IN (WordPress,LocationLanding,LocationPicker,LandingBuild) AND issuetype in (Epic, LandingAG) AND status = QA AND assignee not in (membersOF(QA))";
        List<String> allTasks = buildPage.fetchIssueKeys(email,apiToken,initialJql);
        String suppressionJql = "((status CHANGED TO QA BEFORE -4d) OR (comment ~ \"Build settings done by automation.\")) AND labels NOT IN (WordPress, LocationLanding, LocationPicker, LandingBuild) AND issuetype IN (Epic, LandingAG) AND assignee NOT IN (membersOF(QA)) AND status in (QA)";
        List<String> suppressionList = buildPage.fetchIssueKeys(email,apiToken,suppressionJql);
        String finalTasksListString = buildPage.getFilteredTasksCsv(allTasks,suppressionList);
        if (finalTasksListString == null || finalTasksListString.trim().isEmpty()) {
            System.out.println("No tasks to process — skipping fetchSpotSampleLinks.");
            return;
        }

        String finalJql = "issue in (" + finalTasksListString + ")";
        JSONObject  tasks = buildPage.fetchSpotSampleLinks(email,apiToken,finalJql);
        for (String taskKey : tasks.keySet()) {
            JSONObject task = tasks.getJSONObject(taskKey);
            String key = task.getString("issue_key");
            String spotId = task.getString("spot_id");
            String testSiteUrl = task.getString("test_site_url");
            String spotIdFromUrl = buildPage.extractSpotIdFromUrl(testSiteUrl);
            System.out.println("spotId from Jira: " + spotId + " spotIdFromUrl: " + spotIdFromUrl);
            String jiraCommentMessage = "Build settings done by automation.\n";
            if(!spotId.equals(spotIdFromUrl)) {
                jiraCommentMessage = "NOTE FOR QA: Spot ID in Jira task and in test website URL do not match! Build settings done by automation.";
                buildPage.addCommentToIssue(email,apiToken,key,jiraCommentMessage);
                continue;
            }
            System.out.println(key + " " + spotId + " " + testSiteUrl);
            int spotIdInteger = Integer.parseInt(task.getString("spot_id"));
            JSONObject websiteFieldsJson = buildPage.fetchWebsiteFields(spotIdInteger);
            int spotIdInt = Integer.parseInt(spotId);
            boolean is_ada = websiteFieldsJson != null && websiteFieldsJson.has("is_ada")
                    ? websiteFieldsJson.optBoolean("is_ada", false)
                    : false;
            boolean is_real_website = websiteFieldsJson != null && websiteFieldsJson.has("is_real_website")
                    ? websiteFieldsJson.optBoolean("is_real_website", false)
                    : false;
            boolean is_wcache = websiteFieldsJson != null && websiteFieldsJson.has("is_wcache")
                    ? websiteFieldsJson.optBoolean("is_wcache", false)
                    : false;
            boolean is_wcache_test_location = websiteFieldsJson != null && websiteFieldsJson.has("is_wcache_test_location")
                    ? websiteFieldsJson.optBoolean("is_wcache_test_location", false)
                    : false;

            String test_site_number = websiteFieldsJson != null && websiteFieldsJson.has("test_site_number")
                    ? websiteFieldsJson.isNull("test_site_number") ? null : websiteFieldsJson.optString("test_site_number", null)
                    : null;

            String need_website_feedback = websiteFieldsJson != null && websiteFieldsJson.has("need_website_feedback")
                    ? websiteFieldsJson.isNull("need_website_feedback") ? null : websiteFieldsJson.optString("need_website_feedback", null)
                    : null;

            if (!is_ada) {
                boolean changeSuccess = buildPage.updateWebsiteBooleanField(spotIdInteger, "is_ada", true);
                if (changeSuccess) {
                    String message = "ADA checkbox has been set to true";
                    jiraCommentMessage += message + "\n";
                    System.out.println(message);
                }


            }
            if (!is_real_website) {
                boolean changeSuccess = buildPage.updateWebsiteBooleanField(spotIdInteger, "is_real_website", true);
                if (changeSuccess) {
                    String message = "Real Website has been set to true";
                    jiraCommentMessage += message + "\n";
                    System.out.println(message);
                }
            }

            if (!is_wcache) {
                boolean changeSuccess = buildPage.updateWebsiteBooleanField(spotIdInteger, "is_wcache", true);
                if (changeSuccess) {
                    String message = "Wcache checkbox has been set to true";
                    jiraCommentMessage += message + "\n";
                    System.out.println(message);
                }
            }
            if (!is_wcache_test_location) {
                boolean changeSuccess = buildPage.updateWebsiteBooleanField(spotIdInteger, "is_wcache_test_location", true);
                if (changeSuccess) {
                    String message = "Wcache test location checkbox has been set to true";
                    jiraCommentMessage += message + "\n";
                    System.out.println(message);
                }
            }

            if (test_site_number == null || test_site_number.isEmpty()) {
                int startIndex = testSiteUrl.indexOf("https://spot-sample-") + "https://spot-sample-".length();
                int endIndex = testSiteUrl.indexOf(".spotapps.co");
                String testSiteNumber = testSiteUrl.substring(startIndex, endIndex);
                boolean changeSuccess = buildPage.updateStringField(spotIdInt, "test_site_number", testSiteNumber);
                if (changeSuccess) {
                    String message = "Test site number has been updated: " + testSiteNumber;
                    jiraCommentMessage += message + "\n";
                    System.out.println(message);
                }
            }

            if (need_website_feedback == null || need_website_feedback.isEmpty()) {
                boolean changeSuccess = buildPage.updateFieldAndTriggerBuild(spotIdInt, "need_website_feedback", "Don't Need It");
                if (changeSuccess) {
                    String message = "Start Build button clicked";
                    jiraCommentMessage += message + "\n";
                    System.out.println(message);
                }
            }

            boolean invalidationSuccess = buildPage.wcacheInvalidation(testSiteUrl);
            if (invalidationSuccess) {
                String message = "Wcache invalidated.";
                jiraCommentMessage += message + "\n";
                System.out.println(message);
            }


            // Enter comment in Jira
            buildPage.addCommentToIssue(email,apiToken,key,jiraCommentMessage);
            System.out.println("-------- Spot ID: " + spotIdInteger);
            System.out.println("is_ada: " + is_ada);
            System.out.println("is_wcache: " + is_wcache);
            System.out.println("test_site_number: " + test_site_number);
            System.out.println("need_website_feedback: " + need_website_feedback);
            System.out.println("is_real_website: " + is_real_website);
            System.out.println("is_wcache_test_location: " + is_wcache_test_location);
            System.out.println(jiraCommentMessage);
        } // for loop
        System.out.println("End!");
        System.exit(0);
    } // main
} // class
