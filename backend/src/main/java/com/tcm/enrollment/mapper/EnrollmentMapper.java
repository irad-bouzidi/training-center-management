package com.tcm.enrollment.mapper;

import com.tcm.course.model.Course;
import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                toUserSummary(enrollment.getStudent()),
                toCourseSummary(enrollment.getCourse()),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getDecidedAt(),
                toUserSummary(enrollment.getDecidedBy()));
    }

    private static EnrollmentResponse.UserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new EnrollmentResponse.UserSummary(user.getId(), user.getFirstName() + " " + user.getLastName(), user.getEmail());
    }

    private static EnrollmentResponse.CourseSummary toCourseSummary(Course course) {
        if (course == null) {
            return null;
        }
        return new EnrollmentResponse.CourseSummary(course.getId(), course.getCode(), course.getName());
    }
}
