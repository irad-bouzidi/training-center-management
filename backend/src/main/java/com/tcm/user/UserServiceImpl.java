package com.tcm.user;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.user.dto.ResetPasswordResponse;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.mapper.UserMapper;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("password is required");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("A user with this email already exists");
        }

        User user = userMapper.toNewEntity(request, passwordEncoder.encode(request.password()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse update(UUID id, UserRequest request, boolean requesterIsAdmin) {
        User user = getOrThrow(id);

        if (!requesterIsAdmin && request.role() != user.getRole()) {
            throw new AccessDeniedException("Only administrators can change a user's role");
        }
        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("A user with this email already exists");
        }

        userMapper.applyUpdate(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse changeStatus(UUID id, UserStatus status) {
        User user = getOrThrow(id);
        user.setStatus(status);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse findById(UUID id) {
        return userMapper.toResponse(getOrThrow(id));
    }

    @Override
    public Page<UserResponse> search(Role role, UserStatus status, String name, Pageable pageable) {
        return userRepository.search(role, status, name, pageable).map(userMapper::toResponse);
    }

    @Override
    public ResetPasswordResponse resetPassword(UUID id) {
        User user = getOrThrow(id);
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return new ResetPasswordResponse(tempPassword);
    }

    private User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id));
    }

    private static String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
