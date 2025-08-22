package cta.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import java.util.List;
import org.jboss.aerogear.security.otp.Totp;


public class LoginPage extends BasePage {
    // Variables
    private WebDriver driver;



    // Locators
    @FindBy(xpath = "//input[@id='identifierId']")
    public WebElement emailGoogleLocator;

    @FindBy(xpath = "//div[@id='identifierNext']//button")
    public WebElement nextEmailGoogleButtonLocator;

    @FindBy(xpath = "//input[@name='Passwd']")
    public WebElement passwordGoogleLocator;

    @FindBy(xpath = "//div[@id='passwordNext']//button")
    public WebElement nextPasswordGoogleButtonLocator;

    @FindBy(xpath = "//input[@name='totpPin']")
    public WebElement enterCodeFieldLocator;

    @FindBy(xpath = "//div[@id='totpNext']//button")
    public WebElement nextAuthCodeGoogleButtonLocator;

    @FindBy(xpath ="//button[@name='googleSignUpButton']")
	List <WebElement> googleContinueWithGoogleLocator;

    @FindBy(xpath ="//button//span[contains(text(),'Continue')]")
	WebElement googleAccountContinueLocator;

    @FindBy(xpath ="//input[@id = 'login_field' and @name='login']")
	WebElement githubIssueUserNameLocator;

    @FindBy(xpath ="//input[@id = 'password' and @name='password']")
	WebElement githubIssuePasswordLocator;

    @FindBy(xpath ="//input[@value = 'Sign in' and @name='commit' and @type='submit']")
	WebElement githubIssueSignInLocator;
    
    @FindBy(xpath ="//input[@id = 'app_totp' and @name='app_otp']")
	WebElement githubIssueAuthenticatiorSixDigitsCodeLocator;

    @FindBy(xpath = "//button[contains(@id,'google-auth-button')]")
	WebElement signInGoogleInJiraLocator;

    @FindBy(xpath ="//input[@id='totpPin']")
	List<WebElement> googleAuthenticatorCodeInJiraLocator;

    @FindBy(xpath ="//div[@data-email='vanja.mojsilovic@spothopperapp.com']")
	WebElement chooseVanjaAccountLocator;



    // Constructor
    public LoginPage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // Methods
    public void jiraSignIn(WebDriver driver,String googleSecretKey){
        clickSignInGoogle();
    	clickGoogleAccunt();
        System.out.println("GoogleAccunt clicked!");
    	List<WebElement> elements = waitForVisibilityOfElements(driver,googleAuthenticatorCodeInJiraLocator, 15);
    	if(elements.size()>0) {
    		Totp totp = new Totp(googleSecretKey);
    	    String verificationCodeFromPopupOrJson = totp.now();
            enterGoogleAuthenticatorCode(verificationCodeFromPopupOrJson);
            System.out.println("GoogleAuthenticatorCode entered!");
            clickGoogleAuthenticatorCodeNextButton();
            System.out.println("GoogleAuthenticatorCodeNextButton clicked!");
    	}
    }

    public void clickSignInGoogle() {
		WebElement element = waitForVisibilityOfElement(driver,signInGoogleInJiraLocator, 15);
		element.click();
	}

    public void clickGoogleAccunt() {
		WebElement element = waitForVisibilityOfElement(driver,chooseVanjaAccountLocator, 15);
		element.click();
	}


    public void githubVerificationWithAuth(WebDriver driver,String emailGoogle,String githubPassword,String githubSecretKey){
        WebElement emailElement = waitForVisibilityOfElement(driver, githubIssueUserNameLocator, 3);
		emailElement.clear();
		emailElement.sendKeys(emailGoogle);
        System.out.println("Github username entered!");
		WebElement passwordElement = waitForVisibilityOfElement(driver, githubIssuePasswordLocator, 3);
		passwordElement.clear();
		passwordElement.sendKeys(githubPassword);
        System.out.println("Github password entered!");
		WebElement signInElement = waitForVisibilityOfElement(driver, githubIssueSignInLocator, 3);
		signInElement.click();
        System.out.println("Github sign in button clicked!");
	    Totp githubTotp = new Totp(githubSecretKey);
	    String githubVerificationCode = githubTotp.now();
		WebElement authenticatorCodeElement = waitForVisibilityOfElement(driver, githubIssueAuthenticatiorSixDigitsCodeLocator, 3);
		authenticatorCodeElement.clear();
		authenticatorCodeElement.sendKeys(githubVerificationCode);
        System.out.println("Github authetication code entered!");
    }


    public void spothopperappLogin(){
		List<WebElement> elements = waitForVisibilityOfElements(driver, googleContinueWithGoogleLocator, 5);
		if(!elements.isEmpty()) {
			clickElement(driver, elements.get(0), "googleAccountLocator", 15);
            System.out.println("Spothopper App Continue With Google Clicked!");
		}
        
    }

    public void googleLogin(String emailGoogle,String passwordGoogle,String googleSecretKey){
        enterEmailGoogle(emailGoogle);
        clickNextEmailGoogle();
        enterPasswordGoogle(passwordGoogle);
        clickNextPasswordGoogle();
        enterGoogleAuthenticatorCode(googleSecretKey);
        clickGoogleAuthenticatorCodeNextButton();
    }

    public void enterGoogleAuthenticatorCode(String googleSecretKey){
        WebElement element = waitForVisibilityOfElement(driver,enterCodeFieldLocator,15);
		Totp totp = new Totp(googleSecretKey);
		String verificationCode = totp.now();
        element.clear();
        element.sendKeys(verificationCode);
        System.out.println("Authentication Code is entered!");
    }

    public void clickGoogleAuthenticatorCodeNextButton(){
        WebElement element = waitForVisibilityOfElement(driver,nextAuthCodeGoogleButtonLocator,15);
        element.click();
        System.out.println("Next Auth Code clicked!");
    }


    public void clickNextPasswordGoogle(){
        WebElement element = waitForVisibilityOfElement(driver,nextPasswordGoogleButtonLocator,15);
        element.click();
        System.out.println("Next password clicked!");
    }

    public void enterPasswordGoogle(String password) {
        WebElement element = waitForVisibilityOfElement(driver,passwordGoogleLocator,15);
        element.clear();
        element.sendKeys(password);
        String enteredValue = element.getAttribute("value");
        if (enteredValue.equals(password)) {
            System.out.println("Password entered successfully.");
        } else {
            System.out.println("Password entry failed. ");
        }
    }

    public void clickNextEmailGoogle(){
        WebElement element = waitForVisibilityOfElement(driver, nextEmailGoogleButtonLocator, 15);
        element.click();
        System.out.println("Next Email clicked!");
    }

    public void enterEmailGoogle(String username) {
        WebElement element = waitForVisibilityOfElement(driver, emailGoogleLocator, 15);
        element.clear();
        element.sendKeys(username);
        String enteredValue = element.getAttribute("value");
        if (enteredValue.equals(username)) {
            System.out.println("Email entered successfully.");
        } else {
            System.out.println("Email entry failed. ");
        }
    }


    


}
