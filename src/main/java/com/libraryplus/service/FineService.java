package com.libraryplus.service;

import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.LoanDao;
import com.libraryplus.dao.TransactionDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.LoanDaoJdbc;
import com.libraryplus.dao.jdbc.TransactionDaoJdbc;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.Client;
import com.libraryplus.model.Loan;
import com.libraryplus.model.Transaction;
import com.libraryplus.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class FineService {
    private static final Logger logger = LoggerFactory.getLogger(FineService.class);

    private final LoanDao loanDao;
    private final UserDao userDao;
    private final ClientDao clientDao;
    private final TransactionDao transactionDao;
    private final NotificationService notificationService;

    public FineService() {
        this.loanDao = new LoanDaoJdbc();
        this.userDao = new UserDaoJdbc();
        this.clientDao = new ClientDaoJdbc();
        this.transactionDao = new TransactionDaoJdbc();
        this.notificationService = new NotificationService();
    }

    public void calculateAndApplyFines() {
        try {
            List<Loan> activeLoans = loanDao.findActiveLoans();
            LocalDateTime now = LocalDateTime.now();

            for (Loan loan : activeLoans) {
                if (loan.getExpectedReturnDate() != null && now.isAfter(loan.getExpectedReturnDate())) {
                    long daysOverdue = Duration.between(loan.getExpectedReturnDate(), now).toDays();
                    if (daysOverdue <= 0) {
                        daysOverdue = 1;
                    }
                    double totalFine = Math.min(20.00, daysOverdue * 1.00); // 1.00 DT per day overdue (capped at 20 DT)
                    double previousFine = loan.getFineAmount();
                    double diff = totalFine - previousFine;

                    if (diff > 0) {
                        loan.setFineAmount(totalFine);
                        loanDao.updateLoan(loan);

                        Optional<Client> clientOpt = clientDao.findById(loan.getClientId());
                        if (clientOpt.isPresent()) {
                            Optional<User> userOpt = userDao.findById(clientOpt.get().getUserId());
                            if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                double newBalance = Math.round((user.getCardBalance() - diff) * 100.0) / 100.0;
                                user.setCardBalance(newBalance);
                                userDao.updateUser(user);

                                try {
                                    Transaction tx = new Transaction();
                                    tx.setClientId(clientOpt.get().getId());
                                    tx.setAmount(diff);
                                    tx.setReason("Overdue loan fine (" + daysOverdue + "d late): " + loan.getBookIsbn());
                                    tx.setResultingBalance(newBalance);
                                    transactionDao.createTransaction(tx);
                                } catch (Exception exTx) {
                                    logger.warn("Failed to record fine transaction: {}", exTx.getMessage());
                                }

                                notificationService.sendOverdueNotification(user, loan, diff);

                                if (newBalance < 0) {
                                    logger.warn("ADMIN ALERT: User {} has negative balance ({}) due to overdue fines.",
                                            user.getEmail(), newBalance);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error calculating overdue fines", e);
        }
    }
}
