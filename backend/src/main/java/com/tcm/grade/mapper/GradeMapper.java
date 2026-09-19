package com.tcm.grade.mapper;

import com.tcm.course.model.Course;
import com.tcm.grade.dto.GradeResponse;
import com.tcm.grade.model.Grade;
import com.tcm.user.model.User;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

    public GradeResponse toResponse(Grade grade) {
        return new GradeResponse(
                grade.getId(),
                toUserSummary(grade.getStudent()),
                toCourseSummary(grade.getCourse()),
                grade.getAssessmentType(),
                grade.getTitle(),
                grade.getScore(),
                grade.getMaxScore(),
                grade.getWeight(),
                percentage(grade),
                toUserSummary(grade.getGradedBy()),
                grade.getGradedAt(),
                grade.getComments());
    }

    /** One decimal place, the same precision the weighted averages report. */
    private static Double percentage(Grade grade) {
        return grade.getScore()
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(grade.getMaxScore(), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static GradeResponse.UserSummary toUserSummary(User user) {
        return new GradeResponse.UserSummary(user.getId(), user.getFirstName() + " " + user.getLastName());
    }

    private static GradeResponse.CourseSummary toCourseSummary(Course course) {
        return new GradeResponse.CourseSummary(course.getId(), course.getCode(), course.getName());
    }
}
