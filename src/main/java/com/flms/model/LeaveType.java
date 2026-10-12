package com.flms.model;

public enum LeaveType {
    CASUAL("Casual leave"),
    SICK("Sick leave"),
    EARNED("Earned leave"),
    DUTY("On duty"),
    OTHER("Other");

    private final String label;

    LeaveType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
