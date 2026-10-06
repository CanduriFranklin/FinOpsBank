# FinOpsBank Setup Guide

Step-by-step installation and configuration guide for the **FinOpsBank API** project on Windows with WSL2.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [1. Configure WSL2](#1-configure-wsl2)
- [2. Install Podman Desktop](#2-install-podman-desktop)
- [3. Setup PostgreSQL](#3-setup-postgresql)
- [4. Setup Apache Kafka](#4-setup-apache-kafka)
- [5. Configure Windows Firewall](#5-configure-windows-firewall)
- [6. Build the Application](#6-build-the-application)
- [7. Run the Container](#7-run-the-container)
- [8. Verify the System](#8-verify-the-system)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before starting, verify that you have the following installed and configured:

| Tool | Minimum version | Verification command |
|---|---|---|
| Windows 10/11 | 22H2+ | `winver` |
| WSL2 | 2.0+ | `wsl --version` |
| Java JDK | 25 LTS | `java --version` |
| Git | 2.30+ | `git --version` |
| PostgreSQL | 18+ | `psql --version` |
| Apache Kafka | 4.3.1+ | `kafka-server-start.bat --version` |
| Podman Desktop | 6.1.3+ | `podman --version` |

> **Warning**: Podman Desktop **6.0.2** has a known bug that breaks port forwarding from WSL2 to Windows. **Upgrade to 6.1.3 or higher** which fixes this issue.

---

## 1. Configure WSL2

### Enable systemd and cgroups v2

Create or edit `C:\Users\<your-user>\.wslconfig`:

```ini
[wsl2]
kernelCommandLine = cgroup_no_v1=all
memory=8GB
processors=4
Restart WSL
powershell
wsl --shutdown
Wait 10 seconds, then verify:

powershell
wsl --list --verbose
Both distributions should show Stopped. Then start Podman Desktop to bring them back up.

Verify cgroups v2
powershell
podman info | Select-String "cgroupManager"
Expected output:

text
cgroupManager: cgroupfs
If it shows systemd, the WSL configuration did not take effect. Re-run wsl --shutdown and try again.

2. Install Podman Desktop
Download
Get the latest MSI from the official releases page:

Podman Desktop Downloads

Podman Releases (GitHub)

Install
Run podman-installer-windows-amd64.msi.

Choose Install (not Repair or Uninstall).

Wait for the installation to complete.

Verify installation
Open a new PowerShell window and run:

powershell
podman --version
Expected: podman version 6.1.3 or higher.

Initialize the machine
powershell
podman machine init
podman machine start
Verify:

powershell
podman info | Select-String "cgroupManager|hostname"
Expected:

text
cgroupManager: cgroupfs
hostname: <your-machine-name>
3. Setup PostgreSQL
Install PostgreSQL 18
Download from postgresql.org and install with default settings.

Create the database
Open psql as the postgres user:

powershell
psql -h localhost -U postgres
Create the database:

sql
CREATE DATABASE finopsbank_db;
\c finopsbank_db
Apply the schema
Create the tables (accounts, customers, transactions, outbox_events). Example minimal schema:

sql
-- customers
CREATE TABLE customers (
    id              VARCHAR(255) PRIMARY KEY,
    document_number VARCHAR(255) NOT NULL UNIQUE,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(255),
    status          VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- accounts
CREATE TABLE accounts (
    account_number VARCHAR(255) PRIMARY KEY,
    customer_id    VARCHAR(36) NOT NULL,
    account_type   VARCHAR(255) NOT NULL CHECK (account_type IN ('SAVINGS', 'CHECKING', 'FINOPS_CORE')),
    currency       VARCHAR(3) NOT NULL DEFAULT 'USD',
    balance        NUMERIC(38,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    owner_name     VARCHAR(255) NOT NULL,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id);
Verify the database
sql
\dt
Expected output:

text
 public | accounts      | table | postgres
 public | customers     | table | postgres
 public | outbox_events | table | postgres
 public | transactions  | table | postgres
Seed a test customer
sql
INSERT INTO customers (id, document_number, first_name, last_name, email, phone, status)
VALUES ('CUST-001', '12345678', 'Candido', 'Admin', 'candu@finopsbank.com', '+584140000000', 'ACTIVE');
4. Setup Apache Kafka
Download Kafka 4.3.1
Download from kafka.apache.org/downloads. Extract to C:\kafka\kafka_2.13-4.3.1.

Configure server.properties
Edit C:\kafka\kafka_2.13-4.3.1\config\server.properties:

properties
process.roles=broker,controller
node.id=1
controller.quorum.bootstrap.servers=localhost:9093

# Multiple listeners: local + container
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,PLAINTEXT_HOST://0.0.0.0:9094
inter.broker.listener.name=PLAINTEXT
advertised.listeners=PLAINTEXT://localhost:9092,PLAINTEXT_HOST://<WSL_IP>:9094
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

log.dirs=C:/kafka/kafka_2.13-4.3.1/kraft-logs
Replace <WSL_IP> with the IP of your vEthernet (WSL) adapter. Find it with:

powershell
ipconfig | Select-String "IPv4"
Look for the adapter vEthernet (WSL) — usually 172.x.x.1.

Format KRaft storage
powershell
cd C:\kafka\kafka_2.13-4.3.1
.\bin\windows\kafka-storage.bat random-uuid
Copy the UUID returned, then:

powershell
.\bin\windows\kafka-storage.bat format -t <UUID> -c .\config\server.properties --standalone
Expected output:

text
Formatting metadata directory C:\kafka\kafka_2.13-4.3.1\kraft-logs with metadata.version 4.3-IV0.
Start Kafka
In a dedicated terminal:

powershell
cd C:\kafka\kafka_2.13-4.3.1
.\bin\windows\kafka-server-start.bat .\config\server.properties
Wait for:

text
INFO [KafkaRaftServer nodeId=1] Kafka Server started (kafka.server.KafkaRaftServer)
Create the topics
In another terminal:

powershell
cd C:\kafka\kafka_2.13-4.3.1
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --create --topic finopsbank-transactions --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --list
Expected output:

text
__consumer_offsets
finopsbank-transactions
5. Configure Windows Firewall
Open PowerShell as Administrator and allow the required ports:

powershell
New-NetFirewallRule -DisplayName "Kafka for Podman 9092" -Direction Inbound -Protocol TCP -LocalPort 9092 -Action Allow -Profile Any
New-NetFirewallRule -DisplayName "Kafka for Podman 9094" -Direction Inbound -Protocol TCP -LocalPort 9094 -Action Allow -Profile Any
New-NetFirewallRule -DisplayName "PostgreSQL for Podman 5432" -Direction Inbound -Protocol TCP -LocalPort 5432 -Action Allow -Profile Any
Verify connectivity from a container
powershell
podman run --rm bash:5 bash -c "timeout 3 bash -c '</dev/tcp/<WSL_IP>/9094' && echo KAFKA_OPEN || echo KAFKA_CLOSED"
podman run --rm bash:5 bash -c "timeout 3 bash -c '</dev/tcp/<WSL_IP>/5432' && echo POSTGRES_OPEN || echo POSTGRES_CLOSED"
Both should print KAFKA_OPEN and POSTGRES_OPEN.

6. Build the Application
Clone the repository
powershell
git clone https://github.com/CanduriFranklin/FinOpsBank.git
cd FinOpsBank
Update application.yml
Edit app/src/main/resources/application.yml:

yaml
spring:
  datasource:
    url: jdbc:postgresql://<WSL_IP>:5432/finopsbank_db
    username: postgres
    password: <your-password>
  kafka:
    bootstrap-servers: <WSL_IP>:9094
Replace <WSL_IP> with the same IP used in Kafka's advertised.listeners.

Build the jar
powershell
.\gradlew clean :app:bootJar
Expected: BUILD SUCCESSFUL.

Build the OCI image
powershell
podman build --no-cache -t localhost/finopsbank-api:v1.0 .
Expected: Successfully tagged localhost/finopsbank-api:v1.0.

7. Run the Container
powershell
podman rm -f finopsbank-api
podman run -d --name finopsbank-api -p 8080:8080 localhost/finopsbank-api:v1.0
Start-Sleep -Seconds 60
Verify startup
powershell
podman ps
podman logs --tail 20 finopsbank-api
Expected output ends with:

text
Started FinOpsBankApplication in X.XXX seconds
8. Verify the System
1. Login to get a JWT
powershell
$body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $body
$token = $r.token
$token
Expected: a JWT token (long base64 string).

2. Create a test account
powershell
$body = @{
    accountNumber  = "ACC-001"
    customerId     = "CUST-001"
    ownerName      = "Candido Admin"
    initialBalance = 1000.00
    accountType    = "SAVINGS"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts" `
    -Method POST `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body $body
Expected: 200 OK with the created account.

3. List accounts
powershell
curl.exe -i http://localhost:8080/api/v1/accounts -H "Authorization: Bearer $token"
Expected: 200 OK with a JSON array.

4. Verify unauthorized access is blocked
powershell
curl.exe -i http://localhost:8080/api/v1/accounts
Expected: 403 Forbidden.

Troubleshooting
crun: controller 'pids' is not available
Cause: cgroups v1 is enabled in WSL2.

Solution: Add kernelCommandLine = cgroup_no_v1=all to .wslconfig and restart WSL with wsl --shutdown.

Connection refused from container
Cause: The container is trying to reach localhost, which is the container itself, not Windows.

Solution: Use the WSL IP (172.x.x.1) or host.containers.internal. Verify with ipconfig.

Unsupported class file major version 69
Cause: Spring Boot 3.4.x does not support Java 25 bytecode.

Solution: Upgrade to Spring Boot 4.1.1 or higher.

Port 8080 not reachable from Windows
Cause: Podman Desktop 6.0.2 has a known port forwarding bug (issue #29377).

Solution: Upgrade Podman Desktop to 6.1.3 or higher.

database "finopsbank" does not exist
Cause: The database name is finopsbank_db, not finopsbank.

Solution: Update application.yml and recreate the container.

Kafka consumer: Timed out waiting for a node assignment
Cause: advertised.listeners is not configured for the container network.

Solution: Add PLAINTEXT_HOST listener on port 9094 and configure advertised.listeners with the WSL IP.

403 Forbidden on POST requests
Cause: CSRF enabled in Spring Security 7.

Solution: Add .csrf(AbstractHttpConfigurer::disable) to SecurityConfig.

Additional Resources
Podman Docs

WSL2 Docs

PostgreSQL Docs

Apache Kafka Docs

Spring Boot Docs

<p align="center"> <strong>FinOpsBank</strong> - Setup Guide </p>