package com.flms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected so tests can fix "today". */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
