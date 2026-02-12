package com.libraryplus.dao;

import com.libraryplus.model.Subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionDao {
    Optional<Subscription> findActiveByClientId(int clientId);

    List<Subscription> findByClientId(int clientId);

    void createSubscription(int clientId, LocalDate startDate, LocalDate endDate);

    int countActiveSubscriptions();
}
