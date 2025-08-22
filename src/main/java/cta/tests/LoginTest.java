package cta.tests;

import cta.pages.*;

public class LoginTest extends BaseTest {
    

    public void runTest() {
        
        LoginPage loginPage = new LoginPage(driver);
        VariablesPage variablesPage = new VariablesPage(driver);

        navigate(variablesPage.googleLoginPage);
        sleep(1000);

        loginPage.googleLogin(variablesPage.emailGoogle,variablesPage.passwordGoogle,variablesPage.googleSecretKey);
        sleep(4000);

        navigate(variablesPage.spothopperappPage);
 
        loginPage.spothopperappLogin();
        sleep(3000);

        navigate(variablesPage.githubIssueUrl);
        sleep(2000);
        
        loginPage.githubVerificationWithAuth(driver,variablesPage.emailGoogle,variablesPage.githubPassword,variablesPage.githubSecretKey);
        sleep(3000);

        navigate(variablesPage.jiraUrl);
        sleep(3000);

        loginPage.jiraSignIn(driver,variablesPage.googleSecretKey);
        sleep(8000);

        
    }
}
