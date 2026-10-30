package com.flms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Journey 6: substitute arrangement, supporting document and the leave calendar. */
class NewFeaturesUiIT extends BaseUiIT {

    private long applyWith(String type, LocalDate start, LocalDate end, String reason, String substitute, Path file) {
        open("/leaves/new");
        fillLeaveForm(type, start, end, reason);
        if (substitute != null) {
            new Select(driver.findElement(By.id("substitute"))).selectByValue(substitute);
        }
        if (file != null) {
            driver.findElement(By.id("attachment")).sendKeys(file.toAbsolutePath().toString());
        }
        driver.findElement(By.id("submit-leave")).click();
        String url = driver.getCurrentUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

    private static Path tempFile(String name, String content) throws IOException {
        Path dir = Files.createTempDirectory("flms-upload");
        return Files.writeString(dir.resolve(name), content);
    }

    @Test
    @DisplayName("Faculty applies with a substitute and a certificate; the substitute sees it on their dashboard")
    void substituteAndCertificate() throws IOException {
        login("faculty1");
        String reason = unique("Viral fever");
        long id = applyWith("SICK", future(2), future(3), reason, "faculty3", tempFile("certificate.pdf", "%PDF-1.4"));

        assertThat(text("leave-status")).isEqualTo("PENDING");
        assertThat(text("leave-substitute")).isEqualTo("Prof. Neha Joshi");
        assertThat(text("attachment-link")).isEqualTo("certificate.pdf");
        logout();

        login("faculty3");
        assertThat(driver.findElement(By.id("covering-" + id)).getText()).contains("Dr. Asha Patil").contains(reason);
        open("/leaves/" + id);
        assertThat(exists("substitute-note")).isTrue();
        assertThat(exists("owner-actions")).isFalse();
        assertThat(exists("attachment-link")).isFalse();
    }

    @Test
    @DisplayName("A file that is not PDF, JPG or PNG is refused and nothing is saved")
    void wrongFileTypeIsRefused() throws IOException {
        login("faculty1");
        String reason = unique("Wrong file");
        open("/leaves/new");
        fillLeaveForm("SICK", future(4), future(4), reason);
        driver.findElement(By.id("attachment")).sendKeys(tempFile("notes.txt", "hello").toAbsolutePath().toString());
        driver.findElement(By.id("submit-leave")).click();

        assertThat(text("flash-error")).isEqualTo("Only PDF, JPG or PNG documents can be attached");
        open("/leaves?q=" + reason.replace(' ', '+'));
        assertThat(exists("no-results")).isTrue();
    }

    @Test
    @DisplayName("The HOD sees the leave on each day of the calendar")
    void calendarShowsLeave() {
        login("faculty2");
        LocalDate start = future(40);
        applyWith("DUTY", start, start.plusDays(1), unique("Exam duty"), null, null);
        logout();

        login("hod");
        open("/calendar?month=" + start.toString().substring(0, 7));
        assertThat(text("page-title")).isEqualTo("Leave calendar");
        assertThat(driver.findElement(By.id("day-" + start)).getText()).contains("Prof. Rahul Mehta");
        assertThat(driver.findElement(By.id("day-" + start.plusDays(1))).getText()).contains("Prof. Rahul Mehta");
    }
}
