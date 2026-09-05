package com.tcm.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserStatusRequest;
import com.tcm.user.model.Role;
import com.tcm.user.model.UserStatus;
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
 * Exercises the full user-management API through the real filter chain
 * (MockMvc, not @WebMvcTest) against a real, ephemeral Postgres, covering
 * both the ADMIN-only routes and the self-access rules.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UserControllerIT {

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
    void admin_canCreateListViewUpdateAndDeactivateAUser() throws Exception {
        UserRequest createRequest = new UserRequest(
                "Trina", "Trainer", uniqueEmail(), "Secret123!", "555-0100", Role.TRAINER);

        String createResponse = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TRAINER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(createResponse).get("id").asText();

        // list: no filters, one filter at a time, and combined - each param
        // is independently optional, so each combination must work on its
        // own (a role-only filter with name left null previously 500'd:
        // Hibernate couldn't infer :name's SQL type from a null value).
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("role", "TRAINER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + userId + "')]").exists());
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("name", "Trina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + userId + "')]").exists());
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("role", "TRAINER")
                        .param("name", "Trina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + userId + "')]").exists());

        // view
        mockMvc.perform(get("/api/v1/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Trina"));

        // update
        UserRequest updateRequest = new UserRequest(
                "Trina", "Trainerson", createRequest.email(), null, "555-0100", Role.TRAINER);
        mockMvc.perform(put("/api/v1/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Trainerson"));

        // deactivate
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserStatusRequest(UserStatus.INACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // reset password
        mockMvc.perform(post("/api/v1/users/" + userId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempPassword").isNotEmpty());
    }

    @Test
    void nonAdmin_getsForbiddenOnAdminOnlyRoutes() throws Exception {
        String email = uniqueEmail();
        String studentId = createUser(new UserRequest(
                "Sam", "Student", email, "Secret123!", null, Role.STUDENT));
        String studentToken = login(email, "Secret123!");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "Other", "Person", uniqueEmail(), "Secret123!", null, Role.STUDENT))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/users/" + studentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserStatusRequest(UserStatus.INACTIVE))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users/" + studentId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdmin_canViewAndEditOwnProfile_butNotAnotherUsersOrOwnRole() throws Exception {
        String email = uniqueEmail();
        String studentId = createUser(new UserRequest(
                "Sam", "Student", email, "Secret123!", null, Role.STUDENT));
        String studentToken = login(email, "Secret123!");

        // own profile: viewable
        mockMvc.perform(get("/api/v1/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // own profile: editable (non-role fields)
        mockMvc.perform(put("/api/v1/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "Samuel", "Student", email, null, "555-0199", Role.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Samuel"));

        // own profile: cannot escalate own role
        mockMvc.perform(put("/api/v1/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "Samuel", "Student", email, null, "555-0199", Role.ADMIN))))
                .andExpect(status().isForbidden());

        // someone else's profile: not viewable
        String otherId = createUser(new UserRequest(
                "Other", "Person", uniqueEmail(), "Secret123!", null, Role.STUDENT));
        mockMvc.perform(get("/api/v1/users/" + otherId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
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
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
