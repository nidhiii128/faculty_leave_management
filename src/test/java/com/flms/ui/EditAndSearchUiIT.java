package com.flms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.assertj.core.api.Assertions.assertThat;

/** Journey 3 — update a pending request and find it with search / filters (US-05, US-06, US-08). */
class EditAndSearchUiIT extends BaseUiIT {

    @Test
    @DisplayName("Faculty edits a pending request and the changes are shown")
    void editPendingLeave() {
        login("faculty2");
        long id = applyForLeave("EARNED", future(20), future(24), unique("Hometown visit"));

        driver.findElement(By.id("edit-leave")).click();
        assertThat(text("page-title")).isEqualTo("Edit leave request");
        String newReason = unique("Shorter hometown visit");
        fillLeaveForm("CASUAL", future(20), future(21), newReason);
        driver.findElement(By.id("submit-leave")).click();

        assertThat(driver.getCurrentUrl()).endsWith("/leaves/" + id);
        assertThat(text("flash-success")).isEqualTo("Leave request updated.");
        assertThat(text("leave-days")).isEqualTo("2");
        assertThat(text("leave-reason")).isEqualTo(newReason);
    }

    @Test
    @DisplayName("Keyword, status and type filters find the right record")
    void searchAndFilter() {
        login("faculty1");
        String tag = unique("Conference");
        long id = applyForLeave("DUTY", future(30), future(31), tag + " at IIT Bombay");

        driver.findElement(By.id("nav-leaves")).click();
        driver.findElement(By.id("q")).sendKeys(tag);
        new Select(driver.findElement(By.id("status"))).selectByValue("PENDING");
        new Select(driver.findElement(By.id("type"))).selectByValue("DUTY");
        driver.findElement(By.id("search-btn")).click();

        assertThat(driver.findElements(By.cssSelector("#leave-table tbody tr"))).hasSize(1);
        assertThat(exists("leave-row-" + id)).isTrue();

        new Select(driver.findElement(By.id("status"))).selectByValue("APPROVED");
        driver.findElement(By.id("search-btn")).click();
        assertThat(exists("no-results")).isTrue();
    }

    @Test
    @DisplayName("Faculty cancels a pending request; edit is no longer offered")
    void cancelPendingLeave() {
        login("faculty1");
        applyForLeave("CASUAL", future(40), future(40), unique("Personal work"));

        driver.findElement(By.id("cancel-leave")).click();

        assertThat(text("leave-status")).isEqualTo("CANCELLED");
        assertThat(exists("edit-leave")).isFalse();
    }
}
