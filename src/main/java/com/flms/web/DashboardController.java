package com.flms.web;

import com.flms.service.Actor;
import com.flms.service.LeaveReportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final LeaveReportService reportService;

    public DashboardController(LeaveReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("summary", reportService.summary(Actor.from(auth)));
        return "dashboard";
    }
}
