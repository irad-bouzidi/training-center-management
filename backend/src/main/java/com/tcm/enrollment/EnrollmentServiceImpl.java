package com.tcm.enrollment;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.course.model.CourseStatus;
import com.tcm.enrollment.dto.EnrollmentResponse;
import com.tcm.enrollment.mapper.EnrollmentMapper;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.enrollment.spec.EnrollmentSpecifications;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    public EnrollmentResponse register(UUID studentId, UUID courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new BadRequestException("No user with id " + studentId));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("studentId must reference a user with role STUDENT");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("No course with id " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException("Course is not open for enrollment");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BadRequestException("Student is already enrolled in this course");
        }
        if (isAtCapacity(course)) {
            throw new BadRequestException("Course has reached its capacity");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status(EnrollmentStatus.PENDING)
                .build();
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public EnrollmentResponse decide(UUID enrollmentId, EnrollmentStatus decision, UUID adminId) {
        if (decision != EnrollmentStatus.APPROVED && decision != EnrollmentStatus.REJECTED) {
            throw new BadRequestException("decision must be APPROVED or REJECTED");
        }
        Enrollment enrollment = getOrThrow(enrollmentId);
        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING enrollments can be approved or rejected");
        }
        if (decision == EnrollmentStatus.APPROVED && isAtCapacity(enrollment.getCourse())) {
            throw new BadRequestException("Course has reached its capacity");
        }
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + adminId));

        enrollment.setStatus(decision);
        enrollment.setDecidedAt(Instant.now());
        enrollment.setDecidedBy(admin);
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public EnrollmentResponse cancel(UUID enrollmentId, UUID requesterId, boolean requesterIsAdmin) {
        Enrollment enrollment = getOrThrow(enrollmentId);
        if (!requesterIsAdmin && !enrollment.getStudent().getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only cancel your own enrollment");
        }
        if (enrollment.getStatus() != EnrollmentStatus.PENDING && enrollment.getStatus() != EnrollmentStatus.APPROVED) {
            throw new BadRequestException("Only PENDING or APPROVED enrollments can be cancelled");
        }
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public EnrollmentResponse markCompleted(UUID enrollmentId) {
        Enrollment enrollment = getOrThrow(enrollmentId);
        if (enrollment.getStatus() != EnrollmentStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED enrollments can be marked completed");
        }
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public Page<EnrollmentResponse> search(UUID courseId, UUID studentId, EnrollmentStatus status, Pageable pageable) {
        Specification<Enrollment> spec = Specification
                .where(EnrollmentSpecifications.hasCourse(courseId))
                .and(EnrollmentSpecifications.hasStudent(studentId))
                .and(EnrollmentSpecifications.hasStatus(status));
        return enrollmentRepository.findAll(spec, pageable).map(enrollmentMapper::toResponse);
    }

    @Override
    public Page<EnrollmentResponse> searchForTrainer(UUID trainerId, UUID courseId, EnrollmentStatus status,
                                                       Pageable pageable) {
        if (courseId != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("No course with id " + courseId));
            User trainer = course.getPrimaryTrainer();
            if (trainer == null || !trainer.getId().equals(trainerId)) {
                throw new AccessDeniedException("You may only view enrollments for your own courses");
            }
        }
        Specification<Enrollment> spec = Specification
                .where(EnrollmentSpecifications.hasCourse(courseId))
                .and(EnrollmentSpecifications.hasCourseTrainer(trainerId))
                .and(EnrollmentSpecifications.hasStatus(status));
        return enrollmentRepository.findAll(spec, pageable).map(enrollmentMapper::toResponse);
    }

    private boolean isAtCapacity(Course course) {
        long approvedCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.APPROVED);
        return approvedCount >= course.getCapacity();
    }

    private Enrollment getOrThrow(UUID id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No enrollment with id " + id));
    }
}
