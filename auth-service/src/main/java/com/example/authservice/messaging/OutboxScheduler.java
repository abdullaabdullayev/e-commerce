package com.example.authservice.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxRelay relay;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
    public void publishPending() {
        for (int i = 0; i < 20; i++) {
            if (relay.publishNext() != OutboxRelay.Result.SENT) {
                return;
            }
        }
    }
}
