package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "app.seed-data=false")
@Import(LeaveService.class)
class LeaveServiceTest {

    static final Actor FACULTY1 = new Actor("faculty1", false);
    static final Actor FACULTY2 = new Actor("faculty2", false);
    static final Actor HOD = new Actor("hod", true);
    static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Autowired
    LeaveService service;

    @Test
    void applyCreatesPendingRecordWithProfile() {
        LeaveRequest leave = service.apply(FACULTY1, LeaveType.CASUAL, DAY, DAY.plusDays(2), "  Family function  ");

        assertThat(leave.getId()).isNotNull();
        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(leave.getFacultyName()).isEqualTo("Dr. Asha Patil");
        assertThat(leave.getReason()).isEqualTo("Family function");
        assertThat(leave.getDays()).isEqualTo(3);
    }

    @Test
    void applyRejectsEndBeforeStart() {
        assertThatThrownBy(() -> service.apply(FACULTY1, LeaveType.SICK, DAY, DAY.minusDays(1), "Fever"))
            .isInstanceOf(WorkflowException.class)
            .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    void singleDayLeaveIsAllowed() {
        assertThat(service.apply(FACULTY1, LeaveType.SICK, DAY, DAY, "Medical checkup").getDays()).isEqualTo(1);
    }

    @Test
    void hodCannotApply() {
        assertThatThrownBy(() -> service.apply(HOD, LeaveType.CASUAL, DAY, DAY, "Personal work"))
            .isInstanceOf(WorkflowException.class);
    }

    @Test
    void facultySeesOnlyOwnRecordsHodSeesAll() {
        service.apply(FACULTY1, LeaveType.CASUAL, DAY, DAY, "Personal work");
        service.apply(FACULTY2, LeaveType.DUTY, DAY, DAY, "Exam duty at university");

        assertThat(service.listVisible(FACULTY1)).extracting(LeaveRequest::getFacultyUsername).containsOnly("faculty1");
        assertThat(service.listVisible(HOD)).hasSize(2);
    }

    @Test
    void facultyCannotOpenSomeoneElsesRecord() {
        Long id = service.apply(FACULTY2, LeaveType.CASUAL, DAY, DAY, "Personal work").getId();

        assertThatThrownBy(() -> service.get(FACULTY1, id)).isInstanceOf(LeaveNotFoundException.class);
        assertThat(service.get(HOD, id).getFacultyUsername()).isEqualTo("faculty2");
    }
}
