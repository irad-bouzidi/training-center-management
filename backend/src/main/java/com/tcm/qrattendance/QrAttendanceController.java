package com.tcm.qrattendance;

import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.qrattendance.dto.QrCheckInRequest;
import com.tcm.qrattendance.dto.QrCodeResponse;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QR attendance, per docs/tasks/TCM-27. Producing a code is the assigned
 * trainer's or an admin's; scanning one is a student's, and the student is
 * always themselves - there is no studentId to pass, so a scan can never
 * check anyone else in.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QrAttendanceController {

    private final QrAttendanceService qrAttendanceService;

    @PostMapping("/sessions/{sessionId}/qr")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public QrCodeResponse issue(@PathVariable UUID sessionId, @AuthenticationPrincipal UserPrincipal principal) {
        return qrAttendanceService.issue(
                sessionId, principal.getId(), principal.getUser().getRole() == Role.ADMIN);
    }

    @PostMapping("/attendance/qr-checkin")
    @PreAuthorize("hasRole('STUDENT')")
    public AttendanceResponse checkIn(@Valid @RequestBody QrCheckInRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return qrAttendanceService.checkIn(request.sessionId(), request.token(), principal.getId());
    }
}
