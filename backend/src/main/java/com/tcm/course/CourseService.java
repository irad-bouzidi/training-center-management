package com.tcm.course;

import com.tcm.course.dto.CourseRequest;
import com.tcm.course.dto.CourseResponse;
import com.tcm.course.model.CourseStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    CourseResponse create(CourseRequest request);
    CourseResponse update(UUID id, CourseRequest request);
    CourseResponse changeStatus(UUID id, CourseStatus status);
    CourseResponse findById(UUID id);
    Page<CourseResponse> search(CourseStatus status, String category, UUID trainerId, String query, Pageable pageable);
    Page<CourseResponse> findMine(UUID trainerId, Pageable pageable);
    void delete(UUID id);
}
