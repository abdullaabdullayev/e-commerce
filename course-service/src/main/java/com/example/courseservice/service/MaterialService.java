package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.storage.ObjectStorage;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.util.UUID;
import static com.example.courseservice.dto.MaterialDtos.*;

@Service
public class MaterialService {
    public record Download(String filename, String contentType, long sizeBytes, InputStream content) {}

    private final MaterialTransactions transactions;
    private final MaterialValidator validator;
    private final ObjectStorage storage;

    public MaterialService(MaterialTransactions transactions, MaterialValidator validator, ObjectStorage storage) {
        this.transactions = transactions;
        this.validator = validator;
        this.storage = storage;
    }

    public MaterialResponse upload(Actor actor, UUID lessonId, String title, MultipartFile file) throws IOException {
        actor.requireAdmin();
        var info = validator.validate(file);
        var upload = transactions.beginUpload(actor, lessonId, title, info.filename(), info.contentType(), file.getSize());
        try (InputStream input = file.getInputStream()) {
            storage.put(upload.objectKey(), input, file.getSize(), info.contentType());
        } catch (Exception exception) {
            transactions.failUpload(upload.id());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Upload did not complete");
        }
        try {
            return transactions.finishUpload(actor, upload.id());
        } catch (RuntimeException exception) {
            transactions.failUpload(upload.id());
            throw exception;
        }
    }

    public MaterialResponse link(Actor actor, UUID lessonId, LinkRequest request) {
        actor.requireAdmin();
        validator.validateLink(request.url());
        return transactions.createLink(actor, lessonId, request.title(), request.url());
    }

    public MaterialResponse read(Actor actor, UUID id) {
        return transactions.read(actor, id);
    }

    public Download download(Actor actor, UUID id) {
        var file = transactions.download(actor, id);
        return new Download(file.filename(), file.contentType(), file.sizeBytes(), storage.get(file.objectKey()));
    }

    public void archive(Actor actor, UUID id) {
        transactions.archive(actor, id);
    }
}
