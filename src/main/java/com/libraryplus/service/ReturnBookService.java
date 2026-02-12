package com.libraryplus.service;

import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.LoanDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.LoanDaoJdbc;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.Client;
import com.libraryplus.model.Loan;
import com.libraryplus.model.User;
import com.libraryplus.model.WaitlistEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReturnBookService {
    private final LoanDao loanDao;
    private final WaitlistService waitlistService;
    private final NotificationService notificationService;
    private final ClientDao clientDao;
    private final UserDao userDao;

    public ReturnBookService() {
        this.loanDao = new LoanDaoJdbc();
        this.waitlistService = new WaitlistService();
        this.notificationService = new NotificationService();
        this.clientDao = new ClientDaoJdbc();
        this.userDao = new UserDaoJdbc();
    }

    public void returnBook(String bookIsbn, int clientId) throws Exception {
        
        Optional<Loan> activeLoan = loanDao.findActiveLoanByBookAndClient(bookIsbn, clientId);
        if (!activeLoan.isPresent()) {
            throw new IllegalStateException("No active loan found for this book and client");
        }

        Loan loan = activeLoan.get();
        loan.setActualReturnDate(LocalDateTime.now());
        loanDao.updateLoan(loan);

        System.out.println("Book returned: " + bookIsbn + " by client " + clientId);

        
        WaitlistEntry next = waitlistService.popNext(bookIsbn);
        if (next != null) {
            System.out.println("Notifying next client in waitlist: " + next.getClientId());

            
            try {
                Optional<Client> clientOpt = clientDao.findById(next.getClientId());
                if (clientOpt.isPresent()) {
                    Client client = clientOpt.get();
                    Optional<User> userOpt = userDao.findById(client.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        notificationService.sendWaitlistNotification(user.getEmail(), bookIsbn);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to notify waitlist client: " + e.getMessage());
            }
        }
    }

    public List<Loan> getActiveLoansForClient(int clientId) {
        try {
            return loanDao.findActiveByClientId(clientId);
        } catch (Exception e) {
            System.err.println("Failed to get active loans: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
