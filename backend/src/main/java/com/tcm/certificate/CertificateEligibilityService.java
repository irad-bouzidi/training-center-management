package com.tcm.certificate;

import com.tcm.attendance.AttendanceService;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * What "completed" means for certification, per docs/tasks/TCM-25 - one rule,
 * stated here so nothing else has to guess it:
 *
 * <ol>
 *   <li>the student's enrollment in the course is explicitly
 *       {@link EnrollmentStatus#COMPLETED} - an administrator's decision,
 *       not something inferred from the calendar; and</li>
 *   <li>their attendance on that course is at least
 *       {@code certificates.minimum-attendance-rate} percent.</li>
 * </ol>
 *
 * A student nobody has marked at all has no attendance rate, and an unproven
 * attendance is not a met one: they're ineligible, and told so in as many
 * words rather than being read as 0%.
 */
@Service
@RequiredArgsConstructor
public class CertificateEligibilityService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceService attendanceService;
    private final CertificateProperties properties;

    /**
     * @return {@code null} when the student may be certified, or the reason
     *         they may not - phrased for the person reading the error.
     */
    public String ineligibilityReason(UUID studentId, UUID courseId) {
        Enrollment enrollment = enrollmentRepository.findByStudentId(studentId).stream()
                .filter(candidate -> candidate.getCourse().getId().equals(courseId))
                .findFirst()
                .orElse(null);

        if (enrollment == null) {
            return "This student is not enrolled in this course";
        }
        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            return "This student's enrollment must be marked COMPLETED before a certificate can be issued"
                    + " (it is currently " + enrollment.getStatus() + ")";
        }

        Double attendanceRate = attendanceService.studentCourseAttendanceRate(studentId, courseId);
        if (attendanceRate == null) {
            return "No attendance has been recorded for this student on this course,"
                    + " so the " + properties.getMinimumAttendanceRate() + "% requirement can't be shown to be met";
        }
        if (attendanceRate < properties.getMinimumAttendanceRate()) {
            return "Attendance on this course is " + attendanceRate + "%, below the "
                    + properties.getMinimumAttendanceRate() + "% required for a certificate";
        }
        return null;
    }
}
