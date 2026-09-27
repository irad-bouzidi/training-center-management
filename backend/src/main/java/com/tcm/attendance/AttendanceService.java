package com.tcm.attendance;

import com.tcm.attendance.dto.AttendanceMarkRequest;
import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.dto.CourseAttendanceReportResponse;
import com.tcm.attendance.dto.MyAttendanceResponse;
import com.tcm.attendance.dto.SessionRosterResponse;
import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceStatus;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    /**
     * The session's roster: every student holding an APPROVED (or
     * COMPLETED) enrollment in its course, each with their current mark or
     * null if unmarked.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else may only read a session they are the
     *                         assigned trainer of.
     */
    SessionRosterResponse getRoster(UUID sessionId, UUID requesterId, boolean requesterIsAdmin);

    /**
     * Marks (or re-marks) one student, always as {@link AttendanceMethod#MANUAL}.
     * Access is checked the same way as {@link #getRoster}. A CANCELLED
     * session can't be marked, and re-marking a student with the status they
     * already have leaves their record (method, marker, time) untouched.
     */
    AttendanceResponse markOne(UUID sessionId, UUID studentId, AttendanceStatus status, UUID markerId,
                                boolean requesterIsAdmin);

    /**
     * Marks a student present (or late) by their own QR scan, with
     * {@code method=QR} and no marker - nobody marked them, they turned up.
     * Called only by TCM-27's check-in, which has already established that
     * the scan was of this session's current code; there is no trainer
     * ownership to check here, because there is no trainer involved.
     */
    AttendanceResponse markViaQr(UUID sessionId, UUID studentId, AttendanceStatus status);

    /**
     * Marks a whole roster in one go. Existing marks are updated rather than
     * duplicated, and students left out of {@code entries} are untouched.
     * Returns the resulting records in the order they were submitted. Same
     * rules as {@link #markOne}.
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
     * The caller's own attendance records across every course, newest
     * session first - {@code GET /api/v1/attendance/mine}.
     */
    List<MyAttendanceResponse> findMine(UUID studentId);

    /**
     * A student's overall attendance rate as a percentage across every course
     * of theirs, or null while they have no marks at all. Feeds
     * {@code StudentSummaryResponse#attendanceRate}.
     */
    Double studentAttendanceSummary(UUID studentId);

    /**
     * The same figure for one course, or null while they have no marks on it.
     * Read directly rather than out of {@link #courseAttendanceReport}, so it
     * counts every mark on the course whatever the enrollment's status.
     */
    Double studentCourseAttendanceRate(UUID studentId, UUID courseId);
}
