package com.tcm.attendance.mapper;

import com.tcm.attendance.dto.AttendanceResponse;
import com.tcm.attendance.model.AttendanceRecord;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(AttendanceRecord record) {
        return new AttendanceResponse(
                record.getId(),
                record.getSession().getId(),
                toStudentSummary(record.getStudent()),
                record.getStatus(),
                record.getMarkedAt(),
                record.getMethod());
    }

    public static AttendanceResponse.StudentSummary toStudentSummary(User student) {
        return new AttendanceResponse.StudentSummary(
                student.getId(), student.getFirstName() + " " + student.getLastName(), student.getEmail());
    }
}
