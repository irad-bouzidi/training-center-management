package com.tcm.schedule;

import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Finding sessions by course, trainer or date range is
 * {@code ClassSessionServiceImpl#search}'s job, via
 * {@code ClassSessionSpecifications} and {@link JpaSpecificationExecutor} -
 * composable and paginated, which derived finders here would not be. Overlap
 * detection is the one read specifications can't express readably, so it
 * lives here.
 */
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>,
        JpaSpecificationExecutor<ClassSession> {

    /** How many sessions a course has, for TCM-19's course attendance report. */
    long countByCourseId(UUID courseId);

    /** Whether a trainer is assigned to any session of a course - one half of
     * "does this trainer teach this course?" in TCM-19's report access check. */
    boolean existsByCourseIdAndTrainerId(UUID courseId, UUID trainerId);

    /**
     * Sessions on the same day that would double-book either the trainer or
     * the classroom: two ranges overlap when each starts before the other
     * ends, and touching at an endpoint (one ends exactly when the next
     * starts) deliberately doesn't count.
     *
     * CANCELLED sessions are ignored - a cancelled session frees its room and
     * its trainer. The classroom match is case-insensitive so "Room A" and
     * "room a" can't be booked as if they were different places.
     *
     * The caller gets the clashing rows rather than a boolean so it can say
     * which of the two constraints was hit, and so an update can drop the
     * session being edited from its own result (see
     * {@code ClassSessionServiceImpl#requireNoOverlap}).
     */
    @Query("""
            select s from ClassSession s
            where s.status <> :ignoredStatus
              and s.sessionDate = :sessionDate
              and (s.trainer.id = :trainerId or upper(s.classroom) = upper(:classroom))
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    List<ClassSession> findOverlapping(@Param("sessionDate") LocalDate sessionDate,
                                        @Param("startTime") LocalTime startTime,
                                        @Param("endTime") LocalTime endTime,
                                        @Param("trainerId") UUID trainerId,
                                        @Param("classroom") String classroom,
                                        @Param("ignoredStatus") SessionStatus ignoredStatus);
}
