package com.tcm.payment;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.payment.dto.PaymentRequest;
import com.tcm.payment.dto.PaymentResponse;
import com.tcm.payment.dto.PaymentTransactionRequest;
import com.tcm.payment.mapper.PaymentMapper;
import com.tcm.payment.model.Payment;
import com.tcm.payment.model.PaymentStatus;
import com.tcm.payment.spec.PaymentSpecifications;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** How long an auto-created invoice has to run before it falls due. */
    private static final int PAYMENT_TERM_DAYS = 30;

    private static final Set<PaymentStatus> UNSETTLED =
            Set.of(PaymentStatus.PENDING, PaymentStatus.PARTIAL);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse createInvoice(PaymentRequest request) {
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new BadRequestException("No user with id " + request.studentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("studentId must reference a user with role STUDENT");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BadRequestException("No course with id " + request.courseId()));

        return paymentMapper.toResponse(
                paymentRepository.save(newInvoice(student, course, request.amountDue(), request.dueDate())));
    }

    @Override
    @Transactional
    public void createInvoiceOnApproval(User student, Course course) {
        BigDecimal price = course.getPrice();
        if (price == null || price.signum() <= 0) {
            return;
        }
        if (paymentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            return;
        }
        paymentRepository.save(
                newInvoice(student, course, price, LocalDate.now().plusDays(PAYMENT_TERM_DAYS)));
    }

    @Override
    @Transactional
    public PaymentResponse recordPayment(UUID paymentId, PaymentTransactionRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment with id " + paymentId));

        BigDecimal paid = payment.getAmountPaid().add(request.amount());
        if (paid.compareTo(payment.getAmountDue()) > 0) {
            throw new BadRequestException(
                    "That payment exceeds the " + payment.outstanding() + " still owed on this invoice");
        }

        payment.setAmountPaid(paid);
        if (request.paymentMethod() != null) {
            payment.setPaymentMethod(request.paymentMethod());
        }
        if (request.notes() != null) {
            payment.setNotes(request.notes());
        }

        if (paid.compareTo(payment.getAmountDue()) == 0) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
        } else {
            // Still owing: PARTIAL, even if the invoice had already gone
            // OVERDUE - what's outstanding matters more to the reader than
            // that it once passed its date, and the next sweep restores
            // OVERDUE if it's still late.
            payment.setStatus(PaymentStatus.PARTIAL);
        }
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public int markOverdueSweep() {
        List<Payment> pastDue = paymentRepository.findPastDue(LocalDate.now(), UNSETTLED);
        pastDue.forEach(payment -> payment.setStatus(PaymentStatus.OVERDUE));
        paymentRepository.saveAll(pastDue);
        return pastDue.size();
    }

    @Override
    @Transactional
    public Page<PaymentResponse> search(UUID studentId, UUID courseId, PaymentStatus status, Pageable pageable) {
        // Sweeping here is what keeps OVERDUE honest without a scheduled job:
        // the listing is the only place a stale status would be read from.
        markOverdueSweep();

        Specification<Payment> spec = Specification
                .where(PaymentSpecifications.hasStudent(studentId))
                .and(PaymentSpecifications.hasCourse(courseId))
                .and(PaymentSpecifications.hasStatus(status));
        return paymentRepository.findAll(spec, pageable).map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    public List<PaymentResponse> findMineForStudent(UUID studentId) {
        markOverdueSweep();
        return paymentRepository.findByStudentIdOrderByDueDateDesc(studentId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal outstandingBalance(UUID studentId) {
        BigDecimal balance = paymentRepository.sumOutstandingByStudentId(studentId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private static Payment newInvoice(User student, Course course, BigDecimal amountDue, LocalDate dueDate) {
        return Payment.builder()
                .student(student)
                .course(course)
                .amountDue(amountDue)
                .amountPaid(BigDecimal.ZERO)
                .status(PaymentStatus.PENDING)
                .dueDate(dueDate)
                .build();
    }
}
