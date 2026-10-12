package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Search and dashboard summary, scoped to what the user is allowed to see. */
@Service
@Transactional(readOnly = true)
public class LeaveReportService {

    private final LeaveRequestRepository repository;
    private final Clock clock;

    public LeaveReportService(LeaveRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<LeaveRequest> search(Actor actor, String keyword, LeaveStatus status, LeaveType type) {
        String owner = actor.hod() ? null : actor.username();
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return repository.search(owner, kw, status, type);
    }

    public LeaveSummary summary(Actor actor) {
        List<LeaveRequest> all = search(actor, null, null, null);
        LocalDate today = LocalDate.now(clock);
        List<LeaveRequest> approved = all.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).toList();
        List<LeaveRequest> upcoming = approved.stream()
            .filter(l -> !l.getEndDate().isBefore(today))
            .sorted(Comparator.comparing(LeaveRequest::getStartDate))
            .limit(5)
            .toList();
        return new LeaveSummary(
            all.size(),
            count(all, LeaveStatus.PENDING),
            approved.size(),
            count(all, LeaveStatus.REJECTED),
            count(all, LeaveStatus.CANCELLED),
            approved.stream().mapToLong(LeaveRequest::getDays).sum(),
            upcoming);
    }

    private static long count(List<LeaveRequest> leaves, LeaveStatus status) {
        return leaves.stream().filter(l -> l.getStatus() == status).count();
    }
}
