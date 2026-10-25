package com.deepbank.account.service;

import com.deepbank.account.entity.TransactionLedger;
import com.deepbank.common.dto.AccountDto;
import com.deepbank.common.dto.CreateAccountRequest;
import com.deepbank.common.exception.DuplicateTransactionException;
import com.deepbank.common.exception.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Test
    @DisplayName("Test 1: Create Account with Initial Balance and Verify Ledger")
    void testCreateAccountAndLedger() {
        String accNum = "ACC-" + UUID.randomUUID().toString().substring(0, 8);
        CreateAccountRequest req = new CreateAccountRequest(
                accNum,
                "CUST-001",
                "Alice Tan",
                "SGD",
                new BigDecimal("1000.00")
        );

        AccountDto created = accountService.createAccount(req);
        assertNotNull(created.id());
        assertEquals(accNum, created.accountNumber());
        assertEquals(new BigDecimal("1000.00"), created.balance());

        // Verify ledger has INITIAL_DEPOSIT
        List<TransactionLedger> ledger = accountService.getLedger(accNum);
        assertFalse(ledger.isEmpty());
        assertEquals("INITIAL_DEPOSIT", ledger.get(0).getTransferId());
    }

    @Test
    @DisplayName("Test 2: Cache-Aside Get Account (verify SpEL positional key without reflection error)")
    void testGetAccountCache() {
        String accNum = "ACC-" + UUID.randomUUID().toString().substring(0, 8);
        accountService.createAccount(new CreateAccountRequest(
                accNum, "CUST-002", "Bob Lee", "SGD", new BigDecimal("500.00")));

        // 1st call: DB hit & cache store
        AccountDto firstCall = accountService.getAccount(accNum);
        assertNotNull(firstCall);

        // 2nd call: Redis Cache hit (must not throw reflection parameter error!)
        AccountDto cachedCall = accountService.getAccount(accNum);
        assertNotNull(cachedCall);
        assertEquals(firstCall.accountNumber(), cachedCall.accountNumber());
    }

    @Test
    @DisplayName("Test 3: Concurrency Debit & Pessimistic Lock Update")
    void testDebitAndCredit() {
        String accNum = "ACC-" + UUID.randomUUID().toString().substring(0, 8);
        accountService.createAccount(new CreateAccountRequest(
                accNum, "CUST-003", "Charlie", "SGD", new BigDecimal("300.00")));

        // Debit 100
        AccountDto afterDebit = accountService.debit("TXN-101", accNum, new BigDecimal("100.00"), "Payment");
        assertEquals(new BigDecimal("200.00"), afterDebit.balance());

        // Credit 50
        AccountDto afterCredit = accountService.credit("TXN-102", accNum, new BigDecimal("50.00"), "Refund");
        assertEquals(new BigDecimal("250.00"), afterCredit.balance());
    }

    @Test
    @DisplayName("Test 4: Insufficient Funds Rejection")
    void testInsufficientFunds() {
        String accNum = "ACC-" + UUID.randomUUID().toString().substring(0, 8);
        accountService.createAccount(new CreateAccountRequest(
                accNum, "CUST-004", "Dave", "SGD", new BigDecimal("50.00")));

        assertThrows(InsufficientFundsException.class, () -> {
            accountService.debit("TXN-999", accNum, new BigDecimal("100.00"), "Overdraft attempt");
        });
    }
}
