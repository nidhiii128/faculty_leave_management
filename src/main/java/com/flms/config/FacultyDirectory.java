package com.flms.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Display names / departments for the demo users (stands in for an HR directory in the MVP).
 */
public final class FacultyDirectory {

    public record Profile(String username, String fullName, String department, boolean hod) {}

    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();

    static {
        add(new Profile("faculty1", "Dr. Asha Patil", "Computer Engineering", false));
        add(new Profile("faculty2", "Prof. Rahul Mehta", "Information Technology", false));
        add(new Profile("faculty3", "Prof. Neha Joshi", "Computer Engineering", false));
        add(new Profile("hod", "Dr. S. Kulkarni (HOD)", "Computer Engineering", true));
    }

    private FacultyDirectory() {}

    private static void add(Profile p) {
        PROFILES.put(p.username(), p);
    }

    public static Profile profile(String username) {
        return PROFILES.getOrDefault(username, new Profile(username, username, "General", false));
    }

    /** Faculty who can take another teacher's lectures (everyone except the HOD and the applicant). */
    public static List<Profile> colleaguesOf(String username) {
        return PROFILES.values().stream()
            .filter(p -> !p.hod() && !p.username().equals(username))
            .toList();
    }

    public static boolean isFaculty(String username) {
        Profile p = PROFILES.get(username);
        return p != null && !p.hod();
    }
}
