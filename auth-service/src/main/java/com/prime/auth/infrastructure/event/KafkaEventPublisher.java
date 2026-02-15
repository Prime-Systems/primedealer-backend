package com.prime.auth.infrastructure.event;

import com.prime.common.event.DomainEvent;
import com.prime.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-based event publisher for auth events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    private static final String AUTH_EVENTS_TOPIC = "auth-events";

    @Override
    public void publish(DomainEvent event) {
        publish(AUTH_EVENTS_TOPIC, event);
    }

    @Override
    public void publish(String topic, DomainEvent event) {
        try {
            kafkaTemplate.send(topic, event.getAggregateId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event: type={}, aggregateId={}, error={}",
                                    event.getEventType(), event.getAggregateId(), ex.getMessage());
                        } else {
                            log.debug("Event published: type={}, aggregateId={}, partition={}",
                                    event.getEventType(), event.getAggregateId(),
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event: {}", e.getMessage(), e);
        }
    }
}
