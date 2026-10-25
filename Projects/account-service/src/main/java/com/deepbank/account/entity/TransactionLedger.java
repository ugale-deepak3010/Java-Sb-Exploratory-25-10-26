package com.deepbank.account.entity;

import com.deepbank.common.enums.TransactionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_ledger", indexes = {
        @Index(name = "idx_ledger_account_number", columnList = "accountNumber"),
        @Index(name = "idx_ledger_transfer_id", columnList = "transferId")
})
@EntityListeners(AuditingEntityListener.class)
public class TransactionLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String transferId;

    @Column(nullable = false, length = 32)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 255)
    private String remarks;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TransactionLedger() {}

    public TransactionLedger(String transferId, String accountNumber, TransactionType transactionType,
                             BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, String remarks) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public String getTransferId() { return transferId; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getRemarks() { return remarks; }
    public Instant getCreatedAt() { return createdAt; }
}
