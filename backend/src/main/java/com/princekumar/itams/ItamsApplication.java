package com.princekumar.itams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the IT Asset Management System backend.
 *
 * <p>Boots a Spring Boot application that exposes a REST API on the
 * port configured in {@code application.yml} (default {@code 8080}).</p>
 */
@SpringBootApplication
public class ItamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItamsApplication.class, args);
    }
}
