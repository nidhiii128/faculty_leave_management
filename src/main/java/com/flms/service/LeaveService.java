package com.flms.service;

import com.flms.config.FacultyDirectory;
import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Create and read leave records. Faculty see only their own records; the HOD sees all. */
@Service
@Transactional
public class LeaveService {

    private final LeaveRequestRepository repository;

    public LeaveService(LeaveRequestRepository repository) {
        this.repository = repository;
    }

    public LeaveRequest apply(Actor actor, LeaveType type, LocalDate start, LocalDate end, String reason) {
        if (actor.hod()) {
            throw new WorkflowException("The HOD reviews leave and cannot apply through this screen");
        }
        validateDates(start, end);
        FacultyDirectory.Profile profile = FacultyDirectory.profile(actor.username());
        LeaveRequest leave = new LeaveRequest();
        leave.setFacultyUsername(actor.username());
        leave.setFacultyName(profile.fullName());
        leave.setDepartment(profile.department());
        leave.setLeaveType(type);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setReason(reason.trim());
        leave.setStatus(LeaveStatus.PENDING);
        return repository.save(leave);
    }

    /** Owner may change dates, type and reason while the request is still PENDING. */
    public LeaveRequest update(Actor actor, Long id, LeaveType type, LocalDate start, LocalDate end, String reason) {
        LeaveRequest leave = get(actor, id);
        requireOwner(actor, leave, "edit");
        requirePending(leave, "edited");
        validateDates(start, end);
        leave.setLeaveType(type);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setReason(reason.trim());
        return repository.save(leave);
    }

    @Transactional(readOnly = true)
    public LeaveRequest get(Actor actor, Long id) {
        LeaveRequest leave = repository.findById(id).orElseThrow(() -> new LeaveNotFoundException(id));
        if (!actor.hod() && !leave.getFacultyUsername().equals(actor.username())) {
            // Don't reveal that someone else's record exists
            throw new LeaveNotFoundException(id);
        }
        return leave;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> listVisible(Actor actor) {
        return actor.hod()
            ? repository.findAllByOrderByStartDateDesc()
            : repository.findByFacultyUsernameOrderByStartDateDesc(actor.username());
    }

    static void requireOwner(Actor actor, LeaveRequest leave, String action) {
        if (!leave.getFacultyUsername().equals(actor.username())) {
            throw new WorkflowException("Only the faculty member who applied can " + action + " this request");
        }
    }

    static void requirePending(LeaveRequest leave, String action) {
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new WorkflowException("Only pending requests can be " + action
                + " (this one is " + leave.getStatus() + ")");
        }
    }

    static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new WorkflowException("Start and end dates are required");
        }
        if (end.isBefore(start)) {
            throw new WorkflowException("End date cannot be before start date");
        }
    }
}
