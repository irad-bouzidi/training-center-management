package com.tcm.grade.model;

import com.tcm.course.model.Course;
import com.tcm.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One assessment result for one student on one course, per docs/PLAN.md §5.
 * A student may have many per course - the final figure is the weighted
 * average of them (see {@code GradeServiceImpl#weightedAverage}).
 *
 * {@link #student}, {@link #course} and {@link #gradedBy} are all fetched
 * eagerly: every {@code GradeResponse} renders all three, and
 * {@code spring.jpa.open-in-view} is disabled.
 */
@Entity
@Table(name = "grades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private User student;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 20)
    private AssessmentType assessmentType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    /** This assessment's share of the course's final mark. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "graded_by", nullable = false)
    private User gradedBy;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt;

    @Column(columnDefinition = "text")
    private String comments;
}
