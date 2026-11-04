package com.example.authservice.messaging;

import com.example.authservice.model.OutboxEvent;
import com.example.authservice.repository.OutboxEventRepository;
import com.example.user.api.event.AuthEventsConstants;
import com.example.user.api.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OutboxRelay {
    public enum Result { EMPTY, SENT, RETRY }

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final long confirmTimeoutMs;

    public OutboxRelay(OutboxEventRepository repository, ObjectMapper objectMapper,
                       RabbitTemplate rabbitTemplate,
                       @Value("${app.outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    // A bounded broker wait holds only the selected outbox row, not the account row.
    // A crash after delivery but before DB commit can redeliver the same event.
    @Transactional
    public Result publishNext() {
        OutboxEvent event = repository.lockNextPending().orElse(null);
        if (event == null) {
            return Result.EMPTY;
        }

        try {
            UserRegisteredEvent payload = objectMapper.readValue(event.getPayload(), UserRegisteredEvent.class);
            CorrelationData correlation = new CorrelationData(event.getId().toString());
            rabbitTemplate.convertAndSend(AuthEventsConstants.EXCHANGE,
                    AuthEventsConstants.USER_REGISTERED_KEY, payload, message -> {
                        message.getMessageProperties().setMessageId(event.getId().toString());
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setHeader("eventType", "UserRegistered");
                        message.getMessageProperties().setHeader("eventVersion", 1);
                        message.getMessageProperties().setHeader("occurredAt", event.getCreatedAt().toString());
                        return message;
                    }, correlation);

            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            // An ACK alone does not mean a queue received the message: check mandatory returns too.
            if (!confirm.isAck() || correlation.getReturned() != null) {
                event.retryLater();
                log.warn("Outbox event {} was rejected or unroutable; retry scheduled", event.getId());
                return Result.RETRY;
            }

            event.published();
            return Result.SENT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            event.retryLater();
            return Result.RETRY;
        } catch (Exception e) {
            event.retryLater();
            // Do not log the payload, which contains personal data.
            log.warn("Outbox event {} failed ({}); retry scheduled", event.getId(), e.getClass().getSimpleName());
            return Result.RETRY;
        }
    }
}
