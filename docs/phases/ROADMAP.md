# 🗺️ FinOpsBank - Refactoring & Evolution Roadmap

## Phase 1: Clean Architecture (Multimodule) 🟢 (In Progress)
- [ ] Extract core logic to the domain module.
- [ ] Extract in-memory persistence to the infrastructure module.
- [ ] Configure pp as the orchestrator and entry point.

## Phase 2: Design Patterns (GoF) in Pure Java ⏳ (Pending)
- [ ] **Strategy**: Dynamic fee calculation based on account type.
- [ ] **Chain of Responsibility**: Transaction validation pipeline (balance, limits, fraud).
- [ ] **Factory Method**: Dynamic account instance creation.

## Phase 3: Spring Boot Ecosystem ⏳ (Pending)
- [ ] Dependency Injection (IoC) and Inversion of Control.
- [ ] RESTful API exposure (Spring Web).
- [ ] Repository migration to Spring Data JPA with PostgreSQL.

## Phase 4: Event-Driven Architecture ⏳ (Pending)
- [ ] Apache Kafka integration for emitting financial audit events.
