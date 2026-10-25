package com.dbs.banking.transfer.kafka;

import com.dbs.banking.common.enums.TransferStatus;
import com.dbs.banking.common.events.TransferFailedEvent;
import com.dbs.banking.common.events.TransferSettledEvent;
import com.dbs.banking.transfer.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * CRITICAL FOR 7+ YOE INTERVIEW:
 * Saga Orchestrator Status Resolver in Transfer Service.
 * Listens to settlement and failure events from Kafka to update final transaction status.
 */
@Component
public class SagaResponseListener {

    private static final Logger log = LoggerFactory.getLogger(SagaResponseListener.class);

    private final TransferService transferService;
    private final ObjectMapper objectMapper;

    public SagaResponseListener(TransferService transferService, ObjectMapper objectMapper) {
        this.transferService = transferService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transfer-settlement-events", groupId = "transfer-service-group")
    public void handleSettlement(String payload) {
        try {
            TransferSettledEvent event = objectMapper.readValue(payload, TransferSettledEvent.class);
            log.info("Received TransferSettledEvent for transfer {}", event.transferId());
            transferService.updateTransferStatus(event.transferId(), TransferStatus.SETTLED, null);
        } catch (Exception e) {
            log.error("Error processing settlement event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "transfer-failure-events", groupId = "transfer-service-group")
    public void handleFailure(String payload) {
        try {
            TransferFailedEvent event = objectMapper.readValue(payload, TransferFailedEvent.class);
            log.warn("Received TransferFailedEvent for transfer {}. Reason: {}", event.transferId(), event.reason());
            TransferStatus finalStatus = event.compensationRequired() ? TransferStatus.COMPENSATED : TransferStatus.FAILED;
            transferService.updateTransferStatus(event.transferId(), finalStatus, event.reason());
        } catch (Exception e) {
            log.error("Error processing failure event: {}", e.getMessage(), e);
        }
    }
}
