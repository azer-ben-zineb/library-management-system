package com.libraryplus.model;

import java.time.LocalDateTime;

public class Rating {
    private int id;
    private String bookIsbn;
    private int clientId;
    private int rating; 
    private LocalDateTime createdAt;

    public Rating() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

