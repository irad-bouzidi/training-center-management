package com.tcm.grade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentDecisionRequest;
import com.tcm.enrollment.dto.EnrollmentRequest;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.grade.dto.GradeRequest;
import com.tcm.grade.model.AssessmentType;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exercises the grades API through the real filter chain against a real,
 * ephemeral Postgres - who may grade what, the weighted average, and the
 * student's own view, per docs/tasks/TCM-23.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class GradeControllerIT {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@tcm.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "ChangeMe123!";
    private static final String PASSWORD = "Secret123!";

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
    void trainer_gradesTheirOwnCourse_andTheStudentSeesTheWeightedAverage() throws Exception {
        String trainerEmail = uniqueEmail();
        String courseId = createCourse(createUser(trainer(trainerEmail)));
        String studentEmail = uniqueEmail();
        String studentId = approvedStudent(courseId, studentEmail);
        String trainerToken = login(trainerEmail, PASSWORD);

        grade(trainerToken, studentId, courseId, "18.00", "20.00", "40.00").andExpect(status().isCreated());
        grade(trainerToken, studentId, courseId, "50.00", "100.00", "60.00").andExpect(status().isCreated());

        // 90% at weight 40 and 50% at weight 60 -> 66%.
        mockMvc.perform(get("/api/v1/students/" + studentId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(studentEmail, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grades.length()").value(2))
                .andExpect(jsonPath("$.weightedAverage").value(66.0));
    }

    @Test
    void anotherTrainer_cannotGradeACourseTheyDoNotTeach() throws Exception {
        String courseId = createCourse(createUser(trainer()));
        String studentId = approvedStudent(courseId, uniqueEmail());
        String strangerEmail = uniqueEmail();
        createUser(trainer(strangerEmail));

        grade(login(strangerEmail, PASSWORD), studentId, courseId, "10.00", "20.00", "50.00")
                .andExpect(status().isForbidden());
    }

    @Test
    void gradingAStudentWhoIsNotApprovedOnTheCourse_isRejected() throws Exception {
        String trainerEmail = uniqueEmail();
        String courseId = createCourse(createUser(trainer(trainerEmail)));
        String outsiderId = createUser(student(uniqueEmail()));

        grade(login(trainerEmail, PASSWORD), outsiderId, courseId, "10.00", "20.00", "50.00")
                .andExpect(status().isBadRequest());
    }

    @Test
    void aScoreAboveTheMaximum_isRejected() throws Exception {
        String trainerEmail = uniqueEmail();
        String courseId = createCourse(createUser(trainer(trainerEmail)));
        String studentId = approvedStudent(courseId, uniqueEmail());

        grade(login(trainerEmail, PASSWORD), studentId, courseId, "21.00", "20.00", "50.00")
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheGraderOrAnAdmin_mayAmendOrRemoveAGrade() throws Exception {
        String trainerEmail = uniqueEmail();
        String courseId = createCourse(createUser(trainer(trainerEmail)));
        String studentId = approvedStudent(courseId, uniqueEmail());
        String trainerToken = login(trainerEmail, PASSWORD);
        String gradeId = idOf(grade(trainerToken, studentId, courseId, "18.00", "20.00", "40.00")
                .andExpect(status().isCreated()));

        String strangerEmail = uniqueEmail();
        createUser(trainer(strangerEmail));
        mockMvc.perform(put("/api/v1/grades/" + gradeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(strangerEmail, PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeBody(studentId, courseId, "20.00", "20.00", "40.00")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/grades/" + gradeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeBody(studentId, courseId, "20.00", "20.00", "40.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(100.0));

        mockMvc.perform(delete("/api/v1/grades/" + gradeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void student_cannotGrade_norReadSomeoneElsesGrades() throws Exception {
        String courseId = createCourse(createUser(trainer()));
        String otherStudentId = approvedStudent(courseId, uniqueEmail());
        String studentEmail = uniqueEmail();
        createUser(student(studentEmail));
        String studentToken = login(studentEmail, PASSWORD);

        grade(studentToken, otherStudentId, courseId, "10.00", "20.00", "50.00")
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/students/" + otherStudentId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gradebook_listsEveryApprovedStudent_andTheSummaryCarriesTheOverallGrade() throws Exception {
        String trainerEmail = uniqueEmail();
        String courseId = createCourse(createUser(trainer(trainerEmail)));
        String gradedId = approvedStudent(courseId, uniqueEmail());
        approvedStudent(courseId, uniqueEmail());
        String trainerToken = login(trainerEmail, PASSWORD);
        grade(trainerToken, gradedId, courseId, "18.00", "20.00", "40.00").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/courses/" + courseId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(trainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students.length()").value(2))
                .andExpect(jsonPath("$.students[?(@.studentId=='" + gradedId + "')].weightedAverage")
                        .value(org.hamcrest.Matchers.contains(90.0)));

        mockMvc.perform(get("/api/v1/students/" + gradedId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grades.length()").value(1))
                .andExpect(jsonPath("$.overallGrade").value(90.0));
    }

    private ResultActions grade(String token, String studentId, String courseId, String score, String maxScore,
                                 String weight) throws Exception {
        return mockMvc.perform(post("/api/v1/grades")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradeBody(studentId, courseId, score, maxScore, weight)));
    }

    private String gradeBody(String studentId, String courseId, String score, String maxScore, String weight)
            throws Exception {
        return objectMapper.writeValueAsString(new GradeRequest(
                UUID.fromString(studentId), UUID.fromString(courseId), AssessmentType.EXAM, "Midterm",
                new BigDecimal(score), new BigDecimal(maxScore), new BigDecimal(weight), null));
    }

    /** Registers a student on the course and approves them, returning their id. */
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

    private String createCourse(String trainerId) throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, 20, "Programming", UUID.fromString(trainerId),
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
