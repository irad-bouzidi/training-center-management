package com.tcm.qrattendance;

import com.tcm.attendance.AttendanceService;
import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.qrattendance.dto.QrCodeResponse;
import com.tcm.schedule.model.ClassSession;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The bonus feature, per docs/tasks/TCM-27: a trainer shows a session's QR
 * code, students scan it and are marked present. The marking itself is
 * TCM-19's - this only establishes who may be marked, and with what method.
 */
@Service
@RequiredArgsConstructor
public class QrAttendanceService {

    private final QrTokenService qrTokenService;
    private final QrCodeImageService qrCodeImageService;
    private final AttendanceService attendanceService;
    private final EnrollmentRepository enrollmentRepository;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    /** A fresh code for a session, replacing whatever it had. */
    @Transactional
    public QrCodeResponse issue(UUID sessionId, UUID requesterId, boolean requesterIsAdmin) {
        QrTokenService.IssuedToken issued = qrTokenService.issue(sessionId, requesterId, requesterIsAdmin);
        String checkInUrl = checkInUrl(sessionId, issued.token());

        return new QrCodeResponse(
                issued.token(),
                issued.expiresAt(),
                checkInUrl,
                Base64.getEncoder().encodeToString(qrCodeImageService.render(checkInUrl)));
    }

    /**
     * Marks the scanning student present, with {@code method=QR}. Scanning
     * twice is the same as scanning once - TCM-19's records are one per
     * (session, student), so the second scan corrects the first rather than
     * adding to it.
     *
     * @throws AccessDeniedException if the student holds no APPROVED
     *                               enrollment in the session's course: a
     *                               valid code is not an invitation to a
     *                               course you aren't on.
     */
    @Transactional
    public AttendanceResponse checkIn(UUID sessionId, String token, UUID studentId) {
        ClassSession session = qrTokenService.requireValid(sessionId, token);

        boolean approved = enrollmentRepository
                .findByCourseIdAndStatus(session.getCourse().getId(), EnrollmentStatus.APPROVED).stream()
                .map(Enrollment::getStudent)
                .anyMatch(student -> student.getId().equals(studentId));
        if (!approved) {
            throw new AccessDeniedException("You are not enrolled in this course");
        }

        return attendanceService.markViaQr(sessionId, studentId, AttendanceStatus.PRESENT);
    }

    /** What the QR image encodes: the frontend page that posts the check-in. */
    private String checkInUrl(UUID sessionId, String token) {
        return "%s/attend/%s?token=%s".formatted(
                frontendBaseUrl, sessionId, java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8));
    }
}
