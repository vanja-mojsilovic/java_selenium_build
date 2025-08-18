package cta.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {
    
    // Variables
    protected WebDriver driver;
    
    // Constructor
    public BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
    
    // Methods
   


    public void clickElement(WebDriver driver, WebElement element, String element_name,int numOfSeconds) {
		waitForClickabilityOfElement(driver, element, numOfSeconds);
		if (element.isEnabled()) 
		{
			scrollPageUncoverElement(driver, element);
			element.click();
		}
		else 
		{ 
			System.out.println(element_name+" NOT EXIST!");
			driver.quit();
		}
	}

    public WebElement waitForClickabilityOfElement(WebDriver driver, WebElement element, int numOfSeconds)
	{
		WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(numOfSeconds)); 
		WebElement webElement = w.until(ExpectedConditions.elementToBeClickable(element));
		return webElement;
	}

    public void scrollPageUncoverElement(WebDriver driver, WebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].scrollIntoView(true); window.scrollBy(0, -100);", element);
	}

    public WebElement waitForVisibilityOfElement(WebDriver driver, WebElement element, int numOfSeconds)
	{
		WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(numOfSeconds)); 
		WebElement webElement = w.until(ExpectedConditions.visibilityOf(element));
		return webElement;
	}

    public List<WebElement> waitForVisibilityOfElements(WebDriver driver, List<WebElement> elements, int numOfSeconds)
	{
		List<WebElement> result = new ArrayList<WebElement>();
		WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(numOfSeconds)); 
		for (WebElement element : elements) {
			try {
				w.until(ExpectedConditions.visibilityOf(element));
				result.add(element);
			}catch(org.openqa.selenium.TimeoutException e){
				System.out.println("Element was not visible within the timeout: " + element);
			}
	    }
		return result;
	}
    
   
}
