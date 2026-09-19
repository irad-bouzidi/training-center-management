package com.tcm.grade;

import com.tcm.grade.dto.CourseGradebookResponse;
import com.tcm.grade.dto.GradeRequest;
import com.tcm.grade.dto.GradeResponse;
import com.tcm.grade.dto.StudentGradesResponse;
import java.util.UUID;

public interface GradeService {

    /**
     * Records a result. The student must hold an APPROVED enrollment in the
     * course, and the grader must be the course's trainer or an admin.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else may only grade a course they are the
     *                         primary trainer of.
     */
    GradeResponse create(GradeRequest request, UUID graderId, boolean requesterIsAdmin);

    /**
     * Amends a result. Only the grader who recorded it, or an admin, may:
     * a second trainer on the same course doesn't get to rewrite someone
     * else's marking. The student and course are fixed at creation; a grade
     * for the wrong pair is deleted, not moved.
     */
    GradeResponse update(UUID id, GradeRequest request, UUID requesterId, boolean requesterIsAdmin);

    /** Same access rule as {@link #update}. */
    void delete(UUID id, UUID requesterId, boolean requesterIsAdmin);

    /**
     * A student's results with their weighted average - across every course,
     * or narrowed to one when {@code courseId} is given.
     *
     * <p>Access is the caller's own record, a trainer of the course asked
     * about, or an admin. A trainer asking without a {@code courseId} would
     * be asking about courses that aren't theirs, so that combination is
     * refused rather than filtered.
     */
    StudentGradesResponse findForStudent(UUID studentId, UUID courseId, UUID requesterId, boolean requesterIsAdmin);

    /** The gradebook for a course: every approved student, graded or not. */
    CourseGradebookResponse courseGradebook(UUID courseId, UUID requesterId, boolean requesterIsAdmin);
}
