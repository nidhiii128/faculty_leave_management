package com.flms.ui;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

/**
 * On test failure saves a PNG screenshot (real browsers) and the page HTML (all browsers)
 * to target/selenium-screenshots/. Runs before @AfterEach, so the browser is still open.
 * Jenkins archives this folder.
 */
class ScreenshotOnFailure implements AfterTestExecutionCallback {

    static final Path DIR = Path.of("target", "selenium-screenshots");

    private final Supplier<WebDriver> driver;

    ScreenshotOnFailure(Supplier<WebDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws IOException {
        if (context.getExecutionException().isEmpty() || driver.get() == null) {
            return;
        }
        WebDriver d = driver.get();
        String name = context.getRequiredTestClass().getSimpleName() + "_" + context.getRequiredTestMethod().getName();
        Files.createDirectories(DIR);
        Files.writeString(DIR.resolve(name + ".html"),
            "<!-- URL: " + d.getCurrentUrl() + " -->\n" + d.getPageSource());
        if (d instanceof TakesScreenshot ts) {
            try {
                Path png = ts.getScreenshotAs(OutputType.FILE).toPath();
                Files.copy(png, DIR.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
            } catch (UnsupportedOperationException | org.openqa.selenium.WebDriverException e) {
                // HtmlUnit cannot render images; the HTML snapshot above is the evidence
            }
        }
        System.out.println("[selenium] failure evidence saved: " + DIR.resolve(name).toAbsolutePath() + ".*");
    }
}
