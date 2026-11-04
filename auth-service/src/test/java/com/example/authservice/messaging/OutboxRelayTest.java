package com.example.authservice.messaging;

import com.example.authservice.model.OutboxEvent;
import com.example.authservice.repository.OutboxEventRepository;
import com.example.user.api.event.AuthEventsConstants;
import com.example.user.api.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxRelayTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final OutboxRelay relay = new OutboxRelay(repository, mapper, rabbit, 20);
    private OutboxEvent event;

    @BeforeEach
    void prepare() throws Exception {
        event = OutboxEvent.pending(mapper.writeValueAsString(
                new UserRegisteredEvent(1L, "parent@example.test", "Test", "Parent")));
        when(repository.lockNextPending()).thenReturn(Optional.of(event));
    }

    @Test
    void marksPublishedOnlyAfterBrokerAckAndPreservesDeliveryIdentity() {
        doAnswer(invocation -> {
            Message message = invocation.<MessagePostProcessor>getArgument(3)
                    .postProcessMessage(new Message(new byte[0], new MessageProperties()));
            CorrelationData correlation = invocation.getArgument(4);
            assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.getId().toString());
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(correlation.getId()).isEqualTo(event.getId().toString());
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq(AuthEventsConstants.EXCHANGE), eq(AuthEventsConstants.USER_REGISTERED_KEY),
                any(UserRegisteredEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThat(relay.publishNext()).isEqualTo(OutboxRelay.Result.SENT);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void brokerOutageLeavesEventPendingForLaterDelivery() {
        doThrow(new AmqpConnectException(new ConnectException("offline")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(UserRegisteredEvent.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertPendingRetry();
    }

    @Test
    void ackWithoutDestinationQueueDoesNotMarkPublished() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.setReturned(new ReturnedMessage(new Message(new byte[0]), 312,
                    "NO_ROUTE", AuthEventsConstants.EXCHANGE, AuthEventsConstants.USER_REGISTERED_KEY));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(UserRegisteredEvent.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertPendingRetry();
    }

    @Test
    void negativeAckLeavesEventPending() {
        doAnswer(invocation -> {
            invocation.<CorrelationData>getArgument(4).getFuture()
                    .complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(UserRegisteredEvent.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertPendingRetry();
    }

    @Test
    void missingConfirmationTimesOutWithoutLosingEvent() {
        assertPendingRetry();
    }

    private void assertPendingRetry() {
        assertThat(relay.publishNext()).isEqualTo(OutboxRelay.Result.RETRY);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(event.getCreatedAt());
    }
}
