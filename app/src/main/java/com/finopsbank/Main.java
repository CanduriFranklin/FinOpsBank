package com.finopsbank;

import com.finopsbank.business.*;
import com.finopsbank.data.*;

/**
 * Main entry point for the FinOpsBank console application.
 */
public class Main {
    public static void main(String[] args) {
        AccountRepository accountRepository = new InMemoryAccountRepository();
        TransactionRepository transactionRepository = new InMemoryTransactionRepository();
        AccountService accountService = new AccountService(accountRepository, transactionRepository);
        InvestmentService investmentService = new InvestmentService(accountRepository);
        TransactionService transactionService = new TransactionService(transactionRepository);

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.println("\n=== FinOpsBank Main Menu ===");
            System.out.println("1. Create Account");
            System.out.println("2. Access Account");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");
            String option = scanner.nextLine();
            switch (option) {
                case "1":
                    System.out.print("Enter customer ID: ");
                    String customerId = scanner.nextLine();
                    System.out.print("Enter account number: ");
                    String accountNumber = scanner.nextLine();
                    System.out.print("Initial balance: ");
                    double balance = Double.parseDouble(scanner.nextLine());
                    System.out.print("Account type (checking/savings): ");
                    String type = scanner.nextLine();
                    if (type.equalsIgnoreCase("checking")) {
                        accountRepository.save(new com.finopsbank.core.CheckingAccount(accountNumber, customerId, balance));
                        System.out.println("Checking account created.");
                    } else if (type.equalsIgnoreCase("savings")) {
                        System.out.print("Interest rate (e.g., 0.01): ");
                        double rate = Double.parseDouble(scanner.nextLine());
                        accountRepository.save(new com.finopsbank.core.SavingsAccount(accountNumber, customerId, balance, rate));
                        System.out.println("Savings account created.");
                    } else {
                        System.out.println("Invalid account type.");
                    }
                    break;
                case "2":
                    System.out.print("Enter account number: ");
                    String accNum = scanner.nextLine();
                    var accOpt = accountRepository.findByAccountNumber(accNum);
                    if (accOpt.isEmpty()) {
                        System.out.println("Account not found.");
                        break;
                    }
                    boolean inAccount = true;
                    while (inAccount) {
                        System.out.println("\n--- Account Menu ---");
                        System.out.println("1. Check Balance");
                        System.out.println("2. Deposit");
                        System.out.println("3. Withdraw");
                        System.out.println("4. PIX Transfer");
                        System.out.println("5. Create Investment");
                        System.out.println("6. View History");
                        System.out.println("7. Return");
                        System.out.print("Select an option: ");
                        String accOptStr = scanner.nextLine();
                        try {
                            switch (accOptStr) {
                                case "1":
                                    System.out.println("Balance: " + accOpt.get().getBalance());
                                    break;
                                case "2":
                                    System.out.print("Amount to deposit: ");
                                    double dep = Double.parseDouble(scanner.nextLine());
                                    accountService.deposit(accNum, dep);
                                    System.out.println("Deposit successful.");
                                    break;
                                case "3":
                                    System.out.print("Amount to withdraw: ");
                                    double wd = Double.parseDouble(scanner.nextLine());
                                    accountService.withdraw(accNum, wd);
                                    System.out.println("Withdrawal successful.");
                                    break;
                                case "4":
                                    System.out.print("Destination account number: ");
                                    String dest = scanner.nextLine();
                                    System.out.print("Amount to transfer: ");
                                    double amt = Double.parseDouble(scanner.nextLine());
                                    accountService.transfer(accNum, dest, amt);
                                    System.out.println("Transfer successful.");
                                    break;
                                case "5":
                                    System.out.print("Investment type: ");
                                    String invType = scanner.nextLine();
                                    System.out.print("Amount: ");
                                    double invAmt = Double.parseDouble(scanner.nextLine());
                                    investmentService.createInvestment(accNum, invType, invAmt);
                                    System.out.println("Investment created.");
                                    break;
                                case "6":
                                    var history = transactionService.getTransactionHistory(accNum);
                                    if (history.isEmpty()) {
                                        System.out.println("No transactions found.");
                                    } else {
                                        for (var t : history) {
                                            System.out.println(t.getDate() + " - " + t.getDescription() + " - " + t.getAmount());
                                        }
                                    }
                                    break;
                                case "7":
                                    inAccount = false;
                                    break;
                                default:
                                    System.out.println("Invalid option.");
                            }
                        } catch (Exception e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                    break;
                case "3":
                    running = false;
                    System.out.println("Exiting FinOpsBank. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }
}
