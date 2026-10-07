# Design Patterns

Catalogue of the design patterns used in **FinOpsBank**, with references to where they are applied and why.

---

## Table of Contents

- [Overview](#overview)
- [Architectural Patterns](#architectural-patterns)
- [Gang of Four (GoF) Patterns](#gang-of-four-gof-patterns)
- [Enterprise Integration Patterns](#enterprise-integration-patterns)
- [Cloud-Native Patterns](#cloud-native-patterns)
- [Anti-Patterns Avoided](#anti-patterns-avoided)
- [Pattern Interactions](#pattern-interactions)
- [Further Reading](#further-reading)

---

## Overview

FinOpsBank combines **architectural**, **Gang of Four**, **enterprise integration**, and **cloud-native** patterns. This document catalogues each pattern, shows where it is applied, and explains the reasoning.

The combination of these patterns produces a codebase that is:
- **Testable**: the domain can be tested without frameworks.
- **Extensible**: new features are added without modifying existing code.
- **Observable**: events and logs make behavior visible.
- **Resilient**: failures are isolated and recoverable.

---

## Architectural Patterns

### 1. Layered Architecture

**Where**: `app` / `infrastructure` / `domain` modules.

**Why**: separates concerns into horizontal layers. Each layer depends only on lower layers.
Presentation (Controllers) -> Application (Services) -> Domain (Business) <- Infrastructure (JPA, Kafka)

text

**Benefit**: the domain has zero dependencies on Spring, JPA, or Kafka. Swapping the framework does not affect business logic.

See [LAYERED_ARCHITECTURE.md](LAYERED_ARCHITECTURE.md) for details.

### 2. Multi-Module Architecture (Gradle)

**Where**: `settings.gradle.kts` with `include("app", "domain", "infrastructure")`.

**Why**: enforces dependency rules at compile time. `domain` cannot import `jakarta.persistence` because the dependency is not declared.

**Benefit**: architectural violations are caught by the compiler, not by code review.

### 3. Dependency Inversion Principle (DIP)

**Where**: `domain` defines `AccountRepository` (interface). `infrastructure` provides `SpringDataAccountRepository` (implementation).

**Why**: high-level policy (domain) should not depend on low-level details (JPA). Both depend on abstractions.

**Benefit**: the domain can be tested with a fake repository, without spinning up Spring or PostgreSQL.

### 4. Composition Root

**Where**: `FinOpsBankApplication.java` + Spring's `@SpringBootApplication`.

**Why**: there should be a single place where the object graph is wired. Spring's container acts as the composition root.

**Benefit**: dependencies are visible and explicit. No hidden singletons or global state.

### 5. Ports and Adapters (Hexagonal, partial)

**Where**: `AccountRepository` (port) implemented by `SpringDataAccountRepository` (adapter). `TransactionEventPublisher` (port) implemented by Kafka adapter.

**Why**: the application core is isolated from external systems.

**Benefit**: the core is testable without infrastructure. Adapters can be swapped (in-memory for tests, PostgreSQL for production).

### 6. Event-Driven Architecture (partial)

**Where**: `TransactionEventPublisher` (producer) and `KafkaTransactionListener` (consumer).

**Why**: decouples producers from consumers. New consumers can subscribe without modifying the producer.

**Benefit**: asynchronous, scalable, and extensible.

---

## Gang of Four (GoF) Patterns

### 1. Chain of Responsibility

**Where**: Spring Security `FilterChain` (12 filters in sequence).

**Why**: each request passes through a chain of filters. Each filter decides to process, pass through, or reject.

**Benefit**: filter logic is composable. Adding a new filter (like `JwtAuthenticationFilter`) does not require modifying others.

**Code reference**:

```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
2. Template Method
Where: JwtAuthenticationFilter extends OncePerRequestFilter.

Why: OncePerRequestFilter defines the algorithm (ensure the filter runs once per request) and delegates the specific logic to doFilterInternal.

Benefit: no need to reimplement filter lifecycle management.

3. Builder
Where: HttpSecurity, Jwts.builder(), User.builder(), ResponseEntity.ok().

Why: constructing complex immutable objects step by step.

Benefit: readable, fluent, and safe construction.

Code reference:

java
return http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        ...
    )
    .build();
4. Strategy
Where: StringSerializer vs JsonSerializer for Kafka. BCryptPasswordEncoder vs other PasswordEncoder implementations.

Why: algorithms are interchangeable. The client code selects one at configuration time.

Benefit: swap implementations without changing the client.

5. Proxy (Dynamic)
Where: @Transactional methods in AccountService wrapped by Spring AOP.

Why: cross-cutting concerns (transactions) are separated from business logic.

Benefit: business logic does not know about transaction management.

Code reference:

java
@Transactional
public AccountEntity createAccount(CreateAccountRequest request) { ... }
6. Singleton (per Bean)
Where: all @Service, @Component, @Repository are singletons in the Spring container.

Why: shared state is managed by the container. Beans are thread-safe by default (stateless).

Benefit: efficient resource usage. Controllers and services are created once.

7. Factory
Where: AuthenticationManager, PasswordEncoder beans in SecurityConfig.

Why: complex objects need configuration before being used.

Benefit: centralized creation logic.

Code reference:

java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
8. Adapter
Where: SpringDataAccountRepository adapts the domain's AccountRepository interface to Spring Data JPA.

Why: the domain defines a simple interface, but Spring Data has its own conventions.

Benefit: the domain does not depend on Spring Data.

9. Facade
Where: AccountService provides a simplified API over repositories and event publisher.

Why: controllers should not know about the complexity of persistence and messaging.

Benefit: simpler controllers. Easier to change the underlying implementation.

10. Dependency Injection
Where: constructor injection in every service and controller.

Why: dependencies are explicit and testable.

Benefit: no service locators, no hidden dependencies, easier to mock in tests.

Code reference:

java
public AccountService(SpringDataAccountRepository accountRepository,
                      TransactionEventPublisher eventPublisher) {
    this.accountRepository = accountRepository;
    this.eventPublisher = eventPublisher;
}
Enterprise Integration Patterns
1. Repository
Where: SpringDataAccountRepository, SpringDataTransactionRepository.

Why: abstracts the data source. Business logic works with domain objects, not SQL.

Benefit: swapping the database does not affect business logic.

See Spring Data JPA docs.

2. Data Transfer Object (DTO)
Where: LoginRequest, LoginResponse, CreateAccountRequest, DepositRequest, etc. (all Java records).

Why: decouples the API contract from the persistence model. The API can evolve without breaking the database schema.

Benefit: no leaked JPA entities. Clients see only what they need.

Code reference:

java
public record CreateAccountRequest(
    String accountNumber,
    String customerId,
    String ownerName,
    BigDecimal initialBalance,
    String accountType
) {}
3. Service Layer
Where: AccountService.

Why: encapsulates use cases. Controllers are thin; services hold business logic.

Benefit: reusable across multiple entry points (REST, CLI, batch).

4. Producer-Consumer
Where: Kafka topic finopsbank-transactions. Producer: TransactionEventPublisher. Consumer: KafkaTransactionListener.

Why: decouples producers from consumers. Multiple consumers can subscribe.

Benefit: scalable, asynchronous, resilient.

5. Outbox Pattern
Where: outbox_events table. Currently simplified, planned full implementation.

Why: ensures atomicity between the database and Kafka.

Benefit: no lost events. No phantom events. Consistent state.

See OUTBOX_PATTERN.md for details.

6. Stateless Authentication
Where: JWT + SessionCreationPolicy.STATELESS.

Why: enables horizontal scaling. Any instance can validate a token.

Benefit: no shared session storage. Simpler to scale and deploy.

7. Role-Based Access Control (RBAC)
Where: ROLE_ADMIN, ROLE_TELLER, ROLE_CUSTOMER. Enforced via SecurityConfig.

Why: fine-grained authorization. Different roles can perform different actions.

Benefit: least privilege principle. Auditable permissions.

8. Schema Validation (Contract-First)
Where: spring.jpa.hibernate.ddl-auto: validate.

Why: enforces an explicit contract between code and database. If they diverge, startup fails.

Benefit: catches drift immediately. Forces schema migrations to be intentional.

Cloud-Native Patterns
1. Health Probe (Readiness + Liveness)
Where: k8s/05-api.yaml (readinessProbe, livenessProbe).

Why: Kubernetes needs to know when a pod is ready to serve and when it should be restarted.

Benefit: zero-downtime rollouts. Automatic recovery.

Code reference:

yaml
readinessProbe:
  tcpSocket:
    port: 8080
  initialDelaySeconds: 60
2. Containerized Deployment
Where: Containerfile. Image built with Podman.

Why: reproducible deployments. The image is the unit of deployment.

Benefit: "works on my machine" becomes "works everywhere".

3. Immutable Infrastructure
Where: container image finopsbank-api:v1.0. Config comes from environment variables.

Why: no configuration drift. The image is immutable; config is injected.

Benefit: predictable deployments. Easy rollbacks.

4. Externalized Configuration
Where: ConfigMap and Secret in Kubernetes.

Why: separates config from code. Same image runs in dev/staging/prod with different config.

Benefit: 12-Factor compliant. Same artifact everywhere.

5. Sidecar (not used, but compatible)
Where: could be added later (logging, tracing).

Why: extends functionality without modifying the app.

Benefit: modular. Adopted only when needed.

6. Service Discovery (via Kubernetes DNS)
Where: services are reached via DNS names (postgres-service, kafka-service).

Why: no hardcoded IPs. Kubernetes handles service resolution.

Benefit: dynamic. Services can be moved without changing config.

Anti-Patterns Avoided
1. Anemic Domain Model
Avoided by: not using entities as pure data bags. Business logic lives in the service layer with clear responsibility.

Better: as the project grows, behavior can migrate to the domain (e.g., Account.withdraw(amount)).

2. Fat Controllers
Avoided by: controllers only handle HTTP concerns and delegate to services.

3. God Object
Avoided by: multi-module structure. Each class has a single responsibility.

4. Leaky Abstraction
Avoided by: DTOs at the API boundary. JPA entities are not exposed.

5. Circular Dependencies
Avoided by: Gradle enforces module dependency direction. domain cannot depend on app.

6. Hidden Dependencies
Avoided by: constructor injection. No @Autowired on fields.

7. Global State
Avoided by: no static mutable fields. All state is in beans (singletons) or thread-local (SecurityContext).

8. Premature Optimization
Avoided by: simple, correct code first. Optimizations added when profiling shows a need.

Pattern Interactions
Some patterns work together to solve a problem:

Security + Chain of Responsibility + Strategy
The JWT authentication flow uses:

Chain of Responsibility (FilterChain) to run filters in order.

Strategy (JwtTokenProvider) to validate tokens.

Singleton (filter as a bean) to share the filter across requests.

Repository + Adapter + Dependency Inversion
The persistence layer uses:

Repository (SpringDataAccountRepository) to abstract data access.

Adapter to bridge the domain interface and Spring Data.

DIP so the domain depends on abstractions, not JPA.

Service + Facade + Transaction Script
The AccountService combines:

Service Layer for use case orchestration.

Facade for a simplified API over repositories and Kafka.

Transaction Script via @Transactional for atomic operations.

Outbox + Producer-Consumer + Repository
Event publishing uses:

Outbox to guarantee atomicity.

Producer-Consumer to decouple the publisher and consumers.

Repository to persist events before publishing.

Further Reading
Books
"Design Patterns" — Gang of Four (Gamma, Helm, Johnson, Vlissides)

"Patterns of Enterprise Application Architecture" — Martin Fowler

"Enterprise Integration Patterns" — Hohpe & Woolf

"Clean Architecture" — Robert C. Martin

"Implementing Domain-Driven Design" — Vaughn Vernon

"Microservices Patterns" — Chris Richardson

Online
Refactoring.Guru - Design Patterns

Martin Fowler - Catalog of Patterns

Microservices.io Patterns

12-Factor App

Cloud Native Patterns

<p align="center"> <strong>FinOpsBank</strong> - Design Patterns </p>