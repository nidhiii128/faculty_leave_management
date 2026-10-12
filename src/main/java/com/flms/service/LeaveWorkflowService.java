package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Status transitions: PENDING -> APPROVED | REJECTED (HOD) and PENDING -> CANCELLED (owner).
 * Every other transition is refused.
 */
@Service
@Transactional
public class LeaveWorkflowService {

    private final LeaveService leaveService;
    private final LeaveRequestRepository repository;

    public LeaveWorkflowService(LeaveService leaveService, LeaveRequestRepository repository) {
        this.leaveService = leaveService;
        this.repository = repository;
    }

    public LeaveRequest approve(Actor actor, Long id, String comment) {
        LeaveRequest leave = review(actor, id);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setReviewerComment(blankToNull(comment));
        return repository.save(leave);
    }

    public LeaveRequest reject(Actor actor, Long id, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new WorkflowException("A comment is required when rejecting leave");
        }
        LeaveRequest leave = review(actor, id);
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setReviewerComment(comment.trim());
        return repository.save(leave);
    }

    public LeaveRequest cancel(Actor actor, Long id) {
        LeaveRequest leave = leaveService.get(actor, id);
        LeaveService.requireOwner(actor, leave, "cancel");
        LeaveService.requirePending(leave, "cancelled");
        leave.setStatus(LeaveStatus.CANCELLED);
        return repository.save(leave);
    }

    private LeaveRequest review(Actor actor, Long id) {
        if (!actor.hod()) {
            throw new WorkflowException("Only the HOD can review leave requests");
        }
        LeaveRequest leave = leaveService.get(actor, id);
        LeaveService.requirePending(leave, "reviewed");
        leave.setReviewedBy(actor.username());
        return leave;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
