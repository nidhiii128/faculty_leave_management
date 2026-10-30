package com.flms.web;

import com.flms.config.FacultyDirectory;
import com.flms.model.LeaveAttachment;
import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.service.Actor;
import com.flms.service.AttachmentService;
import com.flms.service.LeaveReportService;
import com.flms.service.LeaveNotFoundException;
import com.flms.service.LeaveService;
import com.flms.service.LeaveWorkflowService;
import com.flms.service.WorkflowException;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveWorkflowService workflowService;
    private final LeaveReportService reportService;
    private final AttachmentService attachmentService;

    public LeaveController(LeaveService leaveService, LeaveWorkflowService workflowService,
                           LeaveReportService reportService, AttachmentService attachmentService) {
        this.leaveService = leaveService;
        this.workflowService = workflowService;
        this.reportService = reportService;
        this.attachmentService = attachmentService;
    }

    @ModelAttribute("colleagues")
    public List<FacultyDirectory.Profile> colleagues(Authentication auth) {
        return auth == null ? List.of() : FacultyDirectory.colleaguesOf(auth.getName());
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
            validateAttachment(form);
            Actor actor = Actor.from(auth);
            LeaveRequest saved = leaveService.apply(actor, form.getLeaveType(),
                form.getStartDate(), form.getEndDate(), form.getReason(), form.getSubstitute());
            storeAttachment(actor, saved.getId(), form);
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

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes redirect) {
        LeaveRequest leave = leaveService.get(Actor.from(auth), id);
        if (!leave.getFacultyUsername().equals(auth.getName())
                || leave.getStatus() != LeaveStatus.PENDING) {
            redirect.addFlashAttribute("error", "Only your own pending requests can be edited.");
            return "redirect:/leaves/" + id;
        }
        model.addAttribute("leaveForm", LeaveForm.from(leave));
        model.addAttribute("editing", true);
        model.addAttribute("leaveId", id);
        model.addAttribute("currentAttachment", leave.getAttachmentName());
        return "leaves/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("leaveForm") LeaveForm form,
                         BindingResult result, Authentication auth, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("leaveId", id);
            return "leaves/form";
        }
        try {
            validateAttachment(form);
            Actor actor = Actor.from(auth);
            leaveService.update(actor, id, form.getLeaveType(),
                form.getStartDate(), form.getEndDate(), form.getReason(), form.getSubstitute());
            storeAttachment(actor, id, form);
            redirect.addFlashAttribute("success", "Leave request updated.");
        } catch (WorkflowException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leaves/" + id;
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<byte[]> attachment(@PathVariable Long id, Authentication auth) {
        LeaveAttachment file = attachmentService.load(Actor.from(auth), id)
            .orElseThrow(() -> new LeaveNotFoundException(id));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(file.getFileName(), StandardCharsets.UTF_8).build().toString())
            .body(file.getData());
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        return transition(id, redirect, "Leave request cancelled.",
            () -> workflowService.cancel(Actor.from(auth), id));
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String comment,
                          Authentication auth, RedirectAttributes redirect) {
        return transition(id, redirect, "Leave request approved.",
            () -> workflowService.approve(Actor.from(auth), id, comment));
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String comment,
                         Authentication auth, RedirectAttributes redirect) {
        return transition(id, redirect, "Leave request rejected.",
            () -> workflowService.reject(Actor.from(auth), id, comment));
    }

    private String transition(Long id, RedirectAttributes redirect, String successMessage, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("success", successMessage);
        } catch (WorkflowException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leaves/" + id;
    }

    private static void validateAttachment(LeaveForm form) {
        if (form.hasAttachment()) {
            AttachmentService.validate(form.getAttachment().getOriginalFilename(), form.getAttachment().getSize());
        }
    }

    private void storeAttachment(Actor actor, Long leaveId, LeaveForm form) {
        if (!form.hasAttachment()) {
            return;
        }
        try {
            attachmentService.store(actor, leaveId, form.getAttachment().getOriginalFilename(),
                form.getAttachment().getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
