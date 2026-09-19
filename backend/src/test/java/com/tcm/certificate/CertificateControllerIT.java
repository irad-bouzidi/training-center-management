package com.tcm.certificate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.attendance.dto.AttendanceBulkMarkRequest;
import com.tcm.attendance.dto.AttendanceMarkRequest;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.certificate.dto.CertificateRequest;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentDecisionRequest;
import com.tcm.enrollment.dto.EnrollmentRequest;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
 * Exercises certificate issue and download end to end against a real,
 * ephemeral Postgres - the eligibility rule, the one-per-pair rule, and who
 * may read what, per docs/tasks/TCM-25. The PDF really is written to
 * {@code certificates.storage-path} (a temp dir under the test profile) and
 * read back through the download endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CertificateControllerIT {

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
    void aCompletedStudentWithFullAttendance_isCertified_andCanDownloadThePdf() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse(trainerId);
        String studentEmail = uniqueEmail();
        String studentId = certifiableStudent(courseId, trainerId, studentEmail, AttendanceStatus.PRESENT);

        String certificateId = idOf(generate(adminToken, studentId, courseId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certificateNumber").value(org.hamcrest.Matchers.startsWith("CERT-")))
                .andExpect(jsonPath("$.student.id").value(studentId)));

        byte[] pdf = mockMvc.perform(get("/api/v1/certificates/" + certificateId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(studentEmail, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        org.assertj.core.api.Assertions.assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");
    }

    @Test
    void aSecondCertificateForTheSamePair_isRefused_namingTheFirst() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse(trainerId);
        String studentId = certifiableStudent(courseId, trainerId, uniqueEmail(), AttendanceStatus.PRESENT);

        String number = objectMapper.readTree(generate(adminToken, studentId, courseId)
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString())
                .get("certificateNumber").asText();

        generate(adminToken, studentId, courseId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(number)));
    }

    @Test
    void aStudentWhoseEnrollmentIsNotCompleted_isRefused_withTheReason() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse(trainerId);
        String studentId = approvedStudent(courseId, uniqueEmail());

        generate(adminToken, studentId, courseId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("COMPLETED")));
    }

    @Test
    void aStudentBelowTheAttendanceThreshold_isRefused_withTheReason() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse(trainerId);
        String studentId = certifiableStudent(courseId, trainerId, uniqueEmail(), AttendanceStatus.ABSENT);

        generate(adminToken, studentId, courseId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("below the")));
    }

    @Test
    void aTrainerMayCertifyOnlyTheirOwnCourse() throws Exception {
        String trainerEmail = uniqueEmail();
        String trainerId = createUser(trainer(trainerEmail));
        String courseId = createCourse(trainerId);
        String studentId = certifiableStudent(courseId, trainerId, uniqueEmail(), AttendanceStatus.PRESENT);

        String strangerEmail = uniqueEmail();
        createUser(trainer(strangerEmail));
        generate(login(strangerEmail, PASSWORD), studentId, courseId).andExpect(status().isForbidden());

        generate(login(trainerEmail, PASSWORD), studentId, courseId).andExpect(status().isCreated());
    }

    @Test
    void anotherStudent_cannotDownloadSomeoneElsesCertificate() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse(trainerId);
        String studentId = certifiableStudent(courseId, trainerId, uniqueEmail(), AttendanceStatus.PRESENT);
        String certificateId = idOf(generate(adminToken, studentId, courseId).andExpect(status().isCreated()));

        String otherEmail = uniqueEmail();
        createUser(student(otherEmail));

        mockMvc.perform(get("/api/v1/certificates/" + certificateId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(otherEmail, PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theStudentSummaryAndTheListing_carryTheIssuedCertificate() throws Exception {
        String trainerId = createUser(trainer());
        String courseId = createCourse(trainerId);
        String studentEmail = uniqueEmail();
        String studentId = certifiableStudent(courseId, trainerId, studentEmail, AttendanceStatus.PRESENT);
        generate(adminToken, studentId, courseId).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/certificates")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(studentEmail, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].course.id").value(courseId));

        mockMvc.perform(get("/api/v1/students/" + studentId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificates.length()").value(1))
                .andExpect(jsonPath("$.certificates[0].certificateNumber")
                        .value(org.hamcrest.Matchers.startsWith("CERT-")));
    }

    /**
     * A student who has finished the course: approved, marked present (or
     * absent, to test the other side of the threshold) at its one session,
     * and with the enrollment marked COMPLETED.
     */
    private String certifiableStudent(String courseId, String trainerId, String email, AttendanceStatus status)
            throws Exception {
        String studentId = approvedStudent(courseId, email);
        String sessionId = createSession(courseId, trainerId);

        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceBulkMarkRequest(
                                List.of(new AttendanceMarkRequest(UUID.fromString(studentId), status))))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/enrollments/" + enrollmentIdOf(studentId, courseId) + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        return studentId;
    }

    private String enrollmentIdOf(String studentId, String courseId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/enrollments")
                        .param("studentId", studentId)
                        .param("courseId", courseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("content").get(0).get("id").asText();
    }

    private ResultActions generate(String token, String studentId, String courseId) throws Exception {
        return mockMvc.perform(post("/api/v1/certificates/generate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CertificateRequest(
                        UUID.fromString(studentId), UUID.fromString(courseId)))));
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
                LocalDate.of(2028, 1, 1).plusDays(DATE_SEQUENCE.getAndIncrement()),
                LocalTime.of(9, 0), LocalTime.of(11, 0));
        return idOf(mockMvc.perform(post("/api/v1/sessions")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()));
    }

    private String createCourse(String trainerId) throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, 20, "Programming", UUID.fromString(trainerId),
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
