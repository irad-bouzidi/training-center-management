package com.tcm.schedule;

import com.tcm.common.BadRequestException;
import com.tcm.common.ConflictException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.schedule.dto.ClassSessionRequest;
import com.tcm.schedule.dto.ClassSessionResponse;
import com.tcm.schedule.mapper.ClassSessionMapper;
import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import com.tcm.schedule.spec.ClassSessionSpecifications;
import com.tcm.user.UserRepository;
import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassSessionServiceImpl implements ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ClassSessionMapper classSessionMapper;

    @Override
    public ClassSessionResponse create(ClassSessionRequest request) {
        validateTimes(request);
        Course course = resolveCourse(request.courseId());
        User trainer = resolveTrainer(request.trainerId());
        requireNoOverlap(request, null);

        ClassSession session = ClassSession.builder()
                .course(course)
                .trainer(trainer)
                .classroom(request.classroom())
                .sessionDate(request.sessionDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(SessionStatus.SCHEDULED)
                .build();
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    @Override
    public ClassSessionResponse update(UUID id, ClassSessionRequest request) {
        ClassSession session = getOrThrow(id);
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED sessions can be rescheduled");
        }
        validateTimes(request);
        Course course = resolveCourse(request.courseId());
        User trainer = resolveTrainer(request.trainerId());
        requireNoOverlap(request, id);

        session.setCourse(course);
        session.setTrainer(trainer);
        session.setClassroom(request.classroom());
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    @Override
    public ClassSessionResponse cancel(UUID id) {
        ClassSession session = getOrThrow(id);
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED sessions can be cancelled");
        }
        session.setStatus(SessionStatus.CANCELLED);
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    @Override
    public ClassSessionResponse markCompleted(UUID id, UUID requesterId, boolean requesterIsAdmin) {
        ClassSession session = getOrThrow(id);
        if (!requesterIsAdmin && !session.getTrainer().getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only complete a session you are assigned to");
        }
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED sessions can be marked completed");
        }
        session.setStatus(SessionStatus.COMPLETED);
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    @Override
    public Page<ClassSessionResponse> search(UUID courseId, UUID trainerId, LocalDate from, LocalDate to,
                                               Pageable pageable) {
        Specification<ClassSession> spec = Specification
                .where(ClassSessionSpecifications.hasCourse(courseId))
                .and(ClassSessionSpecifications.hasTrainer(trainerId))
                .and(ClassSessionSpecifications.from(from))
                .and(ClassSessionSpecifications.to(to));
        return classSessionRepository.findAll(spec, pageable).map(classSessionMapper::toResponse);
    }

    @Override
    public Page<ClassSessionResponse> searchForStudent(UUID studentId, UUID courseId, LocalDate from, LocalDate to,
                                                         Pageable pageable) {
        Specification<ClassSession> spec = Specification
                .where(ClassSessionSpecifications.courseApprovedForStudent(studentId))
                .and(ClassSessionSpecifications.hasCourse(courseId))
                .and(ClassSessionSpecifications.from(from))
                .and(ClassSessionSpecifications.to(to));
        return classSessionRepository.findAll(spec, pageable).map(classSessionMapper::toResponse);
    }

    private static void validateTimes(ClassSessionRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    /**
     * @param excludeId the session being updated, which mustn't be reported as
     *                  clashing with itself; null when creating.
     */
    private void requireNoOverlap(ClassSessionRequest request, UUID excludeId) {
        List<ClassSession> overlapping = classSessionRepository.findOverlapping(
                        request.sessionDate(), request.startTime(), request.endTime(),
                        request.trainerId(), request.classroom(), SessionStatus.CANCELLED)
                .stream()
                .filter(session -> !session.getId().equals(excludeId))
                .toList();

        // A single clash can be both at once (same trainer, same room); say so
        // rather than picking one, so the admin knows what to change.
        boolean trainerClash = overlapping.stream()
                .anyMatch(session -> session.getTrainer().getId().equals(request.trainerId()));
        boolean classroomClash = overlapping.stream()
                .anyMatch(session -> session.getClassroom().equalsIgnoreCase(request.classroom()));

        if (trainerClash && classroomClash) {
            throw new ConflictException(
                    "That trainer and classroom are both already booked for an overlapping session");
        }
        if (trainerClash) {
            throw new ConflictException("That trainer is already booked for an overlapping session");
        }
        if (classroomClash) {
            throw new ConflictException("That classroom is already booked for an overlapping session");
        }
    }

    private Course resolveCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("No course with id " + courseId));
    }

    private User resolveTrainer(UUID trainerId) {
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new BadRequestException("No user with id " + trainerId));
        if (trainer.getRole() != Role.TRAINER) {
            throw new BadRequestException("trainerId must reference a user with role TRAINER");
        }
        return trainer;
    }

    private ClassSession getOrThrow(UUID id) {
        return classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No session with id " + id));
    }
}
