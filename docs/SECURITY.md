# FinOpsBank Security

Documentation of the authentication, authorization and security mechanisms used in **FinOpsBank**.

---

## Table of Contents

- [Overview](#overview)
- [JWT Authentication](#jwt-authentication)
- [Roles and Permissions](#roles-and-permissions)
- [Security Filter Chain](#security-filter-chain)
- [CSRF Protection](#csrf-protection)
- [Error Handling](#error-handling)
- [Users (Development)](#users-development)
- [Production Recommendations](#production-recommendations)

---

## Overview

FinOpsBank uses **stateless JWT authentication** with Spring Security 7. Security features:

- **JWT tokens** signed with HS512 (HMAC-SHA512).
- **Stateless sessions**: no HTTP session, no cookies.
- **Role-based authorization**: `ADMIN`, `TELLER`, `CUSTOMER`.
- **Custom filter**: `JwtAuthenticationFilter` runs on every request.
- **URL rules**: enforced by `SecurityConfig`.
- **Method-level security**: enabled via `@EnableMethodSecurity`.

---

## JWT Authentication

### Token Structure

A JWT has three parts separated by dots:
<header>.<payload>.<signature> ```
Header (base64url encoded):

json
{
  "alg": "HS512"
}
Payload (base64url encoded):

json
{
  "sub": "admin",
  "role": "ROLE_ADMIN",
  "iat": 1791318294,
  "exp": 1791404694
}
Claim	Meaning
sub	Subject (username)
role	User role (custom claim)
iat	Issued At (Unix timestamp)
exp	Expiration (Unix timestamp)
Signature: HMAC-SHA512 over the header and payload using the secret key.

Secret Key
Configured in application.yml:

yaml
jwt:
  secret: 9f8a3b7c1e5d2f4a6b8c0e2d4f6a8b1c3d5e7f9a2b4c6d8e0f1a3b5c7d9e1f2a
  expiration-ms: 86400000
Warning: The secret in this file is a development placeholder. In production, load it from an environment variable or a secret manager.

Token Generation
Handled by JwtTokenProvider.generateToken(username, role):

java
public String generateToken(String username, String role) {
    Date now = new Date();
    return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key)
            .compact();
}
Token Validation
JwtTokenProvider.validateToken(token) parses and verifies the signature. If the signature is invalid or the token is expired, it returns false.

Token Extraction
The client sends the token in the Authorization header:

text
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
JwtAuthenticationFilter extracts it:

java
private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
    }
    return null;
}
Roles and Permissions
Three roles are defined:

Role	Authority string	Capabilities
ADMIN	ROLE_ADMIN	Full access: create accounts, transactions, etc.
TELLER	ROLE_TELLER	Create accounts, process transactions
CUSTOMER	ROLE_CUSTOMER	Read own accounts and transactions
URL Rules (SecurityConfig)
java
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/error").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasAnyAuthority("ROLE_ADMIN", "ROLE_TELLER")
.anyRequest().authenticated()
Why hasAnyAuthority instead of hasAnyRole?
In Spring Security 7, hasAnyRole("ADMIN") internally prepends ROLE_, checking for ROLE_ADMIN. However, if the GrantedAuthority was created with new SimpleGrantedAuthority("ROLE_ADMIN") (already including the prefix), the comparison can fail.

Using hasAnyAuthority("ROLE_ADMIN") compares literally and avoids the ambiguity.

Security Filter Chain
Spring Security builds a filter chain on every request. The relevant filters (in order):

Order	Filter	Purpose
1	DisableEncodeUrlFilter	Disables URL-based session tracking
2	WebAsyncManagerIntegrationFilter	Integrates with async request processing
3	SecurityContextHolderFilter	Loads/saves SecurityContext
4	HeaderWriterFilter	Adds security headers (X-Frame-Options, etc.)
5	LogoutFilter	Handles /logout
6	JwtAuthenticationFilter	Custom JWT validation
7	RequestCacheAwareFilter	Caches requests for redirects
8	SecurityContextHolderAwareRequestFilter	Wraps request with security-aware methods
9	AnonymousAuthenticationFilter	Sets ROLE_ANONYMOUS if no auth
10	SessionManagementFilter	Enforces session policy (STATELESS)
11	ExceptionTranslationFilter	Translates exceptions to HTTP responses
12	AuthorizationFilter	Enforces URL rules
The custom JwtAuthenticationFilter runs at position 6. If it finds a valid token, it sets the SecurityContext with the user's authorities. AuthorizationFilter at position 12 then checks the rules defined in SecurityConfig.

CSRF Protection
CSRF is disabled in this project:

java
.csrf(AbstractHttpConfigurer::disable)
Why?
CSRF attacks exploit automatic cookie sending by the browser. In cookie-based sessions, a malicious site can trigger a request to your API that the browser will authenticate using the session cookie.

With JWT in the Authorization header, the browser does not send the token automatically. The client (SPA, mobile app, CLI) must explicitly include the header. This makes CSRF attacks ineffective.

When to enable CSRF?
Enable CSRF if you move authentication to cookies or sessions. In that case, use:

java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
And expose the CSRF token to the frontend via a cookie or endpoint.

Error Handling
/error must be permitAll
When a controller throws an exception, Spring Boot forwards internally to /error. The security filter chain runs again on that forward. Without permitAll on /error, the client sees a misleading 403 instead of the real error.

Example: a POST fails with 500 (database error). The internal forward to /error is blocked with 403. The client sees 403 instead of the real 500.

Adding .requestMatchers("/error").permitAll() fixes this.

Authentication Entry Point
For requests without a valid JWT, Http403ForbiddenEntryPoint returns 403 Forbidden with an empty body. This is the default for stateless configurations.

If you want 401 Unauthorized instead (semantically more correct), configure:

java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
)
Users (Development)
For development, three users are hardcoded in AuthController:

Username	Password	Role
admin	admin123	ROLE_ADMIN
teller	teller123	ROLE_TELLER
customer	customer123	ROLE_CUSTOMER
Warning: These users are stored in memory as a development placeholder. In production, replace with a UserDetailsService connected to the database.

Password Hashing
Even for in-memory users, passwords are hashed with BCrypt:

java
this.users = Map.of(
    "admin", new UserAccount(passwordEncoder.encode("admin123"), "ROLE_ADMIN"),
    ...
);
BCrypt is a strong adaptive hashing algorithm with a configurable work factor (12 rounds in this project).

Production Recommendations
1. Move the JWT secret to environment variables
yaml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
Then set the variable when running:

powershell
podman run -d --name finopsbank-api `
    -e JWT_SECRET=<strong-random-secret> `
    -p 8080:8080 localhost/finopsbank-api:v1.0
Generate a strong secret (at least 64 hex chars):

powershell
-join ((1..64) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
2. Replace in-memory users with a database table
Create a users table and implement a UserDetailsService that loads users from PostgreSQL.

3. Use refresh tokens
Currently, tokens expire after 24 hours and the user must log in again. For better UX, implement refresh tokens with a longer expiry.

4. Add rate limiting
Protect /api/v1/auth/login from brute-force attacks using Spring Cloud Gateway, Bucket4j, or a reverse proxy.

5. Enable HTTPS
Use TLS (via reverse proxy or Spring Boot's built-in support) to prevent token interception.

6. Enable CSRF if using cookies
If you migrate to cookie-based authentication, re-enable CSRF.

7. Add audit logging
Log all authentication events (login success, failure, logout) for security auditing.

8. Consider asymmetric keys
Replace HS512 (symmetric) with RS512 or ES512 (asymmetric) if multiple services need to verify tokens without sharing the secret.

Further Reading
Spring Security Reference

OWASP Authentication Cheat Sheet

OWASP JWT Cheat Sheet

JWT.io - Online JWT decoder

<p align="center">
  <strong>FinOpsBank</strong> - Security Documentation
</p>