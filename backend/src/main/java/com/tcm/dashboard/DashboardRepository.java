package com.tcm.dashboard;

import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.payment.model.PaymentStatus;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The dashboard's counts, in one place because that is what the dashboard is:
 * questions that cross every domain and belong to none of them. Each method
 * is a single aggregate query - the point of this package is that a summary
 * costs a handful of counts, not a page of rows loaded and counted in Java.
 *
 * The anchor entity ({@link User}) is arbitrary: Spring Data only needs one
 * to bind the interface, and every method names its own root in JPQL. No
 * writes, and no new tables - see docs/tasks/TCM-29.
 */
public interface DashboardRepository extends Repository<User, UUID> {

    @Query("select count(u) from User u where u.role = :role and u.status = :status")
    long countUsers(@Param("role") Role role, @Param("status") UserStatus status);

    @Query("select count(c) from Course c where c.status = :status")
    long countCourses(@Param("status") CourseStatus status);

    @Query("select count(e) from Enrollment e where e.status = :status")
    long countEnrollments(@Param("status") EnrollmentStatus status);

    @Query("""
            select count(s) from ClassSession s
            where s.status = :status and s.sessionDate between :from and :to
            """)
    long countSessionsBetween(@Param("status") SessionStatus status,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    @Query("""
            select count(s) from ClassSession s
            where s.trainer.id = :trainerId
              and s.status = :status
              and s.sessionDate between :from and :to
            """)
    long countSessionsBetweenForTrainer(@Param("trainerId") UUID trainerId,
                                         @Param("status") SessionStatus status,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    @Query("select count(c) from Course c where c.primaryTrainer.id = :trainerId")
    long countCoursesForTrainer(@Param("trainerId") UUID trainerId);

    /** Sessions a trainer has delivered with nobody marked - attendance still owed. */
    @Query("""
            select count(s) from ClassSession s
            where s.trainer.id = :trainerId
              and s.status = :completed
              and not exists (select 1 from AttendanceRecord r where r.session = s)
            """)
    long countCompletedSessionsWithoutAttendance(@Param("trainerId") UUID trainerId,
                                                   @Param("completed") SessionStatus completed);

    /**
     * Students approved on a trainer's courses with nothing graded yet - the
     * best-effort "still to mark" figure docs/tasks/TCM-29 asks for.
     */
    @Query("""
            select count(e) from Enrollment e
            where e.course.primaryTrainer.id = :trainerId
              and e.status = :approved
              and not exists (
                  select 1 from Grade g where g.student = e.student and g.course = e.course)
            """)
    long countUngradedStudentsForTrainer(@Param("trainerId") UUID trainerId,
                                           @Param("approved") EnrollmentStatus approved);

    /** What every student owes, added up. Null when there are no invoices at all. */
    @Query("select sum(p.amountDue - p.amountPaid) from Payment p")
    BigDecimal sumOutstandingPayments();

    @Query("select count(p) from Payment p where p.status = :status")
    long countPayments(@Param("status") PaymentStatus status);

    @Query("select count(r) from AttendanceRecord r where r.status in :statuses")
    long countAttendanceRecords(@Param("statuses") java.util.Collection<AttendanceStatus> statuses);

    @Query("select count(c) from Certificate c where c.issuedAt >= :since")
    long countCertificatesSince(@Param("since") Instant since);
}
