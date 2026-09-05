package com.tcm.enrollment;

import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<Enrollment> findByStudentId(UUID studentId);

    List<Enrollment> findByCourseId(UUID courseId);

    /** Used for course-capacity checks (count of APPROVED enrollments). */
    long countByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

    /** Used for the student directory's active-enrollment count. */
    long countByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    /** Guards {@code CourseServiceImpl#delete} against removing an enrolled-in course. */
    boolean existsByCourseId(UUID courseId);
}
