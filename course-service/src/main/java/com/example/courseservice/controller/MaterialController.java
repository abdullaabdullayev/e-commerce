package com.example.courseservice.controller;

import com.example.courseservice.security.Actor;
import com.example.courseservice.service.MaterialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static com.example.courseservice.dto.MaterialDtos.*;

@RestController
@RequestMapping({"/api/v1/course-materials", "/api/v1/admin/course-materials"})
@Validated
public class MaterialController {
    private final MaterialService materials;

    public MaterialController(MaterialService materials) {
        this.materials = materials;
    }

    @PostMapping(value = "/lessons/{lessonId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId,
                                   @RequestParam @NotBlank @Size(max = 160) String title,
                                   @RequestPart MultipartFile file) throws IOException {
        return materials.upload(Actor.from(jwt), lessonId, title, file);
    }

    @PostMapping("/lessons/{lessonId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse link(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId,
                                 @Valid @RequestBody LinkRequest request) {
        return materials.link(Actor.from(jwt), lessonId, request);
    }

    @GetMapping("/{id}")
    public MaterialResponse read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return materials.read(Actor.from(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        materials.archive(Actor.from(jwt), id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var file = materials.download(Actor.from(jwt), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType())).contentLength(file.sizeBytes())
                .body(new InputStreamResource(file.content()));
    }
}
