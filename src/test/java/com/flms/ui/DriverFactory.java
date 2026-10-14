package com.flms.ui;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.time.Duration;

/**
 * Picks the browser from -Dbrowser=htmlunit|chrome|firefox (default htmlunit).
 * HtmlUnit needs no installed browser, so the suite runs on any Jenkins agent;
 * chrome/firefox run headless and Selenium Manager downloads the matching driver.
 */
final class DriverFactory {

    private DriverFactory() {}

    static WebDriver create() {
        String browser = System.getProperty("browser", "htmlunit").toLowerCase();
        WebDriver driver = switch (browser) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1366,900");
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.addArguments("-headless");
                yield new FirefoxDriver(options);
            }
            default -> new HtmlUnitDriver(true);
        };
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        return driver;
    }
}
