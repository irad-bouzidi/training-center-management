package com.tcm.attendance;

import com.tcm.attendance.dto.AttendanceMarkRequest;
import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.dto.CourseAttendanceReportResponse;
import com.tcm.attendance.dto.SessionRosterResponse;
import com.tcm.attendance.mapper.AttendanceMapper;
import com.tcm.attendance.model.AttendanceMethod;
import com.tcm.attendance.model.AttendanceRecord;
import com.tcm.attendance.model.AttendanceStatus;
import com.tcm.common.BadRequestException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.course.CourseRepository;
import com.tcm.course.model.Course;
import com.tcm.enrollment.EnrollmentRepository;
import com.tcm.enrollment.model.Enrollment;
import com.tcm.enrollment.model.EnrollmentStatus;
import com.tcm.schedule.ClassSessionRepository;
import com.tcm.schedule.mapper.ClassSessionMapper;
import com.tcm.schedule.model.ClassSession;
import com.tcm.user.UserRepository;
import com.tcm.user.model.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;
    private final ClassSessionMapper classSessionMapper;

    @Override
    @Transactional(readOnly = true)
    public SessionRosterResponse getRoster(UUID sessionId, UUID requesterId, boolean requesterIsAdmin) {
        ClassSession session = getSessionOrThrow(sessionId);
        requireSessionAccess(session, requesterId, requesterIsAdmin);

        Map<UUID, AttendanceRecord> marks = new HashMap<>();
        attendanceRepository.findBySessionId(sessionId)
                .forEach(record -> marks.put(record.getStudent().getId(), record));

        List<SessionRosterResponse.Entry> entries = approvedStudents(session.getCourse().getId()).stream()
                .map(student -> {
                    AttendanceRecord record = marks.get(student.getId());
                    return new SessionRosterResponse.Entry(
                            student.getId(),
                            fullName(student),
                            student.getEmail(),
                            record == null ? null : record.getStatus(),
                            record == null ? null : record.getMethod(),
                            record == null ? null : record.getMarkedAt());
                })
                .toList();

        return new SessionRosterResponse(classSessionMapper.toResponse(session), entries);
    }

    @Override
    @Transactional
    public AttendanceResponse markOne(UUID sessionId, UUID studentId, AttendanceStatus status, UUID markerId,
                                       boolean requesterIsAdmin) {
        ClassSession session = getSessionOrThrow(sessionId);
        requireSessionAccess(session, markerId, requesterIsAdmin);
        return attendanceMapper.toResponse(
                upsert(session, studentId, status, markerId, AttendanceMethod.MANUAL, approvedStudentIndex(session)));
    }

    @Override
    @Transactional
    public List<AttendanceResponse> markBulk(UUID sessionId, List<AttendanceMarkRequest> entries, UUID markerId,
                                              boolean requesterIsAdmin) {
        ClassSession session = getSessionOrThrow(sessionId);
        requireSessionAccess(session, markerId, requesterIsAdmin);

        Map<UUID, User> enrolled = approvedStudentIndex(session);
        List<AttendanceResponse> saved = new ArrayList<>(entries.size());
        for (AttendanceMarkRequest entry : entries) {
            saved.add(attendanceMapper.toResponse(
                    upsert(session, entry.studentId(), entry.status(), markerId, AttendanceMethod.MANUAL, enrolled)));
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseAttendanceReportResponse courseAttendanceReport(UUID courseId, UUID requesterId,
                                                                   boolean requesterIsAdmin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("No course with id " + courseId));
        if (!requesterIsAdmin && !teachesCourse(course, requesterId)) {
            throw new AccessDeniedException("You may only report on a course you teach");
        }

        Map<UUID, Map<AttendanceStatus, Long>> tallies = new HashMap<>();
        for (AttendanceRepository.StatusCount row : attendanceRepository
                .countByCourseGroupedByStudentAndStatus(courseId)) {
            tallies.computeIfAbsent(row.getStudentId(), key -> new EnumMap<>(AttendanceStatus.class))
                    .put(row.getStatus(), row.getTotal());
        }

        List<CourseAttendanceReportResponse.StudentRow> rows = approvedStudents(courseId).stream()
                .map(student -> {
                    Map<AttendanceStatus, Long> tally = tallies.getOrDefault(student.getId(), Map.of());
                    long present = tally.getOrDefault(AttendanceStatus.PRESENT, 0L);
                    long absent = tally.getOrDefault(AttendanceStatus.ABSENT, 0L);
                    long late = tally.getOrDefault(AttendanceStatus.LATE, 0L);
                    return new CourseAttendanceReportResponse.StudentRow(
                            student.getId(), fullName(student), student.getEmail(),
                            present, absent, late, present + absent + late,
                            attendanceRate(present, absent, late));
                })
                .toList();

        return new CourseAttendanceReportResponse(
                course.getId(), course.getCode(), course.getName(),
                (int) classSessionRepository.countByCourseId(courseId), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Double studentAttendanceSummary(UUID studentId) {
        Map<AttendanceStatus, Long> tally = new EnumMap<>(AttendanceStatus.class);
        attendanceRepository.countByStudentGroupedByStatus(studentId)
                .forEach(row -> tally.put(row.getStatus(), row.getTotal()));
        return attendanceRate(
                tally.getOrDefault(AttendanceStatus.PRESENT, 0L),
                tally.getOrDefault(AttendanceStatus.ABSENT, 0L),
                tally.getOrDefault(AttendanceStatus.LATE, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public Double studentCourseAttendanceRate(UUID studentId, UUID courseId) {
        Map<AttendanceStatus, Long> tally = new EnumMap<>(AttendanceStatus.class);
        attendanceRepository.countByStudentAndCourseGroupedByStatus(studentId, courseId)
                .forEach(row -> tally.put(row.getStatus(), row.getTotal()));
        return attendanceRate(
                tally.getOrDefault(AttendanceStatus.PRESENT, 0L),
                tally.getOrDefault(AttendanceStatus.ABSENT, 0L),
                tally.getOrDefault(AttendanceStatus.LATE, 0L));
    }

    /**
     * Creates the record or overwrites the existing one - the (session,
     * student) pair is unique, so marking twice corrects a mark instead of
     * stacking up rows. The marker and the timestamp are refreshed too, so
     * the roster always shows who last said what.
     */
    private AttendanceRecord upsert(ClassSession session, UUID studentId, AttendanceStatus status, UUID markerId,
                                     AttendanceMethod method, Map<UUID, User> approvedStudents) {
        User student = approvedStudents.get(studentId);
        if (student == null) {
            throw new BadRequestException(
                    "Student " + studentId + " has no APPROVED enrollment in this session's course");
        }
        AttendanceRecord record = attendanceRepository
                .findBySessionIdAndStudentId(session.getId(), studentId)
                .orElseGet(() -> AttendanceRecord.builder()
                        .session(session)
                        .student(student)
                        .build());
        record.setStatus(status);
        record.setMethod(method);
        record.setMarkedAt(Instant.now());
        // A reference, not a fetch: the marker is the authenticated caller, so
        // the row is known to exist and nothing reads it back on this path.
        record.setMarkedBy(markerId == null ? null : userRepository.getReferenceById(markerId));
        return attendanceRepository.save(record);
    }

    /** Null while nothing is marked - an unmarked student is not a 0% student. */
    private static Double attendanceRate(long present, long absent, long late) {
        long marked = present + absent + late;
        if (marked == 0) {
            return null;
        }
        return Math.round((present + late) * 1000.0 / marked) / 10.0;
    }

    private List<User> approvedStudents(UUID courseId) {
        return enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.APPROVED).stream()
                .map(Enrollment::getStudent)
                .sorted(Comparator.comparing(AttendanceServiceImpl::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<UUID, User> approvedStudentIndex(ClassSession session) {
        Map<UUID, User> index = new HashMap<>();
        approvedStudents(session.getCourse().getId()).forEach(student -> index.put(student.getId(), student));
        return index;
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private ClassSession getSessionOrThrow(UUID sessionId) {
        return classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No session with id " + sessionId));
    }

    private static void requireSessionAccess(ClassSession session, UUID requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !session.getTrainer().getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only manage attendance for a session you are assigned to");
        }
    }

    /** The course's primary trainer, or a trainer of any of its sessions. */
    private boolean teachesCourse(Course course, UUID trainerId) {
        if (course.getPrimaryTrainer() != null && course.getPrimaryTrainer().getId().equals(trainerId)) {
            return true;
        }
        return classSessionRepository.existsByCourseIdAndTrainerId(course.getId(), trainerId);
    }
}
