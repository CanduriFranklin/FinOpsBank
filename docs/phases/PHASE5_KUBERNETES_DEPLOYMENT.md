# Phase 5: Kubernetes Deployment

**Historical milestone: deployment of FinOpsBank to Kubernetes.**

---

## Table of Contents

- [Overview](#overview)
- [Starting Point](#starting-point)
- [Objective](#objective)
- [Work Performed](#work-performed)
- [Challenges Encountered](#challenges-encountered)
- [Final State](#final-state)
- [Lessons Learned](#lessons-learned)
- [Artifacts](#artifacts)

---

## Overview

**Phase**: 5 (Kubernetes Deployment)
**Date**: 2026-10-07
**Duration**: Single session (~14 hours)
**Outcome**: Successful deployment of FinOpsBank to a local Kubernetes cluster (Kind on Podman rootful).

---

## Starting Point

At the end of Phase 4, FinOpsBank was:

- ✅ Working as a REST API with Spring Boot 4.1.1 + Java 25
- ✅ Persisted in PostgreSQL (locally on Windows)
- ✅ Publishing events to Kafka (locally on Windows)
- ✅ Authenticated with JWT
- ✅ Containerized with Podman (rootless, WSL2)
- ✅ Accessible at `http://localhost:8080` from Windows

The application was running as a **single container** on Podman, connected to services on the Windows host. This worked but had limitations:

- No orchestration
- No automatic recovery
- No declarative configuration
- No horizontal scaling
- Manual startup of every component

---

## Objective

Deploy FinOpsBank to Kubernetes with:

1. **Declarative manifests** for all components (API, PostgreSQL, Kafka).
2. **Automatic recovery** if a component fails.
3. **Namespace isolation** (`finopsbank`).
4. **Externalized configuration** via ConfigMap and Secret.
5. **Persistent storage** via StatefulSets + PVCs.
6. **Health probes** for readiness and liveness.
7. **Exposure** to Windows via NodePort.

Local cluster chosen: **Kind** (Kubernetes IN Docker) running on **Podman rootful**.

---

## Work Performed

### 1. Environment preparation

- Changed Podman from rootless to rootful (`podman machine set --rootful`).
- Increased WSL memory from 2 GB to 6 GB via `.wslconfig`.
- Verified `.wslconfig` was named correctly (initially had `.txt` extension, blocking WSL from reading it).

### 2. Cluster creation

- Created `k8s/kind-config.yaml` with port mapping `hostPort 8080 → nodePort 30080`.
- Ran `kind create cluster --config .\k8s\kind-config.yaml`.
- Verified: `kubectl get nodes` → `finopsbank-control-plane Ready v1.29.2`.

### 3. Image loading

- Problem: `kind load docker-image` fails with Podman (no Docker daemon).
- Solution: `podman save` to tar + `kind load image-archive`.
- Verified: `crictl images` shows the image inside the node.

### 4. Manifest design

Replaced the original 5 mismatched manifests with 7 aligned files:

| File | Purpose |
|---|---|
| `00-namespace.yaml` | Namespace `finopsbank` |
| `01-configmap.yaml` | Non-sensitive config |
| `02-secret.yaml` | DB password + JWT secret |
| `03-postgres.yaml` | PostgreSQL 17 StatefulSet + PVC |
| `04-kafka.yaml` | Kafka 4.3.1 KRaft StatefulSet + PVC |
| `05-api.yaml` | API Deployment + NodePort 30080 |
| `kind-config.yaml` | Kind cluster with port mapping |

### 5. Deployment

- Applied namespace and manifests in order.
- Waited for pods to reach `1/1 Running`.
- Encountered `CrashLoopBackOff` on the API due to schema validation error.

### 6. Schema initialization

- Connected to `postgres-0` pod with `kubectl exec`.
- Applied the complete schema (customers, accounts, transactions, outbox_events).
- Seeded the customer `CUST-001`.

### 7. JPA-schema alignment

- **Problem**: `TransactionEntity` had `String id` but the table had `BIGSERIAL`.
- **Fix**: changed `Long id` with `@GeneratedValue(strategy = IDENTITY)`.
- Also removed `transactionReference` and `currency` fields (not in the schema).
- Changed `LocalDateTime` to `OffsetDateTime` for `created_at`.
- Updated `SpringDataTransactionRepository` to `JpaRepository<TransactionEntity, Long>`.

### 8. Verification

- Rebuilt image, loaded into Kind, restarted deployment.
- Pods reached `1/1 Running` with `RESTARTS: 0`.
- End-to-end verified from Windows: login, create account, list accounts, Kafka events.

---

## Challenges Encountered

### 1. `.wslconfig` with wrong extension

The file was named `.wslconfig.txt`, so WSL ignored it. Memory stayed at 2 GB, blocking Kind. Solution: renamed to `.wslconfig`.

### 2. `kind load docker-image` incompatible with Podman

Kind's `docker-image` command requires a Docker daemon. With Podman, the command fails with "image not present locally" even though `podman images` shows it.

**Solution**: use `podman save` + `kind load image-archive`.

### 3. Kubernetes CrashLoopBackOff

The API pod entered a restart loop because:
- The database was empty (no schema).
- Once the schema was applied, the `TransactionEntity` still had type mismatches.

**Solution**: applied the schema and fixed the entity mapping.

### 4. Kafka topic did not exist

The consumer logged `UNKNOWN_TOPIC_OR_PARTITION`. Kafka autocreated the topic on first connection, resolving the warning automatically.

### 5. Multiple pods during rollout

During `kubectl rollout restart`, two pods existed briefly (old terminating, new starting). This is expected behavior and does not affect availability.

---

## Final State

### Pods
$ kubectl get pods -n finopsbank
NAME READY STATUS RESTARTS AGE
finopsbank-api-79b455668d-qtj2m 1/1 Running 0 8m31s
kafka-0 1/1 Running 0 79m
postgres-0 1/1 Running 0 79m

text

### Services
$ kubectl get svc -n finopsbank
NAME TYPE CLUSTER-IP PORT(S)
finopsbank-api-service NodePort 10.x.x.x 8080:30080/TCP
kafka-service ClusterIP None 9092/TCP,9093/TCP
postgres-service ClusterIP None 5432/TCP

text

### End-to-end verification

- ✅ Login: `POST /api/v1/auth/login` → JWT
- ✅ Create account: `POST /api/v1/accounts` → persisted to PostgreSQL
- ✅ List accounts: `GET /api/v1/accounts` → returns data from DB
- ✅ Kafka: `[KAFKA PUBLISH]` and `[KAFKA CONSUME]` in logs
- ✅ External access: `http://localhost:8080` from Windows

### Metrics

- **Total manifests**: 7 files
- **Total lines of YAML**: ~350
- **Time to deploy (from scratch)**: ~10 minutes
- **Restarts**: 0 (after fixes)
- **Pods running**: 3

---

## Lessons Learned

### 1. Container engines are not interchangeable

`kind load docker-image` assumes Docker. With Podman, use `image-archive`. This is a general lesson: always check the container engine's compatibility with tooling.

### 2. Kubernetes recovers gracefully, but not magically

The CrashLoopBackOff showed Kubernetes doing its job: restarting the pod with backoff. But it cannot fix the underlying issue (missing schema, wrong entity type). **The engineer must fix the root cause.**

### 3. Schema validation is a contract

`ddl-auto: validate` catches mismatches between entities and tables at startup. This is a feature, not a bug: it forces the developer to keep them aligned.

### 4. WSL2 does not use `podman machine set` for memory

Memory must be set via `.wslconfig` (Windows side). `podman machine set --memory` only works on QEMU-based VMs (macOS/Linux).

### 5. Kind on Podman requires rootful

Kind creates containers with systemd inside. Podman rootless cannot provide the necessary privileges. Always set `podman machine set --rootful` before creating the cluster.

### 6. Externalized configuration is essential

Moving DB URL, Kafka servers, and secrets to ConfigMap/Secret means the same image can run in dev/staging/prod without rebuild.

---

## Artifacts

### Files created in this phase

- `k8s/00-namespace.yaml`
- `k8s/01-configmap.yaml`
- `k8s/02-secret.yaml`
- `k8s/03-postgres.yaml`
- `k8s/04-kafka.yaml`
- `k8s/05-api.yaml`
- `k8s/kind-config.yaml`

### Files modified in this phase

- `infrastructure/src/main/java/com/finopsbank/persistence/entity/TransactionEntity.java`
- `infrastructure/src/main/java/com/finopsbank/persistence/repository/SpringDataTransactionRepository.java`
- `docs/KUBERNETES.md` (was updated)

### Commits

- `b3edef3` — `feat(k8s): deploy FinOpsBank to Kubernetes and fix JPA-schema alignment`

---

## Next Phase

**Phase 6 — Observability and Production Readiness**

Planned work:

- Spring Boot Actuator (`/actuator/health`, `/actuator/mappings`)
- Swagger/OpenAPI (springdoc-openapi)
- Database-backed `UserDetailsService` (replace in-memory users)
- Environment-based secrets
- Refresh tokens
- Rate limiting

See [PROJECT_STATUS.md](../PROJECT_STATUS.md) for the full roadmap.

---

## Related Documentation

- [docs/KUBERNETES.md](../KUBERNETES.md) — Deployment guide
- [docs/SETUP.md](../SETUP.md) — Installation guide
- [docs/TROUBLESHOOTING.md](../TROUBLESHOOTING.md) — Known issues
- [docs/PROJECT_STATUS.md](../PROJECT_STATUS.md) — Current project status

---

<p align="center">
  <strong>FinOpsBank</strong> - Phase 5: Kubernetes Deployment
</p>