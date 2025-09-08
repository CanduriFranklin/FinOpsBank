Creating a Financial Transaction Control Application with OOP
DESCRIPTION
Create an object-oriented Java (OOP) application, developed to consolidate fundamental concepts of object-oriented programming (OOP), such as inheritance, encapsulation, polymorphism, abstraction, and code reuse. The app simulates a basic banking system that allows account creation, deposits, withdrawals, transfers using PIX, investment creation, and transaction history tracking.
Java
________________________________________
Back-End
Basic

Work Plan: Development of the FinOpsBank Console Application
Project Objective
Create a functional console application that simulates a basic banking system. Development will focus on applying and consolidating the fundamental principles of Object Oriented Programming (OOP) within the already established layered architecture.
OOP principles to be consolidated: Inheritance, Encapsulation, Polymorphism, Abstraction.
Generate Phase 1: Extended Domain Modeling 
Objective: Define and create the core classes that will represent all business concepts, paving the way for inheritance and polymorphism.
•	Execute Tasks:
1.	Account as an Abstract Class:
	Action: Convert the existing Account class to an abstract class.
	Justification: It will serve as the basis for different types of accounts, establishing a common contract. This introduces the concept of Abstraction.
2.	Creating Specific Accounts (CheckingAccount, SavingsAccount):
	Action: Create two new classes that inherit from Account. SavingsAccount might have logic for calculating interest, for example.
	Justification: Direct application of the Inheritance.
3.	Transaction Model:
	Action: Create a Transaction base class (can be abstract) with common attributes (ID, date, amount, description). Then, create child classes such as DepositTransaction, WithdrawalTransaction, and TransferTransaction.
	Justification: It prepares the system for transaction history and is an excellent example of Inheritance and Polymorphism (all transactions can be treated generically).
4.	Investment Model:
	Action: Create a new Investment class in the core package. It must be associated with an account and have attributes such as type of investment, initial amount and date of creation.
	Justification: Introduces a new required business entity into the project.
Generate Phase 2: Persistence in Memory and Data Access
Objective: Implement the data layer using an in-memory approach. This allows you to develop and test business logic without the need for a real database.
•	Execute Tasks:
1.	Implementar AccountRepository:
	Action: Create an InMemoryAccountRepository class that implements the AccountRepository interface. It will use a HashMap to save the accounts in memory.
	Justification: Demonstrates Abstraction, as the business layer will not know how the data is saved, it will only interact with the interface.
2.	Create and Deploy TransactionRepository:
	Action: Define a TransactionRepository interface and its in-memory implementation to store transaction history, probably in a List<Transaction> within a Map by account number.
	Justification: It allows you to comply with the requirement to consult the history.
Generate Phase 3: Implementing Business Logic
Objective: To develop the services (business) that will orchestrate the operations and contain the business rules, using the models and repositories of the previous phases.
•	Execute Tasks:
1.	Expandir AccountService:
	Action: Add methods for deposit, withdraw, and refine transfer to work like a PIX transfer (in essence, an immediate transfer that must be recorded in the history). Ensure that each operation creates and saves the corresponding Transaction object.
	Justification: Materializes the main banking operations and applies Encapsulation by protecting the business logic within the service.
2.	Crear InvestmentService:
	Action: Create a new service to handle the inversion logic. The primary method will be createInvestment, which will withdraw funds from an account and create a new Investment object.
	Justification: Separates responsibilities, keeping the AccountService focused only on accounts.
3.	Crear TransactionService:
	Action: Implement a service to query history. You'll have a getTransactionHistory(String accountNumber) method that will use the TransactionRepository.
	Rationale: Provides a clean way to access transaction data.
Generate Phase 4: Building the Console Interface
Objective: To create the end user interface that will allow interaction with the system from the command line.
•	Execute Tasks:
1.	Create the Main Class (Main.java):
	Action: Create a class with a main method. In this method, the application will be "wired": the repositories will be instantiated in memory and then the services, injecting the repositories into their constructors.
2.	Develop the Main Menu:
	Action: Using a loop and switch (or if-else), display the user with a menu with options such as: "Create Customer and Account", "Access Account", "Log Out".
	Justification: It is the entry point for the user.
3.	Develop the Account Menu:
	Action: Once the user logs in to an account, display a submenu: "Check Balance", "Deposit", "Withdraw", "PIX Transfer", "Create Investment", "View History", "Return".
4.	Input and Output Management:
	Action: Use java.util.Scanner to read user input. Display clear messages and handle possible errors (e.g. entering text instead of a number).
Generate Phase 5: Testing, Refinement, and Documentation (Duration: 1 Week)
Objective: To ensure the quality of the code, the robustness of the application and to update the project documentation.
•	Execute Tasks:
1.	Additional Unit Tests:
	Action: Add unit tests in the test package for the new services (InvestmentService, TransactionService).
2.	E2E (End-to-End) Manual Testing:
	Action: Run the console application and manually test all possible flows to find errors.
3.	Javadoc documentation:
	Action: Add Javadoc comments to all new classes and public methods to explain their purpose.
4.	Update README.md:
	Action: Add a section to the README.md that explains how to build and run the console app.