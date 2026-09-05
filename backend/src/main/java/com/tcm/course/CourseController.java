package com.tcm.course;

import com.tcm.common.PageResponse;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseResponse;
import com.tcm.course.dto.CourseStatusRequest;
import com.tcm.course.model.CourseStatus;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    /**
     * Non-admins always see only {@code PUBLISHED} courses here, regardless
     * of any {@code status} they pass - the status filter only takes effect
     * for ADMIN callers.
     */
    @GetMapping
    public PageResponse<CourseResponse> search(@RequestParam(required = false) CourseStatus status,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) UUID trainerId,
                                                @RequestParam(required = false) String query,
                                                @PageableDefault(size = 20) Pageable pageable,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        CourseStatus effectiveStatus = isAdmin ? status : CourseStatus.PUBLISHED;
        return PageResponse.from(courseService.search(effectiveStatus, category, trainerId, query, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TRAINER')")
    public PageResponse<CourseResponse> mine(@PageableDefault(size = 20) Pageable pageable,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return PageResponse.from(courseService.findMine(principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    public CourseResponse findById(@PathVariable UUID id) {
        return courseService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody CourseStatusRequest request) {
        return courseService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        courseService.delete(id);
    }
}
