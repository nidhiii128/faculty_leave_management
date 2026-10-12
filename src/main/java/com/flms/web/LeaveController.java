package com.flms.web;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.service.Actor;
import com.flms.service.LeaveReportService;
import com.flms.service.LeaveService;
import com.flms.service.WorkflowException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveReportService reportService;

    public LeaveController(LeaveService leaveService, LeaveReportService reportService) {
        this.leaveService = leaveService;
        this.reportService = reportService;
    }

    @ModelAttribute("leaveTypes")
    public LeaveType[] leaveTypes() {
        return LeaveType.values();
    }

    @ModelAttribute("statuses")
    public LeaveStatus[] statuses() {
        return LeaveStatus.values();
    }

    @GetMapping
    public String list(Authentication auth, Model model,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) LeaveStatus status,
                       @RequestParam(required = false) LeaveType type) {
        model.addAttribute("leaves", reportService.search(Actor.from(auth), q, status, type));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        return "leaves/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("leaveForm", new LeaveForm());
        model.addAttribute("editing", false);
        return "leaves/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("leaveForm") LeaveForm form, BindingResult result,
                         Authentication auth, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            return "leaves/form";
        }
        try {
            LeaveRequest saved = leaveService.apply(Actor.from(auth), form.getLeaveType(),
                form.getStartDate(), form.getEndDate(), form.getReason());
            redirect.addFlashAttribute("success", "Leave request submitted and is pending HOD approval.");
            return "redirect:/leaves/" + saved.getId();
        } catch (WorkflowException e) {
            model.addAttribute("editing", false);
            model.addAttribute("error", e.getMessage());
            return "leaves/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("leave", leaveService.get(Actor.from(auth), id));
        return "leaves/view";
    }
}
