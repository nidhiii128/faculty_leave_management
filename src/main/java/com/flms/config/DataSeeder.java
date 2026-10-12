package com.flms.config;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Sample records so the dashboard isn't empty on a fresh install. Disable with SEED_DATA=false. */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final LeaveRequestRepository repository;

    public DataSeeder(LeaveRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        repository.save(leave("faculty1", LeaveType.CASUAL, today.plusDays(7), today.plusDays(8),
            "Family function in Nashik", LeaveStatus.PENDING, null));
        repository.save(leave("faculty1", LeaveType.DUTY, today.plusDays(14), today.plusDays(14),
            "External examiner duty at University of Mumbai", LeaveStatus.APPROVED, "Approved"));
        repository.save(leave("faculty2", LeaveType.SICK, today.minusDays(10), today.minusDays(9),
            "Viral fever, medical certificate attached", LeaveStatus.APPROVED, "Get well soon"));
        repository.save(leave("faculty2", LeaveType.EARNED, today.plusDays(20), today.plusDays(24),
            "Hometown visit", LeaveStatus.REJECTED, "Unit test week, please reschedule"));
        log.info("Seeded {} sample leave records", repository.count());
    }

    private static LeaveRequest leave(String user, LeaveType type, LocalDate start, LocalDate end,
                                      String reason, LeaveStatus status, String comment) {
        FacultyDirectory.Profile profile = FacultyDirectory.profile(user);
        LeaveRequest l = new LeaveRequest();
        l.setFacultyUsername(user);
        l.setFacultyName(profile.fullName());
        l.setDepartment(profile.department());
        l.setLeaveType(type);
        l.setStartDate(start);
        l.setEndDate(end);
        l.setReason(reason);
        l.setStatus(status);
        if (status != LeaveStatus.PENDING) {
            l.setReviewedBy("hod");
            l.setReviewerComment(comment);
        }
        return l;
    }
}
