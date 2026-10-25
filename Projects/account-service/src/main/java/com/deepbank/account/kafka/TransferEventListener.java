package com.deepbank.account.kafka;

import com.deepbank.account.service.AccountService;
import com.deepbank.common.events.TransferFailedEvent;
import com.deepbank.common.events.TransferRequestedEvent;
import com.deepbank.common.events.TransferSettledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * CRITICAL FOR 7+ YOE INTERVIEW:
 * Saga Step Choreography in Account Service.
 * Consumes TransferRequestedEvent -> executes debit on source account -> executes credit on destination account.
 * Publishes settlement or failure events back onto Kafka.
 */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);
    public static final String SETTLEMENT_TOPIC = "transfer-settlement-events";
    public static final String FAILURE_TOPIC = "transfer-failure-events";

    private final AccountService accountService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TransferEventListener(AccountService accountService,
                                 KafkaTemplate<String, Object> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transfer-events", groupId = "account-service-group")
    public void handleTransferRequested(String payload) {
        try {
            TransferRequestedEvent event = objectMapper.readValue(payload, TransferRequestedEvent.class);
            log.info("Processing transfer request event {} for transfer {}", event.eventId(), event.transferId());

            boolean debited = false;
            try {
                // 1. Debit Source Account
                accountService.debit(event.transferId(), event.sourceAccountNumber(), event.amount(),
                        "Transfer to " + event.destinationAccountNumber());
                debited = true;

                // 2. Credit Destination Account
                accountService.credit(event.transferId(), event.destinationAccountNumber(), event.amount(),
                        "Transfer from " + event.sourceAccountNumber());

                // 3. Emit Settlement Event
                TransferSettledEvent settledEvent = new TransferSettledEvent(
                        UUID.randomUUID().toString(),
                        event.transferId(),
                        event.sourceAccountNumber(),
                        event.destinationAccountNumber(),
                        event.amount(),
                        event.currency(),
                        Instant.now()
                );
                kafkaTemplate.send(SETTLEMENT_TOPIC, event.transferId(), objectMapper.writeValueAsString(settledEvent));
                log.info("Transfer {} successfully settled and event published", event.transferId());

            } catch (Exception ex) {
                log.error("Transfer {} failed during execution: {}", event.transferId(), ex.getMessage());

                // Compensating action if debit succeeded but credit failed
                if (debited) {
                    try {
                        log.warn("Triggering compensation refund for source account {}", event.sourceAccountNumber());
                        accountService.credit(event.transferId() + "-COMP", event.sourceAccountNumber(),
                                event.amount(), "Refund for failed transfer: " + event.transferId());
                    } catch (Exception compEx) {
                        log.error("CRITICAL: Compensation failed for transfer {}: {}", event.transferId(), compEx.getMessage());
                    }
                }

                // Emit Failure Event
                TransferFailedEvent failedEvent = new TransferFailedEvent(
                        UUID.randomUUID().toString(),
                        event.transferId(),
                        event.sourceAccountNumber(),
                        event.destinationAccountNumber(),
                        event.amount(),
                        ex.getMessage(),
                        debited,
                        Instant.now()
                );
                kafkaTemplate.send(FAILURE_TOPIC, event.transferId(), objectMapper.writeValueAsString(failedEvent));
            }

        } catch (Exception e) {
            log.error("Fatal error deserializing transfer event: {}", e.getMessage(), e);
            throw new RuntimeException("Deser error triggers DLT retry", e);
        }
    }
}
