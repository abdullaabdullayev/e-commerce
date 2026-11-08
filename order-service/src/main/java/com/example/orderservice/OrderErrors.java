package com.example.orderservice;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class OrderErrors {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Order or payment reference conflicts with an existing record");
    }
    @ExceptionHandler(RestClientException.class)
    public ProblemDetail upstream() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "A required service is temporarily unavailable");
    }
}
