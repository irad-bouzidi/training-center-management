package com.tcm.user;

import com.tcm.common.PageResponse;
import com.tcm.security.UserPrincipal;
import com.tcm.user.dto.ResetPasswordResponse;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.dto.UserStatusRequest;
import com.tcm.user.model.Role;
import com.tcm.user.model.UserStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> search(@RequestParam(required = false) Role role,
                                              @RequestParam(required = false) UserStatus status,
                                              @RequestParam(required = false) String name,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(userService.search(role, status, name, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserResponse findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserResponse update(@PathVariable UUID id,
                               @Valid @RequestBody UserRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        boolean requesterIsAdmin = principal.getUser().getRole() == Role.ADMIN;
        return userService.update(id, request, requesterIsAdmin);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody UserStatusRequest request) {
        return userService.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResetPasswordResponse resetPassword(@PathVariable UUID id) {
        return userService.resetPassword(id);
    }
}
