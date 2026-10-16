package com.example.userservice.service;

import com.example.user.api.event.UserRegisteredEvent;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
