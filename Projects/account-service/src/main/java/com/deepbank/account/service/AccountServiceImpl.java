package com.deepbank.account.service;

import com.deepbank.account.config.RedisConfig;
import com.deepbank.account.entity.Account;
import com.deepbank.account.entity.TransactionLedger;
import com.deepbank.account.repository.AccountRepository;
import com.deepbank.account.repository.TransactionLedgerRepository;
import com.deepbank.common.dto.AccountDto;
import com.deepbank.common.dto.CreateAccountRequest;
import com.deepbank.common.enums.AccountStatus;
import com.deepbank.common.enums.TransactionType;
import com.deepbank.common.exception.AccountFrozenException;
import com.deepbank.common.exception.AccountNotFoundException;
import com.deepbank.common.exception.BankingException;
import com.deepbank.common.exception.InsufficientFundsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionLedgerRepository ledgerRepository;

    public AccountServiceImpl(AccountRepository accountRepository, TransactionLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new BankingException("Account number already exists: " + request.accountNumber(), "ACCOUNT_EXISTS");
        }

        Account account = new Account(
                request.accountNumber(),
                request.customerId(),
                request.accountHolderName(),
                request.currency().toUpperCase(),
                request.initialBalance(),
                AccountStatus.ACTIVE
        );

        Account saved = accountRepository.save(account);

        // Record initial balance into ledger
        if (request.initialBalance().compareTo(BigDecimal.ZERO) > 0) {
            TransactionLedger initialLedger = new TransactionLedger(
                    "INITIAL_DEPOSIT",
                    saved.getAccountNumber(),
                    TransactionType.CREDIT,
                    request.initialBalance(),
                    BigDecimal.ZERO,
                    request.initialBalance(),
                    "Initial account opening deposit"
            );
            ledgerRepository.save(initialLedger);
        }

        log.info("Created account {} for customer {}", saved.getAccountNumber(), saved.getCustomerId());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.ACCOUNT_CACHE, key = "#accountNumber")
    public AccountDto getAccount(String accountNumber) {
        log.info("Fetching account {} from Database (Cache miss)", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return toDto(account);
    }

    /**
     * CRITICAL FOR 7+ YOE INTERVIEW:
     * 1. Uses Pessimistic Write Lock (SELECT ... FOR UPDATE) to serialize debit operations on this account.
     * 2. Checks active status and balance sufficiency.
     * 3. Saves ledger line item.
     * 4. Evicts Redis cache to maintain cache-aside consistency.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = RedisConfig.ACCOUNT_CACHE, key = "#accountNumber")
    public AccountDto debit(String transferId, String accountNumber, BigDecimal amount, String remarks) {
        log.info("Debiting {} from account {} for transfer {}", amount, accountNumber, transferId);

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(accountNumber);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber);
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        account.setBalance(balanceAfter);
        Account updated = accountRepository.save(account);

        TransactionLedger ledger = new TransactionLedger(
                transferId,
                accountNumber,
                TransactionType.DEBIT,
                amount,
                balanceBefore,
                balanceAfter,
                remarks
        );
        ledgerRepository.save(ledger);

        log.info("Debited successfully. New balance for account {}: {}", accountNumber, balanceAfter);
        return toDto(updated);
    }

    /**
     * Credits destination account using Pessimistic Write Lock and evicts cache.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = RedisConfig.ACCOUNT_CACHE, key = "#accountNumber")
    public AccountDto credit(String transferId, String accountNumber, BigDecimal amount, String remarks) {
        log.info("Crediting {} to account {} for transfer {}", amount, accountNumber, transferId);

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(accountNumber);
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        account.setBalance(balanceAfter);
        Account updated = accountRepository.save(account);

        TransactionLedger ledger = new TransactionLedger(
                transferId,
                accountNumber,
                TransactionType.CREDIT,
                amount,
                balanceBefore,
                balanceAfter,
                remarks
        );
        ledgerRepository.save(ledger);

        log.info("Credited successfully. New balance for account {}: {}", accountNumber, balanceAfter);
        return toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionLedger> getLedger(String accountNumber) {
        return ledgerRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    private AccountDto toDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getAccountHolderName(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
