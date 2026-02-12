package com.libraryplus.dao;

import com.libraryplus.model.WaitlistEntry;

import java.util.List;
import java.util.Optional;

public interface WaitlistDao {
    void enqueue(String bookIsbn, int clientId);

    Optional<WaitlistEntry> dequeueNext(String bookIsbn);

    List<WaitlistEntry> getQueueForBook(String bookIsbn);

    boolean isClientInQueue(String bookIsbn, int clientId);
}
