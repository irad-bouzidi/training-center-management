package com.tcm.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.payment.dto.PaymentResponse;
import com.tcm.payment.dto.PaymentTransactionRequest;
import com.tcm.payment.mapper.PaymentMapper;
import com.tcm.payment.model.Payment;
import com.tcm.payment.model.PaymentStatus;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test with mocked repositories (per TCM-21). The real
 * {@link PaymentMapper} is used as-is since it's pure mapping logic.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository, userRepository, courseRepository, new PaymentMapper());
    }

    @Test
    void recordPayment_forPartOfTheFee_movesTheInvoiceToPartial() {
        Payment invoice = givenInvoice(new BigDecimal("500.00"), BigDecimal.ZERO, PaymentStatus.PENDING);

        PaymentResponse response = paymentService.recordPayment(
                PAYMENT_ID, new PaymentTransactionRequest(new BigDecimal("200.00"), "CASH", "deposit"));

        assertThat(response.status()).isEqualTo(PaymentStatus.PARTIAL);
        assertThat(response.amountPaid()).isEqualByComparingTo("200.00");
        assertThat(response.outstanding()).isEqualByComparingTo("300.00");
        assertThat(response.paidAt()).isNull();
        assertThat(invoice.getPaymentMethod()).isEqualTo("CASH");
    }

    @Test
    void recordPayment_thatSettlesTheInvoice_movesItToPaid_andStampsPaidAt() {
        givenInvoice(new BigDecimal("500.00"), new BigDecimal("200.00"), PaymentStatus.PARTIAL);

        PaymentResponse response = paymentService.recordPayment(
                PAYMENT_ID, new PaymentTransactionRequest(new BigDecimal("300.00"), "CARD", null));

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.outstanding()).isEqualByComparingTo("0.00");
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    void recordPayment_beyondWhatIsOwed_isRejected() {
        givenInvoice(new BigDecimal("500.00"), new BigDecimal("450.00"), PaymentStatus.PARTIAL);

        assertThatThrownBy(() -> paymentService.recordPayment(
                PAYMENT_ID, new PaymentTransactionRequest(new BigDecimal("100.00"), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("50.00");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordPayment_againstAnOverdueInvoice_leavesItPartialUntilTheNextSweep() {
        givenInvoice(new BigDecimal("500.00"), BigDecimal.ZERO, PaymentStatus.OVERDUE);

        assertThat(paymentService.recordPayment(
                PAYMENT_ID, new PaymentTransactionRequest(new BigDecimal("100.00"), null, null)).status())
                .isEqualTo(PaymentStatus.PARTIAL);
    }

    @Test
    void markOverdueSweep_movesUnsettledPastDueInvoices_andReportsHowMany() {
        Payment pending = invoice(new BigDecimal("100.00"), BigDecimal.ZERO, PaymentStatus.PENDING);
        Payment partial = invoice(new BigDecimal("100.00"), new BigDecimal("40.00"), PaymentStatus.PARTIAL);
        when(paymentRepository.findPastDue(any(), any())).thenReturn(List.of(pending, partial));

        assertThat(paymentService.markOverdueSweep()).isEqualTo(2);
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(partial.getStatus()).isEqualTo(PaymentStatus.OVERDUE);
    }

    @Test
    void createInvoiceOnApproval_raisesOneForThePriceOfTheCourse() {
        when(paymentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.createInvoiceOnApproval(student(), course(new BigDecimal("500.00")));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getAmountDue()).isEqualByComparingTo("500.00");
        assertThat(saved.getValue().getAmountPaid()).isEqualByComparingTo("0");
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getValue().getDueDate()).isAfter(LocalDate.now());
    }

    @Test
    void createInvoiceOnApproval_forAFreeCourse_raisesNothing() {
        paymentService.createInvoiceOnApproval(student(), course(BigDecimal.ZERO));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createInvoiceOnApproval_whenThePairIsAlreadyInvoiced_raisesNothing() {
        when(paymentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);

        paymentService.createInvoiceOnApproval(student(), course(new BigDecimal("500.00")));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void outstandingBalance_isZeroRatherThanNull_forAStudentWithNoInvoices() {
        when(paymentRepository.sumOutstandingByStudentId(STUDENT_ID)).thenReturn(null);

        assertThat(paymentService.outstandingBalance(STUDENT_ID)).isEqualByComparingTo("0");
    }

    /** The invoice {@code recordPayment} will find, saved back as-is. */
    private Payment givenInvoice(BigDecimal due, BigDecimal paid, PaymentStatus status) {
        Payment payment = invoice(due, paid, status);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        return payment;
    }

    private static Payment invoice(BigDecimal due, BigDecimal paid, PaymentStatus status) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .student(student())
                .course(course(due))
                .amountDue(due)
                .amountPaid(paid)
                .status(status)
                .dueDate(LocalDate.of(2026, 1, 31))
                .build();
    }

    private static User student() {
        return User.builder()
                .id(STUDENT_ID)
                .firstName("Sam")
                .lastName("Student")
                .email("sam@tcm.local")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private static Course course(BigDecimal price) {
        return Course.builder()
                .id(COURSE_ID).code("JAVA-101").name("Java Fundamentals")
                .durationHours(40).capacity(20).price(price)
                .status(CourseStatus.PUBLISHED)
                .build();
    }
}
