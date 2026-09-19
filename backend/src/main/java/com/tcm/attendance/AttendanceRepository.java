package com.tcm.attendance;

import com.tcm.attendance.model.AttendanceRecord;
import com.tcm.attendance.model.AttendanceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    /** The roster read: every mark already made for a session. */
    List<AttendanceRecord> findBySessionId(UUID sessionId);

    /** Upsert lookup - see {@code AttendanceServiceImpl#markOne}. */
    Optional<AttendanceRecord> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);

    /**
     * Per-student tallies across every session of one course, for
     * {@code AttendanceServiceImpl#courseAttendanceReport}. Grouping in the
     * database keeps the report one query regardless of how many sessions
     * and students the course has. Students with no marks at all are absent
     * from the result; the service fills them in as zero rows so the report
     * still lists everyone enrolled.
     */
    @Query("""
            select r.student.id as studentId, r.status as status, count(r) as total
            from AttendanceRecord r
            where r.session.course.id = :courseId
            group by r.student.id, r.status
            """)
    List<StatusCount> countByCourseGroupedByStudentAndStatus(@Param("courseId") UUID courseId);

    /**
     * The same tallies for one student across every course, for
     * {@code AttendanceServiceImpl#studentAttendanceSummary} (which feeds
     * {@code StudentSummaryResponse#attendanceRate}).
     */
    @Query("""
            select r.status as status, count(r) as total
            from AttendanceRecord r
            where r.student.id = :studentId
            group by r.status
            """)
    List<StatusTotal> countByStudentGroupedByStatus(@Param("studentId") UUID studentId);

    /** Projection for the per-student groupings. */
    interface StatusCount {

        UUID getStudentId();

        AttendanceStatus getStatus();

        long getTotal();
    }

    /** Projection for the single-student grouping. */
    interface StatusTotal {

        AttendanceStatus getStatus();

        long getTotal();
    }
}
