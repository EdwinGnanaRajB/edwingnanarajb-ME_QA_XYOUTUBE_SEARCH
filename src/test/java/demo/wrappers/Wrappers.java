package demo.wrappers;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Wrappers {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private Wrappers() {
    }

    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static List<WebElement> waitForVisibleElements(WebDriver driver, By locator) {
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        return driver.findElements(locator);
    }

    public static void clickElement(WebDriver driver, By locator) {
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    public static void clickElement(WebDriver driver, WebElement element) {
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    public static String getElementText(WebDriver driver, By locator) {
        return waitForVisible(driver, locator).getText();
    }

    public static void typeElement(WebDriver driver, By locator, String value) {
        WebElement element = waitForVisible(driver, locator);
        element.clear();
        element.sendKeys(value);
    }

    public static void submitElement(WebDriver driver, By locator) {
        waitForVisible(driver, locator).sendKeys(Keys.ENTER);
    }

    public static void scrollToBottom(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, document.body.scrollHeight)");
    }

    public static void scrollDown(WebDriver driver) {
        driver.findElement(By.tagName("body")).sendKeys(Keys.PAGE_DOWN);
    }
}
