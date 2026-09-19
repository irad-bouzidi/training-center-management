package com.tcm.grade.dto;

import java.util.List;

/**
 * A student's results, either across every course or narrowed to one.
 * {@code weightedAverage} is Σ(score/maxScore × weight) / Σweight as a
 * percentage, and null while they have no grades at all - an ungraded
 * student is not a 0% student.
 */
public record StudentGradesResponse(
        List<GradeResponse> grades,
        Double weightedAverage
) {
}
