package com.tcm.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.auth.dto.LoginRequest;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserStatusRequest;
import com.tcm.user.model.Role;
import com.tcm.user.model.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exercises login + /me through the real filter chain (MockMvc, not
 * @WebMvcTest) against a real, ephemeral Postgres, so the bootstrap admin
 * seeded in TCM-6 is actually there to authenticate against.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@tcm.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "ChangeMe123!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_withBootstrapAdminCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(BOOTSTRAP_ADMIN_EMAIL))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(BOOTSTRAP_ADMIN_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@example.com", "whatever123")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * DaoAuthenticationProvider throws DisabledException for an INACTIVE
     * account - which used to fall through to the catch-all as a 500, and
     * told the caller the account existed. It must look exactly like a
     * wrong password.
     */
    @Test
    void login_asAnInactiveUser_is401_withTheSameMessageAsAWrongPassword() throws Exception {
        String adminToken = token(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
        String email = uniqueEmail();
        String userId = createStudent(adminToken, email);
        setStatus(adminToken, userId, UserStatus.INACTIVE);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "Secret123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(BOOTSTRAP_ADMIN_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void aTokenIssuedBeforeDeactivation_stopsWorking() throws Exception {
        String adminToken = token(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
        String email = uniqueEmail();
        String userId = createStudent(adminToken, email);
        String studentToken = token(email, "Secret123!");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk());

        setStatus(adminToken, userId, UserStatus.INACTIVE);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isUnauthorized());
    }

    /** The token names its user by id, so a changed email doesn't orphan it. */
    @Test
    void aTokenKeepsWorkingAfterItsUsersEmailChanges() throws Exception {
        String adminToken = token(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
        String email = uniqueEmail();
        String userId = createStudent(adminToken, email);
        String studentToken = token(email, "Secret123!");
        String newEmail = uniqueEmail();

        mockMvc.perform(put("/api/v1/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("Sam", "Student", newEmail, null, null, Role.STUDENT))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(newEmail));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsCallerProfile() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(BOOTSTRAP_ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /**
     * The malformed-request cases every endpoint inherits from
     * GlobalExceptionHandler, tested here on the one endpoint that needs no
     * token to reach. Both used to read as 500s - found by TCM-30's smoke
     * pass.
     */
    @Test
    void aMissingOrUnparseableBody_is400_inTheStandardErrorShape() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPathValueOfTheWrongType_is400_ratherThan500() throws Exception {
        String token = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/v1/courses/not-a-uuid").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a valid value")));
    }

    private String token(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createStudent(String adminToken, String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserRequest("Sam", "Student", email, "Secret123!", null, Role.STUDENT))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void setStatus(String adminToken, String userId, UserStatus status) throws Exception {
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserStatusRequest(status))))
                .andExpect(status().isOk());
    }

    private static String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@example.com";
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(email, password));
    }
}
