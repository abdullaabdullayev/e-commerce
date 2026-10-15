package com.example.userservice.messaging;

import com.example.user.api.event.UserRegisteredEvent;
import com.example.userservice.config.RabbitConfig;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final UserService userService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handle(UserRegisteredEvent event){
        userService.createProfileIfAbsent(event);
    }
}
