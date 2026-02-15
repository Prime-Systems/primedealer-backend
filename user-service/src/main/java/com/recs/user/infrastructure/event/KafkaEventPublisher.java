package com.prime.user.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prime.common.event.DomainEvent;
import com.prime.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-based event publisher implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_TOPIC = "user-events";

    @Override
    public void publish(DomainEvent event) {
        publish(DEFAULT_TOPIC, event);
    }

    @Override
    public void publish(String topic, DomainEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            kafkaTemplate.send(topic, event.getAggregateId(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event: eventId={}, type={}, error={}",
                                    event.getEventId(), event.getEventType(), ex.getMessage());
                        } else {
                            log.debug("Event published: eventId={}, type={}, topic={}, partition={}",
                                    event.getEventId(), event.getEventType(), 
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize event: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }
}
