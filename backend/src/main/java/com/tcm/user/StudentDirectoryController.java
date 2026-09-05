package com.tcm.user;

import com.tcm.common.PageResponse;
import com.tcm.user.dto.StudentDirectoryResponse;
import com.tcm.user.dto.StudentSummaryResponse;
import com.tcm.user.model.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student-facing aggregation endpoints, per docs/tasks/TCM-13: a dedicated,
 * purpose-named directory (rather than {@code GET /users?role=STUDENT}) plus
 * a per-student summary that later tasks (TCM-14/19/21/23/25) progressively
 * fill in without changing the response contract.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentDirectoryController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public PageResponse<StudentDirectoryResponse> search(@RequestParam(required = false) UserStatus status,
                                                          @RequestParam(required = false) String name,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(userService.searchStudents(status, name, pageable));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER') or #id == authentication.principal.id")
    public StudentSummaryResponse summary(@PathVariable UUID id) {
        return userService.getStudentSummary(id);
    }
}
