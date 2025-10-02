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
        BuildTest buildTest = new BuildTest();
        buildTest.setUp();
        WebDriver driver = buildTest.driver;
        BuildPage buildPage = new BuildPage(driver);
        VariablesPage variablesPage = new VariablesPage(driver);
        boolean isCi = System.getenv("CI") != null;
        String email = VariablesPage.get("VANJA_EMAIL");
        String apiToken = VariablesPage.get("JIRA_API_KEY");
        System.out.println("Loaded Email: '" + email + "'");
        System.out.println("Loaded API Token Length: " + (apiToken != null ? apiToken.length() : "null"));
        if (email == null || email.trim().isEmpty()) {
            System.err.println("ERROR: VANJA_EMAIL environment variable is not set or is empty!");
            System.exit(1);
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            System.err.println(" ERROR: JIRA_API_KEY environment variable is not set or is empty!");
            System.exit(1);
        }


        String jql = "labels NOT IN (WordPress,LocationLanding,LocationPicker,LandingBuild) AND issuetype in (Epic, LandingAG, Redesign) AND status = QA AND assignee not in (membersOF(QA))";
        JSONObject  tasks = buildPage.fetchSpotSampleLinks(email, apiToken);
        for (String taskKey : tasks.keySet()) {
            JSONObject task = tasks.getJSONObject(taskKey);
            String key = task.getString("issue_key");
            String spotId = task.getString("spot_id");
            String testSiteUrl = task.getString("test_site_url");
            System.out.println(key + " " + spotId + " " + testSiteUrl);
        }
        int spotId = 321387;
        String fieldName = "city";
        String newValue = "Seattle";
        buildPage.updateSpotField(spotId,fieldName,newValue);

        System.exit(0);
    }
}
