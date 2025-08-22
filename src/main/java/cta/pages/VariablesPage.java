package cta.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import com.github.dockerjava.core.dockerfile.DockerfileStatement.Env;
import io.github.cdimascio.dotenv.Dotenv;


public class VariablesPage extends BasePage{
    
    
    // Variables
    //private WebDriver driver;
    private static final Dotenv dotenv = Dotenv.configure()
                                                .ignoreIfMissing()
                                                .load();
    public String emailGoogle = get("VANJA_EMAIL");
    public String passwordGoogle = get("VANJA_GOOGLE_PASSWORD");
    public String googleSecretKey = get("VANJA_GOOGLE_SECRET_KEY");
    public String githubPassword = get("H_PASSWORD_VANJA");
    public String githubSecretKey = get("H_SECRET_KEY_VANJA");
    public String googleLoginPage = "https://accounts.google.com/";
    public String spothopperappPage = "https://www.spothopperapp.com/admin/spots/";
    public String githubIssueUrl = "https://github.com/SpotHopperLLC/content/issues/";
    public String jiraUrl = "https://spothopper.atlassian.net/";

    // Constructor
    public VariablesPage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // Methods
     

    public static String get(String key) {
        String value = System.getenv(key); 
        if (value == null || value.isEmpty()) {
            value = dotenv.get(key);       
        }
        return value;
    }


}
