package com.tcm.payment;

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
import com.tcm.payment.dto.PaymentRequest;
import com.tcm.payment.dto.PaymentTransactionRequest;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.model.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Exercises the payment API through the real filter chain against a real,
 * ephemeral Postgres - the invoice raised by approving an enrollment, the
 * PENDING -> PARTIAL -> PAID walk, the overdue sweep and who may see what,
 * per docs/tasks/TCM-21.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PaymentControllerIT {

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
    void approvingAnEnrollment_raisesAnInvoiceForTheCoursePrice() throws Exception {
        String courseId = createCourse(new BigDecimal("500.00"));
        String studentEmail = uniqueEmail();
        String studentId = createUser(student(studentEmail));
        approveEnrollment(studentId, courseId, login(studentEmail, PASSWORD));

        mockMvc.perform(get("/api/v1/payments")
                        .param("studentId", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].course.id").value(courseId))
                .andExpect(jsonPath("$.content[0].amountDue").value(500.00))
                .andExpect(jsonPath("$.content[0].amountPaid").value(0))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void approvingAnEnrollmentOnAFreeCourse_raisesNoInvoice() throws Exception {
        String courseId = createCourse(BigDecimal.ZERO);
        String studentEmail = uniqueEmail();
        String studentId = createUser(student(studentEmail));
        approveEnrollment(studentId, courseId, login(studentEmail, PASSWORD));

        mockMvc.perform(get("/api/v1/payments")
                        .param("studentId", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void anInvoiceWalksFromPendingToPartialToPaid() throws Exception {
        String paymentId = createInvoice(createUser(student(uniqueEmail())), createCourse(new BigDecimal("500.00")),
                new BigDecimal("500.00"), LocalDate.now().plusDays(30));

        pay(paymentId, new BigDecimal("200.00"), "CASH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.outstanding").value(300.00))
                .andExpect(jsonPath("$.paidAt").isEmpty());

        pay(paymentId, new BigDecimal("300.00"), "CARD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.outstanding").value(0))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }

    @Test
    void payingMoreThanIsOwed_isRejected() throws Exception {
        String paymentId = createInvoice(createUser(student(uniqueEmail())), createCourse(new BigDecimal("500.00")),
                new BigDecimal("500.00"), LocalDate.now().plusDays(30));

        pay(paymentId, new BigDecimal("500.01"), null).andExpect(status().isBadRequest());
        pay(paymentId, new BigDecimal("500.00"), null).andExpect(status().isOk());
    }

    @Test
    void aPastDueInvoice_isSweptToOverdue_andASettledOneIsNot() throws Exception {
        String courseId = createCourse(new BigDecimal("500.00"));
        String lateId = createInvoice(createUser(student(uniqueEmail())), courseId,
                new BigDecimal("500.00"), LocalDate.now().minusDays(1));
        String settledId = createInvoice(createUser(student(uniqueEmail())), courseId,
                new BigDecimal("500.00"), LocalDate.now().minusDays(1));
        pay(settledId, new BigDecimal("500.00"), "CASH").andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/overdue-sweep")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/payments")
                        .param("status", "OVERDUE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + lateId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + settledId + "')]").doesNotExist());
    }

    @Test
    void student_seesTheirOwnInvoicesAndBalance_butNotTheAdminListing() throws Exception {
        String courseId = createCourse(new BigDecimal("500.00"));
        String studentEmail = uniqueEmail();
        String studentId = createUser(student(studentEmail));
        String studentToken = login(studentEmail, PASSWORD);
        approveEnrollment(studentId, courseId, studentToken);

        String otherStudentId = createUser(student(uniqueEmail()));
        createInvoice(otherStudentId, courseId, new BigDecimal("500.00"), LocalDate.now().plusDays(30));

        mockMvc.perform(get("/api/v1/payments/mine").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].student.id").value(studentId));

        mockMvc.perform(get("/api/v1/payments").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/students/" + studentId + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentBalance").value(500.00));
    }

    @Test
    void student_cannotRaiseAnInvoiceOrRecordAPayment() throws Exception {
        String studentEmail = uniqueEmail();
        String studentId = createUser(student(studentEmail));
        String courseId = createCourse(new BigDecimal("500.00"));
        String paymentId = createInvoice(studentId, courseId, new BigDecimal("500.00"), LocalDate.now().plusDays(30));
        String studentToken = login(studentEmail, PASSWORD);

        mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest(
                                UUID.fromString(studentId), UUID.fromString(courseId),
                                BigDecimal.ONE, LocalDate.now()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PaymentTransactionRequest(BigDecimal.ONE, "CASH", null))))
                .andExpect(status().isForbidden());
    }

    private ResultActions pay(String paymentId, BigDecimal amount, String method) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/" + paymentId + "/transactions")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PaymentTransactionRequest(amount, method, null))));
    }

    private String createInvoice(String studentId, String courseId, BigDecimal amountDue, LocalDate dueDate)
            throws Exception {
        String response = mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest(
                                UUID.fromString(studentId), UUID.fromString(courseId), amountDue, dueDate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void approveEnrollment(String studentId, String courseId, String studentToken) throws Exception {
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
    }

    private String createCourse(BigDecimal price) throws Exception {
        CourseRequest request = new CourseRequest(
                uniqueCode(), "Course", null, 40, 20, "Programming", null, price, CourseStatus.PUBLISHED);
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
