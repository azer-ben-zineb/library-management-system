package com.libraryplus.dao;

import com.libraryplus.model.Transaction;

import java.util.List;

public interface TransactionDao {
    int createTransaction(Transaction transaction) throws Exception;

    List<Transaction> findByClientId(int clientId) throws Exception;

    List<Transaction> findAll() throws Exception;
}
