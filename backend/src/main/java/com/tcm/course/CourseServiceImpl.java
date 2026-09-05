package com.tcm.course;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseResponse;
import com.tcm.course.mapper.CourseMapper;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.course.spec.CourseSpecifications;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;

    @Override
    public CourseResponse create(CourseRequest request) {
        if (courseRepository.existsByCode(request.code())) {
            throw new BadRequestException("A course with this code already exists");
        }
        User trainer = resolveTrainer(request.primaryTrainerId());
        Course course = courseMapper.toNewEntity(request, trainer);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse update(UUID id, CourseRequest request) {
        Course course = getOrThrow(id);
        if (!course.getCode().equalsIgnoreCase(request.code()) && courseRepository.existsByCode(request.code())) {
            throw new BadRequestException("A course with this code already exists");
        }
        User trainer = resolveTrainer(request.primaryTrainerId());
        courseMapper.applyUpdate(course, request, trainer);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse changeStatus(UUID id, CourseStatus status) {
        Course course = getOrThrow(id);
        course.setStatus(status);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse findById(UUID id) {
        return courseMapper.toResponse(getOrThrow(id));
    }

    @Override
    public Page<CourseResponse> search(CourseStatus status, String category, UUID trainerId, String query,
                                        Pageable pageable) {
        Specification<Course> spec = Specification
                .where(CourseSpecifications.hasStatus(status))
                .and(CourseSpecifications.hasCategory(category))
                .and(CourseSpecifications.hasTrainer(trainerId))
                .and(CourseSpecifications.nameOrCodeContains(query));
        return courseRepository.findAll(spec, pageable).map(courseMapper::toResponse);
    }

    @Override
    public Page<CourseResponse> findMine(UUID trainerId, Pageable pageable) {
        Specification<Course> spec = Specification.where(CourseSpecifications.hasTrainer(trainerId));
        return courseRepository.findAll(spec, pageable).map(courseMapper::toResponse);
    }

    @Override
    public void delete(UUID id) {
        // TODO(TCM-14): reject deletion once the course has enrollments.
        courseRepository.delete(getOrThrow(id));
    }

    private User resolveTrainer(UUID trainerId) {
        if (trainerId == null) {
            return null;
        }
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new BadRequestException("No user with id " + trainerId));
        if (trainer.getRole() != Role.TRAINER) {
            throw new BadRequestException("primaryTrainerId must reference a user with role TRAINER");
        }
        return trainer;
    }

    private Course getOrThrow(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No course with id " + id));
    }
}
