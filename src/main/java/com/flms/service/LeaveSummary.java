package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveType;

import java.util.List;
import java.util.Map;

/** Dashboard figures for the records visible to the current user. */
public record LeaveSummary(long total, long pending, long approved, long rejected, long cancelled,
                           long approvedDays, List<LeaveRequest> upcomingApproved,
                           Map<LeaveType, Long> approvedDaysByType, List<LeaveRequest> covering) {

    /** Largest value in approvedDaysByType, used to scale the bar chart (never 0). */
    public long maxTypeDays() {
        return Math.max(1, approvedDaysByType.values().stream().mapToLong(Long::longValue).max().orElse(1));
    }
}
