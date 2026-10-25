package com.dbs.banking.account.repository;

import com.dbs.banking.account.entity.TransactionLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLedgerRepository extends JpaRepository<TransactionLedger, Long> {
    List<TransactionLedger> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<TransactionLedger> findByTransferId(String transferId);
}
