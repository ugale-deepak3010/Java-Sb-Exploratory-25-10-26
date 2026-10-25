package com.dbs.banking.notification.kafka;

import com.dbs.banking.common.events.TransferFailedEvent;
import com.dbs.banking.common.events.TransferSettledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Audit & Customer Notification Consumer.
 * Handles customer alerts and regulatory audit emission for DBS FAST transfers.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final ObjectMapper objectMapper;

    public NotificationEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transfer-settlement-events", groupId = "notification-service-group")
    public void onTransferSettled(String payload) {
        try {
            TransferSettledEvent event = objectMapper.readValue(payload, TransferSettledEvent.class);
            log.info(">> [SMS & PAYLAH PUSH] Transfer {}: Transferred {} {} from {} to {}. Settled at {}.",
                    event.transferId(), event.amount(), event.currency(),
                    event.sourceAccountNumber(), event.destinationAccountNumber(), event.settledAt());
        } catch (Exception e) {
            log.error("Failed to parse settlement notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transfer-failure-events", groupId = "notification-service-group")
    public void onTransferFailed(String payload) {
        try {
            TransferFailedEvent event = objectMapper.readValue(payload, TransferFailedEvent.class);
            log.warn(">> [SMS ALERT] Transfer {} FAILED: {} {} from {}. Reason: {}. Compensated: {}",
                    event.transferId(), event.amount(),
                    event.sourceAccountNumber(), event.reason(), event.compensationRequired());
        } catch (Exception e) {
            log.error("Failed to parse failure notification: {}", e.getMessage());
        }
    }
}
