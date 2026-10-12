package com.flms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Form login with two roles:
 * FACULTY applies for / edits / cancels own leave; HOD reviews (approves / rejects) everyone's leave.
 * Demo users are in-memory (MVP scope); see docs/03-srs-architecture.md for the future LDAP/DB plan.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String DEMO_PASSWORD = "password";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/leaves/*/approve", "/leaves/*/reject").hasRole("HOD")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        String pw = encoder.encode(DEMO_PASSWORD);
        return new InMemoryUserDetailsManager(
            User.withUsername("faculty1").password(pw).roles("FACULTY").build(),
            User.withUsername("faculty2").password(pw).roles("FACULTY").build(),
            User.withUsername("hod").password(pw).roles("HOD").build());
    }
}
