package com.tcm.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test with the aggregate repository and payment service mocked: every
 * figure is a count the repository hands back, so what's under test is which
 * count lands in which field, the window "upcoming" means, and the two
 * derived values (outstanding balance default, average attendance rate).
 *
 * There is no student dashboard to test - a student's home page is their own
 * summary (see DashboardController).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final UUID TRAINER_ID = UUID.randomUUID();

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private PaymentService paymentService;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(dashboardRepository, paymentService);
    }

    @Test
    void adminSummary_putsEachCountInItsField() {
        LocalDate today = LocalDate.now();
        when(dashboardRepository.countUsers(Role.STUDENT, UserStatus.ACTIVE)).thenReturn(120L);
        when(dashboardRepository.countUsers(Role.TRAINER, UserStatus.ACTIVE)).thenReturn(8L);
        when(dashboardRepository.countCourses(CourseStatus.PUBLISHED)).thenReturn(15L);
        when(dashboardRepository.countEnrollments(EnrollmentStatus.PENDING)).thenReturn(4L);
        when(dashboardRepository.countSessionsBetween(SessionStatus.SCHEDULED, today, today.plusDays(7)))
                .thenReturn(11L);
        when(dashboardRepository.sumOutstandingPayments()).thenReturn(new BigDecimal("2450.50"));
        when(dashboardRepository.countPayments(PaymentStatus.OVERDUE)).thenReturn(3L);
        givenAttendance(8, 6);
        when(dashboardRepository.countCertificatesSince(startOfThisMonth())).thenReturn(2L);

        AdminDashboardResponse response = dashboardService.adminSummary();

        assertThat(response.activeStudents()).isEqualTo(120);
        assertThat(response.activeTrainers()).isEqualTo(8);
        assertThat(response.publishedCourses()).isEqualTo(15);
        assertThat(response.pendingEnrollments()).isEqualTo(4);
        assertThat(response.upcomingSessions()).isEqualTo(11);
        assertThat(response.outstandingBalance()).isEqualByComparingTo("2450.50");
        assertThat(response.overdueInvoices()).isEqualTo(3);
        assertThat(response.averageAttendanceRate()).isEqualTo(75.0);
        assertThat(response.certificatesThisMonth()).isEqualTo(2);
    }

    @Test
    void adminSummary_runsTheOverdueSweepBeforeCountingOverdueInvoices() {
        dashboardService.adminSummary();

        InOrder order = inOrder(paymentService, dashboardRepository);
        order.verify(paymentService).markOverdueSweep();
        order.verify(dashboardRepository).countPayments(PaymentStatus.OVERDUE);
    }

    @Test
    void adminSummary_withNoInvoicesAtAll_reportsAZeroBalanceRatherThanNull() {
        when(dashboardRepository.sumOutstandingPayments()).thenReturn(null);

        assertThat(dashboardService.adminSummary().outstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void adminSummary_withNothingMarked_reportsNoAttendanceRate_notZero() {
        givenAttendance(0, 0);

        assertThat(dashboardService.adminSummary().averageAttendanceRate()).isNull();
        // with nothing marked, the "attended" count isn't worth asking for
        verify(dashboardRepository, never()).countAttendanceRecords(
                argThat(statuses -> statuses != null && statuses.size() == 2));
    }

    @Test
    void adminSummary_roundsTheAttendanceRateToOneDecimal() {
        givenAttendance(3, 2);

        assertThat(dashboardService.adminSummary().averageAttendanceRate()).isEqualTo(66.7);
    }

    @Test
    void adminSummary_countsLateAsAttended_andAbsentOnlyAsMarked() {
        givenAttendance(4, 4);

        assertThat(dashboardService.adminSummary().averageAttendanceRate()).isEqualTo(100.0);
        verify(dashboardRepository).countAttendanceRecords(argThat(statuses -> matches(statuses,
                Set.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.ABSENT))));
        verify(dashboardRepository).countAttendanceRecords(argThat(statuses -> matches(statuses,
                Set.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE))));
    }

    @Test
    void trainerSummary_scopesEveryCountToTheTrainer() {
        LocalDate today = LocalDate.now();
        when(dashboardRepository.countCoursesForTrainer(TRAINER_ID)).thenReturn(3L);
        when(dashboardRepository.countSessionsBetweenForTrainer(
                TRAINER_ID, SessionStatus.SCHEDULED, today, today.plusDays(7))).thenReturn(5L);
        when(dashboardRepository.countCompletedSessionsWithoutAttendance(TRAINER_ID, SessionStatus.COMPLETED))
                .thenReturn(2L);
        when(dashboardRepository.countUngradedStudentsForTrainer(TRAINER_ID, EnrollmentStatus.APPROVED))
                .thenReturn(9L);

        TrainerDashboardResponse response = dashboardService.trainerSummary(TRAINER_ID);

        assertThat(response).isEqualTo(new TrainerDashboardResponse(3, 5, 2, 9));
    }

    @Test
    void trainerSummary_touchesNoPlatformWideFigures() {
        dashboardService.trainerSummary(TRAINER_ID);

        verify(paymentService, never()).markOverdueSweep();
        verify(dashboardRepository, never()).countSessionsBetween(any(), any(), any());
        verify(dashboardRepository, never()).countUsers(any(), any());
        verify(dashboardRepository, never()).sumOutstandingPayments();
    }

    private void givenAttendance(long marked, long attended) {
        when(dashboardRepository.countAttendanceRecords(argThat(statuses -> statuses != null && statuses.size() == 3)))
                .thenReturn(marked);
        if (marked > 0) {
            when(dashboardRepository.countAttendanceRecords(
                    argThat(statuses -> statuses != null && statuses.size() == 2))).thenReturn(attended);
        }
    }

    private static boolean matches(Collection<AttendanceStatus> actual, Set<AttendanceStatus> expected) {
        return actual != null && actual.size() == expected.size() && expected.containsAll(actual);
    }

    private static Instant startOfThisMonth() {
        return YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
