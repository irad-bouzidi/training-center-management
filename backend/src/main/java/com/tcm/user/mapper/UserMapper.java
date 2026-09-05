package com.tcm.user.mapper;

import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    public User toNewEntity(UserRequest request, String passwordHash) {
        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordHash)
                .phone(request.phone())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();
    }

    /** Password is deliberately not touched here - see {@link UserRequest}. */
    public void applyUpdate(User user, UserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
    }
}
