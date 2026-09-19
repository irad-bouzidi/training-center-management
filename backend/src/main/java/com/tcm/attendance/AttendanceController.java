package com.tcm.attendance;

import com.tcm.attendance.dto.AttendanceBulkMarkRequest;
import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.dto.CourseAttendanceReportResponse;
import com.tcm.attendance.dto.SessionRosterResponse;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Attendance reads and writes, per docs/tasks/TCM-19. The role annotations
 * only keep students out; which particular session or course a trainer may
 * touch is an ownership question, and the service layer settles it.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/sessions/{sessionId}/attendance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public SessionRosterResponse roster(@PathVariable UUID sessionId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return attendanceService.getRoster(sessionId, principal.getId(), isAdmin(principal));
    }

    @PostMapping("/sessions/{sessionId}/attendance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public List<AttendanceResponse> mark(@PathVariable UUID sessionId,
                                          @Valid @RequestBody AttendanceBulkMarkRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return attendanceService.markBulk(sessionId, request.entries(), principal.getId(), isAdmin(principal));
    }

    @GetMapping("/courses/{courseId}/attendance-report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public CourseAttendanceReportResponse report(@PathVariable UUID courseId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return attendanceService.courseAttendanceReport(courseId, principal.getId(), isAdmin(principal));
    }

    private static boolean isAdmin(UserPrincipal principal) {
        return principal.getUser().getRole() == Role.ADMIN;
    }
}
