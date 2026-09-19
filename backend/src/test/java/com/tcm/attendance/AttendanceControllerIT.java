package com.tcm.attendance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.attendance.dto.AttendanceBulkMarkRequest;
import com.tcm.attendance.dto.AttendanceMarkRequest;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentDecisionRequest;
import com.tcm.enrollment.dto.EnrollmentRequest;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
 * Exercises the attendance API through the real filter chain against a real,
 * ephemeral Postgres - covering the ownership rules, the upsert semantics of
 * re-marking, and report aggregation, per docs/tasks/TCM-19.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AttendanceControllerIT {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@tcm.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "ChangeMe123!";
    private static final String PASSWORD = "Secret123!";

    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime ELEVEN = LocalTime.of(11, 0);

    private static final AtomicInteger DATE_SEQUENCE = new AtomicInteger();

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
    void assignedTrainer_readsRoster_thenBulkMarksIt() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(trainer(trainerEmail));
        String courseId = createCourse();
        String sessionId = createSession(courseId, trainerId);
        String studentId = approvedStudent(courseId);
        String trainerToken = login(trainerEmail, PASSWORD);

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value(sessionId))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].studentId").value(studentId))
                .andExpect(jsonPath("$.entries[0].status").isEmpty());

        mark(sessionId, trainerToken, new AttendanceMarkRequest(UUID.fromString(studentId), AttendanceStatus.PRESENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PRESENT"))
                .andExpect(jsonPath("$[0].method").value("MANUAL"));

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].status").value("PRESENT"))
                .andExpect(jsonPath("$.entries[0].markedAt").isNotEmpty());
    }

    @Test
    void reMarkingAStudent_updatesTheirRecord_ratherThanAddingASecond() throws Exception {
        String trainerEmail = uniqueEmail();
        String sessionId = createSession(createCourse(), createUser(trainer(trainerEmail)));
        String courseId = courseOf(sessionId);
        String studentId = approvedStudent(courseId);
        String trainerToken = login(trainerEmail, PASSWORD);

        String firstId = mark(sessionId, trainerToken,
                new AttendanceMarkRequest(UUID.fromString(studentId), AttendanceStatus.ABSENT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(firstId).get(0).get("id").asText();

        mark(sessionId, trainerToken, new AttendanceMarkRequest(UUID.fromString(studentId), AttendanceStatus.LATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(recordId))
                .andExpect(jsonPath("$[0].status").value("LATE"));

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].status").value("LATE"));
    }

    @Test
    void anotherTrainer_canNeitherReadNorMarkSomeoneElsesSession() throws Exception {
        String sessionId = createSession(createCourse(), createUser(trainer()));
        String strangerEmail = uniqueEmail();
        createUser(trainer(strangerEmail));
        String strangerToken = login(strangerEmail, PASSWORD);

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(strangerToken)))
                .andExpect(status().isForbidden());
        mark(sessionId, strangerToken,
                new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotReadOrMarkARoster() throws Exception {
        String sessionId = createSession(createCourse(), createUser(trainer()));
        String studentEmail = uniqueEmail();
        createUser(student(studentEmail));
        String studentToken = login(studentEmail, PASSWORD);

        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
        mark(sessionId, studentToken,
                new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void markingAStudentWhoIsNotApprovedOnTheCourse_isRejected() throws Exception {
        String sessionId = createSession(createCourse(), createUser(trainer()));
        String outsiderId = createUser(student(uniqueEmail()));

        mark(sessionId, adminToken, new AttendanceMarkRequest(UUID.fromString(outsiderId), AttendanceStatus.PRESENT))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_pullsCourseAttendanceReport_andStudentSummaryShowsTheSameRate() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String studentId = approvedStudent(courseId);

        // Three sittings of the same course: present, late, absent -> 66.7%.
        markAs(createSession(courseId, trainerId), studentId, AttendanceStatus.PRESENT);
        markAs(createSession(courseId, trainerId), studentId, AttendanceStatus.LATE);
        markAs(createSession(courseId, trainerId), studentId, AttendanceStatus.ABSENT);

        mockMvc.perform(get("/api/v1/courses/" + courseId + "/attendance-report")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCount").value(3))
                .andExpect(jsonPath("$.students.length()").value(1))
                .andExpect(jsonPath("$.students[0].present").value(1))
                .andExpect(jsonPath("$.students[0].late").value(1))
                .andExpect(jsonPath("$.students[0].absent").value(1))
                .andExpect(jsonPath("$.students[0].attendanceRate").value(66.7));

        mockMvc.perform(get("/api/v1/students/" + studentId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceRate").value(66.7));
    }

    @Test
    void trainerOfAnotherCourse_cannotPullTheReport() throws Exception {
        String courseId = createCourse();
        createSession(courseId, createUser(trainer()));
        String strangerEmail = uniqueEmail();
        createUser(trainer(strangerEmail));

        mockMvc.perform(get("/api/v1/courses/" + courseId + "/attendance-report")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(strangerEmail, PASSWORD))))
                .andExpect(status().isForbidden());
    }

    private void markAs(String sessionId, String studentId, AttendanceStatus status) throws Exception {
        mark(sessionId, adminToken, new AttendanceMarkRequest(UUID.fromString(studentId), status))
                .andExpect(status().isOk());
    }

    private ResultActions mark(String sessionId, String token, AttendanceMarkRequest... entries) throws Exception {
        return mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/attendance")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AttendanceBulkMarkRequest(List.of(entries)))));
    }

    /** Registers a student on the course and approves them, returning their id. */
    private String approvedStudent(String courseId) throws Exception {
        String email = uniqueEmail();
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
                UUID.fromString(courseId), UUID.fromString(trainerId), "Room A", uniqueDate(), NINE, ELEVEN);
        String response = mockMvc.perform(post("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String courseOf(String sessionId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode session : objectMapper.readTree(response).get("content")) {
            if (session.get("id").asText().equals(sessionId)) {
                return session.get("course").get("id").asText();
            }
        }
        throw new AssertionError("session " + sessionId + " not found");
    }

    private String createCourse() throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, 20, "Programming", null,
                BigDecimal.valueOf(500), CourseStatus.PUBLISHED);
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

    /** Every session books its own day, so the scheduler's overlap check can
     * never be what fails one of these tests. */
    private static LocalDate uniqueDate() {
        return LocalDate.of(2027, 1, 1).plusDays(DATE_SEQUENCE.getAndIncrement());
    }
}
