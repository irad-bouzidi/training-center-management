package com.tcm.schedule.model;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One concrete dated/timed sitting of a course, per docs/PLAN.md §5.
 * {@link #course} and {@link #trainer} are fetched eagerly - every
 * {@code ClassSessionResponse} needs both summaries anyway, and
 * {@code spring.jpa.open-in-view} is disabled (same reasoning as
 * {@code Enrollment#student}).
 *
 * {@link #qrToken}/{@link #qrExpiresAt} are the columns TCM-27's QR
 * attendance flow will populate; nothing writes them yet.
 */
@Entity
@Table(name = "class_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSession {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @Column(nullable = false, length = 100)
    private String classroom;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "qr_token", length = 255)
    private String qrToken;

    @Column(name = "qr_expires_at")
    private Instant qrExpiresAt;
}
