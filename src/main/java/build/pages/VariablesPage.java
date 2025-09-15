package build.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import io.github.cdimascio.dotenv.Dotenv;

public class VariablesPage extends AbstractClass{
    // Load environment variables from .env file or system environment
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    // Variables
    public String emailGoogle;
    public String jiraApiKey;
    public String googleSecretKey;
    public String passwordGoogle;
    public String githubPassword;
    public String githubSecretKey;

    // URLs - 🔴 Fixed: Removed trailing spaces!
    public String googleLoginPage = "https://accounts.google.com";
    public String spothopperappPage = "https://www.spothopperapp.com/admin/spots/";
    public String githubIssueUrl = "https://github.com/SpotHopperLLC/content/issues/";
    public String jiraUrl = "https://spothopper.atlassian.net";
    public String jiraFilterPageUrl = jiraUrl + "/issues/"; // Avoids using filter=-2

    // Constructor
    public VariablesPage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        PageFactory.initElements(driver, this);

        // Load values using get() method
        this.emailGoogle = get("VANJA_EMAIL");
        this.jiraApiKey = get("JIRA_API_KEY");
        this.passwordGoogle = get("VANJA_GOOGLE_PASSWORD");
        this.googleSecretKey = get("VANJA_GOOGLE_SECRET_KEY");
        this.githubPassword = get("H_PASSWORD_VANJA");
        this.githubSecretKey = get("H_SECRET_KEY_VANJA");
    }

    // Get value from environment or .env file
    public static String get(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = dotenv.get(key);
        }
        return value != null ? value.trim() : null;
    }
}
