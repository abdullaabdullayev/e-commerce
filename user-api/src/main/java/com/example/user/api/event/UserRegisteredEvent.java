package com.example.user.api.event;

public record UserRegisteredEvent(
        Long accountId,
        String email,
        String firstName,
        String lastName
){}
