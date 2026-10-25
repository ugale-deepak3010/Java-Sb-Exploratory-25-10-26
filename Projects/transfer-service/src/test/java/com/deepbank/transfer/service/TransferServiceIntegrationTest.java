package com.deepbank.transfer.service;

import com.deepbank.common.dto.TransferRequest;
import com.deepbank.common.dto.TransferResponse;
import com.deepbank.common.enums.TransferStatus;
import com.deepbank.common.exception.DuplicateTransactionException;
import com.deepbank.transfer.entity.OutboxEvent;
import com.deepbank.transfer.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("Test 1: Initiate Transfer and Verify Transactional Outbox Entry")
    void testInitiateTransferAndOutbox() {
        String key = "KEY-" + UUID.randomUUID();
        TransferRequest req = new TransferRequest(
                key,
                "ACC-SRC-1",
                "ACC-DST-2",
                new BigDecimal("150.00"),
                "SGD",
                "Test transfer"
        );

        TransferResponse response = transferService.initiateTransfer(req);
        assertNotNull(response);
        assertEquals(TransferStatus.INITIATED, response.status());
        assertEquals(key, response.idempotencyKey());

        // Verify Transactional Outbox has captured this event for Kafka poller
        List<OutboxEvent> outboxEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        boolean eventFound = outboxEvents.stream()
                .anyMatch(e -> e.getAggregateId().equals(response.transferId()));
        assertTrue(eventFound, "Transactional Outbox must contain event for transfer " + response.transferId());
    }

    @Test
    @DisplayName("Test 2: Redis Idempotency Rejection on Duplicate Request")
    void testDuplicateTransferRejection() {
        String key = "DUP-KEY-" + UUID.randomUUID();
        TransferRequest req = new TransferRequest(
                key,
                "ACC-SRC-1",
                "ACC-DST-2",
                new BigDecimal("75.00"),
                "SGD",
                "Duplicate test"
        );

        // First transfer succeeds
        TransferResponse firstResponse = transferService.initiateTransfer(req);
        assertNotNull(firstResponse);

        // Immediate identical retry must be rejected by Redis SETNX lock
        assertThrows(DuplicateTransactionException.class, () -> {
            transferService.initiateTransfer(req);
        });
    }
}
