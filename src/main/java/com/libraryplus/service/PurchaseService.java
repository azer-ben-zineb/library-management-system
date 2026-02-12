package com.libraryplus.service;

import com.libraryplus.dao.PurchaseDao;
import com.libraryplus.dao.SubscriptionDao;
import com.libraryplus.dao.jdbc.PurchaseDaoJdbc;
import com.libraryplus.dao.jdbc.SubscriptionDaoJdbc;
import com.libraryplus.model.Purchase;
import com.libraryplus.model.Subscription;

import java.util.Optional;

public class PurchaseService {
    private final PurchaseDao purchaseDao;
    private final SubscriptionDao subscriptionDao;

    public PurchaseService() {
        this.purchaseDao = new PurchaseDaoJdbc();
        this.subscriptionDao = new SubscriptionDaoJdbc();
    }

    public int processPurchase(Purchase purchase) throws Exception {
        
        Optional<Subscription> sub = subscriptionDao.findActiveByClientId(purchase.getClientId());
        if (sub.isPresent()) {
            
            double originalPrice = purchase.getUnitPrice();
            double discountedPrice = originalPrice * 0.90;
            purchase.setUnitPrice(discountedPrice);
            System.out.println("Applied subscription discount: " + originalPrice + " -> " + discountedPrice);
        }

        return purchaseDao.createPurchase(purchase);
    }
}
