package com.example.courseservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, updatable = false)
    private Lesson lesson;
    @Column(nullable = false, updatable = false)
    private long uploaderAccountId;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 10)
    private String kind;
    @Column(nullable = false, length = 20)
    private String visibility = "TEACHER_ONLY";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialState state;
    @Column(length = 200)
    private String objectKey;
    @Column(length = 200)
    private String filename;
    @Column(length = 100)
    private String contentType;
    private Long sizeBytes;
    @Column(length = 2048)
    private String url;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant cleanupStartedAt;

    public static Material file(Lesson lesson, long uploader, String title, String filename, String mime, long size) {
        Material material = create(lesson, uploader, title);
        material.kind = "FILE";
        material.state = MaterialState.PENDING;
        material.objectKey = "lessons/" + lesson.getId() + "/" + material.id;
        material.filename = filename;
        material.contentType = mime;
        material.sizeBytes = size;
        return material;
    }

    public static Material link(Lesson lesson, long uploader, String title, String url) {
        Material material = create(lesson, uploader, title);
        material.kind = "LINK";
        material.state = MaterialState.READY;
        material.url = url;
        return material;
    }

    private static Material create(Lesson lesson, long uploader, String title) {
        Material material = new Material();
        material.id = UUID.randomUUID();
        material.lesson = lesson;
        material.uploaderAccountId = uploader;
        material.title = title.trim();
        material.createdAt = Instant.now();
        return material;
    }

    public void publish() { state = MaterialState.READY; }
    public void fail() { state = MaterialState.FAILED; }
    public void archive() { state = MaterialState.ARCHIVED; }
    public void claimCleanup(Instant now) {
        state = MaterialState.CLEANING;
        cleanupStartedAt = now;
    }
}
