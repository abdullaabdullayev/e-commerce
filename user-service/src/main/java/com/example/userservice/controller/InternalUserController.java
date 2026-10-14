package com.example.userservice.controller;

import com.example.user.api.dto.CreateProfileRequest;
import com.example.user.api.dto.UserProfileDto;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/profiles")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileDto createProfile(@RequestBody CreateProfileRequest request) {
        return userService.createProfile(request);
    }
}

