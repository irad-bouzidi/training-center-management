package com.tcm.grade.dto;

import java.util.List;
import java.util.UUID;

/**
 * The trainer's gradebook: every APPROVED-enrolled student on a course with
 * their results and weighted average. A student with nothing recorded yet
 * still gets a row, with an empty list and a null average - so the gaps are
 * as visible as the marks.
 */
public record CourseGradebookResponse(
        UUID courseId,
        String courseCode,
        String courseName,
        List<StudentRow> students
) {
    public record StudentRow(
            UUID studentId,
            String studentName,
            String email,
            List<GradeResponse> grades,
            Double weightedAverage
    ) {
    }
}
