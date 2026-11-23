package com.example.userservice.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parent_student_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentStudentLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private long parentAccountId;
    @Column(nullable = false, updatable = false)
    private long studentAccountId;

    public ParentStudentLink(long parentAccountId, long studentAccountId) {
        this.parentAccountId = parentAccountId;
        this.studentAccountId = studentAccountId;
    }
}
