package com.tcm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Placeholder security configuration. Permits all requests for now so the
 * bootstrap health-check endpoint is reachable without auth. This is
 * replaced by the real JWT-based, role-restricted filter chain in TCM-7 -
 * see docs/tasks/TCM-7-authentication-jwt.md.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
