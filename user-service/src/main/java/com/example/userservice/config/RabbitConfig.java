package com.example.userservice.config;

import com.example.user.api.event.AuthEventsConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "user-service.user-registered";
    public static final String DLX = "auth.events.dlx";
    public static final String DLQ = "user-service.user-registered.dlq";


    @Bean
    TopicExchange authEventExchange() {
        return new TopicExchange(AuthEventsConstants.EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue userRegisterQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue userRegisteredDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding userRegisterBinding() {
        return BindingBuilder.bind(userRegisterQueue())
                .to(authEventExchange())
                .with(AuthEventsConstants.USER_REGISTERED_KEY);
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(userRegisteredDlq())
                .to(deadLetterExchange())
                .with(DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
