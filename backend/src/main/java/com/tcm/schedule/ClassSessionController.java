package com.tcm.schedule;

import com.tcm.common.BadRequestException;
import com.tcm.common.PageResponse;
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.schedule.dto.ClassSessionResponse;
import com.tcm.schedule.dto.ClassSessionStatusRequest;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassSessionResponse create(@Valid @RequestBody ClassSessionRequest request) {
        return classSessionService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassSessionResponse update(@PathVariable UUID id, @Valid @RequestBody ClassSessionRequest request) {
        return classSessionService.update(id, request);
    }

    /**
     * CANCELLED is ADMIN-only; COMPLETED is open to the session's assigned
     * TRAINER as well (ownership is enforced in the service layer). Moving a
     * session back to SCHEDULED isn't a transition the lifecycle has.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public ClassSessionResponse changeStatus(@PathVariable UUID id,
                                              @Valid @RequestBody ClassSessionStatusRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        return switch (request.status()) {
            case COMPLETED -> classSessionService.markCompleted(id, principal.getId(), isAdmin);
            case CANCELLED -> {
                if (!isAdmin) {
                    throw new AccessDeniedException("Only an administrator may cancel a session");
                }
                yield classSessionService.cancel(id);
            }
            case SCHEDULED -> throw new BadRequestException("status must be CANCELLED or COMPLETED");
        };
    }

    /**
     * ADMIN sees every session, filterable by course/trainer/date range. A
     * TRAINER is restricted to their own sessions - a {@code trainerId} they
     * pass is ignored rather than honored. A STUDENT sees only sessions of
     * courses they hold an APPROVED enrollment in.
     */
    @GetMapping
    public PageResponse<ClassSessionResponse> search(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID trainerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return PageResponse.from(switch (principal.getUser().getRole()) {
            case ADMIN -> classSessionService.search(courseId, trainerId, from, to, pageable);
            case TRAINER -> classSessionService.search(courseId, principal.getId(), from, to, pageable);
            case STUDENT -> classSessionService.searchForStudent(principal.getId(), courseId, from, to, pageable);
        });
    }
}
