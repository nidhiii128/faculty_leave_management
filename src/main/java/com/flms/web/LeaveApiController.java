package com.flms.web;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.service.Actor;
import com.flms.service.LeaveReportService;
import com.flms.service.LeaveService;
import com.flms.service.LeaveSummary;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only JSON API (same visibility rules as the web pages). */
@RestController
@RequestMapping("/api")
public class LeaveApiController {

    private final LeaveService leaveService;
    private final LeaveReportService reportService;

    public LeaveApiController(LeaveService leaveService, LeaveReportService reportService) {
        this.leaveService = leaveService;
        this.reportService = reportService;
    }

    @GetMapping("/leaves")
    public List<LeaveRequest> search(Authentication auth,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) LeaveStatus status,
                                     @RequestParam(required = false) LeaveType type) {
        return reportService.search(Actor.from(auth), q, status, type);
    }

    @GetMapping("/leaves/{id}")
    public LeaveRequest get(Authentication auth, @PathVariable Long id) {
        return leaveService.get(Actor.from(auth), id);
    }

    @GetMapping("/summary")
    public LeaveSummary summary(Authentication auth) {
        return reportService.summary(Actor.from(auth));
    }
}
