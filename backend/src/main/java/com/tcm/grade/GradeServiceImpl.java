package com.tcm.grade;

import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.grade.dto.CourseGradebookResponse;
import com.tcm.grade.dto.GradeRequest;
import com.tcm.grade.dto.GradeResponse;
import com.tcm.grade.dto.StudentGradesResponse;
import com.tcm.grade.mapper.GradeMapper;
import com.tcm.grade.model.Grade;
import com.tcm.user.UserRepository;
import com.tcm.user.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeMapper gradeMapper;

    @Override
    @Transactional
    public GradeResponse create(GradeRequest request, UUID graderId, boolean requesterIsAdmin) {
        Course course = getCourseOrThrow(request.courseId());
        requireTeaches(course, graderId, requesterIsAdmin);
        validateScore(request);

        User student = approvedStudent(request.studentId(), course);
        User grader = userRepository.findById(graderId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + graderId));

        Grade grade = Grade.builder()
                .student(student)
                .course(course)
                .gradedBy(grader)
                .gradedAt(Instant.now())
                .build();
        apply(grade, request);
        return gradeMapper.toResponse(gradeRepository.save(grade));
    }

    @Override
    @Transactional
    public GradeResponse update(UUID id, GradeRequest request, UUID requesterId, boolean requesterIsAdmin) {
        Grade grade = getOrThrow(id);
        requireOwnGrading(grade, requesterId, requesterIsAdmin);
        validateScore(request);

        apply(grade, request);
        grade.setGradedAt(Instant.now());
        return gradeMapper.toResponse(gradeRepository.save(grade));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID requesterId, boolean requesterIsAdmin) {
        Grade grade = getOrThrow(id);
        requireOwnGrading(grade, requesterId, requesterIsAdmin);
        gradeRepository.delete(grade);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGradesResponse findForStudent(UUID studentId, UUID courseId, UUID requesterId,
                                                  boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !studentId.equals(requesterId)) {
            if (courseId == null) {
                throw new AccessDeniedException("Ask about one of your own courses, or read your own grades");
            }
            requireTeaches(getCourseOrThrow(courseId), requesterId, false);
        }

        List<Grade> grades = courseId == null
                ? gradeRepository.findByStudentIdOrderByGradedAtDesc(studentId)
                : gradeRepository.findByStudentIdAndCourseIdOrderByGradedAtDesc(studentId, courseId);
        return new StudentGradesResponse(grades.stream().map(gradeMapper::toResponse).toList(),
                weightedAverage(grades));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseGradebookResponse courseGradebook(UUID courseId, UUID requesterId, boolean requesterIsAdmin) {
        Course course = getCourseOrThrow(courseId);
        requireTeaches(course, requesterId, requesterIsAdmin);

        Map<UUID, List<Grade>> byStudent = gradeRepository.findByCourseIdOrderByGradedAtDesc(courseId).stream()
                .collect(Collectors.groupingBy(grade -> grade.getStudent().getId()));

        List<CourseGradebookResponse.StudentRow> rows = enrollmentRepository
                .findByCourseIdAndStatus(courseId, EnrollmentStatus.APPROVED).stream()
                .map(Enrollment::getStudent)
                .sorted(Comparator.comparing(GradeServiceImpl::fullName, String.CASE_INSENSITIVE_ORDER))
                .map(student -> {
                    List<Grade> grades = byStudent.getOrDefault(student.getId(), List.of());
                    return new CourseGradebookResponse.StudentRow(
                            student.getId(), fullName(student), student.getEmail(),
                            grades.stream().map(gradeMapper::toResponse).toList(),
                            weightedAverage(grades));
                })
                .toList();

        return new CourseGradebookResponse(course.getId(), course.getCode(), course.getName(), rows);
    }

    /**
     * Σ(score/maxScore × weight) / Σweight, as a percentage. Null for no
     * grades at all: an ungraded student is not a 0% student. Weights need
     * not add up to 100 - dividing by their total is what lets a course be
     * graded before every assessment has been set.
     */
    static Double weightedAverage(List<Grade> grades) {
        if (grades.isEmpty()) {
            return null;
        }
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Grade grade : grades) {
            weighted = weighted.add(grade.getScore()
                    .divide(grade.getMaxScore(), 6, RoundingMode.HALF_UP)
                    .multiply(grade.getWeight()));
            totalWeight = totalWeight.add(grade.getWeight());
        }
        return weighted.multiply(BigDecimal.valueOf(100))
                .divide(totalWeight, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static void apply(Grade grade, GradeRequest request) {
        grade.setAssessmentType(request.assessmentType());
        grade.setTitle(request.title());
        grade.setScore(request.score());
        grade.setMaxScore(request.maxScore());
        grade.setWeight(request.weight());
        grade.setComments(request.comments());
    }

    private static void validateScore(GradeRequest request) {
        if (request.score().compareTo(request.maxScore()) > 0) {
            throw new BadRequestException("score must not exceed maxScore");
        }
    }

    private User approvedStudent(UUID studentId, Course course) {
        return enrollmentRepository.findByCourseIdAndStatus(course.getId(), EnrollmentStatus.APPROVED).stream()
                .map(Enrollment::getStudent)
                .filter(student -> student.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Student " + studentId + " has no APPROVED enrollment in this course"));
    }

    /** The course's primary trainer, or an admin. */
    private static void requireTeaches(Course course, UUID requesterId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        User trainer = course.getPrimaryTrainer();
        if (trainer == null || !trainer.getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only grade courses you teach");
        }
    }

    /** The grader who recorded it, or an admin. */
    private static void requireOwnGrading(Grade grade, UUID requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !grade.getGradedBy().getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only change a grade you recorded");
        }
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private Course getCourseOrThrow(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("No course with id " + courseId));
    }

    private Grade getOrThrow(UUID id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No grade with id " + id));
    }
}
