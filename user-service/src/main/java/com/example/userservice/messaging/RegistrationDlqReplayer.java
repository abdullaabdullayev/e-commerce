package com.example.userservice.messaging;

import com.example.userservice.config.RabbitConfig;
import com.rabbitmq.client.GetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Registrations that failed every retry land in the DLQ and would otherwise never get a profile.
 * An administrator can inspect the backlog and move messages back once the cause is fixed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationDlqReplayer {
    public record Backlog(long pending) {}
    public record Replayed(int moved, long remaining) {}

    private final RabbitTemplate rabbit;

    public Backlog backlog() {
        return new Backlog(rabbit.execute(channel -> channel.messageCount(RabbitConfig.DLQ)));
    }

    public Replayed replay(int max) {
        int moved = rabbit.execute(channel -> {
            int count = 0;
            while (count < max) {
                GetResponse message = channel.basicGet(RabbitConfig.DLQ, false);
                if (message == null) break;
                // Republish first, then acknowledge: a crash in between duplicates (harmless,
                // profile creation is idempotent) instead of losing the registration.
                channel.basicPublish("", RabbitConfig.QUEUE, message.getProps(), message.getBody());
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                count++;
            }
            return count;
        });
        if (moved > 0) log.info("Replayed {} user registration message(s) from the DLQ", moved);
        return new Replayed(moved, backlog().pending());
    }
}
