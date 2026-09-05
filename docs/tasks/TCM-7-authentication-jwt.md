# TCM-7 — Authentication (JWT)

**Branch**: `TCM-7-authentication-jwt`
**Depends on**: TCM-6

## Goal

Stateless JWT authentication: login endpoint, token issuance/validation,
Spring Security filter chain enforcing role-based access from here on.

## Steps

1. Add `com.tcm.security` package:
   - `UserPrincipal.java` — wraps `User`, implements `UserDetails`
     (`getAuthorities()` maps `role` → `ROLE_ADMIN`/`ROLE_TRAINER`/
     `ROLE_STUDENT`).
   - `UserDetailsServiceImpl.java` — loads by email via `UserRepository`.
   - `JwtService.java` — issues/validates HS256 tokens using
     `JWT_SECRET`/`JWT_EXPIRATION_MS` from config; claims include `sub`
     (user id), `email`, `role`.
   - `JwtAuthenticationFilter.java extends OncePerRequestFilter` — reads
     `Authorization: Bearer`, validates, sets `SecurityContext`.
2. Add `com.tcm.config.SecurityConfig`:
   - Disable CSRF (stateless API), session policy `STATELESS`.
   - Permit `/api/v1/auth/**`, `/api/v1/health`, Swagger UI paths.
   - Require authentication for everything else.
   - Register the `JwtAuthenticationFilter` before
     `UsernamePasswordAuthenticationFilter`.
   - `PasswordEncoder` bean → `BCryptPasswordEncoder`.
   - CORS config allowing the frontend origin (from env var
     `CORS_ALLOWED_ORIGIN`).
3. Add `com.tcm.auth` package (controller-service-repository still respected
   — repository reused from `user` package):
   - `dto/LoginRequest.java` (email, password), `dto/AuthResponse.java`
     (token, expiresAt, user summary: id, name, email, role).
   - `AuthService.java` — authenticates via `AuthenticationManager`, issues
     JWT on success.
   - `AuthController.java` — `POST /api/v1/auth/login`. (Public
     self-registration is intentionally NOT exposed here — accounts are
     created by Administrators via `TCM-8`; document this decision.)
   - `GET /api/v1/auth/me` — returns the current authenticated user's
     profile from the `SecurityContext`.
4. Add global exception handling for `BadCredentialsException` →
   401 with a clean `ApiError` body (avoid leaking whether the email
   exists).
5. Tests: `AuthControllerIT` (MockMvc/Testcontainers) covering successful
   login, wrong password, unknown email, and an authenticated `/me` call.

## Acceptance Criteria

- `POST /api/v1/auth/login` with the bootstrap admin credentials returns a
  valid JWT.
- Calling any protected endpoint without a token returns 401; with a token,
  `SecurityContext` carries the correct authorities.
- `GET /api/v1/auth/me` returns the caller's profile.

## Out of Scope

- User CRUD (`TCM-8`).
- Frontend login screen (`TCM-9`).
- Refresh tokens / logout blacklisting (not required by the brief — flag as
  a possible future enhancement only if the user asks).
