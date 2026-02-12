package com.libraryplus.dao;

import com.libraryplus.model.Purchase;

import java.util.List;
import java.util.Optional;

public interface PurchaseDao {
    int createPurchase(Purchase p) throws Exception;
    List<Purchase> findByClient(int clientId) throws Exception;
}

