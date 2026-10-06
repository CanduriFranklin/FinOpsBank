# Layered Architecture

Deep dive into the **layered architecture** applied in FinOpsBank and its interaction with the multi-module Gradle structure.

---

## Table of Contents

- [Overview](#overview)
- [The Layers](#the-layers)
- [Dependency Rule](#dependency-rule)
- [Layer Interaction](#layer-interaction)
- [Multi-Module Mapping](#multi-module-mapping)
- [Code Examples](#code-examples)
- [Benefits](#benefits)
- [Common Pitfalls](#common-pitfalls)
- [Further Reading](#further-reading)

---

## Overview

FinOpsBank follows a **layered architecture** combined with a **Gradle multi-module** structure. Each layer has a clear responsibility and only depends on the layers below it.

### Layers (top to bottom)
+-----------------------------------------------+
| Presentation Layer (REST Controllers) | <-- HTTP boundary
+-----------------------------------------------+
| Application Layer (Services, DTOs) | <-- orchestration
+-----------------------------------------------+
| Domain Layer (Business rules) | <-- core logic
+-----------------------------------------------+
| Infrastructure Layer (JPA, Kafka, adapters) | <-- external systems
+-----------------------------------------------+
| Persistence / Brokers (PostgreSQL, Kafka) | <-- external services
+-----------------------------------------------+

text

**Key principle**: dependencies point **downward only**. The domain does not know about Spring, JPA, or Kafka.

---

## The Layers

### 1. Presentation Layer (Controllers)

Responsibility: handle HTTP requests and responses.

- Deserialize request bodies into DTOs.
- Validate input (basic validation via annotations).
- Delegate to services.
- Serialize responses into JSON.

**Example**: `AccountController`, `AuthController`, `TransactionController`.

**Located in**: `app/src/main/java/com/finopsbank/controller/`

### 2. Application Layer (Services)

Responsibility: orchestrate business operations.

- Apply business rules (with help from domain).
- Coordinate multiple repositories and adapters.
- Handle transactions (`@Transactional`).
- Publish events.

**Example**: `AccountService`.

**Located in**: `app/src/main/java/com/finopsbank/service/`

### 3. Domain Layer (Business Core)

Responsibility: hold pure business logic.

- Domain entities: `Customer`, `Account`, `Transaction`.
- Repository contracts (interfaces).
- Business rules that do not depend on frameworks.

**Example**: `Customer`, `AccountRepository` (interface).

**Located in**: `domain/src/main/java/com/finopsbank/core/` and `domain/src/main/java/com/finopsbank/data/`

### 4. Infrastructure Layer (Adapters)

Responsibility: implement ports for external systems.

- JPA entities and Spring Data repositories.
- Kafka publishers and listeners.
- Adapters for other external systems (email, storage, etc.).

**Example**: `AccountEntity`, `SpringDataAccountRepository`, `TransactionEventPublisher`, `KafkaTransactionListener`.

**Located in**: `infrastructure/src/main/java/com/finopsbank/persistence/` and `infrastructure/src/main/java/com/finopsbank/messaging/`

### 5. Persistence / Brokers

Responsibility: provide the actual data storage and messaging.

- PostgreSQL database.
- Apache Kafka broker.

**Not code**: these are external systems.

---

## Dependency Rule

The dependency rule states: **code in a layer may only depend on code in the layers below it**.
Presentation --> Application --> Domain
^
|
Infrastructure -----------------------+

text

Notice that **infrastructure depends on domain** (to implement its interfaces), but **domain does not depend on infrastructure**.

### Enforced by Gradle

The multi-module setup **enforces the dependency rule at compile time**:

| Module | Can depend on |
|---|---|
| `app` | `domain`, `infrastructure` |
| `infrastructure` | `domain` |
| `domain` | *(nothing)* |

If someone tries to add `implementation(project(":app"))` to the `domain` build file, the build fails with a circular dependency error.

### Why This Matters

- **Testability**: domain can be tested without Spring, JPA, or Kafka.
- **Reusability**: domain can be reused in other contexts (CLI, batch, mobile backend).
- **Frameworks are details**: swapping Spring for another framework does not affect the domain.
- **Enforced discipline**: violations are caught by the compiler, not by code review.

---

## Layer Interaction

A full request flows through the layers:
HTTP Request
|
v
+-------------------------------+
| Controller |
| @PostMapping("/accounts") |
+-------------------------------+
| (1) receives CreateAccountRequest DTO
v
+-------------------------------+
| Service |
| @Transactional |
| createAccount() |
+-------------------------------+
| (2) validates via domain rules
| (3) calls repository.save()
v
+-------------------------------+
| Repository (Interface) |
| in domain |
+-------------------------------+
| (4) implemented by
v
+-------------------------------+
| Repository (Implementation) |
| in infrastructure |
+-------------------------------+
| (5) writes to
v
+-------------------------------+
| PostgreSQL |
+-------------------------------+
|
v
(Service continues)
| (6) publishes event via
v
+-------------------------------+
| TransactionEventPublisher |
| in infrastructure |
+-------------------------------+
| (7) sends to
v
+-------------------------------+
| Kafka |
+-------------------------------+
|
v
HTTP Response (JSON)

text

### Step-by-Step

1. **Controller** receives the request and deserializes it into a **DTO** (a Java record).
2. **Service** validates and applies business rules.
3. **Service** calls `repository.save(entity)`. The repository is an **interface** defined in `domain`.
4. Spring injects the **implementation** from `infrastructure` at runtime (Dependency Inversion).
5. The implementation writes to **PostgreSQL** via JPA/Hibernate.
6. **Service** calls `eventPublisher.publishEvent(...)`.
7. The publisher sends the event to **Kafka**.
8. **Controller** serializes the response and returns it to the client.

---

## Multi-Module Mapping

Here is how the layers map to Gradle modules:

| Layer | Module | Example Classes |
|---|---|---|
| Presentation | `app` | `AccountController`, `AuthController` |
| Application | `app` | `AccountService` |
| Domain | `domain` | `Customer`, `AccountRepository` |
| Infrastructure | `infrastructure` | `AccountEntity`, `SpringDataAccountRepository`, `TransactionEventPublisher` |
| Persistence / Brokers | *(external)* | PostgreSQL, Kafka |

Note that **Presentation** and **Application** live in the same module (`app`). This is acceptable because both are tightly coupled to the Spring framework. If the project grew, they could be split into `api` and `application` modules.

---

## Code Examples

### Example 1: Controller (Presentation Layer)

```java
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountEntity> createAccount(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    @GetMapping
    public ResponseEntity<List<AccountEntity>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }
}
Responsibilities: parse HTTP, delegate to service, format response.

Example 2: Service (Application Layer)
java
@Service
public class AccountService {

    private final SpringDataAccountRepository accountRepository;
    private final TransactionEventPublisher eventPublisher;

    public AccountService(SpringDataAccountRepository accountRepository,
                          TransactionEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountEntity createAccount(CreateAccountRequest request) {
        String type = (request.accountType() != null) ? request.accountType() : "SAVINGS";
        AccountEntity account = new AccountEntity(
                request.accountNumber(),
                request.customerId(),
                request.ownerName(),
                request.initialBalance(),
                type
        );
        AccountEntity saved = accountRepository.save(account);
        eventPublisher.publishEvent(saved.getAccountNumber(),
                "ACCOUNT_CREATED: Initial balance " + saved.getBalance());
        return saved;
    }
}
Responsibilities: orchestrate, apply business rules, manage transaction.

Example 3: Repository Interface (Domain Layer)
java
package com.finopsbank.data;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findAll();
}
Responsibilities: define the contract. No framework code.

Example 4: Repository Implementation (Infrastructure Layer)
java
package com.finopsbank.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.finopsbank.persistence.entity.AccountEntity;

public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, String> {
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
}
Responsibilities: implement the domain contract using Spring Data JPA.

Benefits
1. Testability
Domain tests run without Spring (fast, isolated).

Service tests can mock repositories (no database needed).

Integration tests run the full stack (slow, but comprehensive).

2. Framework Independence
The domain is not tied to Spring, JPA, or Kafka. If you migrate to a different framework, the domain remains unchanged.

3. Enforced Boundaries
Gradle enforces module dependencies at compile time. A domain class cannot accidentally import jakarta.persistence.Entity. The build will fail.

4. Parallel Development
Different teams can work on different modules in parallel, as long as the interfaces stay stable.

5. Reusability
The domain can be reused in other applications: a CLI, a batch job, a mobile backend, or a desktop app.

6. Clear Ownership
Each layer has a clear owner and clear responsibility. New contributors understand where to put new code.

Common Pitfalls
1. Leaky Abstractions
Bad: A controller returns a JPA entity directly, exposing lazy-loaded collections and internal IDs.

Good: The controller returns a DTO (record) that only contains what the client needs.

java
// Bad
public ResponseEntity<AccountEntity> getAccount(...) { ... }

// Good
public ResponseEntity<AccountResponse> getAccount(...) { ... }
2. Domain Depending on Frameworks
Bad:

java
package com.finopsbank.core;

import jakarta.persistence.Entity;  // ❌ Framework in the domain

@Entity
public class Account { ... }
Good:

java
package com.finopsbank.core;

public class Account { ... }  // ✅ Pure Java
The JPA entity should live in infrastructure as AccountEntity.

3. Business Logic in Controllers
Bad: A controller contains complex business rules.

Good: The controller delegates to a service, which contains the rules.

4. Anemic Domain Model
Bad: The domain is just a bag of getters and setters with no behavior. All logic lives in services.

Better: The domain contains meaningful behavior (e.g., account.withdraw(amount) that checks for sufficient balance). Services orchestrate.

5. Circular Dependencies
Bad: app depends on infrastructure, and infrastructure depends on app.

Good: dependencies flow in one direction. Gradle prevents this.

6. Skipping DTOs
Returning JPA entities directly exposes the database schema to clients. If the schema changes, clients break. Always use DTOs at the API boundary.

Further Reading
Alistair Cockburn - Hexagonal Architecture

Robert C. Martin - Clean Architecture

Martin Fowler - PresentationDomainDataLayering

Martin Fowler - AnemicDomainModel

Gradle - Multi-Project Builds

<p align="center"> <strong>FinOpsBank</strong> - Layered Architecture </p>