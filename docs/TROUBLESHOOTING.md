# FinOpsBank Troubleshooting

Known issues and their solutions, collected from real debugging sessions during the project's development.

---

## Table of Contents

- [Container Runtime](#container-runtime)
- [Spring Boot Build](#spring-boot-build)
- [Database](#database)
- [Kafka](#kafka)
- [Security](#security)
- [Podman Networking](#podman-networking)
- [WSL2 Issues](#wsl2-issues)
- [Quick Diagnostics](#quick-diagnostics)

---

## Container Runtime

### `crun: controller 'pids' is not available under /sys/fs/cgroup/...`

**Symptom:**
Error: crun: controller 'pids' is not available under /sys/fs/cgroup/...: OCI runtime error

text

**Cause:** cgroups v1 is enabled in WSL2, or the controller is not delegated to the rootless user.

**Solution:**

1. Add to `C:\Users\<user>\.wslconfig`:

```ini
[wsl2]
kernelCommandLine = cgroup_no_v1=all
Restart WSL completely:

powershell
wsl --shutdown
Verify the change:

powershell
podman info | Select-String "cgroupManager"
Expected: cgroupManager: cgroupfs

Notes:

In Podman 6.1.x with the WSL provider, cgroupfs is configured by default on newly created machines.

If the value is still systemd, recreate the machine (podman machine rm -f and podman machine init).

Spring Boot Build
ClassNotFoundException: com.finopsbank.FinopsbankApplication
Symptom: The container starts but throws:

text
Exception in thread "main" java.lang.ClassNotFoundException: com.finopsbank.FinopsbankApplication
Cause: The mainClass in build.gradle.kts points to an old class name.

Solution:

Verify the actual class name in the source file:

powershell
Get-ChildItem -Recurse -Filter "*.java" | Where-Object { $_.Name -match "Application" } | Select-Object FullName
Update app/build.gradle.kts:

kotlin
springBoot {
    mainClass.set("com.finopsbank.FinOpsBankApplication")
}
Verify the JAR manifest:

powershell
$tmp = ".\_tmp_manifest"
New-Item -ItemType Directory -Path $tmp -Force | Out-Null
Push-Location $tmp
jar xf ..\app\build\libs\app-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF
Get-Content .\META-INF\MANIFEST.MF | Select-String "Start-Class"
Pop-Location
Remove-Item -Recurse -Force $tmp
Expected: Start-Class: com.finopsbank.FinOpsBankApplication

Unsupported class file major version 69
Symptom:

text
Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 69
Cause: Spring Boot 3.4.x does not support Java 25 bytecode (class file version 69).

Solution: Upgrade to Spring Boot 4.1.1 or higher, which supports Java 25.

Edit build.gradle.kts:

kotlin
plugins {
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}
Notes:

Spring Boot 3.5.x also supports Java 25 but is the last version in the 3.x line.

Spring Boot 4.x uses Spring Framework 7 and Spring Security 7, which include breaking changes.

package org.springframework.boot.autoconfigure.domain does not exist
Symptom: Compilation error:

text
error: package org.springframework.boot.autoconfigure.domain does not exist
import org.springframework.boot.autoconfigure.domain.EntityScan;
Cause: Spring Boot 4 moved @EntityScan to a new package.

Solution: Update the import:

java
// Spring Boot 3.x
import org.springframework.boot.autoconfigure.domain.EntityScan;

// Spring Boot 4.x
import org.springframework.boot.persistence.autoconfigure.EntityScan;
KafkaTemplate not found on startup
Symptom:

text
Parameter 0 of constructor in com.finopsbank.messaging.TransactionEventPublisher required a bean of type 'org.springframework.kafka.core.KafkaTemplate' that could not be found.
Cause: Missing spring-boot-starter-kafka dependency.

Solution: Add the dependency to app/build.gradle.kts:

kotlin
implementation("org.springframework.boot:spring-boot-starter-kafka")
Then rebuild:

powershell
.\gradlew clean :app:bootJar
Database
database "finopsbank" does not exist
Symptom:

text
FATAL: database "finopsbank" does not exist
Cause: The database name is finopsbank_db, not finopsbank.

Solution: Update application.yml:

yaml
spring:
  datasource:
    url: jdbc:postgresql://<WSL_IP>:5432/finopsbank_db
null value in column "customer_id" violates not-null constraint
Symptom:

text
ERROR: null value in column "customer_id" of relation "accounts" violates not-null constraint
Cause: The CreateAccountRequest DTO and AccountEntity do not include customerId, but the database column is NOT NULL.

Solution:

Add the field to AccountEntity:

java
@Column(name = "customer_id", nullable = false)
private String customerId;
Add the field to CreateAccountRequest:

java
public record CreateAccountRequest(
    String accountNumber,
    String customerId,
    String ownerName,
    BigDecimal initialBalance,
    String accountType
) {}
Update AccountService.createAccount() to pass the new parameter.

Insert a valid customer before creating the account:

sql
INSERT INTO customers (id, document_number, first_name, last_name, email, phone, status)
VALUES ('CUST-001', '12345678', 'Candido', 'Admin', 'candu@finopsbank.com', '+584140000000', 'ACTIVE');
relation "accounts" does not exist
Symptom: psql reports Did not find any relation named "accounts".

Cause: You are connected to the wrong database (default postgres).

Solution: Switch to the correct database:

sql
\c finopsbank_db
\dt
Expected output:

text
 public | accounts      | table | postgres
 public | customers     | table | postgres
 public | outbox_events | table | postgres
 public | transactions  | table | postgres
Kafka
Timed out waiting for a node assignment. Call: createTopics
Symptom:

text
Error while executing topic command : Timed out waiting for a node assignment. Call: createTopics
Cause: Kafka's advertised.listeners is not accessible from the client. When the client is a container, localhost refers to the container itself, not Windows.

Solution: Configure multiple listeners in server.properties:

properties
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,PLAINTEXT_HOST://0.0.0.0:9094
inter.broker.listener.name=PLAINTEXT
advertised.listeners=PLAINTEXT://localhost:9092,PLAINTEXT_HOST://<WSL_IP>:9094
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
Replace <WSL_IP> with the WSL adapter's IP (find with ipconfig | Select-String "IPv4").

Restart Kafka and verify with:

powershell
netstat -an | Select-String ":9092|:9094"
Both should show LISTENING.

Bootstrap broker disconnected repeatedly in logs
Symptom: Logs show:

text
Bootstrap broker 172.25.208.1:9094 (id: -1 rack: null isFenced: false) disconnected
Cause: Either Kafka is not running, or the WSL IP has changed, or the Windows firewall is blocking the port.

Solution:

Verify Kafka is running:

powershell
netstat -an | Select-String ":9094"
If not running, start it:

powershell
cd C:\kafka\kafka_2.13-4.3.1
.\bin\windows\kafka-server-start.bat .\config\server.properties
Verify the WSL IP hasn't changed:

powershell
ipconfig | Select-String "IPv4"
If the IP changed, update both server.properties (advertised.listeners) and application.yml (bootstrap-servers).

Add firewall rules (as Administrator):

powershell
New-NetFirewallRule -DisplayName "Kafka for Podman 9094" -Direction Inbound -Protocol TCP -LocalPort 9094 -Action Allow -Profile Any
Security
403 Forbidden on POST requests (with a valid token)
Symptom: GET works with JWT, but POST returns 403 Forbidden.

Cause (multiple possibilities):

CSRF enabled: Spring Security 7 enables CSRF by default.

/error not permitted: failures in the controller trigger an internal forward to /error which is blocked, hiding the real error.

Role check uses wrong authority prefix: hasAnyRole vs hasAnyAuthority.

Solution:

Disable CSRF explicitly:

java
.csrf(AbstractHttpConfigurer::disable)
Add the import:

java
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
Permit /error:

java
.requestMatchers("/error").permitAll()
Use hasAnyAuthority with the full prefix:

java
.requestMatchers(HttpMethod.POST, "/api/v1/accounts")
    .hasAnyAuthority("ROLE_ADMIN", "ROLE_TELLER")
Diagnostic tip: Enable Spring Security debug logging temporarily:

yaml
logging:
  level:
    org.springframework.security: TRACE
    org.springframework.security.web.FilterChainProxy: TRACE
Look for Securing POST /api/v1/accounts and the filter decisions.

Invalid CSRF token found
Symptom: Log shows Invalid CSRF token found for http://....

Cause: CSRF is enabled but the client does not send a CSRF token (JWT is stateless, no cookies).

Solution: Disable CSRF:

java
.csrf(AbstractHttpConfigurer::disable)
Or, if you want to keep CSRF and use it only in a browser context:

java
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
Podman Networking
Connection refused from container to Windows services
Symptom:

text
bash: connect: Connection refused
When testing connectivity with:

powershell
podman run --rm bash:5 bash -c "timeout 3 bash -c '</dev/tcp/host.containers.internal/9094' && echo KAFKA_OPEN || echo KAFKA_CLOSED"
Cause: host.containers.internal resolves to the Podman bridge gateway (10.88.0.1), which is not Windows from WSL2's perspective.

Solution: Use the WSL virtual adapter IP instead. Find it:

powershell
ipconfig | Select-String "IPv4"
Look for the adapter vEthernet (WSL), typically 172.x.x.1.

Verify connectivity:

powershell
podman run --rm bash:5 bash -c "timeout 3 bash -c '</dev/tcp/172.25.208.1/9094' && echo KAFKA_OPEN || echo KAFKA_CLOSED"
Use this IP in application.yml:

yaml
spring:
  datasource:
    url: jdbc:postgresql://172.25.208.1:5432/finopsbank_db
  kafka:
    bootstrap-servers: 172.25.208.1:9094
Note: The WSL IP is dynamic. It can change after restarting Windows or WSL. Consider using the mirrored networking mode (Windows 11 22H2+ with WSL 2.0+):

ini
[wsl2]
networkingMode=mirrored
With mirrored, localhost works between Windows and containers.

Port 8080 not reachable from Windows (Podman 6.0.2)
Symptom: Container is running with -p 8080:8080, but netstat -an | Select-String ":8080" shows nothing and curl.exe http://localhost:8080 fails.

Cause: Known bug in Podman 6.0.2 on WSL2 (issue #29377). The rootlessport process fails to bind IPv4 and IPv6 correctly.

Solution: Upgrade to Podman 6.1.3 or higher.

Download the MSI: Podman releases

Install podman-installer-windows-amd64.msi

Verify:

powershell
podman --version
Expected: podman version 6.1.3

Recreate the machine to pick up the new defaults:

powershell
podman machine stop
podman machine rm -f
podman machine init
podman machine start
Rebuild and run:

powershell
podman build --no-cache -t localhost/finopsbank-api:v1.0 .
podman run -d --name finopsbank-api -p 8080:8080 localhost/finopsbank-api:v1.0
Verify:

powershell
netstat -an | Select-String ":8080"
curl.exe -i http://localhost:8080
Expected: TCP 127.0.0.1:8080 LISTENING and HTTP/1.1 403.

WSL2 Issues
Podman machine in inconsistent state (split-brain)
Symptom:

text
Error: unable to start "podman-machine-default": already running
But podman machine inspect podman-machine-default --format "{{.State}}" returns stopped.

Cause: WSL says the distribution is Running while the Podman CLI state file says stopped.

Solution:

Force shutdown of WSL:

powershell
wsl --shutdown
Wait 10 seconds.

Verify both distributions are Stopped:

powershell
wsl --list --verbose
Verify Podman state:

powershell
podman machine inspect podman-machine-default --format "{{.State}}"
Start normally:

powershell
podman machine start
If the issue persists, recreate the machine:

powershell
podman machine rm -f
podman machine init
podman machine start
Failed to connect to user scope bus via local transport
Symptom: When running systemctl --user status inside the Podman VM:

text
Failed to connect to user scope bus via local transport: $DBUS_SESSION_BUS_ADDRESS and $XDG_RUNTIME_DIR not defined
Cause: No user-level systemd session is active in the WSL2 environment.

Solution: Do not use systemd as the cgroup manager in WSL2. Use cgroupfs instead:

ini
[engine]
cgroup_manager = "cgroupfs"
In Podman 6.1.x with the WSL provider, this is set by default on new machines.

Quick Diagnostics
Full system check
Run this sequence to verify everything is working:

powershell
# 1. Podman version and machine
podman --version
podman machine list

# 2. cgroup manager (should be cgroupfs)
podman info | Select-String "cgroupManager"

# 3. Container status
podman ps -a

# 4. WSL IP (remember this)
ipconfig | Select-String "IPv4"

# 5. Kafka and PostgreSQL ports listening
netstat -an | Select-String ":9092|:9094|:5432"

# 6. Connectivity from container
podman run --rm bash:5 bash -c "timeout 3 bash -c '</dev/tcp/172.25.208.1/9094' && echo KAFKA_OPEN || echo KAFKA_CLOSED"
podman run --rm bash:5 bash -c "timeout 3 bash -c '</dev/tcp/172.25.208.1/5432' && echo POSTGRES_OPEN || echo POSTGRES_CLOSED"

# 7. App port from Windows
netstat -an | Select-String ":8080"
curl.exe -i http://localhost:8080

# 8. Container logs (last 20 lines)
podman logs --tail 20 finopsbank-api
Log levels for debugging
Temporarily enable verbose logging in application.yml:

yaml
logging:
  level:
    root: INFO
    org.springframework.security: TRACE
    org.springframework.security.web: TRACE
    org.springframework.security.web.FilterChainProxy: TRACE
    org.springframework.security.web.csrf: TRACE
    com.finopsbank: DEBUG
Rebuild and check logs:

powershell
podman logs --tail 100 finopsbank-api | Select-String "FilterChainProxy|AuthorizationFilter|AccessDenied|JwtAuthenticationFilter"
Remember to remove the TRACE levels after debugging.

Further Reading
Podman issue #29377

Kafka Listeners Explained

Spring Security CSRF

WSL Configuration

<p align="center"> <strong>FinOpsBank</strong> - Troubleshooting Guide </p>