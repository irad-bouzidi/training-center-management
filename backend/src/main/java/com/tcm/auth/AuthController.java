package com.tcm.auth;

import com.tcm.auth.dto.AuthResponse;
import com.tcm.auth.dto.LoginRequest;
import com.tcm.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public self-registration is intentionally NOT exposed here - accounts are
 * created by Administrators via the user management API (TCM-8).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthResponse.UserSummary me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal);
    }
}
