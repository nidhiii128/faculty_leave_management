package com.flms.web;

import com.flms.config.FacultyDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Values every page needs: environment badge, version, current user and role.
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAdvice {

    @Value("${app.environment}")
    private String environment;

    @Value("${app.version}")
    private String version;

    @ModelAttribute("appEnv")
    public String appEnv() {
        return environment;
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return version;
    }

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    @ModelAttribute("currentUser")
    public String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return FacultyDirectory.profile(auth.getName()).fullName();
    }

    @ModelAttribute("isHod")
    public boolean isHod() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
    }
}
