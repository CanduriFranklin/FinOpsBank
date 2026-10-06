# Changelog

All notable changes to the **FinOpsBank API** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned

- Kubernetes deployment manifests (Kind or Minikube cluster)
- Replace in-memory users with database-backed `UserDetailsService`
- Swagger/OpenAPI documentation (springdoc-openapi)
- Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/mappings`)
- Refresh token support
- Migrate to `@TransactionalEventListener(phase = AFTER_COMMIT)` for event publishing
- Scheduled Outbox processor with retry logic
- Rate limiting on `/api/v1/auth/login`
- Docker Compose / Podman Compose stack (`compose.yaml`)

---

## [1.0.0-SNAPSHOT] - 2026-10-06

### Major Migration

Complete migration from a console-based OOP application to a containerized REST API with modern infrastructure.

### Added

#### Security Layer

- `SecurityConfig` with Spring Security 7 configuration
- `JwtTokenProvider` for JWT generation and validation (HS512)
- `JwtAuthenticationFilter` as a custom filter in the security chain
- `AuthController` exposing `POST /api/v1/auth/login`
- `LoginRequest` and `LoginResponse` DTO records
- Three roles: `ROLE_ADMIN`, `ROLE_TELLER`, `ROLE_CUSTOMER`
- BCrypt password encoder with 12 rounds
- Stateless session management (`SessionCreationPolicy.STATELESS`)
- URL-based authorization rules
- `AbstractHttpConfigurer::disable` for CSRF
- `/error` endpoint permitted to expose real errors after internal forwards
- `hasAnyAuthority` for role checks (avoiding Spring Security 7 prefix issues)

#### Domain and Persistence

- `customerId` field added to `AccountEntity` (FK to `customers.id`)
- `customerId` field added to `CreateAccountRequest` DTO
- `AccountService.createAccount()` updated to accept the new parameter
- Database schema: `customers`, `accounts`, `transactions`, `outbox_events` tables
- Foreign key constraints and check constraints for data integrity

#### Tests

- `AccountSecurityIntegrationTest` for role and permission verification
- `AccountConcurrencyStressTest` for concurrency and stress testing
- `AccountServiceTest` for pure business logic
- `tests/performance/k6-load-test.js` for load testing with k6

#### Documentation

- Complete rewrite of `README.md` in English, reflecting the new stack
- `docs/README.md` — documentation index
- `docs/ARCHITECTURE.md` — detailed architecture with request flow
- `docs/SETUP.md` — step-by-step installation guide
- `docs/API.md` — complete REST API reference
- `docs/SECURITY.md` — JWT, roles, CSRF explanation
- `docs/KAFKA.md` — KRaft configuration, topics, Outbox pattern
- `docs/TROUBLESHOOTING.md` — known issues and solutions
- `docs/CHANGELOG.md` — this file
- `docs/CONTRIBUTING.md` — contribution guidelines
- `docs/KUBERNETES.md` — Kubernetes deployment plan
- `docs/patterns/OUTBOX_PATTERN.md` — Outbox pattern deep dive
- `docs/patterns/LAYERED_ARCHITECTURE.md` — Layered architecture deep dive

#### Repository Hygiene

- `.gitignore` consolidated (IDE, build, backups, logs)
- Removed `.idea/` from version control
- Removed `application.properties` duplicate (now uses only `application.yml`)
- Removed temporary backup files

### Changed

#### Technology Stack

| Component | Before | After |
|---|---|---|
| Spring Boot | 3.4.3 | 4.1.1 |
| Spring Framework | 6.2.3 | 7.0.9 |
| Spring Security | 6.x | 7.1.1 |
| Hibernate | 6.6.8 | 7.4.5 |
| Java | (implied 17+) | 25 LTS |
| Gradle Wrapper | 9.1.0 | 9.8.0 |
| Kafka clients | 3.8.1 | 4.2.1 |
| PostgreSQL driver | 42.7.5 | 42.7.13 |
| HikariCP | 5.1.0 | 7.0.2 |
| Tomcat | 10.1.36 | 11.0.24 |

#### Code Changes

- `FinOpsBankApplication.java` updated for Boot 4 `@EntityScan` package
- `AccountService.createAccount()` signature updated
- `application.yml` rewritten with new datasource, Kafka, and logging configuration
- `Containerfile` reviewed and adjusted

#### Configuration

- Kafka configured with **multiple listeners**:
  - `PLAINTEXT` on port 9092 for Windows clients
  - `PLAINTEXT_HOST` on port 9094 for container clients
  - `CONTROLLER` on port 9093 for KRaft internal communication
- `advertised.listeners` uses the WSL virtual adapter IP (`172.x.x.1`)
- `log.dirs` moved from `/tmp/kraft-combined-logs` to `C:/kafka/kafka_2.13-4.3.1/kraft-logs`
- Datasource URL uses WSL IP instead of `localhost`

### Fixed

- `crun: controller 'pids' is not available` — resolved with `cgroupfs` cgroup manager
- `ClassNotFoundException: FinopsbankApplication` — fixed `mainClass` in Gradle
- Jar with old manifest — resolved with `podman build --no-cache`
- `Unsupported class file major version 69` — resolved with Spring Boot 4.1.1
- `package org.springframework.boot.autoconfigure.domain does not exist` — updated import
- `KafkaTemplate not found` — added `spring-boot-starter-kafka`
- `database "finopsbank" does not exist` — renamed to `finopsbank_db`
- `Connection refused` from container — use WSL IP instead of `localhost`
- Kafka `Timed out waiting for a node assignment` — multiple listeners
- Podman 6.0.2 port forwarding bug — upgraded to Podman 6.1.3
- Podman machine split-brain — resolved with `wsl --shutdown` + `podman machine rm/init`
- `customer_id NOT NULL constraint violation` — added `customerId` to entity and DTO
- Misleading `403` on POST — caused by `/error` forward being blocked; fixed with `permitAll`
- `Invalid CSRF token` — disabled CSRF for stateless JWT API

### Removed

- `.idea/` folder from version control (IDE-specific configuration)
- `application.properties` (kept only `application.yml`)
- `application.properties.bak`
- `project_structure.txt`
- Old console-based README (preserved in `docs/phases/README_LEGACY_CONSOLE.md`)

### Security

- Upgraded Spring Security from 6.x to 7.1.1 (addresses multiple CVEs)
- Upgraded Kafka client to 4.2.1 (addresses CVE-2025-11395 and others)
- Upgraded HikariCP to 7.0.2 (addresses connection pool vulnerabilities)
- Upgraded PostgreSQL driver to 42.7.13

---

## Historical Versions

See `docs/phases/` for documentation of the console-based versions:

- `PHASE1_SKELETON_STRUCTURE.md` — Initial project structure
- `PHASE2_CONSOLE_AND_LOGIC.md` — Console logic and domain classes
- `PHASE3_TESTING_AND_STATUS.md` — Testing and status
- `PHASE4_MANUAL_TESTING.md` — Manual testing with screenshots
- `PROJECT_STATE.md` — Project state snapshot
- `ROADMAP.md` — Original roadmap
- `README_LEGACY_CONSOLE.md` — Original README (console app)

---

## Version History Summary

| Version | Date | Highlights |
|---|---|---|
| **1.0.0-SNAPSHOT** | 2026-10-06 | REST API, JWT, PostgreSQL, Kafka, Podman, Spring Boot 4 |
| **0.x (console)** | 2026-09 to 2026-10 | Console app, OOP, in-memory repository, JUnit tests |

---

## How to Maintain This File

1. **Add entries under `[Unreleased]`** as you work.
2. **When releasing**, move entries from `[Unreleased]` to a new version section with a date.
3. **Use categories**: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`.
4. **Link to issues/PRs** when applicable.
5. **Follow [Keep a Changelog](https://keepachangelog.com/)** conventions.

---

## Further Reading

- [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
- [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

<p align="center">
  <strong>FinOpsBank</strong> - Changelog
</p>