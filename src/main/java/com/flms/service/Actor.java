package com.flms.service;

import org.springframework.security.core.Authentication;

/** The logged-in user as the service layer sees it. */
public record Actor(String username, boolean hod) {

    public static Actor from(Authentication auth) {
        boolean hod = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        return new Actor(auth.getName(), hod);
    }
}
