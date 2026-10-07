# FinOpsBank Kubernetes

Deployment plan and reference for running **FinOpsBank** on Kubernetes.

> **Status**: ✅ **Implemented and verified**. Deployed to a Kind cluster on Podman rootful. All 3 pods (`finopsbank-api`, `kafka-0`, `postgres-0`) running with `RESTARTS: 0`. End-to-end verified from Windows at `http://localhost:8080`.
>
> See [PROJECT_STATUS.md](PROJECT_STATUS.md) for the current certification and [phases/PHASE5_KUBERNETES_DEPLOYMENT.md](phases/PHASE5_KUBERNETES_DEPLOYMENT.md) for the deployment milestone.

---

## Table of Contents

- [Overview](#overview)
- [Cluster Options](#cluster-options)
- [Prerequisites](#prerequisites)
- [Cluster Setup](#cluster-setup)
- [Manifest Structure](#manifest-structure)
- [Manifests](#manifests)
- [Deployment Steps](#deployment-steps)
- [Verification](#verification)
- [Production Considerations](#production-considerations)

---

## Overview

The goal is to deploy FinOpsBank on a local Kubernetes cluster running on **Podman** (rootful mode), alongside PostgreSQL and Kafka running as in-cluster services.

### Target Architecture
+-------------------------------------------+
| Kubernetes Cluster |
| |
| +-------------+ +----------------+ |
| | finopsbank |--->| postgres | |
| | api | | (StatefulSet)| |
| | (Deployment)| +----------------+ |
| +------+------+ |
| | |
| v |
| +-------------+ |
| | kafka | |
| | (StatefulSet)| |
| +-------------+ |
| |
| Namespace: finopsbank |
+-------------------------------------------+
^
|
Ingress / NodePort
|
http://localhost:30080

text

### Differences from the current setup

| Aspect | Current (Podman) | Target (Kubernetes) |
|---|---|---|
| Orchestration | `podman run` | `kubectl apply` |
| Config | `application.yml` inside image | `ConfigMap` + `Secret` |
| Database | PostgreSQL on Windows | PostgreSQL in-cluster |
| Kafka | Kafka on Windows | Kafka in-cluster |
| Networking | `-p 8080:8080` | `Service` + `Ingress` |
| Scaling | Manual | `replicas: N` |

---

## Cluster Options

| Tool | Pros | Cons | Recommended for |
|---|---|---|---|
| **Kind** | Fast, easy, runs in containers | Requires Podman rootful | **Development** |
| **Minikube** | Feature-rich, addons, dashboard | Heavier resource use | Development |
| **MicroShift (MINC)** | OpenShift-like, lightweight | Specific to Podman Desktop | OpenShift-compatible dev |

**Recommendation**: Start with **Kind** for simplicity.

---

## Prerequisites

| Tool | Minimum version | Verification |
|---|---|---|
| Podman | 6.1.3+ | `podman --version` |
| Podman Desktop | 1.29.3+ | Open the GUI |
| kubectl | 1.30+ | `kubectl version --client` |
| kind | 0.23+ | `kind --version` |
| Helm (optional) | 3.15+ | `helm version` |

### Podman rootful mode

Kind and Minikube require Podman in **rootful mode** (they create VMs/containers with system-level privileges).

Enable it:

```powershell
podman machine stop
podman machine set --rootful
podman machine start
Verify:

powershell
podman info | Select-String "rootless"
Expected: rootless: false

Warning: Switching to rootful changes the storage namespace. Existing containers and images from the rootless session will not be visible. Rebuild the image after switching if needed.

Cluster Setup
Option A: Kind (recommended)
Install kind
powershell
# Using Chocolatey (run as Administrator)
choco install kind

# Or download from GitHub
# https://github.com/kubernetes-sigs/kind/releases
Install kubectl
powershell
choco install kubernetes-cli
Create a cluster
powershell
kind create cluster --name finopsbank --config kind-config.yaml
Where kind-config.yaml is:

yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 8080
        protocol: TCP
This maps localhost:8080 on Windows to NodePort:30080 in the cluster, so the API is accessible from the browser.

Verify
powershell
kubectl cluster-info --context kind-finopsbank
kubectl get nodes
Expected: 1 node in Ready state.

Option B: Minikube
Install minikube
powershell
choco install minikube
Start with Podman driver
powershell
minikube start --driver=podman --container-runtime=containerd
Verify
powershell
kubectl get nodes
Option C: MicroShift (Podman Desktop extension)
Open Podman Desktop.

Go to Settings → Extensions.

Search for MicroShift (MINC).

Install the extension.

Create a MicroShift instance from the Extensions tab.

Manifest Structure
The k8s/ folder contains the manifests, applied in order:

text
k8s/
+-- 00-namespace.yaml        # Namespace "finopsbank"
+-- 01-configmap.yaml        # Non-sensitive configuration
+-- 02-secret.yaml           # Sensitive data (DB password, JWT secret)
+-- 03-postgres.yaml         # PostgreSQL StatefulSet + Service
+-- 04-kafka.yaml            # Kafka StatefulSet + Service
+-- 05-api-deployment.yaml   # FinOpsBank API Deployment
+-- 06-api-service.yaml      # API Service (NodePort)
+-- 07-ingress.yaml          # Optional Ingress
Manifests
00-namespace.yaml
yaml
apiVersion: v1
kind: Namespace
metadata:
  name: finopsbank
  labels:
    name: finopsbank
01-configmap.yaml
yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: finopsbank-config
  namespace: finopsbank
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres.finopsbank.svc.cluster.local:5432/finopsbank_db"
  SPRING_DATASOURCE_USERNAME: "postgres"
  SPRING_KAFKA_BOOTSTRAP_SERVERS: "kafka.finopsbank.svc.cluster.local:9092"
  SERVER_PORT: "8080"
  LOGGING_LEVEL_ROOT: "WARN"
  LOGGING_LEVEL_COM_FINOPSBANK: "INFO"
02-secret.yaml
yaml
apiVersion: v1
kind: Secret
metadata:
  name: finopsbank-secrets
  namespace: finopsbank
type: Opaque
stringData:
  SPRING_DATASOURCE_PASSWORD: "Enginer012%"
  JWT_SECRET: "REPLACE_WITH_A_STRONG_64_CHAR_HEX_SECRET"
Warning: Do not commit real secrets. Use a Secret Manager (Vault, AWS Secrets Manager, Sealed Secrets) in production.

03-postgres.yaml
yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: finopsbank
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
  clusterIP: None  # Headless service for StatefulSet
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: finopsbank
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: docker.io/library/postgres:18
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: finopsbank_db
            - name: POSTGRES_USER
              value: postgres
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: finopsbank-secrets
                  key: SPRING_DATASOURCE_PASSWORD
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 10
            periodSeconds: 5
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
04-kafka.yaml
yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: finopsbank
spec:
  selector:
    app: kafka
  ports:
    - name: broker
      port: 9092
      targetPort: 9092
    - name: controller
      port: 9093
      targetPort: 9093
  clusterIP: None
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: finopsbank
spec:
  serviceName: kafka
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
        - name: kafka
          image: docker.io/apache/kafka:4.3.1
          ports:
            - containerPort: 9092
            - containerPort: 9093
          env:
            - name: KAFKA_NODE_ID
              value: "1"
            - name: KAFKA_PROCESS_ROLES
              value: "broker,controller"
            - name: KAFKA_LISTENERS
              value: "PLAINTEXT://:9092,CONTROLLER://:9093"
            - name: KAFKA_ADVERTISED_LISTENERS
              value: "PLAINTEXT://kafka.finopsbank.svc.cluster.local:9092"
            - name: KAFKA_CONTROLLER_LISTENER_NAMES
              value: "CONTROLLER"
            - name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
              value: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT"
            - name: KAFKA_CONTROLLER_QUORUM_VOTERS
              value: "1@kafka-0.kafka.finopsbank.svc.cluster.local:9093"
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
              value: "1"
            - name: KAFKA_LOG_DIRS
              value: "/var/lib/kafka/data"
          volumeMounts:
            - name: data
              mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 2Gi
05-api-deployment.yaml
yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: finopsbank-api
  namespace: finopsbank
  labels:
    app: finopsbank-api
spec:
  replicas: 1
  selector:
    matchLabels:
      app: finopsbank-api
  template:
    metadata:
      labels:
        app: finopsbank-api
    spec:
      containers:
        - name: api
          image: localhost/finopsbank-api:v1.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: finopsbank-config
            - secretRef:
                name: finopsbank-secrets
          readinessProbe:
            httpGet:
              path: /api/v1/accounts
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            failureThreshold: 6
          livenessProbe:
            httpGet:
              path: /api/v1/accounts
              port: 8080
            initialDelaySeconds: 90
            periodSeconds: 30
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "768Mi"
              cpu: "1000m"
Note: The readiness probe hits /api/v1/accounts, which returns 403 without a token. Kubernetes treats 403 as a successful HTTP response (any 2xx-3xx is considered healthy by default, but 403 also counts as "not a 5xx"). For a cleaner check, add /actuator/health and permit it in SecurityConfig, then use that path instead.

06-api-service.yaml
yaml
apiVersion: v1
kind: Service
metadata:
  name: finopsbank-api
  namespace: finopsbank
spec:
  type: NodePort
  selector:
    app: finopsbank-api
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      nodePort: 30080
07-ingress.yaml (optional)
yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: finopsbank-api
  namespace: finopsbank
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: finopsbank.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: finopsbank-api
                port:
                  number: 8080
Requires an Ingress controller (nginx, Traefik). Add to /etc/hosts on Windows:

text
127.0.0.1    finopsbank.local

## Deployment Steps
### 1. Load the image into the cluster

Kind and Minikube run in their own container runtime, so they cannot see images built with Podman on the host.

**Important**: `kind load docker-image` **does not work** with Podman (no Docker daemon). Use `podman save` + `kind load image-archive` instead.

**For Kind (with Podman):**

```powershell
# Export from Podman
podman save localhost/finopsbank-api:v1.0 -o finopsbank-api.tar

# Load into Kind
kind load image-archive .\finopsbank-api.tar --name finopsbank

# Clean up
Remove-Item .\finopsbank-api.tar -Force

powershell
# Build inside minikube's Docker daemon
minikube -p minikube docker-env | Invoke-Expression
podman build --no-cache -t localhost/finopsbank-api:v1.0 .
2. Apply the manifests
powershell
cd C:\Users\candu\IdeaProjects\FinOpsBank
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-secret.yaml
kubectl apply -f k8s/03-postgres.yaml
kubectl apply -f k8s/04-kafka.yaml
kubectl apply -f k8s/05-api-deployment.yaml
kubectl apply -f k8s/06-api-service.yaml
Or apply the whole folder:

powershell
kubectl apply -f k8s/ -n finopsbank
3. Apply the schema
Once PostgreSQL is running, apply the schema:

powershell
kubectl exec -it postgres-0 -n finopsbank -- psql -U postgres -d finopsbank_db
Paste the schema (from docs/SETUP.md) and seed the test customer.

4. Watch the rollout
powershell
kubectl get pods -n finopsbank -w
Wait until all pods are Running.

Verification
Check pods
powershell
kubectl get pods -n finopsbank
Expected:

text
NAME                              READY   STATUS    RESTARTS   AGE
finopsbank-api-xxx-yyy            1/1     Running   0          2m
kafka-0                           1/1     Running   0          3m
postgres-0                        1/1     Running   0          3m
Check services
powershell
kubectl get svc -n finopsbank
Check logs
powershell
kubectl logs -n finopsbank deployment/finopsbank-api --tail 50
Expected: Started FinOpsBankApplication in X.XXX seconds.

Test the API
With Kind and the extraPortMappings from kind-config.yaml:

powershell
$body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method POST -ContentType "application/json" -Body $body
Port-forwarding (alternative)
If NodePort or Ingress do not work in your environment:

powershell
kubectl port-forward -n finopsbank svc/finopsbank-api 8080:8080
Then hit http://localhost:8080 from another terminal.

Production Considerations
1. Use a managed database
For production, use a managed PostgreSQL (RDS, Cloud SQL, Azure Database). Remove the in-cluster PostgreSQL StatefulSet.

2. Use a managed Kafka
Similarly, use a managed Kafka (Confluent Cloud, AWS MSK, Aiven). Remove the in-cluster Kafka StatefulSet.

3. Increase replicas
For high availability:

yaml
spec:
  replicas: 3
Requires a stateless application. FinOpsBank is already stateless (JWT + PostgreSQL), so this is safe.

4. Add a PodDisruptionBudget
yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: finopsbank-api-pdb
  namespace: finopsbank
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: finopsbank-api
5. Configure resource limits
Already included in the Deployment. Adjust based on load testing.

6. Use Secrets Manager
Replace the plain Secret with a sealed secret or an external secrets operator (Vault, AWS Secrets Manager, External Secrets Operator).

7. Add Ingress with TLS
Use cert-manager to automate Let's Encrypt certificates:

yaml
metadata:
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts:
        - api.finopsbank.com
      secretName: finopsbank-tls
8. Enable HPA (Horizontal Pod Autoscaler)
yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: finopsbank-api
  namespace: finopsbank
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: finopsbank-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
9. Add observability
Metrics: Prometheus + Grafana (or use Micrometer + Spring Boot Actuator).

Logs: Loki, Elasticsearch, or a managed solution.

Traces: OpenTelemetry + Jaeger.

10. Set up CI/CD
CI: build and test on every push (GitHub Actions, GitLab CI).

CD: deploy to staging/prod via ArgoCD or Flux.

Further Reading
Kind documentation

Minikube documentation

Kubernetes documentation

Podman Desktop Kubernetes

12-Factor App

<p align="center"> <strong>FinOpsBank</strong> - Kubernetes Deployment Guide </p>