package com.tcm.schedule.mapper;

import com.tcm.course.model.Course;
import com.tcm.schedule.dto.ClassSessionResponse;
import com.tcm.schedule.model.ClassSession;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class ClassSessionMapper {

    public ClassSessionResponse toResponse(ClassSession session) {
        return new ClassSessionResponse(
                session.getId(),
                toCourseSummary(session.getCourse()),
                toTrainerSummary(session.getTrainer()),
                session.getClassroom(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus());
    }

    private static ClassSessionResponse.CourseSummary toCourseSummary(Course course) {
        return new ClassSessionResponse.CourseSummary(course.getId(), course.getCode(), course.getName());
    }

    private static ClassSessionResponse.TrainerSummary toTrainerSummary(User trainer) {
        return new ClassSessionResponse.TrainerSummary(
                trainer.getId(), trainer.getFirstName() + " " + trainer.getLastName());
    }
}
