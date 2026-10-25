package com.dbs.banking.transfer.service;

import com.dbs.banking.common.dto.TransferRequest;
import com.dbs.banking.common.dto.TransferResponse;
import com.dbs.banking.common.enums.TransferStatus;
import com.dbs.banking.common.events.TransferRequestedEvent;
import com.dbs.banking.common.exception.BankingException;
import com.dbs.banking.common.exception.DuplicateTransactionException;
import com.dbs.banking.transfer.entity.OutboxEvent;
import com.dbs.banking.transfer.entity.Transfer;
import com.dbs.banking.transfer.repository.OutboxEventRepository;
import com.dbs.banking.transfer.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public TransferServiceImpl(TransferRepository transferRepository,
                               OutboxEventRepository outboxEventRepository,
                               IdempotencyService idempotencyService,
                               ObjectMapper objectMapper) {
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * CRITICAL FOR 7+ YOE INTERVIEW:
     * 1. Acquire Redis distributed lock / idempotency token (SETNX).
     * 2. Insert Transfer record in status INITIATED.
     * 3. Insert OutboxEvent record into Outbox table in the SAME ACID transaction.
     * 4. Return HTTP 202 ACCEPTED / 200 OK immediately with transferId.
     */
    @Override
    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request) {
        // 1. Prevent duplicate requests at Redis level
        idempotencyService.acquireLock(request.idempotencyKey());

        // 2. Guard against DB duplicate idempotency key
        if (transferRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        String transferId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transfer transfer = new Transfer(
                transferId,
                request.idempotencyKey(),
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.currency().toUpperCase(),
                TransferStatus.INITIATED
        );
        Transfer saved = transferRepository.save(transfer);

        // 3. Build domain event and save in Transactional Outbox
        TransferRequestedEvent event = new TransferRequestedEvent(
                UUID.randomUUID().toString(),
                transferId,
                request.idempotencyKey(),
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.currency().toUpperCase(),
                Instant.now()
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    "TRANSFER",
                    transferId,
                    "TransferRequestedEvent",
                    payloadJson
            );
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event for transfer {}: {}", transferId, e.getMessage());
            throw new BankingException("Event serialization failed: " + e.getMessage());
        }

        // 4. Mark idempotency as registered
        idempotencyService.markCompleted(request.idempotencyKey(), transferId);

        log.info("Initiated transfer {} for amount {} {} from {} to {}",
                transferId, request.amount(), request.currency(), request.sourceAccountNumber(), request.destinationAccountNumber());

        return toDto(saved, "Transfer initiated and queued for processing");
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(String transferId) {
        Transfer transfer = transferRepository.findByTransferId(transferId)
                .orElseThrow(() -> new BankingException("Transfer not found: " + transferId, "TRANSFER_NOT_FOUND"));
        return toDto(transfer, "Transfer status retrieved");
    }

    @Override
    @Transactional
    public void updateTransferStatus(String transferId, TransferStatus status, String reason) {
        Transfer transfer = transferRepository.findByTransferId(transferId)
                .orElseThrow(() -> new BankingException("Transfer not found: " + transferId, "TRANSFER_NOT_FOUND"));
        transfer.setStatus(status);
        if (reason != null) {
            transfer.setFailureReason(reason);
        }
        transferRepository.save(transfer);
        log.info("Transfer {} transitioned to status {}", transferId, status);
    }

    private TransferResponse toDto(Transfer transfer, String message) {
        return new TransferResponse(
                transfer.getTransferId(),
                transfer.getIdempotencyKey(),
                transfer.getSourceAccountNumber(),
                transfer.getDestinationAccountNumber(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                message,
                transfer.getCreatedAt()
        );
    }
}
