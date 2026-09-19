package com.tcm.dashboard;

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
 * Checks the dashboard's arithmetic against data this test puts there
 * itself, per docs/tasks/TCM-29 step 4. Counts are asserted as *deltas*
 * across a known change rather than as absolutes: the summary is
 * platform-wide, so every other test in the class contributes to it and an
 * absolute number would only be true in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DashboardControllerIT {

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

    private String adminToken;

    @BeforeEach
    void loginAsBootstrapAdmin() throws Exception {
        adminToken = login(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
    }

    @Test
    void adminSummary_countsWhatWasJustAdded() throws Exception {
        JsonNode before = adminSummary();

        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String studentEmail = uniqueEmail();
        String studentId = createUser(student(studentEmail));
        // One PENDING enrollment, and one approved on a 500.00 course, which
        // raises its invoice (TCM-21).
        String pendingEnrollment = enroll(login(studentEmail, PASSWORD), courseId);
        String otherCourseId = createCourse();
        approve(enroll(login(studentEmail, PASSWORD), otherCourseId));
        createSession(otherCourseId, trainerId, LocalDate.now().plusDays(2));

        JsonNode after = adminSummary();

        assertDelta(before, after, "activeStudents", 1);
        assertDelta(before, after, "activeTrainers", 1);
        assertDelta(before, after, "publishedCourses", 2);
        assertDelta(before, after, "pendingEnrollments", 1);
        assertDelta(before, after, "upcomingSessions", 1);
        org.assertj.core.api.Assertions
                .assertThat(after.get("outstandingBalance").decimalValue()
                        .subtract(before.get("outstandingBalance").decimalValue()))
                .isEqualByComparingTo("500.00");

        // Approving the pending one moves it out of the pending count.
        approve(pendingEnrollment);
        assertDelta(before, adminSummary(), "pendingEnrollments", 0);
        org.assertj.core.api.Assertions.assertThat(studentId).isNotBlank();
    }

    @Test
    void adminSummary_averageAttendanceRateFollowsTheMarks() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse();
        String studentEmail = uniqueEmail();
        String studentId = createUser(student(studentEmail));
        approve(enroll(login(studentEmail, PASSWORD), courseId));

        String present = createSession(courseId, trainerId, LocalDate.now().plusDays(3));
        mark(present, studentId, AttendanceStatus.PRESENT);

        Double rate = adminSummary().get("averageAttendanceRate").asDouble();
        org.assertj.core.api.Assertions.assertThat(rate).isBetween(0.0, 100.0);

        // An absence can only pull the platform-wide rate down.
        String absent = createSession(courseId, trainerId, LocalDate.now().plusDays(4));
        mark(absent, studentId, AttendanceStatus.ABSENT);

        org.assertj.core.api.Assertions
                .assertThat(adminSummary().get("averageAttendanceRate").asDouble())
                .isLessThan(rate);
    }

    @Test
    void trainerSummary_isScopedToTheirOwnWork() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(trainer(trainerEmail));
        String courseId = createCourseFor(trainerId);
        String studentEmail = uniqueEmail();
        createUser(student(studentEmail));
        approve(enroll(login(studentEmail, PASSWORD), courseId));
        createSession(courseId, trainerId, LocalDate.now().plusDays(1));

        // Another trainer's course and session must not show up here.
        String otherTrainerId = createUser(trainer());
        createSession(createCourseFor(otherTrainerId), otherTrainerId, LocalDate.now().plusDays(1));

        mockMvc.perform(get("/api/v1/dashboard/trainer-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(trainerEmail, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myCourses").value(1))
                .andExpect(jsonPath("$.upcomingSessions").value(1))
                .andExpect(jsonPath("$.sessionsAwaitingAttendance").value(0))
                .andExpect(jsonPath("$.studentsAwaitingGrades").value(1));
    }

    @Test
    void trainerSummary_countsADeliveredSessionNobodyMarked() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(trainer(trainerEmail));
        String courseId = createCourseFor(trainerId);
        String sessionId = createSession(courseId, trainerId, LocalDate.now().plusDays(5));

        String trainerToken = login(trainerEmail, PASSWORD);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/sessions/" + sessionId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/trainer-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsAwaitingAttendance").value(1))
                .andExpect(jsonPath("$.upcomingSessions").value(0));
    }

    @Test
    void eachDashboardIsForItsOwnRoleOnly() throws Exception {
        String trainerEmail = uniqueEmail();
        createUser(trainer(trainerEmail));
        String studentEmail = uniqueEmail();
        createUser(student(studentEmail));

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(trainerEmail, PASSWORD))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(studentEmail, PASSWORD))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/trainer-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden());
    }

    private static void assertDelta(JsonNode before, JsonNode after, String field, long expected) {
        org.assertj.core.api.Assertions
                .assertThat(after.get(field).asLong() - before.get(field).asLong())
                .as(field)
                .isEqualTo(expected);
    }

    private JsonNode adminSummary() throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private void mark(String sessionId, String studentId, AttendanceStatus status) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceBulkMarkRequest(
                                List.of(new AttendanceMarkRequest(UUID.fromString(studentId), status))))))
                .andExpect(status().isOk());
    }

    private String enroll(String studentToken, String courseId) throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/enrollments")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrollmentRequest(UUID.fromString(courseId), null))))
                .andExpect(status().isCreated()));
    }

    private void approve(String enrollmentId) throws Exception {
        mockMvc.perform(post("/api/v1/enrollments/" + enrollmentId + "/decision")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnrollmentDecisionRequest(EnrollmentStatus.APPROVED))))
                .andExpect(status().isOk());
    }

    private String createSession(String courseId, String trainerId, LocalDate date) throws Exception {
        // Each session gets its own hour of the day, so same-day sessions in
        // one test never trip the double-booking rule.
        int hour = 8 + DATE_SEQUENCE.getAndIncrement() % 10;
        ClassSessionRequest request = new ClassSessionRequest(
                UUID.fromString(courseId), UUID.fromString(trainerId), "Room " + hour, date,
                LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0));
        return idOf(mockMvc.perform(post("/api/v1/sessions")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()));
    }

    private String createCourse() throws Exception {
        return createCourseFor(null);
    }

    private String createCourseFor(String trainerId) throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, 20, "Programming",
                trainerId == null ? null : UUID.fromString(trainerId),
                new BigDecimal("500.00"), CourseStatus.PUBLISHED);
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
