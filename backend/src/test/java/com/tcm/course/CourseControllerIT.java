package com.tcm.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseStatusRequest;
import com.tcm.course.model.CourseStatus;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.math.BigDecimal;
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
 * Exercises the course-management API through the real filter chain (MockMvc,
 * not @WebMvcTest) against a real, ephemeral Postgres, covering ADMIN-only
 * mutation routes and role-based visibility of DRAFT vs PUBLISHED courses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CourseControllerIT {

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
    void admin_canCreateListUpdatePublishAndArchiveACourse() throws Exception {
        CourseRequest createRequest = new CourseRequest(
                uniqueCode(), "Java Fundamentals", "Intro course", 40, 20,
                "Programming", null, BigDecimal.valueOf(500), CourseStatus.DRAFT);

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String courseId = objectMapper.readTree(createResponse).get("id").asText();

        // admin sees it in the unfiltered list (DRAFT included) and by status filter
        mockMvc.perform(get("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + courseId + "')]").exists());

        // update
        CourseRequest updateRequest = new CourseRequest(
                createRequest.code(), "Java Fundamentals, Revised", "Updated", 45, 25,
                "Programming", null, BigDecimal.valueOf(550), CourseStatus.DRAFT);
        mockMvc.perform(put("/api/v1/courses/" + courseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java Fundamentals, Revised"));

        // publish
        mockMvc.perform(patch("/api/v1/courses/" + courseId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseStatusRequest(CourseStatus.PUBLISHED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // archive
        mockMvc.perform(patch("/api/v1/courses/" + courseId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseStatusRequest(CourseStatus.ARCHIVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void assigningNonTrainerAsPrimaryTrainer_isRejected() throws Exception {
        String studentId = createUser(new UserRequest(
                "Sam", "Student", uniqueEmail(), "Secret123!", null, Role.STUDENT));

        CourseRequest request = new CourseRequest(
                uniqueCode(), "Java Fundamentals", null, 40, 20,
                "Programming", UUID.fromString(studentId), BigDecimal.valueOf(500), CourseStatus.DRAFT);

        mockMvc.perform(post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdmin_cannotCreateUpdateOrChangeStatus() throws Exception {
        String email = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", email, "Secret123!", null, Role.STUDENT));
        String studentToken = login(email, "Secret123!");
        String courseId = createCourse(uniqueCode(), null, CourseStatus.PUBLISHED);

        mockMvc.perform(post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseRequest(
                                uniqueCode(), "X", null, 10, 10, null, null, BigDecimal.TEN, CourseStatus.DRAFT))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/courses/" + courseId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseStatusRequest(CourseStatus.ARCHIVED))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsAndTrainers_onlySeePublishedCoursesThroughGeneralListEndpoint() throws Exception {
        String draftCode = uniqueCode();
        String publishedCode = uniqueCode();
        String draftId = createCourse(draftCode, null, CourseStatus.DRAFT);
        String publishedId = createCourse(publishedCode, null, CourseStatus.PUBLISHED);

        String studentEmail = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", studentEmail, "Secret123!", null, Role.STUDENT));
        String studentToken = login(studentEmail, "Secret123!");

        mockMvc.perform(get("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + publishedId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + draftId + "')]").doesNotExist());

        // a student passing status=DRAFT is still restricted to PUBLISHED
        mockMvc.perform(get("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + draftId + "')]").doesNotExist());
    }

    @Test
    void trainer_seesOwnCoursesViaMineRegardlessOfStatus() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(new UserRequest(
                "Tina", "Trainer", trainerEmail, "Secret123!", null, Role.TRAINER));
        String trainerToken = login(trainerEmail, "Secret123!");

        String draftId = createCourse(uniqueCode(), trainerId, CourseStatus.DRAFT);
        String publishedId = createCourse(uniqueCode(), trainerId, CourseStatus.PUBLISHED);

        mockMvc.perform(get("/api/v1/courses/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + draftId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + publishedId + "')]").exists());

        // but the general list still only shows the published one to this trainer
        mockMvc.perform(get("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + publishedId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + draftId + "')]").doesNotExist());
    }

    private String createCourse(String code, String trainerId, CourseStatus status) throws Exception {
        CourseRequest request = new CourseRequest(
                code, "Course " + code, null, 40, 20, "Programming",
                trainerId == null ? null : UUID.fromString(trainerId), BigDecimal.valueOf(500), status);
        String response = mockMvc.perform(post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
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

    private static String uniqueCode() {
        return "CODE-" + UUID.randomUUID();
    }
}
