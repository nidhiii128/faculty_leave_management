package com.flms.ui;

import com.flms.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the real application on a random port and drives it through a browser.
 * Each test gets a fresh browser session.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.seed-data=false", "logging.level.org.htmlunit=ERROR"})
abstract class BaseUiIT {

    @LocalServerPort
    int port;

    WebDriver driver;

    @RegisterExtension
    final ScreenshotOnFailure screenshots = new ScreenshotOnFailure(() -> driver);

    @BeforeEach
    void openBrowser() {
        driver = DriverFactory.create();
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---- helpers shared by the journeys ----

    void open(String path) {
        driver.get("http://localhost:" + port + path);
    }

    void login(String username) {
        login(username, SecurityConfig.DEMO_PASSWORD);
    }

    void login(String username, String password) {
        open("/login");
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("login-btn")).click();
    }

    void logout() {
        driver.findElement(By.id("logout")).click();
    }

    /** Fills and submits the apply form; returns the new record id (from the details URL). */
    long applyForLeave(String type, LocalDate start, LocalDate end, String reason) {
        open("/leaves/new");
        fillLeaveForm(type, start, end, reason);
        driver.findElement(By.id("submit-leave")).click();
        assertThat(text("leave-status")).isEqualTo("PENDING");
        String url = driver.getCurrentUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

    void fillLeaveForm(String type, LocalDate start, LocalDate end, String reason) {
        new Select(driver.findElement(By.id("leaveType"))).selectByValue(type);
        setDate("startDate", start);
        setDate("endDate", end);
        WebElement r = driver.findElement(By.id("reason"));
        r.clear();
        r.sendKeys(reason);
    }

    /** Date inputs are typed differently per browser/locale, so set the ISO value directly. */
    void setDate(String id, LocalDate date) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];", driver.findElement(By.id(id)), date.toString());
    }

    String text(String id) {
        return driver.findElement(By.id(id)).getText().trim();
    }

    boolean exists(String id) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            return !driver.findElements(By.id(id)).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        }
    }

    static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    static LocalDate future(int days) {
        return LocalDate.now().plusDays(days);
    }
}
