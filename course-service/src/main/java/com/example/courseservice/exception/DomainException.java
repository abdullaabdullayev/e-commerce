package com.example.courseservice.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
    private final HttpStatus status;

    public DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
    public static DomainException notFound(String resource) {
        return new DomainException(HttpStatus.NOT_FOUND, resource + " not found");
    }
    public static DomainException forbidden() {
        return new DomainException(HttpStatus.FORBIDDEN, "Access denied");
    }
    public static DomainException conflict(String message) {
        return new DomainException(HttpStatus.CONFLICT, message);
    }
    public static DomainException badRequest(String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, message);
    }
}
