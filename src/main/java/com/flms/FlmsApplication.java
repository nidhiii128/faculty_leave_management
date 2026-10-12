package com.flms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Entry point. Runs standalone ("java -jar flms.war") or as a WAR inside Tomcat.
 */
@SpringBootApplication
public class FlmsApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(FlmsApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(FlmsApplication.class);
    }
}
