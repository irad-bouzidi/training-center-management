package com.tcm.course.mapper;

import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseResponse;
import com.tcm.course.model.Course;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getDescription(),
                course.getDurationHours(),
                course.getCapacity(),
                course.getCategory(),
                toTrainerSummary(course.getPrimaryTrainer()),
                course.getPrice(),
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }

    public Course toNewEntity(CourseRequest request, User primaryTrainer) {
        return Course.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .durationHours(request.durationHours())
                .capacity(request.capacity())
                .category(request.category())
                .primaryTrainer(primaryTrainer)
                .price(request.price())
                .status(request.status())
                .build();
    }

    public void applyUpdate(Course course, CourseRequest request, User primaryTrainer) {
        course.setCode(request.code());
        course.setName(request.name());
        course.setDescription(request.description());
        course.setDurationHours(request.durationHours());
        course.setCapacity(request.capacity());
        course.setCategory(request.category());
        course.setPrimaryTrainer(primaryTrainer);
        course.setPrice(request.price());
        course.setStatus(request.status());
    }

    private static CourseResponse.TrainerSummary toTrainerSummary(User trainer) {
        if (trainer == null) {
            return null;
        }
        return new CourseResponse.TrainerSummary(trainer.getId(), trainer.getFirstName() + " " + trainer.getLastName());
    }
}
