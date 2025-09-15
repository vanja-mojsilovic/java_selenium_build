package build.tests;

import java.io.IOException;
import build.pages.*;

public class BuildTest extends BaseTest{
    public static void main(String[] args) throws IOException {
        BuildTest test = new BuildTest();

        boolean isCi = System.getenv("CI") != null;
        String email = VariablesPage.get("VANJA_EMAIL");
        String apiToken = VariablesPage.get("JIRA_API_KEY");
        System.out.println("Debug - Loaded Email: '" + email + "'");
        System.out.println("Debug - Loaded API Token Length: " + (apiToken != null ? apiToken.length() : "null"));
        if (email == null || email.trim().isEmpty()) {
            System.err.println("ERROR: VANJA_EMAIL environment variable is not set or is empty!");
            System.exit(1);
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            System.err.println(" ERROR: JIRA_API_KEY environment variable is not set or is empty!");
            System.exit(1);
        }
        BuildPage buildPage = new BuildPage(null);
        try {
            buildPage.testMyself(email, apiToken);
        } catch (IOException e) {
            System.err.println("Authentication test failed. Check credentials above.");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println(" Authentication successful! Proceeding with task execution...");
        TaskTest taskTest = new TaskTest();
        taskTest.searchTasks();
        System.exit(0);
    }
}
