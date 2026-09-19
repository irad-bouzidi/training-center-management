package com.tcm.attendance;

import com.tcm.attendance.dto.AttendanceMarkRequest;
import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.dto.CourseAttendanceReportResponse;
import com.tcm.attendance.dto.SessionRosterResponse;
import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceStatus;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    /**
     * The session's roster: every student holding an APPROVED enrollment in
     * its course, each with their current mark or null if unmarked.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else may only read a session they are the
     *                         assigned trainer of.
     */
    SessionRosterResponse getRoster(UUID sessionId, UUID requesterId, boolean requesterIsAdmin);

    /**
     * Marks (or re-marks) one student, always as {@link AttendanceMethod#MANUAL}.
     * Access is checked the same way as {@link #getRoster}.
     */
    AttendanceResponse markOne(UUID sessionId, UUID studentId, AttendanceStatus status, UUID markerId,
                                boolean requesterIsAdmin);

    /**
     * Marks a whole roster in one go. Existing marks are updated rather than
     * duplicated, and students left out of {@code entries} are untouched.
     * Returns the resulting records in the order they were submitted.
     */
    List<AttendanceResponse> markBulk(UUID sessionId, List<AttendanceMarkRequest> entries, UUID markerId,
                                       boolean requesterIsAdmin);

    /**
     * Per-student attendance tallies across every session of a course.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else must be a trainer of the course (its
     *                         primary trainer, or assigned to one of its
     *                         sessions).
     */
    CourseAttendanceReportResponse courseAttendanceReport(UUID courseId, UUID requesterId, boolean requesterIsAdmin);

    /**
     * A student's overall attendance rate as a percentage across every course
     * of theirs, or null while they have no marks at all. Feeds
     * {@code StudentSummaryResponse#attendanceRate}.
     */
    Double studentAttendanceSummary(UUID studentId);

    /**
     * The same figure for one course, or null while they have no marks on it.
     * Read directly rather than out of {@link #courseAttendanceReport}, whose
     * rows are the course's APPROVED roster: certification (TCM-25) asks
     * about students whose enrollment is already COMPLETED.
     */
    Double studentCourseAttendanceRate(UUID studentId, UUID courseId);
}
