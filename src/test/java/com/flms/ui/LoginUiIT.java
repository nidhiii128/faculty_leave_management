package com.flms.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Journey 1 — authentication (US-01). */
class LoginUiIT extends BaseUiIT {

    @Test
    @DisplayName("Wrong password shows an error and stays on the login page")
    void invalidLoginIsRejected() {
        login("faculty1", "wrong-password");

        assertThat(driver.getCurrentUrl()).contains("/login?error");
        assertThat(text("login-error")).isEqualTo("Invalid username or password.");
    }

    @Test
    @DisplayName("Faculty logs in, sees the dashboard with their name, and logs out")
    void validLoginAndLogout() {
        login("faculty1");

        assertThat(driver.getCurrentUrl()).endsWith("/dashboard");
        assertThat(text("page-title")).isEqualTo("Dashboard");
        assertThat(text("current-user")).isEqualTo("Dr. Asha Patil");

        logout();
        assertThat(text("logout-msg")).isEqualTo("You have been logged out.");
    }

    @Test
    @DisplayName("Protected pages redirect an anonymous user to login")
    void protectedPageRedirectsToLogin() {
        open("/leaves");

        assertThat(driver.getCurrentUrl()).endsWith("/login");
    }
}
