# FinOpsBank Documentation

General index of the **FinOpsBank API** project documentation.

---

## Main Documentation

| Document | Description | Audience |
|---|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Multi-module architecture, design decisions, flow diagram | Architects, Tech Leads |
| [SETUP.md](SETUP.md) | Step-by-step installation guide (WSL2, Kafka, Postgres, Podman) | Developers |
| [API.md](API.md) | Complete REST endpoint reference | Frontend, Consumers |
| [SECURITY.md](SECURITY.md) | JWT authentication, roles, CSRF, security filters | Backend, Security |
| [KAFKA.md](KAFKA.md) | KRaft configuration, topics, Outbox pattern | Backend, DevOps |
| [KUBERNETES.md](KUBERNETES.md) | Kubernetes cluster deployment guide | DevOps, SRE |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | Certification of current project state (v1.1.0) | Everyone |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Known errors and solutions | Everyone |
| [CHANGELOG.md](CHANGELOG.md) | Version history | Everyone |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution guide and conventions | Contributors |

---

## Historical Documentation

Initial project phase documents are kept in [`phases/`](phases/) for reference:

- [PHASE1_SKELETON_STRUCTURE.md](phases/PHASE1_SKELETON_STRUCTURE.md) - Initial skeleton structure
- [PHASE2_CONSOLE_AND_LOGIC.md](phases/PHASE2_CONSOLE_AND_LOGIC.md) - Console logic
- [PHASE3_TESTING_AND_STATUS.md](phases/PHASE3_TESTING_AND_STATUS.md) - Testing and status
- [PHASE4_MANUAL_TESTING.md](phases/PHASE4_MANUAL_TESTING.md) - Manual testing
- [PHASE5_KUBERNETES_DEPLOYMENT.md](phases/PHASE5_KUBERNETES_DEPLOYMENT.md) - Kubernetes deployment milestone
- [PROJECT_STATE.md](phases/PROJECT_STATE.md) - Project state (historical)
- [ROADMAP.md](phases/ROADMAP.md) - Roadmap (historical)
- [README_LEGACY_CONSOLE.md](phases/README_LEGACY_CONSOLE.md) - Original README (console app)

---

## Design Patterns

Applied in the project, documented in [`patterns/`](patterns/):

- [DESIGN_PATTERNS.md](patterns/DESIGN_PATTERNS.md) - Catalogue of GoF, enterprise, and cloud-native patterns
- [OUTBOX_PATTERN.md](patterns/OUTBOX_PATTERN.md) - Outbox pattern for reliable event publishing
- [LAYERED_ARCHITECTURE.md](patterns/LAYERED_ARCHITECTURE.md) - Layered architecture

---

## Detailed Architecture

- [01_MULTIMODULE_DESIGN.md](architecture/01_MULTIMODULE_DESIGN.md) - Multi-module design (Gradle)

---

## External Resources

### Spring Ecosystem

- [Spring Boot 4.1.1](https://docs.spring.io/spring-boot/index.html)
- [Spring Security 7](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/index.html)
- [Spring Kafka](https://docs.spring.io/spring-kafka/reference/index.html)

### Infrastructure

- [Podman](https://docs.podman.io/)
- [Podman Desktop](https://podman-desktop.io/docs)
- [WSL2](https://learn.microsoft.com/windows/wsl/)
- [PostgreSQL 18](https://www.postgresql.org/docs/18/index.html)
- [Apache Kafka](https://kafka.apache.org/documentation/)

### Java & Build

- [OpenJDK 25](https://openjdk.org/projects/jdk/25/)
- [Gradle 9.8](https://docs.gradle.org/9.8.0/userguide/userguide.html)
- [JJWT](https://github.com/jwtk/jjwt)

---

## How to Maintain This Documentation

1. **New documents**: add them to the main table with a new row.
2. **Major changes**: update `CHANGELOG.md` and affected sections.
3. **Deprecations**: move to `phases/` with a note in the document explaining why.
4. **Language**: keep documentation in English for consistency.
5. **Encoding**: always use UTF-8 without BOM when writing files.

---

<p align="center">
  <strong>FinOpsBank</strong> - Documentation
</p>