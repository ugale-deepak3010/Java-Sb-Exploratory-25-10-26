package com.dbs.banking.transfer.entity;

import com.dbs.banking.common.enums.TransferStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_id", columnList = "transferId", unique = true),
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_source_account", columnList = "sourceAccountNumber")
})
@EntityListeners(AuditingEntityListener.class)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transferId;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 32)
    private String sourceAccountNumber;

    @Column(nullable = false, length = 32)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status;

    @Column(length = 255)
    private String failureReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Transfer() {}

    public Transfer(String transferId, String idempotencyKey, String sourceAccountNumber,
                    String destinationAccountNumber, BigDecimal amount, String currency, TransferStatus status) {
        this.transferId = transferId;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getTransferId() { return transferId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
