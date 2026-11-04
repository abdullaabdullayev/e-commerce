package com.example.authservice.messaging;

import com.example.authservice.model.OutboxEvent;
import com.example.authservice.repository.OutboxEventRepository;
import com.example.user.api.event.UserRegisteredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishUserRegistered(UserRegisteredEvent event){
        try {
            outboxRepository.save(OutboxEvent.pending(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize user registration event", e);
        }
    }
}
