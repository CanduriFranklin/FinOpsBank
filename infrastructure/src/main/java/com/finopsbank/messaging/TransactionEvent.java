package com.finopsbank.messaging;

import java.math.BigDecimal;

public class TransactionEvent {
    private String eventId;
    private String type;
    private String accountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String timestamp;

    public TransactionEvent() {}

    public TransactionEvent(String eventId, String type, String accountNumber, String targetAccountNumber, BigDecimal amount, BigDecimal balanceAfter, String timestamp) {
        this.eventId = eventId;
        this.type = type;
        this.accountNumber = accountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getTargetAccountNumber() { return targetAccountNumber; }
    public void setTargetAccountNumber(String targetAccountNumber) { this.targetAccountNumber = targetAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}