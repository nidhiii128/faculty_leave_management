package com.flms.config;

import java.util.Map;

/**
 * Display names / departments for the demo users (stands in for an HR directory in the MVP).
 */
public final class FacultyDirectory {

    public record Profile(String fullName, String department) {}

    private static final Map<String, Profile> PROFILES = Map.of(
        "faculty1", new Profile("Dr. Asha Patil", "Computer Engineering"),
        "faculty2", new Profile("Prof. Rahul Mehta", "Information Technology"),
        "hod", new Profile("Dr. S. Kulkarni (HOD)", "Computer Engineering"));

    private FacultyDirectory() {}

    public static Profile profile(String username) {
        return PROFILES.getOrDefault(username, new Profile(username, "General"));
    }
}
