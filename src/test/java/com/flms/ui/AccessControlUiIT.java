package com.flms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Journey 5 — role-based access (FR-4, FR-6). */
class AccessControlUiIT extends BaseUiIT {

    @Test
    @DisplayName("Faculty never sees the HOD review controls")
    void facultyHasNoReviewControls() {
        login("faculty1");
        applyForLeave("CASUAL", future(9), future(9), unique("Bank work"));

        assertThat(exists("review-form")).isFalse();
        assertThat(exists("approve-leave")).isFalse();
        assertThat(exists("owner-actions")).isTrue();
    }

    @Test
    @DisplayName("Faculty cannot open another faculty member's record")
    void facultyCannotSeeOthersRecords() {
        login("faculty1");
        long id = applyForLeave("CASUAL", future(11), future(11), unique("Private matter"));
        logout();

        login("faculty2");
        open("/leaves/" + id);
        assertThat(exists("not-found")).isTrue();
        open("/leaves");
        assertThat(exists("leave-row-" + id)).isFalse();
    }

    @Test
    @DisplayName("HOD sees all records and has no Apply link")
    void hodSeesAllRecords() {
        login("faculty2");
        long id = applyForLeave("DUTY", future(12), future(12), unique("Seminar"));
        logout();

        login("hod");
        open("/leaves");
        assertThat(text("page-title")).isEqualTo("All leave records");
        assertThat(exists("leave-row-" + id)).isTrue();
        assertThat(exists("nav-new")).isFalse();
    }
}
