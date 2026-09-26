package com.tcm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcm.auth.dto.AuthResponse;
import com.tcm.auth.dto.LoginRequest;
import com.tcm.security.JwtService;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Unit test with a mocked {@link AuthenticationManager} and {@link JwtService}:
 * the service's own job is only to turn an authenticated principal into a
 * token plus the caller's summary.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "tina@example.com";
    private static final String PASSWORD = "Secret123!";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService);
    }

    @Test
    void login_withValidCredentials_returnsTokenExpiryAndUserSummary() {
        UserPrincipal principal = new UserPrincipal(user());
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");
        when(jwtService.extractExpiration("jwt-token")).thenReturn(expiresAt);

        AuthResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.user().name()).isEqualTo("Tina Trainer");
        assertThat(response.user().email()).isEqualTo(EMAIL);
        assertThat(response.user().role()).isEqualTo(Role.TRAINER);
        verify(authenticationManager).authenticate(argThat(auth ->
                EMAIL.equals(auth.getPrincipal()) && PASSWORD.equals(auth.getCredentials())));
    }

    @Test
    void login_withBadCredentials_propagatesAndIssuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void me_returnsThePrincipalsSummary() {
        AuthResponse.UserSummary summary = authService.me(new UserPrincipal(user()));

        assertThat(summary).isEqualTo(new AuthResponse.UserSummary(USER_ID, "Tina Trainer", EMAIL, Role.TRAINER));
    }

    private static User user() {
        return User.builder()
                .id(USER_ID)
                .firstName("Tina")
                .lastName("Trainer")
                .email(EMAIL)
                .passwordHash("hash")
                .role(Role.TRAINER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
