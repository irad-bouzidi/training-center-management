package com.tcm.user.mapper;

import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.user.dto.StudentDirectoryResponse;
import com.tcm.user.dto.StudentSummaryResponse;
import com.tcm.user.dto.UserRequest;
import com.tcm.user.dto.UserResponse;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.util.List;
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

    /** See docs/tasks/TCM-13. {@code activeEnrollments} is the student's count of APPROVED enrollments. */
    public StudentDirectoryResponse toDirectoryResponse(User student, long activeEnrollments) {
        return new StudentDirectoryResponse(toResponse(student), (int) activeEnrollments);
    }

    /** See docs/tasks/TCM-13 for the documented, stable response shape. */
    public StudentSummaryResponse toSummaryResponse(User student, List<EnrollmentResponse> enrollments) {
        return new StudentSummaryResponse(
                toResponse(student),
                enrollments,
                null, // TODO(TCM-19): populate real attendance rate
                List.of(), // TODO(TCM-23): populate real grades
                null, // TODO(TCM-21): populate real payment balance
                List.of()); // TODO(TCM-25): populate real certificates
    }
}
