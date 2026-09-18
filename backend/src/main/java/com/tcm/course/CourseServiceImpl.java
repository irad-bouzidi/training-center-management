package com.tcm.course;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseResponse;
import com.tcm.course.mapper.CourseMapper;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.course.spec.CourseSpecifications;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public CourseResponse create(CourseRequest request) {
        if (courseRepository.existsByCode(request.code())) {
            throw new BadRequestException("A course with this code already exists");
        }
        User trainer = resolveTrainer(request.primaryTrainerId());
        Course course = courseMapper.toNewEntity(request, trainer);
        return toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse update(UUID id, CourseRequest request) {
        Course course = getOrThrow(id);
        if (!course.getCode().equalsIgnoreCase(request.code()) && courseRepository.existsByCode(request.code())) {
            throw new BadRequestException("A course with this code already exists");
        }
        User trainer = resolveTrainer(request.primaryTrainerId());
        courseMapper.applyUpdate(course, request, trainer);
        return toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse changeStatus(UUID id, CourseStatus status) {
        Course course = getOrThrow(id);
        course.setStatus(status);
        return toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    public Page<CourseResponse> search(CourseStatus status, String category, UUID trainerId, String query,
                                        Pageable pageable) {
        Specification<Course> spec = Specification
                .where(CourseSpecifications.hasStatus(status))
                .and(CourseSpecifications.hasCategory(category))
                .and(CourseSpecifications.hasTrainer(trainerId))
                .and(CourseSpecifications.nameOrCodeContains(query));
        return toResponsePage(courseRepository.findAll(spec, pageable));
    }

    @Override
    public Page<CourseResponse> findMine(UUID trainerId, Pageable pageable) {
        Specification<Course> spec = Specification.where(CourseSpecifications.hasTrainer(trainerId));
        return toResponsePage(courseRepository.findAll(spec, pageable));
    }

    @Override
    public void delete(UUID id) {
        Course course = getOrThrow(id);
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new BadRequestException("Cannot delete a course that has enrollments");
        }
        courseRepository.delete(course);
    }

    private CourseResponse toResponse(Course course) {
        return courseMapper.toResponse(course,
                enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.APPROVED));
    }

    /** One grouped count query for the whole page rather than one per row. */
    private Page<CourseResponse> toResponsePage(Page<Course> page) {
        List<Course> courses = page.getContent();
        if (courses.isEmpty()) {
            return page.map(course -> courseMapper.toResponse(course, 0L));
        }
        Map<UUID, Long> approvedCounts = enrollmentRepository
                .countByCourseIdInAndStatus(courses.stream().map(Course::getId).toList(), EnrollmentStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(EnrollmentRepository.CourseStatusCount::getCourseId,
                        EnrollmentRepository.CourseStatusCount::getTotal));
        return page.map(course -> courseMapper.toResponse(course, approvedCounts.getOrDefault(course.getId(), 0L)));
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
