package com.tcm.schedule;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.schedule.dto.ClassSessionStatusRequest;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exercises the scheduling API through the real filter chain (MockMvc, not
 * @WebMvcTest) against a real, ephemeral Postgres, covering overlap rejection
 * against the actual query and the role-scoped visibility rules - per
 * docs/tasks/TCM-17.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ClassSessionControllerIT {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@tcm.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "ChangeMe123!";
    private static final String PASSWORD = "Secret123!";

    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime ELEVEN = LocalTime.of(11, 0);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final LocalTime NOON = LocalTime.of(12, 0);

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
    void admin_canScheduleSession_forCourseWithTrainerAndClassroom() throws Exception {
        String courseId = createCourse();
        String trainerId = createUser(trainer());

        createSession(courseId, trainerId, "Room A", uniqueDate(), NINE, ELEVEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.classroom").value("Room A"))
                .andExpect(jsonPath("$.trainer.id").value(trainerId))
                .andExpect(jsonPath("$.course.id").value(courseId));
    }

    @Test
    void scheduling_aTrainerWhoIsAlreadyBooked_conflicts() throws Exception {
        String trainerId = createUser(trainer());
        LocalDate date = uniqueDate();

        createSession(createCourse(), trainerId, "Room A", date, NINE, ELEVEN).andExpect(status().isCreated());

        // Same trainer, different room, overlapping 10:00-12:00.
        createSession(createCourse(), trainerId, "Room B", date, TEN, NOON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("trainer")));
    }

    @Test
    void scheduling_aClassroomThatIsAlreadyBooked_conflicts() throws Exception {
        LocalDate date = uniqueDate();

        createSession(createCourse(), createUser(trainer()), "Room A", date, NINE, ELEVEN)
                .andExpect(status().isCreated());

        // Different trainer, same room, overlapping 10:00-12:00.
        createSession(createCourse(), createUser(trainer()), "Room A", date, TEN, NOON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("classroom")));
    }

    /** Back-to-back sessions touch at 11:00 but don't overlap. */
    @Test
    void scheduling_backToBackInTheSameRoom_isAllowed() throws Exception {
        String trainerId = createUser(trainer());
        LocalDate date = uniqueDate();

        createSession(createCourse(), trainerId, "Room A", date, NINE, ELEVEN).andExpect(status().isCreated());
        createSession(createCourse(), trainerId, "Room A", date, ELEVEN, NOON).andExpect(status().isCreated());
    }

    @Test
    void scheduling_overACancelledSession_isAllowed() throws Exception {
        String trainerId = createUser(trainer());
        LocalDate date = uniqueDate();

        String sessionId = sessionId(createSession(createCourse(), trainerId, "Room A", date, NINE, ELEVEN)
                .andExpect(status().isCreated()));
        changeStatus(sessionId, SessionStatus.CANCELLED, adminToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        createSession(createCourse(), trainerId, "Room A", date, NINE, ELEVEN).andExpect(status().isCreated());
    }

    @Test
    void trainer_seesOnlyTheirOwnSessions() throws Exception {
        String mineEmail = uniqueEmail();
        String myTrainerId = createUser(trainer(mineEmail));
        String otherTrainerId = createUser(trainer());
        LocalDate date = uniqueDate();

        String mySessionId = sessionId(createSession(createCourse(), myTrainerId, "Room A", date, NINE, ELEVEN)
                .andExpect(status().isCreated()));
        String otherSessionId = sessionId(createSession(createCourse(), otherTrainerId, "Room B", date, NINE, ELEVEN)
                .andExpect(status().isCreated()));

        String trainerToken = login(mineEmail, PASSWORD);
        mockMvc.perform(get("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + mySessionId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + otherSessionId + "')]").doesNotExist());
    }

    /** A trainerId query param doesn't widen a trainer's own view. */
    @Test
    void trainer_cannotWidenTheirViewWithATrainerIdFilter() throws Exception {
        String mineEmail = uniqueEmail();
        createUser(trainer(mineEmail));
        String otherTrainerId = createUser(trainer());

        String otherSessionId = sessionId(
                createSession(createCourse(), otherTrainerId, "Room B", uniqueDate(), NINE, ELEVEN)
                        .andExpect(status().isCreated()));

        String trainerToken = login(mineEmail, PASSWORD);
        mockMvc.perform(get("/api/v1/sessions")
                        .param("trainerId", otherTrainerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + otherSessionId + "')]").doesNotExist());
    }

    @Test
    void student_seesSessionsOnlyForCoursesTheyAreApprovedIn() throws Exception {
        String trainerId = createUser(trainer());
        String approvedCourseId = createCourse();
        String pendingCourseId = createCourse();
        String unrelatedCourseId = createCourse();
        LocalDate date = uniqueDate();

        String approvedSessionId = sessionId(
                createSession(approvedCourseId, trainerId, "Room A", date, NINE, ELEVEN)
                        .andExpect(status().isCreated()));
        String pendingSessionId = sessionId(
                createSession(pendingCourseId, trainerId, "Room B", date, NINE, ELEVEN)
                        .andExpect(status().isCreated()));
        String unrelatedSessionId = sessionId(
                createSession(unrelatedCourseId, trainerId, "Room C", date, NINE, ELEVEN)
                        .andExpect(status().isCreated()));

        String studentEmail = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", studentEmail, PASSWORD, null, Role.STUDENT));
        String studentToken = login(studentEmail, PASSWORD);

        decide(enroll(studentToken, approvedCourseId), EnrollmentStatus.APPROVED).andExpect(status().isOk());
        enroll(studentToken, pendingCourseId);

        mockMvc.perform(get("/api/v1/sessions").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + approvedSessionId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + pendingSessionId + "')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id=='" + unrelatedSessionId + "')]").doesNotExist());
    }

    @Test
    void search_filtersByDateRange() throws Exception {
        String trainerId = createUser(trainer());
        LocalDate inRange = uniqueDate();
        LocalDate outOfRange = inRange.plusDays(30);

        String inRangeId = sessionId(createSession(createCourse(), trainerId, "Room A", inRange, NINE, ELEVEN)
                .andExpect(status().isCreated()));
        String outOfRangeId = sessionId(createSession(createCourse(), trainerId, "Room A", outOfRange, NINE, ELEVEN)
                .andExpect(status().isCreated()));

        mockMvc.perform(get("/api/v1/sessions")
                        .param("from", inRange.toString())
                        .param("to", inRange.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + inRangeId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + outOfRangeId + "')]").doesNotExist());
    }

    @Test
    void assignedTrainer_canCompleteOwnSession_butNotSomeoneElses() throws Exception {
        String mineEmail = uniqueEmail();
        String myTrainerId = createUser(trainer(mineEmail));
        String otherEmail = uniqueEmail();
        createUser(trainer(otherEmail));
        LocalDate date = uniqueDate();

        String sessionId = sessionId(createSession(createCourse(), myTrainerId, "Room A", date, NINE, ELEVEN)
                .andExpect(status().isCreated()));

        changeStatus(sessionId, SessionStatus.COMPLETED, login(otherEmail, PASSWORD))
                .andExpect(status().isForbidden());
        changeStatus(sessionId, SessionStatus.COMPLETED, login(mineEmail, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void trainer_cannotCancelASession() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(trainer(trainerEmail));

        String sessionId = sessionId(createSession(createCourse(), trainerId, "Room A", uniqueDate(), NINE, ELEVEN)
                .andExpect(status().isCreated()));

        changeStatus(sessionId, SessionStatus.CANCELLED, login(trainerEmail, PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotScheduleASession() throws Exception {
        String studentEmail = uniqueEmail();
        createUser(new UserRequest("Sam", "Student", studentEmail, PASSWORD, null, Role.STUDENT));
        String courseId = createCourse();
        String trainerId = createUser(trainer());

        mockMvc.perform(post("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(studentEmail, PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClassSessionRequest(
                                UUID.fromString(courseId), UUID.fromString(trainerId), "Room A",
                                uniqueDate(), NINE, ELEVEN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withEndTimeBeforeStartTime_isRejected() throws Exception {
        createSession(createCourse(), createUser(trainer()), "Room A", uniqueDate(), ELEVEN, NINE)
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withANonTrainerAsTrainer_isRejected() throws Exception {
        String studentEmail = uniqueEmail();
        String studentId = createUser(new UserRequest("Sam", "Student", studentEmail, PASSWORD, null, Role.STUDENT));

        createSession(createCourse(), studentId, "Room A", uniqueDate(), NINE, ELEVEN)
                .andExpect(status().isBadRequest());
    }

    private ResultActions createSession(String courseId, String trainerId, String classroom, LocalDate date,
                                          LocalTime start, LocalTime end) throws Exception {
        ClassSessionRequest request = new ClassSessionRequest(
                UUID.fromString(courseId), UUID.fromString(trainerId), classroom, date, start, end);
        return mockMvc.perform(post("/api/v1/sessions")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions changeStatus(String sessionId, SessionStatus status, String token) throws Exception {
        return mockMvc.perform(patch("/api/v1/sessions/" + sessionId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClassSessionStatusRequest(status))));
    }

    private String enroll(String studentToken, String courseId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EnrollmentRequest(UUID.fromString(courseId), null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private ResultActions decide(String enrollmentId, EnrollmentStatus status) throws Exception {
        return mockMvc.perform(post("/api/v1/enrollments/" + enrollmentId + "/decision")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrollmentDecisionRequest(status))));
    }

    private String sessionId(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
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
        String body = objectMapper.writeValueAsString(new com.tcm.auth.dto.LoginRequest(email, password));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private static UserRequest trainer() {
        return trainer(uniqueEmail());
    }

    private static UserRequest trainer(String email) {
        return new UserRequest("Tina", "Trainer", email, PASSWORD, null, Role.TRAINER);
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

    /** Every test books its own day, so one test's sessions can never be the
     * thing that makes another's overlap check fire. */
    private static LocalDate uniqueDate() {
        return LocalDate.of(2026, 1, 1).plusDays(DATE_SEQUENCE.getAndIncrement());
    }

    private static final java.util.concurrent.atomic.AtomicInteger DATE_SEQUENCE =
            new java.util.concurrent.atomic.AtomicInteger();
}
