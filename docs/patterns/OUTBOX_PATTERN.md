# Outbox Pattern

Deep dive into the **Outbox pattern** as applied in FinOpsBank for reliable event publishing.

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [The Dual-Write Problem](#the-dual-write-problem)
- [Outbox Solution](#outbox-solution)
- [Implementation in FinOpsBank](#implementation-in-finopsbank)
- [Event Types](#event-types)
- [Trade-offs](#trade-offs)
- [Alternatives](#alternatives)
- [Further Reading](#further-reading)

---

## Problem Statement

When a service needs to **both** update its database **and** publish an event to a message broker, there are two operations that must succeed together:

1. Save the business entity (e.g., create an account).
2. Publish an event to Kafka (e.g., `ACCOUNT_CREATED`).

If only one succeeds, the system becomes inconsistent:

- Database updated, event not published -> consumers miss the change.
- Event published, database not updated -> consumers see a change that does not exist.

This is known as the **dual-write problem**.

---

## The Dual-Write Problem

A naive implementation looks like this:

```java
@Transactional
public AccountEntity createAccount(CreateAccountRequest request) {
    AccountEntity account = new AccountEntity(...);
    AccountEntity saved = accountRepository.save(account);  // (1)
    kafkaTemplate.send(TOPIC, saved.getAccountNumber(), "ACCOUNT_CREATED");  // (2)
    return saved;
}
What can go wrong?
Scenario A: Kafka send fails

Line (1) commits the account to PostgreSQL.

Line (2) throws an exception (Kafka broker down, network issue).

The @Transactional rolls back the database insert.

Result: no account created, no event -> consistent but the operation failed.

Scenario B: Kafka send succeeds, database commit fails

Line (2) sends the event to Kafka.

The @Transactional commit fails (deadlock, constraint violation on commit).

Kafka has the event, but the database has no account.

Result: inconsistent state.

Scenario C: The process crashes between (1) and (2)

Line (1) commits.

The JVM crashes before line (2).

Result: no event published, but the database has the change.

Why retries alone do not solve it
Retrying the Kafka send in a try-catch block still leaves a window between "database committed" and "event sent". If the process crashes in that window, the event is lost.

You need atomicity between the two writes. But Kafka and PostgreSQL do not share a transaction.

Outbox Solution
The Outbox pattern solves this by writing the event into the same database transaction as the business operation.

Steps
text
+---------------------------------------------------------------+
|                    SINGLE DATABASE TRANSACTION                |
|                                                               |
|   1. INSERT INTO accounts (...)                               |
|   2. INSERT INTO outbox_events (event_type, payload, status)  |
|                                                               |
|   COMMIT (atomic)                                             |
+---------------------------------------------------------------+
                              |
                              v
+---------------------------------------------------------------+
|  Asynchronous process: read from outbox_events, publish to    |
|  Kafka, mark the event as sent.                               |
+---------------------------------------------------------------+
Benefits
Atomicity: both the business change and the event are committed together.

Guaranteed delivery: if the publish fails, the event remains in the outbox and is retried.

Order preservation: events are stored in the order they occurred (using an ID or timestamp).

Auditability: all events are recorded in a table for debugging or replay.

Trade-offs
Eventual consistency: consumers see events slightly delayed (usually milliseconds).

Additional table: the outbox table must be maintained.

Cleanup: sent events need periodic pruning (retention policy).

Implementation in FinOpsBank
Current Implementation (Simplified)
FinOpsBank uses a simplified version of the Outbox pattern for the current milestone. The event is published directly via TransactionEventPublisher after the service returns:

java
@Transactional
public AccountEntity createAccount(CreateAccountRequest request) {
    AccountEntity account = new AccountEntity(...);
    AccountEntity saved = accountRepository.save(account);
    eventPublisher.publishEvent(saved.getAccountNumber(), "ACCOUNT_CREATED: ...");
    return saved;
}
java
@Component
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "finopsbank-transactions";

    public void publishEvent(String key, String message) {
        kafkaTemplate.send(TOPIC, key, message);
    }
}
The database has an outbox_events table to support a future full implementation, but the current code publishes directly.

Table Structure
sql
CREATE TABLE outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    event_type   VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload      TEXT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at      TIMESTAMPTZ,
    retry_count  INTEGER DEFAULT 0,
    last_error   TEXT
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_created ON outbox_events(created_at);
Recommended Implementation (Next Step)
1. Publish events after the transaction commits
Use @TransactionalEventListener(phase = AFTER_COMMIT):

java
public record AccountCreatedEvent(String accountNumber, String message) {}

@Service
public class AccountService {

    private final ApplicationEventPublisher publisher;
    private final SpringDataAccountRepository accountRepository;

    @Transactional
    public AccountEntity createAccount(CreateAccountRequest request) {
        AccountEntity account = new AccountEntity(...);
        AccountEntity saved = accountRepository.save(account);
        publisher.publishEvent(new AccountCreatedEvent(
            saved.getAccountNumber(),
            "ACCOUNT_CREATED: Initial balance " + saved.getBalance()
        ));
        return saved;
    }
}
java
@Component
public class OutboxEventListener {

    private final OutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountCreated(AccountCreatedEvent event) {
        OutboxEvent outbox = new OutboxEvent(
            "ACCOUNT_CREATED",
            event.accountNumber(),
            event.message()
        );
        outboxRepository.save(outbox);
    }
}
2. Scheduled outbox processor
java
@Component
@EnableScheduling
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPending() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByStatusOrderByIdAsc("PENDING");
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send("finopsbank-transactions", event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);  // synchronous wait
                event.setStatus("SENT");
                event.setSentAt(Instant.now());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                }
            }
            outboxRepository.save(event);
        }
    }
}
3. Periodic cleanup
Delete sent events older than N days:

java
@Scheduled(cron = "0 0 3 * * *")  // daily at 3 AM
@Transactional
public void cleanupSentEvents() {
    Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
    outboxRepository.deleteByStatusAndSentAtBefore("SENT", cutoff);
}
Event Types
Event Type	Triggered by	Aggregate	Payload
ACCOUNT_CREATED	createAccount()	Account number	ACCOUNT_CREATED: Initial balance <amount>
DEPOSIT_COMPLETED	deposit()	Account number	DEPOSIT_COMPLETED: Amount <amount> | New Balance <amount>
WITHDRAWAL_COMPLETED	withdraw()	Account number	WITHDRAWAL_COMPLETED: Amount <amount> | New Balance <amount>
TRANSFER_SENT	transfer()	Source account	TRANSFER_SENT: To <account> | Amount <amount>
TRANSFER_RECEIVED	transfer()	Target account	TRANSFER_RECEIVED: From <account> | Amount <amount>
Event Key
The event key is the account number. Kafka uses the key to determine the partition:

text
partition = hash(key) % num_partitions
This ensures all events for a given account go to the same partition, preserving order per account.

Trade-offs
Aspect	Outbox	Direct publish
Atomicity	Guaranteed	Not guaranteed
Latency	Slight delay (polling interval)	Immediate
Complexity	Higher (table, scheduler)	Simpler
Order preservation	Yes	Depends on Kafka
Replayable	Yes (events stored)	No
Storage overhead	Yes (event rows)	No
For financial systems (which FinOpsBank is), the Outbox pattern is the correct choice. The extra complexity is justified by the guarantee of not losing events.

Alternatives
1. Two-Phase Commit (2PC)
Use a distributed transaction coordinator (XA) to commit both PostgreSQL and Kafka atomically.

Pros: True atomicity.
Cons: Kafka does not support XA; complex; poor performance.

2. Change Data Capture (CDC)
Use Debezium to capture changes in the database and publish them to Kafka.

Pros: No application-level changes needed.
Cons: Requires Debezium infrastructure; harder to filter.

3. Transactional Messaging
Some brokers support transactional producers (Kafka does). Combine with @Transactional to sync.

Pros: Kafka-native.
Cons: Does not cover the "database committed, Kafka failed" window.

4. Event Sourcing
Store all state changes as events in the database, then replay to build state.

Pros: Full audit trail.
Cons: Significant architectural change; not suitable for all domains.

Recommendation: The Outbox pattern is the best balance of simplicity and reliability for FinOpsBank.

Further Reading
Microservices.io - Transactional Outbox

Debezium Outbox Event Router

Chris Richardson - Microservices Patterns (Book)

Confluent - Kafka Transactions

<p align="center"> <strong>FinOpsBank</strong> - Outbox Pattern </p>