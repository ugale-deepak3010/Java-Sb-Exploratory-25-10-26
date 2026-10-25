package com.dbs.banking.transfer.service;

import com.dbs.banking.transfer.entity.OutboxEvent;
import com.dbs.banking.transfer.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * CRITICAL FOR 7+ YOE INTERVIEW:
 * Transactional Outbox Poller.
 * Avoids Dual-Write inconsistency between PostgreSQL and Kafka.
 * Poller drains pending events and forwards them reliably to Kafka.
 */
@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    public static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Keyed on aggregateId / account to guarantee partition-level ordering
                kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.debug("Published outbox event {} to Kafka topic {}", event.getId(), TRANSFER_EVENTS_TOPIC);
                            } else {
                                log.error("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
                            }
                        });

                event.setStatus("SENT");
                event.setSentAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error dispatching outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() > 5) {
                    event.setStatus("FAILED");
                }
                outboxEventRepository.save(event);
            }
        }
    }
}
