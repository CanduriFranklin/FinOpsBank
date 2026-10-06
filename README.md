# FinOpsBank API

> **Modern banking backend** built with Spring Boot 4, Java 25 LTS, PostgreSQL, Apache Kafka and containerized deployment with Podman on WSL2.

[![Java](https://img.shields.io/badge/Java-25%20LTS-orange?logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.8.0-blue?logo=gradle)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.4-blue?logo=postgresql)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Kafka-4.3.1-black?logo=apachekafka)](https://kafka.apache.org/)
[![Podman](https://img.shields.io/badge/Podman-6.1.3-purple?logo=podman)](https://podman.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [API Endpoints](#api-endpoints)
- [JWT Authentication](#jwt-authentication)
- [Kafka Messaging](#kafka-messaging)
- [Testing](#testing)
- [Kubernetes](#kubernetes)
- [Additional Documentation](#additional-documentation)
- [License](#license)

---

## Overview

**FinOpsBank** is a REST API for banking services built with a clean, modular architecture. It provides core financial operations such as account management, deposits, withdrawals and transfers, with JWT authentication, role-based authorization and event publishing to Apache Kafka through the **Outbox pattern**.

### Key Features

- **Multi-module architecture** (domain, infrastructure, app)
- **Secure REST API** with stateless JWT and roles (ADMIN, TELLER, CUSTOMER)
- **PostgreSQL persistence** with Hibernate/JPA
- **Asynchronous messaging** with Apache Kafka in KRaft mode
- **Podman containerization** rootless on WSL2
- **Java 25 LTS** with Spring Boot 4.1.1
- **Gradle 9.8** with Kotlin DSL and toolchains

---

## Tech Stack

| Technology | Version | Purpose | Documentation |
|---|---|---|---|
| **Java (OpenJDK)** | 25 LTS | Language & platform | [OpenJDK 25](https://openjdk.org/projects/jdk/25/) |
| **Spring Boot** | 4.1.1 | Application framework | [Spring Boot Docs](https://docs.spring.io/spring-boot/index.html) |
| **Spring Security** | 7.x | Authentication & authorization | [Spring Security Docs](https://docs.spring.io/spring-security/reference/index.html) |
| **Spring Data JPA** | 4.x (Hibernate 7) | ORM persistence | [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/index.html) |
| **Spring Kafka** | 4.x | Kafka client | [Spring Kafka Docs](https://docs.spring.io/spring-kafka/reference/index.html) |
| **Gradle** | 9.8.0 | Build system | [Gradle 9.8](https://docs.gradle.org/9.8.0/release-notes.html) |
| **PostgreSQL** | 18.4 | Relational database | [PostgreSQL 18](https://www.postgresql.org/docs/18/index.html) |
| **Apache Kafka** | 4.3.1 (KRaft) | Message broker | [Kafka 4.3.1](https://kafka.apache.org/documentation/) |
| **JJWT** | 0.12.6 | JWT generation & validation | [JJWT GitHub](https://github.com/jwtk/jjwt) |
| **Podman** | 6.1.3 | Rootless container engine | [Podman Docs](https://docs.podman.io/) |
| **Podman Desktop** | 1.29.3 | Container management GUI | [Podman Desktop](https://podman-desktop.io/) |
| **WSL2** | 2.x | Linux Subsystem on Windows | [WSL Docs](https://learn.microsoft.com/windows/wsl/) |

### Official Links by Technology

**Java & Spring Ecosystem:**
- [Java 25 Release Notes (Oracle)](https://www.oracle.com/java/technologies/javase/25-relnote-issues.html)
- [Spring Boot 4.1.1 Release Notes](https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now)
- [Spring Security 7.0 Migration Guide](https://docs.spring.io/spring-security/reference/whats-new.html)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/)

**Database & Messaging:**
- [PostgreSQL 18 Features](https://www.postgresql.org/about/news/postgresql-18-released-3126/)
- [Apache Kafka 4.3.1 Release Notes](https://dist.apache.org/repos/dist/dev/kafka/4.3.1-rc2/RELEASE_NOTES.html)

**Containers & Orchestration:**
- [Podman Installation Guide](https://podman.io/docs/installation)
- [Podman Desktop Documentation](https://podman-desktop.io/docs)
- [Kubernetes with Podman Desktop](https://podman-desktop.io/docs/kubernetes)

**Build & JWT:**
- [Gradle 9.8.0 Documentation](https://docs.gradle.org/9.8.0/userguide/userguide.html)
- [JJWT 0.12.6 API Reference](https://javadoc.io/doc/io.jsonwebtoken/jjwt-api/0.12.6/index.html)

---

## Architecture
FinOpsBank/
+-- app/ # Main module (Spring Boot, controllers, security)
| +-- src/main/java/com/finopsbank/
| | +-- config/ # SecurityConfig, beans
| | +-- controller/ # AuthController, AccountController, TransactionController
| | +-- dto/ # Request/response records
| | +-- security/ # JwtTokenProvider, JwtAuthenticationFilter
| | +-- service/ # AccountService (business logic)
| +-- src/main/resources/ # application.yml
+-- domain/ # Pure domain model
+-- infrastructure/ # JPA persistence, Kafka, adapters
+-- k8s/ # Kubernetes manifests (pending)
+-- docs/ # Additional documentation
+-- Containerfile # OCI image

text

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

### Outbox Pattern

The project uses the **Outbox pattern** to guarantee eventual consistency between the database and Kafka:

1. The business operation is persisted in `accounts`/`transactions`.
2. The event is stored in `outbox_events`.
3. A publisher reads from `outbox_events` and publishes to Kafka.
4. Consumers process events asynchronously.

---

## Prerequisites

| Tool | Minimum version | Verification |
|---|---|---|
| Windows 10/11 | 22H2+ | `winver` |
| WSL2 | 2.0+ | `wsl --version` |
| Java JDK | 25 LTS | `java --version` |
| Podman | 6.1.3+ | `podman --version` |
| PostgreSQL | 18+ | `psql --version` |
| Apache Kafka | 4.3.1+ | `kafka-server-start.bat --version` |

> **Important**: Podman Desktop 6.0.2 has a known port forwarding bug on WSL2. Use **6.1.3 or higher** which fixes this issue.

See [docs/SETUP.md](docs/SETUP.md) for the complete installation guide.

---

## Installation

### 1. Clone the repository

```powershell
git clone https://github.com/CanduriFranklin/FinOpsBank.git
cd FinOpsBank
2. Build the project
powershell
.\gradlew clean :app:bootJar
3. Build the OCI image
powershell
podman build --no-cache -t localhost/finopsbank-api:v1.0 .
4. Run the container
powershell
podman run -d --name finopsbank-api -p 8080:8080 localhost/finopsbank-api:v1.0
Start-Sleep -Seconds 60
Check logs:

powershell
podman logs --tail 20 finopsbank-api
Expected output: Started FinOpsBankApplication in X.XXX seconds

API Endpoints
Authentication
Method	Endpoint	Description	Auth
POST	/api/v1/auth/login	Get JWT token	No
Example:

powershell
$body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $body
$token = $r.token
Accounts
Method	Endpoint	Description	Required Role
POST	/api/v1/accounts	Create account	ADMIN, TELLER
GET	/api/v1/accounts	List accounts	Authenticated
GET	/api/v1/accounts/{accountNumber}	Get account	Authenticated
Transactions
Method	Endpoint	Description	Required Role
POST	/api/v1/transactions/deposit	Deposit	Authenticated
POST	/api/v1/transactions/withdraw	Withdraw	Authenticated
POST	/api/v1/transactions/transfer	Transfer	Authenticated
See docs/API.md for the complete reference.

JWT Authentication
Test Users
User	Password	Role
admin	admin123	ROLE_ADMIN
teller	teller123	ROLE_TELLER
customer	customer123	ROLE_CUSTOMER
Important: Users are stored in memory (AuthController) as a placeholder. In production, replace with a UserDetailsService connected to the database.

Using the Token
powershell
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts" -Headers $headers
JWT Structure
json
{
  "sub": "admin",
  "role": "ROLE_ADMIN",
  "iat": 1791318294,
  "exp": 1791404694
}
Algorithm: HS512 (HMAC-SHA512)

Expiration: 24 hours

Custom claim: role

See docs/SECURITY.md for full details.

Kafka Messaging
Topics
Topic	Purpose
finopsbank-transactions	Account and transaction events
__consumer_offsets	Consumer offsets (internal)
Outbox Pattern
text
[Service] -> [PostgreSQL: outbox_events] -> [Publisher] -> [Kafka] -> [Consumer]
See docs/KAFKA.md for full configuration.

Testing
Unit and integration tests
powershell
.\gradlew test
Included tests:

AccountSecurityIntegrationTest.java — role and permission verification

AccountConcurrencyStressTest.java — concurrency and stress

AccountServiceTest.java — pure business logic

tests/performance/k6-load-test.js — load testing with k6

Kubernetes
Status: Pending implementation. The k8s/ folder is reserved for manifests.

Recommended local cluster options
Tool	Description	Requirement
Kind	Kubernetes IN Docker	Podman rootful
Minikube	Full local cluster	Podman rootful
MicroShift (MINC)	Lightweight OpenShift cluster	Podman Desktop extension
See docs/KUBERNETES.md for the deployment plan.

Resources:

Kind

Minikube with Podman

Podman Desktop Kubernetes

Additional Documentation
Document	Description
docs/README.md	Documentation index
docs/ARCHITECTURE.md	Detailed system architecture
docs/SETUP.md	Step-by-step installation guide
docs/API.md	Complete API reference
docs/SECURITY.md	JWT authentication, roles and CSRF
docs/KAFKA.md	KRaft configuration, topics and Outbox
docs/KUBERNETES.md	Kubernetes deployment
docs/TROUBLESHOOTING.md	Known issues and solutions
docs/CHANGELOG.md	Change history
docs/CONTRIBUTING.md	Contribution guide
docs/phases/	Historical documentation (phases 1-4)
docs/patterns/	Applied design patterns
Logging by Environment
Development/Documentation (current):

yaml
logging:
  level:
    root: INFO
    org.springframework.security: TRACE
    org.springframework.security.web: TRACE
    com.finopsbank.security: TRACE
    com.finopsbank: DEBUG
Production (recommended):

yaml
logging:
  level:
    root: WARN
    com.finopsbank: INFO
Recommendation: Use Spring profiles (application-dev.yml, application-prod.yml) and activate with SPRING_PROFILES_ACTIVE.

Contributing
See docs/CONTRIBUTING.md for commit conventions, branches and pull requests.

License
This project is licensed under the Apache License 2.0. See LICENSE for details.

Acknowledgments
Red Hat for Podman and Podman Desktop

Spring Team for Spring Boot and Spring Security

Oracle for Java 25 LTS

Apache Software Foundation for Kafka

PostgreSQL Global Development Group

<p align="center"> <strong>FinOpsBank</strong> - Modern banking Backend with Java 25 + Spring Boot 4 + Gradle </p>