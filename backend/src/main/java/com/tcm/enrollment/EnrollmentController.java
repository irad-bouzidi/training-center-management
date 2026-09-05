package com.tcm.enrollment;

import com.tcm.common.BadRequestException;
import com.tcm.common.PageResponse;
import com.tcm.enrollment.dto.EnrollmentDecisionRequest;
import com.tcm.enrollment.dto.EnrollmentRequest;
import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * A STUDENT always self-registers - any {@code studentId} in the body is
     * ignored. An ADMIN registering a student on their behalf must supply one.
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse register(@Valid @RequestBody EnrollmentRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        UUID studentId = isAdmin ? requireStudentId(request.studentId()) : principal.getId();
        return enrollmentService.register(studentId, request.courseId());
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse decide(@PathVariable UUID id,
                                      @Valid @RequestBody EnrollmentDecisionRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return enrollmentService.decide(id, request.status(), principal.getId());
    }

    /** Ownership (a non-admin may only cancel their own enrollment) is enforced in the service layer. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public EnrollmentResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        return enrollmentService.cancel(id, principal.getId(), isAdmin);
    }

    /**
     * ADMIN sees every enrollment, filterable by course/student/status. A
     * TRAINER is restricted to enrollments in their own courses - an explicit
     * {@code courseId} must be one of their own, and omitting it lists across
     * all of them; {@code studentId} isn't honored for a TRAINER caller.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public PageResponse<EnrollmentResponse> search(@RequestParam(required = false) UUID courseId,
                                                    @RequestParam(required = false) UUID studentId,
                                                    @RequestParam(required = false) EnrollmentStatus status,
                                                    @PageableDefault(size = 20) Pageable pageable,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getUser().getRole() == Role.TRAINER) {
            return PageResponse.from(enrollmentService.searchForTrainer(principal.getId(), courseId, status, pageable));
        }
        return PageResponse.from(enrollmentService.search(courseId, studentId, status, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResponse<EnrollmentResponse> mine(@RequestParam(required = false) EnrollmentStatus status,
                                                  @PageableDefault(size = 20) Pageable pageable,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return PageResponse.from(enrollmentService.search(null, principal.getId(), status, pageable));
    }

    private static UUID requireStudentId(UUID studentId) {
        if (studentId == null) {
            throw new BadRequestException("studentId is required when an administrator registers a student");
        }
        return studentId;
    }
}
