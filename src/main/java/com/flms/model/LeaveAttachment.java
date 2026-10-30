package com.flms.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/** Supporting document for one leave request, stored in the database so it travels with the data volume. */
@Entity
public class LeaveAttachment {

    /** Same value as the LeaveRequest id (one document per request). */
    @Id
    private Long leaveId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    private long size;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    protected LeaveAttachment() {}

    public LeaveAttachment(Long leaveId, String fileName, String contentType, byte[] data) {
        this.leaveId = leaveId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.size = data.length;
    }

    public Long getLeaveId() { return leaveId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public byte[] getData() { return data; }
}
