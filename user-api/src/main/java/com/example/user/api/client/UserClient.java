package com.example.user.api.client;

import com.example.user.api.dto.CreateProfileRequest;
import com.example.user.api.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserClient {
    @PostMapping("/internal/profiles")
    UserProfileDto createProfile(@RequestBody CreateProfileRequest request);
}
