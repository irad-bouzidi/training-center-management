package com.tcm.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.user.dto.ResetPasswordResponse;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.mapper.UserMapper;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit test with a mocked repository (per TCM-8) - the real {@link UserMapper}
 * is used as-is since it's pure mapping logic, not the thing under test.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, new UserMapper(), passwordEncoder);
    }

    private static User existingUser(UUID id, Role role) {
        return User.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .passwordHash("old-hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void create_withValidRequest_hashesPasswordAndSaves() {
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", "Secret123!", null, Role.STUDENT);
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(request);

        assertThat(response.email()).isEqualTo("jane.doe@example.com");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void create_withBlankPassword_throwsBadRequest() {
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", " ", null, Role.STUDENT);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withDuplicateEmail_throwsBadRequest() {
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", "Secret123!", null, Role.STUDENT);
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_nonAdminChangingRole_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", null, null, Role.TRAINER);

        assertThatThrownBy(() -> userService.update(id, request, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_nonAdminKeepingSameRole_succeeds() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserRequest request = new UserRequest("Janet", "Doe", "jane.doe@example.com", null, null, Role.STUDENT);

        UserResponse response = userService.update(id, request, false);

        assertThat(response.firstName()).isEqualTo("Janet");
    }

    @Test
    void update_adminChangingRole_succeeds() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserRequest request = new UserRequest("Jane", "Doe", "jane.doe@example.com", null, null, Role.TRAINER);

        UserResponse response = userService.update(id, request, true);

        assertThat(response.role()).isEqualTo(Role.TRAINER);
    }

    @Test
    void update_toAnotherUsersEmail_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        UserRequest request = new UserRequest("Jane", "Doe", "taken@example.com", null, null, Role.STUDENT);

        assertThatThrownBy(() -> userService.update(id, request, true))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeStatus_updatesStatus() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.changeStatus(id, UserStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void findById_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPassword_hashesAndSavesNewTempPassword() {
        UUID id = UUID.randomUUID();
        User user = existingUser(id, Role.STUDENT);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResetPasswordResponse response = userService.resetPassword(id);

        assertThat(response.tempPassword()).hasSize(12);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }
}
