package com.flms.web;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** Form backing object for apply / edit leave. */
public class LeaveForm {

    @NotNull(message = "Select a leave type")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotBlank(message = "Reason is required")
    @Size(min = 5, max = 500, message = "Reason must be 5 to 500 characters")
    private String reason;

    /** Username of the colleague who will take the lectures (optional). */
    private String substitute;

    /** Optional supporting document, e.g. a medical certificate for sick leave. */
    private MultipartFile attachment;

    @AssertTrue(message = "End date cannot be before start date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public static LeaveForm from(LeaveRequest leave) {
        LeaveForm form = new LeaveForm();
        form.setLeaveType(leave.getLeaveType());
        form.setStartDate(leave.getStartDate());
        form.setEndDate(leave.getEndDate());
        form.setReason(leave.getReason());
        form.setSubstitute(leave.getSubstituteUsername());
        return form;
    }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSubstitute() { return substitute; }
    public void setSubstitute(String substitute) { this.substitute = substitute; }
    public MultipartFile getAttachment() { return attachment; }
    public void setAttachment(MultipartFile attachment) { this.attachment = attachment; }

    public boolean hasAttachment() {
        return attachment != null && !attachment.isEmpty();
    }
}
