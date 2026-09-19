package com.tcm.dashboard;

import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.course.model.CourseStatus;
import com.tcm.dashboard.dto.AdminDashboardResponse;
import com.tcm.dashboard.dto.TrainerDashboardResponse;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.payment.PaymentService;
import com.tcm.payment.model.PaymentStatus;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.user.model.Role;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** "Upcoming" is the week ahead - the horizon a timetable is read at. */
    private static final int UPCOMING_DAYS = 7;

    private static final Set<AttendanceStatus> ATTENDED =
            Set.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE);
    private static final Set<AttendanceStatus> ALL_MARKS =
            Set.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.ABSENT);

    private final DashboardRepository dashboardRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public AdminDashboardResponse adminSummary() {
        // The same sweep the payments listing runs, for the same reason: an
        // invoice that fell due overnight should be counted as overdue here
        // without anyone having asked for it (see PaymentServiceImpl#search).
        paymentService.markOverdueSweep();

        LocalDate today = LocalDate.now();
        BigDecimal outstanding = dashboardRepository.sumOutstandingPayments();

        return new AdminDashboardResponse(
                dashboardRepository.countUsers(Role.STUDENT, UserStatus.ACTIVE),
                dashboardRepository.countUsers(Role.TRAINER, UserStatus.ACTIVE),
                dashboardRepository.countCourses(CourseStatus.PUBLISHED),
                dashboardRepository.countEnrollments(EnrollmentStatus.PENDING),
                dashboardRepository.countSessionsBetween(
                        SessionStatus.SCHEDULED, today, today.plusDays(UPCOMING_DAYS)),
                outstanding == null ? BigDecimal.ZERO : outstanding,
                dashboardRepository.countPayments(PaymentStatus.OVERDUE),
                averageAttendanceRate(),
                dashboardRepository.countCertificatesSince(startOfThisMonth()));
    }

    @Override
    @Transactional(readOnly = true)
    public TrainerDashboardResponse trainerSummary(UUID trainerId) {
        LocalDate today = LocalDate.now();

        return new TrainerDashboardResponse(
                dashboardRepository.countCoursesForTrainer(trainerId),
                dashboardRepository.countSessionsBetweenForTrainer(
                        trainerId, SessionStatus.SCHEDULED, today, today.plusDays(UPCOMING_DAYS)),
                dashboardRepository.countCompletedSessionsWithoutAttendance(trainerId, SessionStatus.COMPLETED),
                dashboardRepository.countUngradedStudentsForTrainer(trainerId, EnrollmentStatus.APPROVED));
    }

    /**
     * Attended (present or late) as a percentage of everything marked, null
     * while nothing has been marked anywhere - unmarked is not zero, here as
     * everywhere else.
     */
    private Double averageAttendanceRate() {
        long marked = dashboardRepository.countAttendanceRecords(List.copyOf(ALL_MARKS));
        if (marked == 0) {
            return null;
        }
        long attended = dashboardRepository.countAttendanceRecords(List.copyOf(ATTENDED));
        return Math.round(attended * 1000.0 / marked) / 10.0;
    }

    private static java.time.Instant startOfThisMonth() {
        return YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
