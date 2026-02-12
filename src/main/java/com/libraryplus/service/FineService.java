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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class FineService {
    private final LoanDao loanDao;
    private final UserDao userDao;
    private final ClientDao clientDao;
    private final NotificationService notificationService;

    public FineService() {
        this.loanDao = new LoanDaoJdbc();
        this.userDao = new UserDaoJdbc();
        this.clientDao = new ClientDaoJdbc();
        this.notificationService = new NotificationService();
    }

    public void calculateAndApplyFines() {
        try {
            List<Loan> activeLoans = loanDao.findActiveLoans();
            LocalDateTime now = LocalDateTime.now();

            for (Loan loan : activeLoans) {
                if (loan.getExpectedReturnDate() != null && now.isAfter(loan.getExpectedReturnDate())) {
                    long hoursOverdue = Duration.between(loan.getExpectedReturnDate(), now).toHours();
                    if (hoursOverdue > 0) {
                        double totalFine = hoursOverdue * 1.0; 
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
                                    user.setCardBalance(user.getCardBalance() - diff);
                                    userDao.updateUser(user);

                                    
                                    notificationService.sendOverdueNotification(user, loan, diff);

                                    
                                    if (user.getCardBalance() < 0) {
                                        
                                        System.out.println("ADMIN ALERT: User " + user.getEmail()
                                                + " has negative balance due to fines.");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
