package com.tcm.grade;

import com.tcm.grade.model.Grade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, UUID> {

    /** A student's whole transcript, newest first. */
    List<Grade> findByStudentIdOrderByGradedAtDesc(UUID studentId);

    /** A student's results on one course. */
    List<Grade> findByStudentIdAndCourseIdOrderByGradedAtDesc(UUID studentId, UUID courseId);

    /** The gradebook: every result on one course. */
    List<Grade> findByCourseIdOrderByGradedAtDesc(UUID courseId);
}
