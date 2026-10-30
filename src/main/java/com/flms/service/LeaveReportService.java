package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

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
        Map<LeaveType, Long> byType = new EnumMap<>(LeaveType.class);
        for (LeaveType t : LeaveType.values()) {
            byType.put(t, approved.stream().filter(l -> l.getLeaveType() == t).mapToLong(LeaveRequest::getDays).sum());
        }
        return new LeaveSummary(
            all.size(),
            count(all, LeaveStatus.PENDING),
            approved.size(),
            count(all, LeaveStatus.REJECTED),
            count(all, LeaveStatus.CANCELLED),
            approved.stream().mapToLong(LeaveRequest::getDays).sum(),
            upcoming,
            byType,
            coveringFor(actor));
    }

    /** Upcoming leave of colleagues where this user takes the lectures. */
    public List<LeaveRequest> coveringFor(Actor actor) {
        if (actor.hod()) {
            return List.of();
        }
        return repository.findBySubstituteUsernameAndStatusInAndEndDateGreaterThanEqualOrderByStartDate(
            actor.username(), EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED), LocalDate.now(clock));
    }

    /**
     * Month calendar. The HOD sees pending and approved leave of everyone. Faculty see their own
     * pending/approved leave plus colleagues' approved leave (so they know who is away).
     */
    public CalendarMonth calendar(Actor actor, YearMonth month) {
        LocalDate today = LocalDate.now(clock);
        LocalDate first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate last = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<LeaveRequest> leaves = repository
            .findOverlapping(first, last, EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)).stream()
            .filter(l -> actor.hod() || l.getStatus() == LeaveStatus.APPROVED
                || l.getFacultyUsername().equals(actor.username()))
            .toList();
        List<List<CalendarMonth.Day>> weeks = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusWeeks(1)) {
            List<CalendarMonth.Day> week = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate day = d.plusDays(i);
                List<LeaveRequest> onDay = leaves.stream()
                    .filter(l -> !day.isBefore(l.getStartDate()) && !day.isAfter(l.getEndDate()))
                    .toList();
                week.add(new CalendarMonth.Day(day, YearMonth.from(day).equals(month), day.equals(today),
                    day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY, onDay));
            }
            weeks.add(week);
        }
        return new CalendarMonth(month, month.minusMonths(1), month.plusMonths(1), weeks);
    }

    private static long count(List<LeaveRequest> leaves, LeaveStatus status) {
        return leaves.stream().filter(l -> l.getStatus() == status).count();
    }
}
