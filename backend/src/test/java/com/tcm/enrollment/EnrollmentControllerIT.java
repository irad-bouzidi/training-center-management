package com.tcm.enrollment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentDecisionRequest;
import com.tcm.enrollment.dto.EnrollmentRequest;
import com.tcm.enrollment.model.EnrollmentStatus;
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
 * Exercises the enrollment API through the real filter chain (MockMvc, not
 * @WebMvcTest) against a real, ephemeral Postgres, covering registration,
 * the approve/reject decision flow, capacity/duplicate rejection, and
 * cancel ownership - per docs/tasks/TCM-14.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class EnrollmentControllerIT {

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
    void student_canRegisterForPublishedCourse_andSeeItPendingInMine() throws Exception {
        String studentEmail = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", studentEmail, "Secret123!", null, Role.STUDENT));
        String studentToken = login(studentEmail, "Secret123!");
        String courseId = createCourse(20, CourseStatus.PUBLISHED);

        register(studentToken, courseId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/v1/enrollments/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.course.id=='" + courseId + "' && @.status=='PENDING')]").exists());
    }

    @Test
    void adminApproveAndReject_areReflectedInStudentsMineList() throws Exception {
        String approvedCourseId = createCourse(20, CourseStatus.PUBLISHED);
        String rejectedCourseId = createCourse(20, CourseStatus.PUBLISHED);
        String studentEmail = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", studentEmail, "Secret123!", null, Role.STUDENT));
        String studentToken = login(studentEmail, "Secret123!");

        String toApprove = enrollmentId(register(studentToken, approvedCourseId).andExpect(status().isCreated()));
        String toReject = enrollmentId(register(studentToken, rejectedCourseId).andExpect(status().isCreated()));

        decide(toApprove, EnrollmentStatus.APPROVED).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedAt").exists());
        decide(toReject, EnrollmentStatus.REJECTED).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/enrollments/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + toApprove + "' && @.status=='APPROVED')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + toReject + "' && @.status=='REJECTED')]").exists());
    }

    @Test
    void register_duplicateForSameCourse_isRejected() throws Exception {
        String studentEmail = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", studentEmail, "Secret123!", null, Role.STUDENT));
        String studentToken = login(studentEmail, "Secret123!");
        String courseId = createCourse(20, CourseStatus.PUBLISHED);

        register(studentToken, courseId).andExpect(status().isCreated());
        register(studentToken, courseId).andExpect(status().isBadRequest());
    }

    @Test
    void register_courseAtCapacity_isRejected() throws Exception {
        String courseId = createCourse(1, CourseStatus.PUBLISHED);

        String firstEmail = uniqueEmail();
        createUser(new UserRequest("First", "Student", firstEmail, "Secret123!", null, Role.STUDENT));
        String firstToken = login(firstEmail, "Secret123!");
        String firstEnrollmentId = enrollmentId(register(firstToken, courseId).andExpect(status().isCreated()));
        decide(firstEnrollmentId, EnrollmentStatus.APPROVED).andExpect(status().isOk());

        String secondEmail = uniqueEmail();
        createUser(new UserRequest("Second", "Student", secondEmail, "Secret123!", null, Role.STUDENT));
        String secondToken = login(secondEmail, "Secret123!");

        register(secondToken, courseId).andExpect(status().isBadRequest());
    }

    @Test
    void student_canCancelOwnEnrollment_butNotSomeoneElses() throws Exception {
        String courseId = createCourse(20, CourseStatus.PUBLISHED);

        String ownerEmail = uniqueEmail();
        createUser(new UserRequest("Owner", "Student", ownerEmail, "Secret123!", null, Role.STUDENT));
        String ownerToken = login(ownerEmail, "Secret123!");
        String enrollmentId = enrollmentId(register(ownerToken, courseId).andExpect(status().isCreated()));

        String otherEmail = uniqueEmail();
        createUser(new UserRequest("Other", "Student", otherEmail, "Secret123!", null, Role.STUDENT));
        String otherToken = login(otherEmail, "Secret123!");

        mockMvc.perform(post("/api/v1/enrollments/" + enrollmentId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/enrollments/" + enrollmentId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String studentToken, String courseId) throws Exception {
        return mockMvc.perform(post("/api/v1/enrollments")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrollmentRequest(UUID.fromString(courseId), null))));
    }

    private org.springframework.test.web.servlet.ResultActions decide(String enrollmentId, EnrollmentStatus status) throws Exception {
        return mockMvc.perform(post("/api/v1/enrollments/" + enrollmentId + "/decision")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrollmentDecisionRequest(status))));
    }

    private String enrollmentId(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createCourse(int capacity, CourseStatus status) throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, capacity, "Programming", null, BigDecimal.valueOf(500), status);
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
