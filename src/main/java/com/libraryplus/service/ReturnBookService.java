package com.libraryplus.service;

import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.LoanDao;
import com.libraryplus.dao.TransactionDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.LoanDaoJdbc;
import com.libraryplus.dao.jdbc.TransactionDaoJdbc;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.Book;
import com.libraryplus.model.Client;
import com.libraryplus.model.Loan;
import com.libraryplus.model.Transaction;
import com.libraryplus.model.User;
import com.libraryplus.model.WaitlistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReturnBookService {
    private static final Logger logger = LoggerFactory.getLogger(ReturnBookService.class);

    private final LoanDao loanDao;
    private final BookDao bookDao;
    private final WaitlistService waitlistService;
    private final NotificationService notificationService;
    private final ClientDao clientDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public ReturnBookService() {
        this.loanDao = new LoanDaoJdbc();
        this.bookDao = new BookDaoJdbc();
        this.waitlistService = new WaitlistService();
        this.notificationService = new NotificationService();
        this.clientDao = new ClientDaoJdbc();
        this.userDao = new UserDaoJdbc();
        this.transactionDao = new TransactionDaoJdbc();
    }

    public ReturnBookService(LoanDao loanDao, BookDao bookDao, WaitlistService waitlistService,
                             NotificationService notificationService, ClientDao clientDao,
                             UserDao userDao, TransactionDao transactionDao) {
        this.loanDao = loanDao;
        this.bookDao = bookDao;
        this.waitlistService = waitlistService;
        this.notificationService = notificationService;
        this.clientDao = clientDao;
        this.userDao = userDao;
        this.transactionDao = transactionDao;
    }

    public void returnBook(String bookIsbn, int clientId) throws Exception {
        Optional<Loan> activeLoan = loanDao.findActiveLoanByBookAndClient(bookIsbn, clientId);
        if (activeLoan.isEmpty()) {
            throw new IllegalStateException("No active loan found for ISBN: " + bookIsbn + " and client ID: " + clientId);
        }

        Loan loan = activeLoan.get();
        LocalDateTime returnTime = LocalDateTime.now();
        loan.setActualReturnDate(returnTime);

        // 1. Calculate overdue fines if any (e.g. 1.00 DT per overdue day)
        if (loan.getExpectedReturnDate() != null && returnTime.isAfter(loan.getExpectedReturnDate())) {
            long daysOverdue = Duration.between(loan.getExpectedReturnDate(), returnTime).toDays();
            if (daysOverdue == 0) daysOverdue = 1; // at least 1 day overdue if past date
            double fine = daysOverdue * 1.00;
            loan.setFineAmount(fine);

            // Deduct fine from user balance if possible
            try {
                Optional<Client> clientOpt = clientDao.findById(clientId);
                if (clientOpt.isPresent()) {
                    Optional<User> userOpt = userDao.findById(clientOpt.get().getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        double updatedBal = Math.round((user.getCardBalance() - fine) * 100.0) / 100.0;
                        user.setCardBalance(updatedBal);
                        userDao.updateUser(user);

                        Transaction tx = new Transaction();
                        tx.setClientId(clientId);
                        tx.setAmount(fine);
                        tx.setReason("Late return fine: " + bookIsbn + " (" + daysOverdue + " days overdue)");
                        tx.setResultingBalance(updatedBal);
                        transactionDao.createTransaction(tx);

                        notificationService.sendOverdueNotification(user, loan, fine);
                    }
                }
            } catch (Exception exFine) {
                logger.warn("Could not apply late fine: {}", exFine.getMessage());
            }
        }

        loanDao.updateLoan(loan);
        logger.info("Loan #{} closed for book {} returned by client {}", loan.getId(), bookIsbn, clientId);

        // 2. Increment book stock and restore availability
        try {
            Optional<Book> bookOpt = bookDao.findByIsbn(bookIsbn);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                book.setStock(book.getStock() + 1);
                book.setAvailabilityStatus("AVAILABLE");
                bookDao.updateBook(book);
                logger.info("Restored stock for book {}: new stock = {}", book.getTitle(), book.getStock());
            }
        } catch (Exception exBook) {
            logger.error("Failed to update stock for returned book: {}", exBook.getMessage(), exBook);
        }

        // 3. Notify next client on waitlist if applicable
        try {
            WaitlistEntry next = waitlistService.popNext(bookIsbn);
            if (next != null) {
                logger.info("Waitlist entry popped for client ID: {}", next.getClientId());
                Optional<Client> clientOpt = clientDao.findById(next.getClientId());
                if (clientOpt.isPresent()) {
                    Client client = clientOpt.get();
                    Optional<User> userOpt = userDao.findById(client.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        notificationService.sendWaitlistNotification(user.getEmail(), bookIsbn);
                    }
                }
            }
        } catch (Exception exWaitlist) {
            logger.warn("Failed to notify waitlist client: {}", exWaitlist.getMessage());
        }
    }

    public List<Loan> getActiveLoansForClient(int clientId) {
        try {
            return loanDao.findActiveByClientId(clientId);
        } catch (Exception e) {
            logger.error("Failed to get active loans for client {}: {}", clientId, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    public List<Loan> getAllLoansForClient(int clientId) {
        try {
            return loanDao.findLoansByClient(clientId);
        } catch (Exception e) {
            logger.error("Failed to get all loans for client {}: {}", clientId, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
