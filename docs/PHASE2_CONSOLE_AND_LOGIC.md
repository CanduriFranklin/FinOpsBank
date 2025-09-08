# Phase 2: Business Logic & Console Interface Implementation

## Overview
This document details the implementation of the business logic and the interactive console interface for the FinOpsBank Java application. This phase builds upon the skeleton structure, connecting all layers and enabling real user interaction with the system.

---

## 1. Business Logic Implementation
- **AccountService**: Deposit, withdraw, and transfer (PIX) operations implemented, with transaction recording and exception handling.
- **InvestmentService**: Investment creation logic implemented, withdrawing funds and creating an Investment object.
- **TransactionService**: Transaction history retrieval for any account.
- **Exception Handling**: All business operations now throw and handle custom exceptions for invalid operations, insufficient funds, and missing accounts.

---

## 2. Console Interface
- **Main.java**: Expanded to provide a full-featured command-line interface.
  - Main menu: Create account, access account, exit.
  - Account menu: Check balance, deposit, withdraw, PIX transfer, create investment, view transaction history, return.
  - Input validation and error handling for all user actions.
  - Uses Java's Scanner for input and clear, user-friendly prompts.

---

## 3. Integration
- All layers (domain, data, business, presentation) are now connected and functional.
- Users can perform all main banking operations from the console.
- Transactions are recorded and can be queried per account.

---

## 4. Next Steps
- Write and run unit tests for all service logic.
- Refine and expand business rules as needed.
- Continue improving user experience and documentation.

---

## 5. Notes
- This phase marks the transition from a static skeleton to a working, interactive application.
- The codebase is now ready for thorough testing and further feature development.
