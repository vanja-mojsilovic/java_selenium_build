package cta.tests;

public class BuildTest extends BaseTest{
    public static void main(String[] args) {
        BuildTest test = new BuildTest();
        test.setUp();
        System.out.println("*******Build test********");

        
        LoginTest loginTest = new LoginTest();
        loginTest.driver = test.driver; 
        loginTest.runTest();

        test.tearDown();
        System.exit(0);

    }
}
