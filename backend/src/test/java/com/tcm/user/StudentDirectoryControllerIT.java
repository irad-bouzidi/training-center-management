package com.tcm.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * Exercises {@code /api/v1/students} through the real filter chain, per
 * docs/tasks/TCM-13.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class StudentDirectoryControllerIT {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@tcm.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "ChangeMe123!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void loginAsBootstrapAdmin() throws Exception {
        adminToken = login(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
    }

    @Test
    void admin_canListStudentsAndViewSummary() throws Exception {
        String email = uniqueEmail();
        String studentId = createUser(new UserRequest(
                "Sam", "Student", email, "Secret123!", null, Role.STUDENT));

        mockMvc.perform(get("/api/v1/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("name", "Sam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.profile.id=='" + studentId + "')]").exists());

        mockMvc.perform(get("/api/v1/students/" + studentId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.id").value(studentId))
                .andExpect(jsonPath("$.enrollments").isArray())
                .andExpect(jsonPath("$.enrollments").isEmpty())
                .andExpect(jsonPath("$.attendanceRate").isEmpty())
                .andExpect(jsonPath("$.grades").isArray())
                .andExpect(jsonPath("$.paymentBalance").isEmpty())
                .andExpect(jsonPath("$.certificates").isArray());
    }

    @Test
    void student_canViewOwnSummary_butNotListOrSomeoneElses() throws Exception {
        String email = uniqueEmail();
        String studentId = createUser(new UserRequest(
                "Sam", "Student", email, "Secret123!", null, Role.STUDENT));
        String studentToken = login(email, "Secret123!");

        mockMvc.perform(get("/api/v1/students/" + studentId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.id").value(studentId));

        mockMvc.perform(get("/api/v1/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());

        String otherId = createUser(new UserRequest(
                "Other", "Student", uniqueEmail(), "Secret123!", null, Role.STUDENT));
        mockMvc.perform(get("/api/v1/students/" + otherId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_onNonStudentUser_isBadRequest() throws Exception {
        String trainerId = createUser(new UserRequest(
                "Trina", "Trainer", uniqueEmail(), "Secret123!", null, Role.TRAINER));

        mockMvc.perform(get("/api/v1/students/" + trainerId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    private String createUser(UserRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new com.tcm.auth.dto.LoginRequest(email, password));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "student-" + UUID.randomUUID() + "@example.com";
    }
}
