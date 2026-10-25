# DBS Bank Singapore / IBM 7+ YOE Interview Defense Drill

> **Candidate Briefing:** Tailored for **Prabhakara Rao Inapanuri** (Lead Architect, 16 YOE) & **Amudha K.** (Delivery Tower Lead, 26+ YOE).  
> **Core Domain:** Real-time FAST (Fast And Secure Transfers) & PayLah Microservices.

---

## 1. Concurrency & Double-Spending Defense (Prabhakara Focus)

### Q: "How do you guarantee that a user cannot spend the same SGD 500 balance twice when two concurrent payment requests arrive at the exact same millisecond?"
**Answer Script:**
> *"In our DBS FAST implementation, we employ a defense-in-depth strategy across three layers:*
> 1. **Edge Idempotency (Redis `SETNX`):** The client must pass a client-generated `X-Idempotency-Key` (UUIDv4). In our `transfer-service`, we execute Redis `setIfAbsent(key, "PROCESSING", 120s)`. If two identical requests hit two gateway instances concurrently, only one thread acquires the lock; the duplicate request is rejected immediately with **HTTP 409 Conflict**.
> 2. **Database Row-Level Serialization (`PESSIMISTIC_WRITE`):** At the database tier in `account-service`, the balance debit executes inside a `@Transactional(isolation = Isolation.READ_COMMITTED)` boundary with JPA:
>    ```java
>    @Lock(LockModeType.PESSIMISTIC_WRITE)
>    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
>    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
>    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
>    ```
>    This issues a native `SELECT ... FOR UPDATE` in PostgreSQL. The second transaction blocks waiting on the row lock. Once the first transaction debits the balance and commits, the second transaction reads the updated balance, detects `balance < amount`, and throws `InsufficientFundsException`.
> 3. **Optimistic Versioning (`@Version`) as Defense-in-Depth:** The `Account` entity contains a version column. If any un-locked flow attempts a stale update, Hibernate throws `OptimisticLockException`."*

---

## 2. Distributed Transactions & Dual-Write (Prabhakara Focus)

### Q: "Why not use Two-Phase Commit (2PC / XA)? How does your Saga handle a network failure midway through a transfer?"
**Answer Script:**
> *"2PC/XA holds database locks across distributed network boundaries during the prepare and commit phases. In cloud-native microservices, this kills throughput, introduces single points of failure, and causes cascading timeouts if one service stalls.
> 
> Instead, we implement an **Asynchronous Saga with the Transactional Outbox Pattern**:
> 1. **Eliminating the Dual-Write Hazard:** In `transfer-service`, we never write to the DB and publish to Kafka sequentially in application code. If Kafka is down, the DB transaction would commit while the message is lost. Instead, we save the `Transfer` entity and an `OutboxEvent` entity inside the **same local ACID PostgreSQL transaction**.
> 2. **Guaranteed Delivery:** A background poller (`OutboxPublisher`) reads pending outbox events and pushes them to the Kafka topic `transfer-events`. If Kafka is unreachable, the outbox events remain safe in PostgreSQL until Kafka recovers.
> 3. **Compensating Transactions:** If the source account is debited successfully, but the destination account cannot be credited (e.g., beneficiary account frozen or invalid), `account-service` catches the exception and immediately issues a **compensating credit (refund)** to the source account, publishing a `TransferFailedEvent` so `transfer-service` marks the record as `COMPENSATED`."*

---

## 3. Kafka In-Order Processing & Exactly-Once Semantics (EOS)

### Q: "Kafka is horizontally distributed. How do you guarantee ledger ordering for the same account without sacrificing cluster throughput?"
**Answer Script:**
> *"Ledger transactions on the same bank account must be strictly serialized (FIFO):
> 1. **Partition Key Strategy:** When `OutboxPublisher` publishes `TransferRequestedEvent` to Kafka, it sets the message key to `sourceAccountNumber` (e.g. `kafkaTemplate.send(topic, senderAccount, payload)`). Kafka's Murmur2 hashing algorithm ensures that all transactions for that specific account land on the **same partition**, guaranteeing strict FIFO consumption.
> 2. **Cluster Scalability:** Different bank accounts hash to different partitions, allowing horizontal parallel processing across consumer pods without concurrency bottlenecks.
> 3. **Consumer Idempotency:** The Kafka consumer maintains manual acknowledgment (`AckMode.MANUAL_IMMEDIATE` or Spring Kafka default) and tracks processed `transferId` in the database ledger to discard duplicate Kafka deliveries."*

---

## 4. Cache Consistency (Redis Cache-Aside)

### Q: "How do you prevent cache staleness between Redis and PostgreSQL when high-volume transfers occur?"
**Answer Script:**
> *"We use **Cache-Aside with Eviction on Write**, rather than write-through or cache updates:
> 1. **Read Path:** `getAccount(accountNumber)` checks Redis first via `@Cacheable("accounts")`. If cache miss, it reads DB and populates Redis with a 5-minute TTL.
> 2. **Write Path:** In `debit()` and `credit()`, we apply `@CacheEvict(value = "accounts", key = "#accountNumber")`. The database is updated and committed first; Redis cache for that account is invalidated.
> 3. **Why Evict instead of Update Cache?** Updating the cache directly invites race conditions between concurrent updates (e.g. Thread A writes DB, Thread B writes DB, Thread B updates Cache, Thread A updates Cache $\rightarrow$ cache has stale Thread A value). Eviction guarantees that the next read retrieves the true committed DB state."*

---

## 5. Spring Boot 3 & Java 21 Deep Internals

### Q: "What performance and architectural benefits did migrating to Java 21 and Spring Boot 3 bring to your microservices?"
**Answer Script:**
> 1. **Virtual Threads (Project Loom):** By configuring `spring.threads.virtual.enabled: true` in Spring Boot 3, Tomcat dispatches requests onto lightweight virtual threads (carrier threads). High-concurrency I/O operations (Postgres Hikari queries, Redis round-trips, Kafka dispatch) block virtual threads without pinning OS platform threads, enabling 10x higher request throughput under heavy FAST traffic.
> 2. **Records for DTOs & Events:** Immutable Java records eliminate boilerplate and guarantee thread-safety for all cross-service events (`TransferRequestedEvent`, `ApiResponse`).
> 3. **Jakarta EE 10 & AOT / Observability:** Native Micrometer tracing (`io.micrometer.tracing`) auto-propagates `traceId` and `spanId` across HTTP headers and Kafka record headers for distributed tracing.
> 4. **`@Transactional` Self-Invocation Caveat:** In Spring Boot, calling a `@Transactional` method from within the same class bypasses the CGLIB proxy, meaning the transaction interceptor never runs! We always inject service dependencies or extract transactional boundaries to separate helper components."*

---

## 6. Banking Domain, MAS Compliance & Delivery Reliability (Amudha K. Focus)

### Q: "In DBS Singapore, FAST transfers are governed by MAS guidelines requiring 99.999% availability and zero lost transactions. How does this architecture meet regulatory standards?"
**Answer Script:**
> 1. **Audit Ledger Immutability:** Our `transaction_ledger` table is append-only. No balance change occurs without an accompanying ledger line item containing `transferId`, `balanceBefore`, `balanceAfter`, and timestamp.
> 2. **Zero Message Loss (Outbox + DLT):** Dual-write risks are eliminated by the Outbox pattern. Unparseable or fatal messages do not freeze consumer partitions; our `DeadLetterPublishingRecoverer` automatically routes poison pills to `transfer-events.DLT` after 3 retries, raising an instant alert to the SRE delivery team while allowing normal FAST transfers to continue uninterrupted.
> 3. **Circuit Breaking & Graceful Degradation:** Spring Cloud Gateway integrates Resilience4j circuit breakers. If `account-service` is under rolling deployment or transient load, the gateway triggers `/fallback/accounts`, returning an informative 503 rather than cascading network timeouts across the mobile client.
> 4. **Zero-Downtime Blue/Green Deployments:** All DB schema migrations follow non-breaking additive patterns (expand-and-contract), allowing old and new microservice versions to run concurrently against PostgreSQL during production cutovers."*
