package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.flms.service.LeaveServiceTest.FACULTY1;
import static com.flms.service.LeaveServiceTest.FACULTY2;
import static com.flms.service.LeaveServiceTest.HOD;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "app.seed-data=false")
@Import({LeaveService.class, LeaveReportService.class, LeaveReportServiceTest.FixedClock.class})
class LeaveReportServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }

    @Autowired
    LeaveService leaveService;

    @Autowired
    LeaveReportService reports;

    @Autowired
    LeaveRequestRepository repository;

    @BeforeEach
    void data() {
        leaveService.apply(FACULTY1, LeaveType.CASUAL, TODAY.plusDays(3), TODAY.plusDays(4), "Sister's wedding");
        LeaveRequest past = leaveService.apply(FACULTY1, LeaveType.SICK, TODAY.minusDays(9), TODAY.minusDays(7), "Viral fever");
        LeaveRequest future = leaveService.apply(FACULTY2, LeaveType.DUTY, TODAY.plusDays(10), TODAY.plusDays(10), "Exam paper setting");
        setStatus(past, LeaveStatus.APPROVED);
        setStatus(future, LeaveStatus.APPROVED);
    }

    private void setStatus(LeaveRequest leave, LeaveStatus status) {
        leave.setStatus(status);
        repository.save(leave);
    }

    @Test
    void keywordSearchIsCaseInsensitiveAcrossNameReasonAndDepartment() {
        assertThat(reports.search(HOD, "WEDDING", null, null)).hasSize(1);
        assertThat(reports.search(HOD, "mehta", null, null)).hasSize(1);
        assertThat(reports.search(HOD, "information tech", null, null)).hasSize(1);
    }

    @Test
    void filtersCombine() {
        assertThat(reports.search(HOD, null, LeaveStatus.APPROVED, null)).hasSize(2);
        assertThat(reports.search(HOD, null, LeaveStatus.APPROVED, LeaveType.SICK)).hasSize(1);
        assertThat(reports.search(HOD, "fever", LeaveStatus.PENDING, null)).isEmpty();
    }

    @Test
    void facultySearchIsLimitedToOwnRecords() {
        assertThat(reports.search(FACULTY1, "exam", null, null)).isEmpty();
        assertThat(reports.search(FACULTY1, "", null, null)).hasSize(2);
    }

    @Test
    void summaryCountsAndUpcoming() {
        LeaveSummary hod = reports.summary(HOD);
        assertThat(hod.total()).isEqualTo(3);
        assertThat(hod.pending()).isEqualTo(1);
        assertThat(hod.approved()).isEqualTo(2);
        assertThat(hod.approvedDays()).isEqualTo(4);
        assertThat(hod.upcomingApproved()).extracting(LeaveRequest::getReason).containsExactly("Exam paper setting");

        LeaveSummary faculty1 = reports.summary(FACULTY1);
        assertThat(faculty1.total()).isEqualTo(2);
        assertThat(faculty1.upcomingApproved()).isEmpty();
    }
}
