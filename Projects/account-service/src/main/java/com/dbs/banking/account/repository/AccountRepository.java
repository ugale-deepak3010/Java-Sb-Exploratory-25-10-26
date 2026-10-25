package com.dbs.banking.account.repository;

import com.dbs.banking.account.entity.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * CRITICAL FOR 7+ YOE INTERVIEW:
     * Pessimistic Write Lock (SELECT ... FOR UPDATE).
     * Locks the specific row at DB level until the current transaction commits or rolls back.
     * Prevents race conditions during concurrent debits/credits.
     * QueryHint jakarta.persistence.lock.timeout sets max lock wait time to 3000ms to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
}
