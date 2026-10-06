# FinOpsBank Architecture

Detailed architecture documentation for the **FinOpsBank API** project.

---

## Table of Contents

- [Overview](#overview)
- [Multi-Module Structure](#multi-module-structure)
- [Module Responsibilities](#module-responsibilities)
- [Request Flow](#request-flow)
- [Layered Architecture](#layered-architecture)
- [Design Decisions](#design-decisions)
- [Outbox Pattern](#outbox-pattern)
- [Technology Choices](#technology-choices)

---

## Overview

FinOpsBank is a **REST API** for banking services built with a clean, modular architecture using **Gradle multi-module** setup. The project is designed to demonstrate professional software engineering practices: separation of concerns, dependency inversion, stateless authentication, reliable event publishing, and containerized deployment.

---

## Multi-Module Structure
FinOpsBank/
+-- app/ # Spring Boot application (entry point)
| +-- src/main/java/com/finopsbank/
| | +-- config/ # Security, beans
| | +-- controller/ # REST controllers
| | +-- dto/ # Request/response records
| | +-- security/ # JWT filter and provider
| | +-- service/ # Business orchestration
| +-- src/main/resources/ # application.yml
|
+-- domain/ # Pure domain model (no framework deps)
| +-- src/main/java/com/finopsbank/core/ # Entities
| +-- src/main/java/com/finopsbank/data/ # Repository interfaces
|
+-- infrastructure/ # Adapters: JPA, Kafka
| +-- src/main/java/com/finopsbank/persistence/ # JPA entities and repositories
| +-- src/main/java/com/finopsbank/messaging/ # Kafka publisher and listeners
|
+-- k8s/ # Kubernetes manifests (pending)
+-- docs/ # Documentation
+-- Containerfile # OCI image definition
+-- build.gradle.kts # Root Gradle build

text

---

## Module Responsibilities

### `domain` — Pure Domain Layer

Contains framework-agnostic code:

- **Domain entities**: `Customer`, `Account`, `Transaction`.
- **Repository interfaces**: contracts that define how data is accessed.
- **Business rules**: validations independent of Spring, JPA, or Kafka.

**Key principle**: no dependencies on Spring, Hibernate, or Kafka. This allows the domain to be reused in different contexts (CLI, batch, testing).

### `infrastructure` — Adapters Layer

Implements the ports defined in `domain`:

- **JPA entities**: `AccountEntity`, `CustomerEntity`, `TransactionEntity` with `@Entity` mappings.
- **Spring Data repositories**: `SpringDataAccountRepository`, `SpringDataTransactionRepository`.
- **Kafka adapters**: `TransactionEventPublisher` (producer), `KafkaTransactionListener` (consumer).

**Key principle**: this module knows about the outside world (PostgreSQL, Kafka) but the domain does not know about this module.

### `app` — Application Layer

The Spring Boot application entry point:

- **Controllers**: REST endpoints under `/api/v1/*`.
- **Security**: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`.
- **DTOs**: records for request/response payloads.
- **Services**: orchestration between controllers, domain, and infrastructure.
- **Configuration**: `application.yml`, logging, profiles.

**Key principle**: this is the "composition root" — where all the pieces are wired together.

---

## Request Flow

A complete HTTP request flows through the following stages:
+-----------------+
| HTTP Request |
| POST /api/v1/...|
+--------+--------+
|
v
+-------------------------------+
| 1. Tomcat (Servlet Container) |
+---------------+---------------+
|
v
+-------------------------------+
| 2. Spring Security FilterChain|
| - DisableEncodeUrl |
| - SecurityContextHolder |
| - JwtAuthenticationFilter | <-- JWT validation here
| - AuthorizationFilter | <-- URL + method rules
+---------------+---------------+
|
v
+-------------------------------+
| 3. DispatcherServlet |
+---------------+---------------+
|
v
+-------------------------------+
| 4. Controller |
| AccountController.create() |
+---------------+---------------+
|
v
+-------------------------------+
| 5. Service |
| AccountService.create() |
+---------------+---------------+
|
+--> 6a. Repository.save() --> PostgreSQL
|
+--> 6b. eventPublisher.publish() --> Kafka
|
v
+-------------------------------+
| 7. HTTP Response |
| 200 OK + JSON body |
+-------------------------------+

text

### Detailed Steps

1. **Tomcat** receives the TCP connection and dispatches the request to the servlet container.
2. **Spring Security** runs the filter chain:
   - `JwtAuthenticationFilter` extracts the `Authorization: Bearer <token>` header, validates the JWT, and populates `SecurityContext`.
   - `AuthorizationFilter` checks the URL rules defined in `SecurityConfig`.
3. **DispatcherServlet** routes the request to the matching controller method.
4. **Controller** deserializes the request body into a DTO (record).
5. **Service** applies business rules, coordinates repositories and event publishing.
6. **Persistence and Messaging**:
   - **6a.** Repository saves the entity via JPA to PostgreSQL.
   - **6b.** Event publisher sends a message to Kafka (`TransactionEventPublisher`).
7. **Response** is serialized back to JSON and returned to the client.

---

## Layered Architecture
+------------------------------------------+
| app (application) |
| Controllers | Security | Services | DTOs|
+---------------------+--------------------+
|
v
+------------------------------------------+
| infrastructure (adapters) |
| JPA Entities | Repositories | Kafka |
+---------------------+--------------------+
|
v
+------------------------------------------+
| domain (business core) |
| Entities | Repository Contracts | Rules |
+------------------------------------------+

text

**Dependency direction**: `app` -> `infrastructure` -> `domain`

The `domain` module has **zero dependencies** on the other modules. This is the **Dependency Inversion Principle** in action: high-level policy (domain) does not depend on low-level detail (infrastructure).

---

## Design Decisions

### Why Multi-Module?

- **Enforced boundaries**: it is impossible to import Spring or JPA in the domain module (compilation fails).
- **Independent testing**: domain tests run without booting Spring, making them fast.
- **Reusability**: the same domain can power a REST API, a CLI, or a batch job.
- **Parallel builds**: Gradle can build modules in parallel when dependencies allow.

### Why JWT Stateless?

- **Horizontal scalability**: any instance can validate a token without shared session state.
- **No server-side session storage**: no need for Redis or sticky sessions.
- **Cross-origin friendly**: works with SPA and mobile clients without CORS/cookie complexities.
- **Standard**: JWT is widely adopted and supported by all major frameworks.

Trade-off: tokens cannot be revoked before expiration without an additional blacklist. For FinOpsBank, the 24-hour expiry is considered acceptable for the current scope.

### Why Spring Security with Custom Filter?

- **Stateless**: `SessionCreationPolicy.STATELESS` disables HTTP sessions.
- **Custom filter**: `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter` to validate the JWT and set the security context.
- **Method-level security**: `@EnableMethodSecurity` allows future use of `@PreAuthorize` on service methods.

### Why `hasAnyAuthority` instead of `hasAnyRole`?

When using `SimpleGrantedAuthority("ROLE_ADMIN")`, the authority string already includes the `ROLE_` prefix. `hasAnyRole("ADMIN")` internally adds another prefix, which can fail in Spring Security 7. Using `hasAnyAuthority("ROLE_ADMIN")` compares literally, avoiding the issue.

### Why `/error` must be `permitAll`?

When a controller throws an exception, Spring Boot forwards internally to `/error`. The security filter chain runs again on that forward. Without `permitAll` on `/error`, the client sees a misleading `403` instead of the real error (`500`, `400`, etc.).

### Why CSRF is disabled?

The API is **stateless** and does not use cookies for authentication. CSRF protection is designed for cookie-based sessions, where the browser automatically sends cookies on cross-origin requests. With JWT in the `Authorization` header, the browser does not send it automatically, so CSRF does not apply.

### Why Kafka KRaft instead of ZooKeeper?

- **Simpler operations**: no separate ZooKeeper cluster to manage.
- **Faster startup**: KRaft consensus is built into Kafka.
- **Native in Kafka 4.x**: ZooKeeper support has been removed.

---

## Outbox Pattern

The project uses the **Outbox pattern** to ensure that database changes and Kafka events are consistent, even if Kafka is temporarily unavailable.

### The Problem

Without Outbox:
1. Save to database -> success.
2. Publish to Kafka -> **fails** (network, broker down).
3. Result: the database has the change, but no consumer will ever know.

### The Solution (Outbox)

1. Save the business operation AND the event to the same database transaction:
   - `INSERT INTO accounts ...`
   - `INSERT INTO outbox_events (event_type, payload, status) ...`
2. Both succeed or both fail (transaction atomicity).
3. A **publisher** (scheduled or triggered) reads from `outbox_events`, publishes to Kafka, and marks the event as published.

### Current Implementation

The current implementation uses a `TransactionEventPublisher` that publishes directly after the service returns. A more robust implementation with `@TransactionalEventListener(phase = AFTER_COMMIT)` is a planned improvement.

### Planned Improvements

- Use `@TransactionalEventListener(phase = AFTER_COMMIT)` to publish only after the transaction commits.
- Add a `retry_count` and `status` column in `outbox_events`.
- Add a scheduled job that polls `outbox_events` for unsent events.

---

## Technology Choices

| Decision | Chosen | Alternative | Reason |
|---|---|---|---|
| Language | Java 25 LTS | Kotlin, Scala | LTS, industry standard, Java 25 supported by Spring Boot 4 |
| Framework | Spring Boot 4.1.1 | Quarkus, Micronaut | Mature ecosystem, comprehensive Spring suite |
| Build tool | Gradle 9.8 (Kotlin DSL) | Maven | Faster, flexible, multi-module friendly |
| Database | PostgreSQL 18 | MySQL, H2 | ACID, JSONB, mature, open source |
| Messaging | Apache Kafka 4.3.1 (KRaft) | RabbitMQ, Pulsar | Event streaming, KRaft simplicity, industry standard |
| Auth | JWT (HS512) | Session, OAuth2 | Stateless, scalable, simple for a single service |
| Container | Podman 6.1.3 | Docker | Rootless, daemonless, better security defaults |
| Platform | WSL2 (Windows) | Native Linux | Development on Windows without dual-boot |

---

## Further Reading

- [docs/SETUP.md](SETUP.md) — Step-by-step installation
- [docs/SECURITY.md](SECURITY.md) — JWT and security internals
- [docs/KAFKA.md](KAFKA.md) — Kafka configuration and topics
- [docs/patterns/OUTBOX_PATTERN.md](patterns/OUTBOX_PATTERN.md) — Outbox pattern deep dive
- [docs/patterns/LAYERED_ARCHITECTURE.md](patterns/LAYERED_ARCHITECTURE.md) — Layered architecture deep dive

---

<p align="center">
  <strong>FinOpsBank</strong> - Architecture Documentation
</p>