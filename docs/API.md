# FinOpsBank API Reference

Complete REST API documentation for the **FinOpsBank** service.

**Base URL**: `http://localhost:8080`

**Content-Type**: `application/json`

**Authentication**: JWT Bearer token (except `/api/v1/auth/login`)

---

## Table of Contents

- [Authentication](#authentication)
- [Accounts](#accounts)
- [Transactions](#transactions)
- [Error Responses](#error-responses)
- [Status Codes](#status-codes)

---

## Authentication

### POST /api/v1/auth/login

Authenticates a user and returns a JWT token.

**Auth required**: No

**Request body:**

| Field | Type | Required | Description |
|---|---|---|---|
| `username` | string | Yes | Username (`admin`, `teller`, `customer`) |
| `password` | string | Yes | User password |

**Example request:**

```json
{
  "username": "admin",
  "password": "admin123"
}
Example PowerShell:

powershell
$body = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method POST -ContentType "application/json" -Body $body
$token = $r.token
Success response (200 OK):

json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzkxMzE4Mjk0LCJleHAiOjE3OTE0MDQ2OTR9...",
  "type": "Bearer",
  "expiresIn": 86400
}
Field	Type	Description
token	string	JWT token to use in subsequent requests
type	string	Always Bearer
expiresIn	integer	Token lifetime in seconds (86400 = 24 hours)
Error response (401 Unauthorized):

json
{
  "error": "Invalid credentials"
}
Accounts
POST /api/v1/accounts
Creates a new bank account.

Auth required: Yes

Required role: ROLE_ADMIN or ROLE_TELLER

Request body:

Field	Type	Required	Description
accountNumber	string	Yes	Unique account number (max 255 chars)
customerId	string	Yes	Customer ID (must exist in customers table)
ownerName	string	Yes	Account holder full name
initialBalance	decimal	Yes	Initial balance (>= 0)
accountType	string	Yes	One of: SAVINGS, CHECKING, FINOPS_CORE
Example request:

json
{
  "accountNumber": "ACC-001",
  "customerId": "CUST-001",
  "ownerName": "Candido Admin",
  "initialBalance": 1000.00,
  "accountType": "SAVINGS"
}
Example PowerShell:

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
Success response (200 OK):

json
{
  "accountNumber": "ACC-001",
  "customerId": "CUST-001",
  "ownerName": "Candido Admin",
  "balance": 1000.00,
  "accountType": "SAVINGS"
}
Error responses:

Status	Reason
400 Bad Request	Missing or invalid fields
403 Forbidden	User does not have ROLE_ADMIN or ROLE_TELLER
409 Conflict	Account number already exists
500 Internal Server Error	Database error (e.g., invalid customerId)
GET /api/v1/accounts
Returns all bank accounts.

Auth required: Yes

Required role: Any authenticated user

Example PowerShell:

powershell
curl.exe -i http://localhost:8080/api/v1/accounts -H "Authorization: Bearer $token"
Success response (200 OK):

json
[
  {
    "accountNumber": "ACC-001",
    "customerId": "CUST-001",
    "ownerName": "Candido Admin",
    "balance": 1000.00,
    "accountType": "SAVINGS"
  }
]
Returns an empty array [] if no accounts exist.

GET /api/v1/accounts/{accountNumber}
Returns a specific account by its account number.

Auth required: Yes

Required role: Any authenticated user

Path parameters:

Parameter	Type	Description
accountNumber	string	Account number to look up
Example PowerShell:

powershell
curl.exe -i http://localhost:8080/api/v1/accounts/ACC-001 -H "Authorization: Bearer $token"
Success response (200 OK):

json
{
  "accountNumber": "ACC-001",
  "customerId": "CUST-001",
  "ownerName": "Candido Admin",
  "balance": 1000.00,
  "accountType": "SAVINGS"
}
Error response (500 Internal Server Error):

json
{
  "error": "Cuenta no encontrada: ACC-999"
}
Transactions
POST /api/v1/transactions/deposit
Deposits money into an account.

Auth required: Yes

Required role: Any authenticated user

Request body:

Field	Type	Required	Description
accountNumber	string	Yes	Target account number
amount	decimal	Yes	Amount to deposit (must be > 0)
Example request:

json
{
  "accountNumber": "ACC-001",
  "amount": 500.00
}
Example PowerShell:

powershell
$body = @{ accountNumber = "ACC-001"; amount = 500.00 } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactions/deposit" `
    -Method POST `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body $body
Success response (200 OK):

json
{
  "accountNumber": "ACC-001",
  "customerId": "CUST-001",
  "ownerName": "Candido Admin",
  "balance": 1500.00,
  "accountType": "SAVINGS"
}
Error responses:

Status	Reason
400 Bad Request	Amount is zero or negative
500 Internal Server Error	Account not found
POST /api/v1/transactions/withdraw
Withdraws money from an account.

Auth required: Yes

Required role: Any authenticated user

Request body:

Field	Type	Required	Description
accountNumber	string	Yes	Source account number
amount	decimal	Yes	Amount to withdraw (must be > 0, <= balance)
Example request:

json
{
  "accountNumber": "ACC-001",
  "amount": 200.00
}
Success response (200 OK):

json
{
  "accountNumber": "ACC-001",
  "customerId": "CUST-001",
  "ownerName": "Candido Admin",
  "balance": 1300.00,
  "accountType": "SAVINGS"
}
Error responses:

Status	Reason
400 Bad Request	Amount is zero or negative
500 Internal Server Error	Account not found OR insufficient balance
POST /api/v1/transactions/transfer
Transfers money between two accounts.

Auth required: Yes

Required role: Any authenticated user

Request body:

Field	Type	Required	Description
sourceAccountNumber	string	Yes	Source account
targetAccountNumber	string	Yes	Target account
amount	decimal	Yes	Amount to transfer (must be > 0)
Example request:

json
{
  "sourceAccountNumber": "ACC-001",
  "targetAccountNumber": "ACC-002",
  "amount": 100.00
}
Success response (200 OK):

text
"Transferencia realizada con éxito"
Error responses:

Status	Reason
400 Bad Request	Amount is zero or negative
500 Internal Server Error	Source/target account not found OR insufficient balance
Side effects:

Two Kafka events are published: TRANSFER_SENT and TRANSFER_RECEIVED.

Both accounts are updated in a single transaction.

Error Responses
All error responses follow Spring Boot's default error format:

json
{
  "timestamp": "2026-10-06T22:12:57.123Z",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/v1/accounts"
}
For authentication errors (401), the body contains:

json
{
  "error": "Invalid credentials"
}
For authorization errors (403), the body is empty.

Status Codes
Code	Meaning	When
200 OK	Success	Operation completed
400 Bad Request	Invalid input	Missing or malformed fields
401 Unauthorized	Authentication failed	Bad credentials on login
403 Forbidden	Authorization failed	Missing/invalid token OR insufficient role
404 Not Found	Resource missing	Endpoint does not exist
409 Conflict	Duplicate resource	Account number already exists
500 Internal Server Error	Server error	Database error, business rule violation
Authentication Flow
Login: POST /api/v1/auth/login with username and password.

Receive: JSON response with token, type, and expiresIn.

Use: include the token in the Authorization header of subsequent requests:

text
Authorization: Bearer <token>
Expiry: after 24 hours, the token is no longer valid. Log in again.

cURL Examples Quick Reference
Login
powershell
curl.exe -i -X POST http://localhost:8080/api/v1/auth/login `
    -H "Content-Type: application/json" `
    -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
Create account
powershell
curl.exe -i -X POST http://localhost:8080/api/v1/accounts `
    -H "Authorization: Bearer <TOKEN>" `
    -H "Content-Type: application/json" `
    -d "{\"accountNumber\":\"ACC-001\",\"customerId\":\"CUST-001\",\"ownerName\":\"Candido\",\"initialBalance\":1000,\"accountType\":\"SAVINGS\"}"
List accounts
powershell
curl.exe -i http://localhost:8080/api/v1/accounts -H "Authorization: Bearer <TOKEN>"
Get account
powershell
curl.exe -i http://localhost:8080/api/v1/accounts/ACC-001 -H "Authorization: Bearer <TOKEN>"
Deposit
powershell
curl.exe -i -X POST http://localhost:8080/api/v1/transactions/deposit ^
    -H "Authorization: Bearer <TOKEN>" ^
    -H "Content-Type: application/json" ^
    -d "{\"accountNumber\":\"ACC-001\",\"amount\":500}"
<p align="center"> <strong>FinOpsBank</strong> - API Reference </p>