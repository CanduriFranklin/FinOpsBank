# Contributing to FinOpsBank

Thanks for considering contributing to **FinOpsBank**. This document explains the conventions and workflow for contributing to the project.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Git Workflow](#git-workflow)
- [Commit Conventions](#commit-conventions)
- [Branch Naming](#branch-naming)
- [Code Style](#code-style)
- [Testing](#testing)
- [Pull Requests](#pull-requests)
- [Reporting Issues](#reporting-issues)

---

## Code of Conduct

Be respectful, inclusive, and constructive. Harassment and disrespectful behavior are not tolerated.

---

## Prerequisites

Before contributing, ensure you have the following:

| Tool | Version |
|---|---|
| Java JDK | 25 LTS |
| Gradle Wrapper | 9.8.0 (bundled) |
| Podman | 6.1.3+ |
| PostgreSQL | 18+ |
| Apache Kafka | 4.3.1+ |
| Git | 2.30+ |

See [SETUP.md](SETUP.md) for the complete installation guide.

---

## Getting Started

### 1. Fork the repository

On GitHub, click **Fork** to create your own copy.

### 2. Clone your fork

```powershell
git clone https://github.com/<your-username>/FinOpsBank.git
cd FinOpsBank
3. Add the upstream remote
powershell
git remote add upstream https://github.com/CanduriFranklin/FinOpsBank.git
git fetch upstream
4. Create a feature branch
powershell
git checkout -b feature/your-feature-name
5. Make your changes
Follow the Code Style and Commit Conventions.

6. Test your changes
powershell
.\gradlew clean test
.\gradlew clean :app:bootJar
7. Push and open a PR
powershell
git push origin feature/your-feature-name
Then open a Pull Request against main-developers.

Git Workflow
The project follows a feature branch workflow:

text
main-developers  (stable, deployable)
    ^
    |  merge
    |
feature/xyz      (your work)
Rules
Never commit directly to main-developers.

Always create a feature branch for your work.

Rebase on main-developers before opening a PR (avoid merge commits in your branch).

Keep commits atomic — one logical change per commit.

Example workflow
powershell
# Sync with upstream
git checkout main-developers
git pull upstream main-developers
git push origin main-developers

# Start a feature
git checkout -b feature/add-refresh-tokens

# Work and commit
git add .
git commit -m "feat(auth): add refresh token support"

# Rebase before PR
git fetch upstream
git rebase upstream/main-developers

# Push
git push origin feature/add-refresh-tokens
Commit Conventions
The project uses Conventional Commits for commit messages.

Format
text
<type>(<scope>): <subject>

[optional body]

[optional footer]
Types
Type	Purpose
feat	A new feature
fix	A bug fix
docs	Documentation only
style	Code style (formatting, missing semicolons)
refactor	Code change that neither fixes a bug nor adds a feature
test	Adding or fixing tests
chore	Build process, tooling, dependencies
perf	Performance improvement
ci	CI configuration
Scopes (optional but recommended)
Scope	Area
auth	Authentication, JWT, security
accounts	Accounts module
transactions	Transactions module
kafka	Kafka messaging
persistence	JPA, database
api	REST API
docs	Documentation
build	Gradle, dependencies
container	Podman, Containerfile
Examples
text
feat(auth): add refresh token support
fix(accounts): handle duplicate account number gracefully
docs(setup): clarify WSL2 firewall configuration
refactor(service): extract transaction event publishing
test(security): add role-based access tests
chore(build): upgrade Spring Boot to 4.1.2
Body and footer
For complex changes, add a body:

text
feat(auth): add refresh token support

- New POST /api/v1/auth/refresh endpoint
- Refresh tokens valid for 7 days
- Access tokens remain at 24 hours

Closes #42
Branch Naming
Use descriptive branch names:

Pattern	Example
feature/<short-description>	feature/add-refresh-tokens
fix/<short-description>	fix/null-customer-id
docs/<short-description>	docs/update-setup-guide
refactor/<short-description>	refactor/extract-outbox-publisher
chore/<short-description>	chore/upgrade-gradle
Rules:

Use lowercase and hyphens (-), not underscores (_).

Keep it short but descriptive.

Delete branches after merging.

Code Style
Java
Indentation: 4 spaces (no tabs).

Line length: 120 characters max.

Braces: K&R style (opening brace on the same line).

Imports: no wildcard imports (import java.util.* is discouraged).

Naming:

Classes: PascalCase (AccountService)

Methods and variables: camelCase (createAccount)

Constants: UPPER_SNAKE_CASE (MAX_POOL_SIZE)

Records: use for DTOs when possible (record LoginRequest(...)).

Constructor injection: prefer over field injection (@Autowired on fields is discouraged).

Example:

java
@Service
public class AccountService {

    private final SpringDataAccountRepository accountRepository;
    private final TransactionEventPublisher eventPublisher;

    public AccountService(SpringDataAccountRepository accountRepository,
                          TransactionEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountEntity createAccount(CreateAccountRequest request) {
        // ...
    }
}
Kotlin DSL (Gradle)
Indentation: 4 spaces.

Use toolchains for Java version:

kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
YAML
Indentation: 2 spaces.

Quotes: only when necessary.

Boolean: true / false (lowercase).

Markdown
Headings: use #, ##, ### (avoid skipping levels).

Code blocks: always specify the language (```java, ```yaml).

Tables: align columns for readability.

Links: use descriptive text (not "click here").

Line length: no strict limit, but keep lines reasonable.

Testing
Run all tests
powershell
.\gradlew clean test
Run tests for a specific module
powershell
.\gradlew :app:test
.\gradlew :domain:test
.\gradlew :infrastructure:test
Add tests for your changes
New feature → add unit tests in the matching module.

Bug fix → add a test that reproduces the bug (fails before, passes after).

Refactor → ensure existing tests still pass.

Test naming
Use descriptive names: shouldReturn403WhenTokenMissing

Use JUnit 5 (@Test, @DisplayName).

Prefer AssertJ for assertions (assertThat(...)).

Example test
java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("GET /api/v1/accounts without token returns 403")
    void shouldReturn403WhenTokenMissing() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/accounts", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
Pull Requests
Before opening a PR
□ Rebase on upstream/main-developers.
□ All tests pass (.\gradlew clean test).
□ Build succeeds (.\gradlew clean :app:bootJar).
□ Documentation updated if needed.
□ CHANGELOG.md updated under [Unreleased].
□ Commit messages follow the conventions.
PR title
Use the same format as commit messages:

text
feat(auth): add refresh token support
PR description
Include:

What changed.

Why the change was needed.

How to test it.

Screenshots or logs if applicable.

Related issue (if any): Closes #42.

Review process
A maintainer reviews the PR.

Address feedback in new commits (do not force-push until approved).

Once approved, the maintainer merges via squash or rebase.

Reporting Issues
When reporting a bug, include:

Environment: OS, Java version, Podman version.

Steps to reproduce.

Expected behavior.

Actual behavior.

Logs (from podman logs finopsbank-api).

Screenshots if applicable.

Bug report template
markdown
**Environment:**
- OS: Windows 11 22H2
- Java: OpenJDK 25.0.3
- Podman: 6.1.3
- Spring Boot: 4.1.1

**Steps to reproduce:**
1. `podman run -d --name finopsbank-api -p 8080:8080 localhost/finopsbank-api:v1.0`
2. `curl.exe -i http://localhost:8080/api/v1/accounts`

**Expected:** 403 Forbidden
**Actual:** Connection refused

**Logs:**
(paste relevant logs)

text

**Additional info:** WSL IP is 172.25.208.1
Feature requests
For feature requests, include:

Problem the feature solves.

Proposed solution.

Alternatives considered.

Impact on existing functionality.

Questions?
Open a GitHub Discussion.

Check existing Issues.

Read the docs/ folder.

<p align="center"> <strong>FinOpsBank</strong> - Contributing Guidelines </p>