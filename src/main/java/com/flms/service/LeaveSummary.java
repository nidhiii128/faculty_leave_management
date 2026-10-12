package com.flms.service;

import com.flms.model.LeaveRequest;

import java.util.List;

/** Dashboard figures for the records visible to the current user. */
public record LeaveSummary(long total, long pending, long approved, long rejected, long cancelled,
                           long approvedDays, List<LeaveRequest> upcomingApproved) {
}
