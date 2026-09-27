package com.libraryplus.service;

import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.PurchaseDao;
import com.libraryplus.dao.SubscriptionDao;
import com.libraryplus.dao.TransactionDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.PurchaseDaoJdbc;
import com.libraryplus.dao.jdbc.SubscriptionDaoJdbc;
import com.libraryplus.dao.jdbc.TransactionDaoJdbc;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.Book;
import com.libraryplus.model.Client;
import com.libraryplus.model.Purchase;
import com.libraryplus.model.Subscription;
import com.libraryplus.model.Transaction;
import com.libraryplus.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class PurchaseService {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseService.class);

    private final PurchaseDao purchaseDao;
    private final SubscriptionDao subscriptionDao;
    private final BookDao bookDao;
    private final UserDao userDao;
    private final ClientDao clientDao;
    private final TransactionDao transactionDao;

    public PurchaseService() {
        this.purchaseDao = new PurchaseDaoJdbc();
        this.subscriptionDao = new SubscriptionDaoJdbc();
        this.bookDao = new BookDaoJdbc();
        this.userDao = new UserDaoJdbc();
        this.clientDao = new ClientDaoJdbc();
        this.transactionDao = new TransactionDaoJdbc();
    }

    public PurchaseService(PurchaseDao purchaseDao, SubscriptionDao subscriptionDao,
                           BookDao bookDao, UserDao userDao, ClientDao clientDao,
                           TransactionDao transactionDao) {
        this.purchaseDao = purchaseDao;
        this.subscriptionDao = subscriptionDao;
        this.bookDao = bookDao;
        this.userDao = userDao;
        this.clientDao = clientDao;
        this.transactionDao = transactionDao;
    }

    public int processPurchase(Purchase purchase) throws Exception {
        if (purchase == null) {
            throw new IllegalArgumentException("Purchase request cannot be null");
        }
        if (purchase.getBookIsbn() == null || purchase.getBookIsbn().isBlank()) {
            throw new IllegalArgumentException("Book ISBN is required for purchase");
        }
        int qty = purchase.getQuantity() > 0 ? purchase.getQuantity() : 1;
        purchase.setQuantity(qty);

        // 1. Verify Book
        Optional<Book> bookOpt = bookDao.findByIsbn(purchase.getBookIsbn());
        if (bookOpt.isEmpty()) {
            throw new IllegalStateException("Book not found for ISBN: " + purchase.getBookIsbn());
        }
        Book book = bookOpt.get();

        if (book.getStock() < qty) {
            throw new IllegalStateException("Book is out of stock! Available copies: " + book.getStock());
        }

        // 2. Determine price and subscription discount
        double unitPrice = purchase.getUnitPrice();
        if (unitPrice <= 0) {
            unitPrice = (book.getPrice() > 0) ? book.getPrice() : 15.00;
        }

        Optional<Subscription> sub = subscriptionDao.findActiveByClientId(purchase.getClientId());
        if (sub.isPresent()) {
            double discounted = Math.round((unitPrice * 0.90) * 100.0) / 100.0;
            logger.info("Applying 10% subscriber discount for client {}: {} DT -> {} DT",
                    purchase.getClientId(), unitPrice, discounted);
            unitPrice = discounted;
        }
        purchase.setUnitPrice(unitPrice);
        double totalCost = Math.round((unitPrice * qty) * 100.0) / 100.0;

        // 3. Verify and deduct user balance
        Optional<Client> clientOpt = clientDao.findById(purchase.getClientId());
        if (clientOpt.isEmpty()) {
            throw new IllegalStateException("Client profile not found for ID: " + purchase.getClientId());
        }
        Client client = clientOpt.get();

        Optional<User> userOpt = userDao.findById(client.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalStateException("User account not found for client: " + client.getId());
        }
        User user = userOpt.get();

        if (user.getCardBalance() < totalCost) {
            throw new IllegalStateException(String.format(
                    "Insufficient card balance! Required: %.2f DT, Available: %.2f DT. Please top up your card.",
                    totalCost, user.getCardBalance()));
        }

        // Deduct balance
        double newBalance = Math.round((user.getCardBalance() - totalCost) * 100.0) / 100.0;
        user.setCardBalance(newBalance);
        userDao.updateUser(user);

        // 4. Record financial transaction
        try {
            Transaction tx = new Transaction();
            tx.setClientId(client.getId());
            tx.setAmount(totalCost);
            tx.setReason("Book purchase: " + book.getTitle() + " (qty " + qty + ")");
            tx.setResultingBalance(newBalance);
            transactionDao.createTransaction(tx);
        } catch (Exception exTx) {
            logger.warn("Failed to record transaction log: {}", exTx.getMessage());
        }

        // 5. Decrement book stock
        int newStock = book.getStock() - qty;
        book.setStock(newStock);
        if (newStock <= 0) {
            book.setAvailabilityStatus("OUT_OF_STOCK");
        }
        bookDao.updateBook(book);

        // 6. Record purchase
        int purchaseId = purchaseDao.createPurchase(purchase);
        logger.info("Purchase completed successfully (id={}) for book '{}' by user '{}'. New balance: {} DT",
                purchaseId, book.getTitle(), user.getEmail(), newBalance);

        return purchaseId;
    }
}
