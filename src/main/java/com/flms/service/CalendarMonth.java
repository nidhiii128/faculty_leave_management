package com.flms.service;

import com.flms.model.LeaveRequest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** One month of the leave calendar, Monday-first weeks. */
public record CalendarMonth(YearMonth month, YearMonth previous, YearMonth next, List<List<Day>> weeks) {

    public record Day(LocalDate date, boolean inMonth, boolean today, boolean weekend, List<LeaveRequest> leaves) {}
}
