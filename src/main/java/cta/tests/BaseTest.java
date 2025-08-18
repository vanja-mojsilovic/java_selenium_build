package cta.tests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

public abstract class BaseTest {
    protected WebDriver driver;

    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (System.getenv("CI") != null) {
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-gpu");
        }

        driver = new ChromeDriver(options);
        if (System.getenv("CI") == null) {
            driver.manage().window().maximize();
        }
    }

    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void navigate(String url) {
        driver.get(url);
        System.out.println("Navigate to: " + url);
    }
}
