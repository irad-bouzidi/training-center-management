package com.tcm.grade;

import com.tcm.grade.dto.CourseGradebookResponse;
import com.tcm.grade.dto.GradeRequest;
import com.tcm.grade.dto.GradeResponse;
import com.tcm.grade.dto.StudentGradesResponse;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Grades and assessments, per docs/tasks/TCM-23. The role annotations only
 * say who may hold a pen at all; which course a trainer may grade, and whose
 * marking they may amend, are ownership questions the service settles.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @PostMapping("/grades")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse create(@Valid @RequestBody GradeRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return gradeService.create(request, principal.getId(), isAdmin(principal));
    }

    @PutMapping("/grades/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public GradeResponse update(@PathVariable UUID id, @Valid @RequestBody GradeRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return gradeService.update(id, request, principal.getId(), isAdmin(principal));
    }

    @DeleteMapping("/grades/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        gradeService.delete(id, principal.getId(), isAdmin(principal));
    }

    @GetMapping("/courses/{courseId}/grades")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    public CourseGradebookResponse gradebook(@PathVariable UUID courseId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return gradeService.courseGradebook(courseId, principal.getId(), isAdmin(principal));
    }

    /** A student reads their own; a trainer reads their own course's; an admin reads any. */
    @GetMapping("/students/{studentId}/grades")
    public StudentGradesResponse forStudent(@PathVariable UUID studentId,
                                             @RequestParam(required = false) UUID courseId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return gradeService.findForStudent(studentId, courseId, principal.getId(), isAdmin(principal));
    }

    private static boolean isAdmin(UserPrincipal principal) {
        return principal.getUser().getRole() == Role.ADMIN;
    }
}
