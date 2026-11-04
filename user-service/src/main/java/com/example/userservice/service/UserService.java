package com.example.userservice.service;

import com.example.user.api.event.UserRegisteredEvent;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void createProfileIfAbsent(UserRegisteredEvent event) {
        // PostgreSQL handles concurrent duplicate deliveries atomically.
        // Other constraint failures (e.g. conflicting email) still fail and reach the DLQ.
        userRepository.insertProfileIfAbsent(event.accountId(), event.email(),
                event.firstName(), event.lastName());
    }
}
