package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.*;
import com.example.courseservice.dto.CourseMapper;
import com.example.courseservice.dto.MaterialDtos.MaterialResponse;
import com.example.courseservice.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.example.courseservice.exception.DomainException.*;

@Service
@Transactional(readOnly = true)
public class MaterialTransactions {
    public record StoredFile(UUID id, String objectKey, String filename, String contentType, long sizeBytes) {}
    public record CleanupTicket(UUID id, String objectKey, Instant claimedAt) {}

    private final CourseAccess access;
    private final LessonRepository lessons;
    private final MaterialRepository materials;

    public MaterialTransactions(CourseAccess access, LessonRepository lessons, MaterialRepository materials) {
        this.access = access;
        this.lessons = lessons;
        this.materials = materials;
    }

    public MaterialResponse read(Actor actor, UUID id) {
        return CourseMapper.material(readable(actor, id));
    }

    public StoredFile download(Actor actor, UUID id) {
        Material material = readable(actor, id);
        if (!"FILE".equals(material.getKind())) throw badRequest("This material is a link");
        return file(material);
    }

    private Material readable(Actor actor, UUID id) {
        UUID courseId = materialCourseId(id);
        access.requireStaff(actor, courseId);
        Material material = requireMaterial(id);
        if (material.getState() != MaterialState.READY) throw notFound("Material");
        return material;
    }

    @Transactional
    public StoredFile beginUpload(Actor actor, UUID lessonId, String title, String filename, String mime, long size) {
        Lesson lesson = editableLesson(actor, lessonId);
        checkCapacity(lessonId);
        return file(materials.save(Material.file(lesson, actor.id(), title, filename, mime, size)));
    }

    @Transactional
    public MaterialResponse createLink(Actor actor, UUID lessonId, String title, String url) {
        Lesson lesson = editableLesson(actor, lessonId);
        checkCapacity(lessonId);
        return CourseMapper.material(materials.save(Material.link(lesson, actor.id(), title, url)));
    }

    @Transactional
    public MaterialResponse finishUpload(Actor actor, UUID id) {
        access.lockAdminEditable(actor, materialCourseId(id));
        Material material = requireMaterial(id);
        if (material.getLesson().isArchived() || material.getState() != MaterialState.PENDING) {
            throw conflict("Upload can no longer be published");
        }
        material.publish();
        return CourseMapper.material(material);
    }

    @Transactional
    public void failUpload(UUID id) {
        access.lock(materialCourseId(id));
        Material material = requireMaterial(id);
        if (material.getState() == MaterialState.PENDING) material.fail();
    }

    @Transactional
    public void archive(Actor actor, UUID id) {
        actor.requireAdmin();
        access.lock(materialCourseId(id));
        Material material = requireMaterial(id);
        // In-flight files must still be removed by the cleanup worker.
        if (material.getState() == MaterialState.PENDING) material.fail();
        else if (material.getState() == MaterialState.READY) material.archive();
    }

    public List<UUID> cleanupCandidates() {
        Instant now = Instant.now();
        return materials.findCleanupCandidates(now.minus(1, ChronoUnit.HOURS),
                now.minus(10, ChronoUnit.MINUTES), PageRequest.of(0, 50));
    }

    @Transactional
    public Optional<CleanupTicket> claimCleanup(UUID id) {
        access.lock(materialCourseId(id));
        Material material = requireMaterial(id);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        boolean eligible = material.getState() == MaterialState.FAILED
                || material.getState() == MaterialState.PENDING && material.getCreatedAt().isBefore(now.minus(1, ChronoUnit.HOURS))
                || material.getState() == MaterialState.CLEANING && material.getCleanupStartedAt().isBefore(now.minus(10, ChronoUnit.MINUTES));
        if (!eligible) return Optional.empty();
        material.claimCleanup(now);
        return Optional.of(new CleanupTicket(id, material.getObjectKey(), now));
    }

    @Transactional
    public void completeCleanup(CleanupTicket ticket) {
        access.lock(materialCourseId(ticket.id()));
        Material material = requireMaterial(ticket.id());
        if (material.getState() == MaterialState.CLEANING && ticket.claimedAt().equals(material.getCleanupStartedAt())) {
            material.archive();
        }
    }

    private Lesson editableLesson(Actor actor, UUID lessonId) {
        actor.requireAdmin();
        UUID courseId = lessons.findCourseId(lessonId).orElseThrow(() -> notFound("Lesson"));
        access.lockAdminEditable(actor, courseId);
        Lesson lesson = lessons.findById(lessonId).orElseThrow(() -> notFound("Lesson"));
        if (lesson.isArchived()) throw conflict("Lesson is archived");
        return lesson;
    }

    private void checkCapacity(UUID lessonId) {
        if (materials.countByLessonIdAndStateIn(lessonId, List.of(MaterialState.PENDING, MaterialState.READY)) >= 100) {
            throw conflict("A lesson supports up to 100 materials");
        }
    }

    private UUID materialCourseId(UUID id) {
        return materials.findCourseId(id).orElseThrow(() -> notFound("Material"));
    }

    private Material requireMaterial(UUID id) {
        return materials.findById(id).orElseThrow(() -> notFound("Material"));
    }

    private StoredFile file(Material material) {
        return new StoredFile(material.getId(), material.getObjectKey(), material.getFilename(),
                material.getContentType(), material.getSizeBytes());
    }
}
