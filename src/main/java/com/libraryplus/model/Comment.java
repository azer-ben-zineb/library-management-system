package com.libraryplus.model;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private String bookIsbn;
    private int clientId;
    private String comment;
    private LocalDateTime createdAt;

    public Comment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

