package com.example.userservice.service;

import com.example.user.api.dto.CreateProfileRequest;
import com.example.user.api.dto.UserProfileDto;
import com.example.user.api.event.UserRegisteredEvent;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileDto createProfile(CreateProfileRequest request) {

        User user = User.builder()
                .accountId(request.getAccountId())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        User savedUser = userRepository.save(user);

        return UserProfileDto.builder()
                .id(savedUser.getId())
                .accountId(savedUser.getAccountId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    public void createProfileIfAbsent(UserRegisteredEvent event) {

        if(userRepository.findByAccountId(event.accountId()).isPresent()) {
            return;
        }

        userRepository.save(User.builder()
                .accountId(event.accountId())
                .email(event.email())
                .firstName(event.firstName())
                .lastName(event.lastName())
                .build());
    }
}
