package com.libraryplus.model;

import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int clientId;
    private double amount;
    private String reason;
    private LocalDateTime timestamp;
    private double resultingBalance;

    public Transaction() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public double getResultingBalance() { return resultingBalance; }
    public void setResultingBalance(double resultingBalance) { this.resultingBalance = resultingBalance; }
}

