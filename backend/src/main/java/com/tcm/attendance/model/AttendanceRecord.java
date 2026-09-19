package com.tcm.attendance.model;

import com.tcm.schedule.model.ClassSession;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One student's attendance at one session, per docs/PLAN.md §5. There is at
 * most one row per (session, student) - marking the same student twice
 * updates this row rather than adding another, which the unique constraint
 * enforces at the database as well.
 *
 * {@link #session} and {@link #student} are fetched eagerly: every read of a
 * record renders both (roster rows, report rows), and
 * {@code spring.jpa.open-in-view} is disabled. {@link #markedBy} is lazy and
 * nullable - nothing renders the marker, and a QR self check-in (TCM-27) has
 * none.
 */
@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "marked_at", nullable = false)
    private Instant markedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private User markedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceMethod method;
}
