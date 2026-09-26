package com.tcm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>}, validates it, and - if valid -
 * populates the {@link SecurityContextHolder} so downstream authorization
 * checks (and {@code @AuthenticationPrincipal}) see the caller. Runs before
 * {@code UsernamePasswordAuthenticationFilter} (see SecurityConfig).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isValid(token)) {
                // Resolved by id (the immutable `sub`), not the email claim:
                // an email that has since changed hands must not let an old
                // token authenticate as whoever holds it now. A user who has
                // since been deleted or deactivated simply isn't
                // authenticated, and the entry point answers 401.
                userDetailsService.findById(jwtService.extractUserId(token))
                        .filter(JwtAuthenticationFilter::isUsable)
                        .ifPresent(userDetails -> authenticate(userDetails, request));
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isUsable(UserDetails userDetails) {
        return userDetails.isEnabled() && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired() && userDetails.isCredentialsNonExpired();
    }

    private static void authenticate(UserDetails userDetails, HttpServletRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
