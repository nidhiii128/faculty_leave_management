package com.flms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/** Journey 2 — faculty applies for leave (US-02, US-03, US-04). */
class ApplyLeaveUiIT extends BaseUiIT {

    @Test
    @DisplayName("Faculty applies for leave; it is saved as PENDING and listed in My leave records")
    void applyForLeave() {
        login("faculty1");
        String reason = unique("Family function");

        long id = applyForLeave("CASUAL", future(5), future(7), reason);

        assertThat(text("flash-success")).contains("pending HOD approval");
        assertThat(text("leave-type")).isEqualTo("Casual leave");
        assertThat(text("leave-days")).isEqualTo("3");
        assertThat(text("leave-reason")).isEqualTo(reason);

        driver.findElement(By.id("nav-leaves")).click();
        assertThat(text("page-title")).isEqualTo("My leave records");
        assertThat(driver.findElement(By.id("leave-row-" + id)).getText()).contains(reason).contains("PENDING");
    }

    @Test
    @DisplayName("End date before start date is rejected with a message and nothing is saved")
    void invalidDateRangeIsRejected() {
        login("faculty1");
        String reason = unique("Wrong dates");

        open("/leaves/new");
        fillLeaveForm("SICK", future(10), future(8), reason);
        driver.findElement(By.id("submit-leave")).click();

        assertThat(text("form-errors")).contains("End date cannot be before start date");
        open("/leaves?q=" + reason.replace(' ', '+'));
        assertThat(exists("no-results")).isTrue();
    }

    @Test
    @DisplayName("Reason is mandatory")
    void reasonIsRequired() {
        login("faculty1");

        open("/leaves/new");
        fillLeaveForm("SICK", future(3), future(3), "");
        driver.findElement(By.id("submit-leave")).click();

        assertThat(driver.getPageSource()).contains("Reason is required");
    }
}
