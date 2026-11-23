package com.example.courseservice.service;

import com.example.courseservice.dto.LearningDtos.LearningResponse;
import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.dto.PointDtos.PointHistory;
import com.example.courseservice.dto.ReportDtos.ReportView;
import com.example.courseservice.integration.ProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentLearningService {
    private final ProfileClient profiles;
    private final LearningService learning;
    private final PointService points;
    private final LessonReportService reports;

    public PageResponse<LearningResponse> courses(String token, long studentId, int page, int size) {
        profiles.requireParentLink(token, studentId);
        return learning.forStudent(studentId, page, size);
    }

    public PointHistory history(String token, UUID courseId, long studentId, int page, int size) {
        profiles.requireParentLink(token, studentId);
        return points.familyHistory(courseId, studentId, page, size);
    }

    public PageResponse<ReportView> reports(String token, long studentId, Pageable page) {
        profiles.requireParentLink(token, studentId);
        return reports.publishedForChild(studentId, page);
    }
}
