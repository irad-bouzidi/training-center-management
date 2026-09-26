package com.tcm.attendance.mapper;

import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.dto.MyAttendanceResponse;
import com.tcm.attendance.model.AttendanceRecord;
import com.tcm.course.model.Course;
import com.tcm.schedule.model.ClassSession;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(AttendanceRecord record) {
        ClassSession session = record.getSession();
        Course course = session.getCourse();
        return new AttendanceResponse(
                record.getId(),
                session.getId(),
                toStudentSummary(record.getStudent()),
                record.getStatus(),
                record.getMarkedAt(),
                record.getMethod(),
                course == null ? null : course.getId(),
                course == null ? null : course.getName(),
                session.getSessionDate());
    }

    public MyAttendanceResponse toMyResponse(AttendanceRecord record) {
        ClassSession session = record.getSession();
        Course course = session.getCourse();
        return new MyAttendanceResponse(
                record.getId(),
                session.getId(),
                course.getId(),
                course.getCode(),
                course.getName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                record.getStatus(),
                record.getMethod(),
                record.getMarkedAt());
    }

    public static AttendanceResponse.StudentSummary toStudentSummary(User student) {
        return new AttendanceResponse.StudentSummary(
                student.getId(), student.getFirstName() + " " + student.getLastName(), student.getEmail());
    }
}
