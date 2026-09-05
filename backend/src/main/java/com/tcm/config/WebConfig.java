package com.tcm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Placeholder MVC configuration. CORS is intentionally left unconfigured
 * (default same-origin behavior) until the frontend integration task
 * defines the allowed origins.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS stub disabled for now - see TCM-9/TCM-4 for frontend wiring.
    // Uncomment and configure once the frontend origin is known:
    //
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/api/**")
    //             .allowedOrigins("http://localhost:5173")
    //             .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    // }
}
