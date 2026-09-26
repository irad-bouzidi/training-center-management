package com.tcm.enrollment;

import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<Enrollment> findByStudentId(UUID studentId);

    List<Enrollment> findByCourseId(UUID courseId);

    /** The APPROVED roster of a course. */
    List<Enrollment> findByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

    /**
     * The attendance roster and report of a course (TCM-19): APPROVED plus
     * COMPLETED, so a student who finished the course keeps their row.
     */
    List<Enrollment> findByCourseIdAndStatusIn(UUID courseId, Collection<EnrollmentStatus> statuses);

    /**
     * Whether a trainer teaches any course the student is enrolled in (any
     * enrollment status) - as its primary trainer, or as the trainer of one
     * of its sessions. The same notion of "teaches" as TCM-19's report
     * access check; gates a trainer's {@code GET /students/{id}/summary}.
     */
    @Query("""
            select case when count(e) > 0 then true else false end
            from Enrollment e
            join e.course c
            left join c.primaryTrainer pt
            where e.student.id = :studentId
              and (pt.id = :trainerId
                   or exists (select 1 from ClassSession s
                              where s.course.id = c.id and s.trainer.id = :trainerId))
            """)
    boolean existsByStudentIdTaughtBy(@Param("studentId") UUID studentId, @Param("trainerId") UUID trainerId);

    /** Used for course-capacity checks (count of APPROVED enrollments). */
    long countByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

    /** Used for the student directory's active-enrollment count. */
    long countByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    /** Guards {@code CourseServiceImpl#delete} against removing an enrolled-in course. */
    boolean existsByCourseId(UUID courseId);

    /**
     * The same count as {@link #countByCourseIdAndStatus} for a whole page of
     * courses at once, so {@code CourseServiceImpl#search} can fill each
     * {@code CourseResponse#approvedCount} without a query per row. Courses
     * with no matching enrollment are simply absent from the result.
     */
    @Query("""
            select e.course.id as courseId, count(e) as total
            from Enrollment e
            where e.course.id in :courseIds and e.status = :status
            group by e.course.id
            """)
    List<CourseStatusCount> countByCourseIdInAndStatus(@Param("courseIds") Collection<UUID> courseIds,
                                                        @Param("status") EnrollmentStatus status);

    /** Projection for {@link #countByCourseIdInAndStatus}. */
    interface CourseStatusCount {

        UUID getCourseId();

        long getTotal();
    }
}
