package com.tcm.schedule;

import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.schedule.dto.ClassSessionResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassSessionService {

    /**
     * Validates {@code endTime > startTime} and that {@code trainerId} is a
     * user with role TRAINER, then creates a SCHEDULED session - unless it
     * would double-book that trainer or that classroom, which is a
     * {@link com.tcm.common.ConflictException}.
     */
    ClassSessionResponse create(ClassSessionRequest request);

    /** Same validation as {@link #create}; the session being edited is not
     * counted as clashing with itself. */
    ClassSessionResponse update(UUID id, ClassSessionRequest request);

    /** SCHEDULED -&gt; CANCELLED. A session that already ran can't be cancelled. */
    ClassSessionResponse cancel(UUID id);

    /**
     * SCHEDULED -&gt; COMPLETED.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else may only complete a session they are the
     *                         assigned trainer of.
     */
    ClassSessionResponse markCompleted(UUID id, UUID requesterId, boolean requesterIsAdmin);

    /** {@code GET /api/v1/sessions} for an ADMIN (and, with {@code trainerId}
     * fixed to self, for a TRAINER). */
    Page<ClassSessionResponse> search(UUID courseId, UUID trainerId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * {@code GET /api/v1/sessions} for a STUDENT: restricted to sessions of
     * courses they hold an APPROVED enrollment in. {@code trainerId} isn't
     * honored for a student caller - it would only narrow what they may
     * already see, and the controller doesn't pass it.
     */
    Page<ClassSessionResponse> searchForStudent(UUID studentId, UUID courseId, LocalDate from, LocalDate to,
                                                  Pageable pageable);
}
