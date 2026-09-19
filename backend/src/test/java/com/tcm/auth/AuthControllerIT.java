package com.tcm.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.auth.dto.LoginRequest;
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

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(email, password));
    }
}
