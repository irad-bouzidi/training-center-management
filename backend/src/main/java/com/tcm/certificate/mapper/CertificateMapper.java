package com.tcm.certificate.mapper;

import com.tcm.certificate.dto.CertificateResponse;
import com.tcm.certificate.model.Certificate;
import com.tcm.course.model.Course;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class CertificateMapper {

    public CertificateResponse toResponse(Certificate certificate) {
        return new CertificateResponse(
                certificate.getId(),
                toStudentSummary(certificate.getStudent()),
                toCourseSummary(certificate.getCourse()),
                certificate.getCertificateNumber(),
                certificate.getIssuedAt());
    }

    private static CertificateResponse.StudentSummary toStudentSummary(User student) {
        return new CertificateResponse.StudentSummary(
                student.getId(), student.getFirstName() + " " + student.getLastName(), student.getEmail());
    }

    private static CertificateResponse.CourseSummary toCourseSummary(Course course) {
        return new CertificateResponse.CourseSummary(course.getId(), course.getCode(), course.getName());
    }
}
