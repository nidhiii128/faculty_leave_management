package com.flms.web;

import com.flms.service.Actor;
import com.flms.service.LeaveReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.YearMonth;

@Controller
public class CalendarController {

    private final LeaveReportService reportService;
    private final Clock clock;

    public CalendarController(LeaveReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    /** /calendar?month=2026-11 (defaults to the current month). */
    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                           Authentication auth, Model model) {
        YearMonth shown = month != null ? month : YearMonth.now(clock);
        model.addAttribute("cal", reportService.calendar(Actor.from(auth), shown));
        return "calendar";
    }
}
