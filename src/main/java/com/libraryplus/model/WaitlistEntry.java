package com.libraryplus.model;

import java.time.LocalDateTime;

public class WaitlistEntry {
    private int id;
    private String bookIsbn;
    private int clientId;
    private int position;
    private LocalDateTime createdAt;

    public WaitlistEntry() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

