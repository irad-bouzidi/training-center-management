package com.tcm.payment.mapper;

import com.tcm.course.model.Course;
import com.tcm.payment.dto.PaymentResponse;
import com.tcm.payment.model.Payment;
import com.tcm.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                toStudentSummary(payment.getStudent()),
                toCourseSummary(payment.getCourse()),
                payment.getAmountDue(),
                payment.getAmountPaid(),
                payment.outstanding(),
                payment.getStatus(),
                payment.getDueDate(),
                payment.getPaidAt(),
                payment.getPaymentMethod(),
                payment.getNotes());
    }

    private static PaymentResponse.StudentSummary toStudentSummary(User student) {
        return new PaymentResponse.StudentSummary(
                student.getId(), student.getFirstName() + " " + student.getLastName(), student.getEmail());
    }

    private static PaymentResponse.CourseSummary toCourseSummary(Course course) {
        return new PaymentResponse.CourseSummary(course.getId(), course.getCode(), course.getName());
    }
}
