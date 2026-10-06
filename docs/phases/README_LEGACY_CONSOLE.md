# 🚀 FinOpsBank: Financial Transaction Control System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) ![JUnit](https://img.shields.io/badge/JUnit-25A162?style=for-the-badge&logo=junit5&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white) ![Windows](https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white) ![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

---

## 🌟 Overview
![Java](https://upload.wikimedia.org/wikipedia/en/3/30/Java_programming_language_logo.svg)
FinOpsBank is a Java console application that simulates a basic banking system. This project was developed with a rigorous focus on Object-Oriented Programming (OOP) principles to consolidate concepts such as inheritance, encapsulation, polymorphism, and abstraction. The main objective is to provide a simple, yet robust, and well-structured software solution that manages financial transactions securely and efficiently.

---

## 🎯 Main Project Objectives
- **Apply OOP:** Demonstrate a solid understanding of object-oriented programming paradigms through a practical project.
- **Modular Design:** Create a clean, modular software architecture that separates responsibilities to facilitate scalability and maintenance.
- **Transaction Handling:** Key banking operations such as deposits, withdrawals, and transfers, with adequate error handling.
- **Data History:** Implement functionality to record and query the transaction history of each account.

---

## 🛠️ Technologies Used
![Gradle](https://upload.wikimedia.org/wikipedia/commons/5/5f/Gradle_logo.png)
- **Java:** Primary programming language, used to build backend logic.
- **Gradle:** Build automation tool. Handles dependency management and the project lifecycle.
- **JDK 25:** Java development environment (IBM Semeru Runtime Open Edition).
- **JUnit 5:** Unit testing framework used to ensure code quality and functionality.
- **Windows:** Operating system environment used for development, testing, and deployment scripts.
- **IntelliJ IDEA:** Primary Integrated Development Environment (IDE) used for project structuring, debugging, and execution.

---

## 🔒 Security and Scalability
![Linux](https://upload.wikimedia.org/wikipedia/commons/a/af/Tux.png)
- **Security:** Data encapsulation (private attributes) in domain classes ensures the protection of critical information such as balances and customer data. Financial operations include validations to prevent invalid transactions (e.g., withdrawals with insufficient balance).
- **Scalability:** The modular design allows for future expansion. For example, the data access layer can be replaced with a real database (such as PostgreSQL or MySQL) without requiring changes to the business logic. Additionally, the service layer can be adapted to be a REST API, allowing the system to connect to web or mobile user interfaces in the future.

---

## 📂 Project Structure
![VS Code](https://upload.wikimedia.org/wikipedia/commons/4/4b/Visual_Studio_Code_1.35_icon.svg)
This backend uses a three-tier architecture to ensure a clear separation of responsibilities:

- **Service Layer:** Exposes the system's core functionalities, acting as an orchestrator that coordinates user interface requests.
- **Business Logic Layer (Core):** Contains the core domain logic with classes such as Customer, BankAccount, and Transaction.
- **Data Access Layer (In-Memory Repository):** Simulates an in-memory database for storing and retrieving information. This layer allows business logic to be independent of persistence technology.

---

## 📈 Value Proposition
**Problem to Solve:**
Software development often lacks a solid foundation in programming fundamentals, leading to disorganized projects that are difficult to maintain and scale. This project addresses that problem by providing a clear and functional example of how a clean architecture, even in a console application, can solve complexity.

**Value Proposition:**
FinOpsBank is not just a functional application; it is a demonstration of good professional practices. Its modular architecture and use of quality control standards ensure that the code is readable, maintainable, and scalable. It is an ideal project for a portfolio that demonstrates the ability to build robust systems from scratch.

---

## 🚀 Getting Started

1. ### Clone the Repository

```bash
git clone [https://github.com/CanduriFranklin/FinOpsBank.git](https://github.com/CanduriFranklin/FinOpsBank.git)