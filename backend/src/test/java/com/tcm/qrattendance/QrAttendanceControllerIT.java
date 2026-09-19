package com.tcm.qrattendance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentDecisionRequest;
import com.tcm.enrollment.dto.EnrollmentRequest;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.qrattendance.dto.QrCheckInRequest;
import com.tcm.schedule.ClassSessionRepository;
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exercises QR attendance end to end against a real, ephemeral Postgres, per
 * docs/tasks/TCM-27: issuing a code, checking in with it, and every way that
 * can fail.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class QrAttendanceControllerIT {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@tcm.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "ChangeMe123!";
    private static final String PASSWORD = "Secret123!";

    private static final AtomicInteger DATE_SEQUENCE = new AtomicInteger();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    private String adminToken;

    @BeforeEach
    void loginAsBootstrapAdmin() throws Exception {
        adminToken = login(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
    }

    @Test
    void trainerIssuesACode_andAnApprovedStudentScansItselfPresent() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(trainer(trainerEmail));
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String studentEmail = uniqueEmail();
        String studentId = approvedStudent(courseId, studentEmail);

        String response = issue(sessionId, login(trainerEmail, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.checkInUrl").value(org.hamcrest.Matchers.containsString("/attend/" + sessionId)))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // The image really is a PNG: the magic number is the first 8 bytes.
        byte[] png = Base64.getDecoder().decode(objectMapper.readTree(response).get("imageBase64").asText());
        org.assertj.core.api.Assertions.assertThat(png[0] & 0xFF).isEqualTo(0x89);
        org.assertj.core.api.Assertions.assertThat(new String(png, 1, 3, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("PNG");

        String studentToken = login(studentEmail, PASSWORD);
        checkIn(sessionId, token, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.method").value("QR"))
                .andExpect(jsonPath("$.student.id").value(studentId));

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].status").value("PRESENT"))
                .andExpect(jsonPath("$.entries[0].method").value("QR"));
    }

    @Test
    void scanningTwice_isTheSameAsScanningOnce() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String studentEmail = uniqueEmail();
        approvedStudent(courseId, studentEmail);
        String token = tokenFor(sessionId);
        String studentToken = login(studentEmail, PASSWORD);

        String first = checkIn(sessionId, token, studentToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        checkIn(sessionId, token, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(objectMapper.readTree(first).get("id").asText()));

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.entries.length()").value(1));
    }

    @Test
    void regeneratingACode_stopsThePreviousOneWorking() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String studentEmail = uniqueEmail();
        approvedStudent(courseId, studentEmail);

        String stale = tokenFor(sessionId);
        String fresh = tokenFor(sessionId);
        String studentToken = login(studentEmail, PASSWORD);

        checkIn(sessionId, stale, studentToken)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("replaced")));
        checkIn(sessionId, fresh, studentToken).andExpect(status().isOk());
    }

    @Test
    void anExpiredCode_isGone() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String studentEmail = uniqueEmail();
        approvedStudent(courseId, studentEmail);
        String token = tokenFor(sessionId);

        // Wind the expiry back rather than waiting out the window.
        var session = classSessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        session.setQrExpiresAt(Instant.now().minusSeconds(1));
        classSessionRepository.save(session);

        checkIn(sessionId, token, login(studentEmail, PASSWORD))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    void aForgedCode_isRejected() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String studentEmail = uniqueEmail();
        approvedStudent(courseId, studentEmail);
        tokenFor(sessionId);

        checkIn(sessionId, "made-up.signature", login(studentEmail, PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not valid")));
    }

    @Test
    void aCodeFromAnotherSession_doesNotWorkHere() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String otherSessionId = createSession(courseId, trainerId);
        String studentEmail = uniqueEmail();
        approvedStudent(courseId, studentEmail);

        String otherToken = tokenFor(otherSessionId);
        tokenFor(sessionId);

        checkIn(sessionId, otherToken, login(studentEmail, PASSWORD)).andExpect(status().isBadRequest());
    }

    @Test
    void aStudentWhoIsNotEnrolled_isRefused() throws Exception {
        String trainerId = createUser(trainer());
        String sessionId = createSession(createCourse(), trainerId);
        String token = tokenFor(sessionId);
        String outsiderEmail = uniqueEmail();
        createUser(student(outsiderEmail));

        checkIn(sessionId, token, login(outsiderEmail, PASSWORD)).andExpect(status().isForbidden());
    }

    @Test
    void aTrainerWhoIsNotAssigned_cannotIssueACode() throws Exception {
        String sessionId = createSession(createCourse(), createUser(trainer()));
        String strangerEmail = uniqueEmail();
        createUser(trainer(strangerEmail));

        issue(sessionId, login(strangerEmail, PASSWORD)).andExpect(status().isForbidden());
    }

    @Test
    void aStudent_cannotIssueACode() throws Exception {
        String sessionId = createSession(createCourse(), createUser(trainer()));
        String studentEmail = uniqueEmail();
        createUser(student(studentEmail));

        issue(sessionId, login(studentEmail, PASSWORD)).andExpect(status().isForbidden());
    }

    private String tokenFor(String sessionId) throws Exception {
        String response = issue(sessionId, adminToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private ResultActions issue(String sessionId, String token) throws Exception {
        return mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/qr")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions checkIn(String sessionId, String token, String studentToken) throws Exception {
        return mockMvc.perform(post("/api/v1/attendance/qr-checkin")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new QrCheckInRequest(UUID.fromString(sessionId), token))));
    }

    private String approvedStudent(String courseId, String email) throws Exception {
        String studentId = createUser(student(email));
        String studentToken = login(email, PASSWORD);

        String enrollment = mockMvc.perform(post("/api/v1/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnrollmentRequest(UUID.fromString(courseId), null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/enrollments/" + objectMapper.readTree(enrollment).get("id").asText()
                        + "/decision")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnrollmentDecisionRequest(EnrollmentStatus.APPROVED))))
                .andExpect(status().isOk());
        return studentId;
    }

    private String createSession(String courseId, String trainerId) throws Exception {
        ClassSessionRequest request = new ClassSessionRequest(
                UUID.fromString(courseId), UUID.fromString(trainerId), "Room A",
                LocalDate.of(2029, 1, 1).plusDays(DATE_SEQUENCE.getAndIncrement()),
                LocalTime.of(9, 0), LocalTime.of(11, 0));
        return idOf(mockMvc.perform(post("/api/v1/sessions")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()));
    }

    private String createCourse() throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, 20, "Programming", null,
                BigDecimal.valueOf(500), CourseStatus.PUBLISHED);
        return idOf(mockMvc.perform(post("/api/v1/courses")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()));
    }

    private String createUser(UserRequest request) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()));
    }

    private String idOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.tcm.auth.dto.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private static UserRequest trainer() {
        return trainer(uniqueEmail());
    }

    private static UserRequest trainer(String email) {
        return new UserRequest("Tina", "Trainer", email, PASSWORD, null, Role.TRAINER);
    }

    private static UserRequest student(String email) {
        return new UserRequest("Sam", "Student", email, PASSWORD, null, Role.STUDENT);
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
