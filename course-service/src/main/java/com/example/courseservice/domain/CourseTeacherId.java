package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CourseTeacherId implements Serializable {
    @Column(name = "course_id")
    private UUID courseId;
    @Column(name = "account_id")
    private long accountId;
}
