package com.prime.common.event;

/**
 * Event publisher interface for domain events.
 * Implementations can use Kafka, Redis Streams, or in-memory for testing.
 */
public interface EventPublisher {

    /**
     * Publish a domain event asynchronously.
     */
    void publish(DomainEvent event);

    /**
     * Publish a domain event to a specific topic.
     */
    void publish(String topic, DomainEvent event);
}
