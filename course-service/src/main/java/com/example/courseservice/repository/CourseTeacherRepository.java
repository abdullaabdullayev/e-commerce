package com.example.courseservice.repository;

import com.example.courseservice.domain.CourseTeacher;
import com.example.courseservice.domain.CourseTeacherId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, CourseTeacherId> {
    boolean existsByIdAndActiveTrue(CourseTeacherId id);
    List<CourseTeacher> findByIdCourseIdAndActiveTrueOrderByIdAccountId(UUID courseId);
    List<CourseTeacher> findByIdCourseIdInAndActiveTrue(Collection<UUID> courseIds);
}
