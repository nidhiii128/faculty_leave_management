package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static com.flms.service.LeaveServiceTest.FACULTY1;
import static com.flms.service.LeaveServiceTest.FACULTY2;
import static com.flms.service.LeaveServiceTest.HOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "app.seed-data=false")
@Import({LeaveService.class, LeaveWorkflowService.class})
class LeaveWorkflowServiceTest {

    static final LocalDate DAY = LocalDate.of(2026, 12, 7);

    @Autowired
    LeaveService leaveService;

    @Autowired
    LeaveWorkflowService workflow;

    Long pendingId;

    @BeforeEach
    void createPendingLeave() {
        pendingId = leaveService.apply(FACULTY1, LeaveType.EARNED, DAY, DAY.plusDays(4), "Hometown visit").getId();
    }

    @Test
    void hodApprovesPendingLeave() {
        LeaveRequest leave = workflow.approve(HOD, pendingId, "Enjoy");

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(leave.getReviewedBy()).isEqualTo("hod");
        assertThat(leave.getReviewerComment()).isEqualTo("Enjoy");
    }

    @Test
    void rejectRequiresComment() {
        assertThatThrownBy(() -> workflow.reject(HOD, pendingId, "  "))
            .isInstanceOf(WorkflowException.class).hasMessageContaining("comment is required");

        LeaveRequest leave = workflow.reject(HOD, pendingId, "Exams scheduled that week");
        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    void facultyCannotApprove() {
        assertThatThrownBy(() -> workflow.approve(FACULTY1, pendingId, null))
            .isInstanceOf(WorkflowException.class).hasMessageContaining("Only the HOD");
    }

    @Test
    void reviewedLeaveCannotBeReviewedEditedOrCancelledAgain() {
        workflow.approve(HOD, pendingId, null);

        assertThatThrownBy(() -> workflow.reject(HOD, pendingId, "Changed my mind"))
            .isInstanceOf(WorkflowException.class);
        assertThatThrownBy(() -> leaveService.update(FACULTY1, pendingId, LeaveType.CASUAL, DAY, DAY, "New reason"))
            .isInstanceOf(WorkflowException.class);
        assertThatThrownBy(() -> workflow.cancel(FACULTY1, pendingId))
            .isInstanceOf(WorkflowException.class);
    }

    @Test
    void ownerCanEditAndCancelPendingLeave() {
        LeaveRequest edited = leaveService.update(FACULTY1, pendingId, LeaveType.CASUAL, DAY, DAY, "Shorter trip");
        assertThat(edited.getDays()).isEqualTo(1);
        assertThat(edited.getReason()).isEqualTo("Shorter trip");

        assertThat(workflow.cancel(FACULTY1, pendingId).getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThatThrownBy(() -> workflow.approve(HOD, pendingId, null)).isInstanceOf(WorkflowException.class);
    }

    @Test
    void otherFacultyCannotTouchTheRecord() {
        assertThatThrownBy(() -> workflow.cancel(FACULTY2, pendingId)).isInstanceOf(LeaveNotFoundException.class);
        assertThatThrownBy(() -> leaveService.update(FACULTY2, pendingId, LeaveType.CASUAL, DAY, DAY, "Hijack"))
            .isInstanceOf(LeaveNotFoundException.class);
    }

    @Test
    void editRejectsInvalidDates() {
        assertThatThrownBy(() -> leaveService.update(FACULTY1, pendingId, LeaveType.CASUAL, DAY, DAY.minusDays(3), "Oops"))
            .isInstanceOf(WorkflowException.class);
    }
}
