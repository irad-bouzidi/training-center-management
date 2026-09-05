package com.tcm.user;

import com.tcm.user.dto.ResetPasswordResponse;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.model.Role;
import com.tcm.user.model.UserStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(UserRequest request);

    /**
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN. Anyone
     *                         else attempting to actually change {@code role}
     *                         is rejected with {@code AccessDeniedException}.
     */
    UserResponse update(UUID id, UserRequest request, boolean requesterIsAdmin);

    UserResponse changeStatus(UUID id, UserStatus status);

    UserResponse findById(UUID id);

    Page<UserResponse> search(Role role, UserStatus status, String name, Pageable pageable);

    ResetPasswordResponse resetPassword(UUID id);
}
