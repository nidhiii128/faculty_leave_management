package com.flms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/** Journey 4 — HOD reviews leave and the dashboard reflects it (US-07, US-09). */
class ApprovalWorkflowUiIT extends BaseUiIT {

    @Test
    @DisplayName("HOD approves a request; faculty sees APPROVED with the HOD comment")
    void hodApprovesLeave() {
        login("faculty1");
        long id = applyForLeave("SICK", future(2), future(3), unique("Surgery follow-up"));
        logout();

        login("hod");
        open("/dashboard");
        int pendingBefore = Integer.parseInt(text("count-pending"));
        int approvedBefore = Integer.parseInt(text("count-approved"));

        open("/leaves/" + id);
        assertThat(text("leave-faculty")).isEqualTo("Dr. Asha Patil");
        driver.findElement(By.id("comment")).sendKeys("Get well soon");
        driver.findElement(By.id("approve-leave")).click();

        assertThat(text("flash-success")).isEqualTo("Leave request approved.");
        assertThat(text("leave-status")).isEqualTo("APPROVED");
        assertThat(exists("review-form")).isFalse();

        open("/dashboard");
        assertThat(Integer.parseInt(text("count-pending"))).isEqualTo(pendingBefore - 1);
        assertThat(Integer.parseInt(text("count-approved"))).isEqualTo(approvedBefore + 1);
        logout();

        login("faculty1");
        open("/leaves/" + id);
        assertThat(text("leave-status")).isEqualTo("APPROVED");
        assertThat(text("leave-comment")).isEqualTo("Get well soon");
        assertThat(exists("edit-leave")).isFalse();
    }

    @Test
    @DisplayName("Reject needs a comment; with a comment the request is REJECTED")
    void hodRejectsLeaveWithComment() {
        login("faculty2");
        long id = applyForLeave("EARNED", future(15), future(19), unique("Trip"));
        logout();

        login("hod");
        open("/leaves/" + id);
        driver.findElement(By.id("reject-leave")).click();
        assertThat(text("flash-error")).isEqualTo("A comment is required when rejecting leave");
        assertThat(text("leave-status")).isEqualTo("PENDING");

        driver.findElement(By.id("comment")).sendKeys("University exams that week");
        driver.findElement(By.id("reject-leave")).click();
        assertThat(text("leave-status")).isEqualTo("REJECTED");
        assertThat(text("leave-comment")).isEqualTo("University exams that week");
    }
}
