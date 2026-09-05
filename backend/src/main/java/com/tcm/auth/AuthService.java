package com.tcm.auth;

import com.tcm.auth.dto.AuthResponse;
import com.tcm.auth.dto.LoginRequest;
import com.tcm.security.JwtService;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException for both a wrong password and an
        // unknown email (see UserDetailsServiceImpl) - GlobalExceptionHandler
        // maps that to a generic 401.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, jwtService.extractExpiration(token), toSummary(principal.getUser()));
    }

    public AuthResponse.UserSummary me(UserPrincipal principal) {
        return toSummary(principal.getUser());
    }

    private AuthResponse.UserSummary toSummary(User user) {
        return new AuthResponse.UserSummary(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getRole());
    }
}
