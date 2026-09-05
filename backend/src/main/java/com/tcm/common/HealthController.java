package com.tcm.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Trivial health-check endpoint, purely to prove the app boots and routes
 * work. Not a substitute for Spring Boot Actuator; a real readiness/liveness
 * setup can be added later if needed.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
