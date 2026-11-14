package com.example.courseservice.storage;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.io.InputStream;

@Component
public class S3ObjectStorage implements ObjectStorage {
    private final MinioClient client;
    private final String bucket;

    public S3ObjectStorage(@Value("${courses.storage.endpoint}") String endpoint,
                           @Value("${courses.storage.access-key}") String accessKey,
                           @Value("${courses.storage.secret-key}") String secretKey,
                           @Value("${courses.storage.bucket}") String bucket) {
        if (accessKey.isBlank() || secretKey.length() < 32) {
            throw new IllegalArgumentException("Configure COURSE_STORAGE_ACCESS_KEY and a secret of at least 32 characters");
        }
        client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        client.setTimeout(5000, 60000, 60000);
        this.bucket = bucket;
    }

    private synchronized void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            try {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            } catch (io.minio.errors.ErrorResponseException exception) {
                // A second instance may have created our bucket between the HEAD and PUT.
                if (!"BucketAlreadyOwnedByYou".equals(exception.errorResponse().code())) throw exception;
            }
        }
    }

    @Override
    public void put(String key, InputStream input, long size, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(input, size, -1).contentType(contentType).build());
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    @Override
    public void remove(String key) throws Exception {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Material storage is temporarily unavailable");
    }
}
