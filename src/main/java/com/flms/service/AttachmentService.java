package com.flms.service;

import com.flms.model.LeaveAttachment;
import com.flms.model.LeaveRequest;
import com.flms.repository.LeaveAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Supporting documents (medical certificates, duty letters). PDF, JPG or PNG up to 5 MB. */
@Service
@Transactional
public class AttachmentService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED = Map.of(
        "pdf", "application/pdf",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "png", "image/png");

    private final LeaveAttachmentRepository repository;
    private final LeaveService leaveService;

    public AttachmentService(LeaveAttachmentRepository repository, LeaveService leaveService) {
        this.repository = repository;
        this.leaveService = leaveService;
    }

    /** Throws WorkflowException if the file is not an allowed type or is too big. Call before saving the leave. */
    public static void validate(String fileName, long size) {
        if (size > MAX_BYTES) {
            throw new WorkflowException("The document must be 5 MB or smaller");
        }
        if (contentTypeFor(fileName) == null) {
            throw new WorkflowException("Only PDF, JPG or PNG documents can be attached");
        }
    }

    /** Owner attaches (or replaces) the document of their own request. */
    public void store(Actor actor, Long leaveId, String fileName, byte[] data) {
        LeaveRequest leave = leaveService.get(actor, leaveId);
        LeaveService.requireOwner(actor, leave, "attach a document to");
        String clean = cleanName(fileName);
        validate(clean, data.length);
        if (data.length == 0) {
            throw new WorkflowException("The attached file is empty");
        }
        repository.save(new LeaveAttachment(leaveId, clean, contentTypeFor(clean), data));
        leaveService.recordAttachment(leaveId, clean);
    }

    /** Owner and HOD can open the document. */
    @Transactional(readOnly = true)
    public Optional<LeaveAttachment> load(Actor actor, Long leaveId) {
        LeaveRequest leave = leaveService.get(actor, leaveId);
        if (!actor.hod() && !leave.getFacultyUsername().equals(actor.username())) {
            throw new LeaveNotFoundException(leaveId);
        }
        return repository.findById(leaveId);
    }

    static String contentTypeFor(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        return ALLOWED.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** Keep only the base name and safe characters (browsers may send a full path). */
    static String cleanName(String fileName) {
        String base = fileName == null ? "document" : fileName.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return base.isEmpty() ? "document" : base;
    }
}
