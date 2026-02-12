package com.libraryplus.service;

import com.libraryplus.model.Loan;
import com.libraryplus.model.User;

public class NotificationService {

    public void sendOverdueNotification(User user, Loan loan, double fineAmount) {
        
        System.out.println("==================================================");
        System.out.println("EMAIL TO: " + user.getEmail());
        System.out.println("Subject: Overdue Book Alert");
        System.out.println("Body: Dear " + user.getFullName() + ", your loan for book ISBN " + loan.getBookIsbn() +
                " is overdue. A fine of " + fineAmount + " DT has been deducted from your balance.");
        System.out.println("==================================================");
    }

    public void sendWaitlistNotification(String recipientEmail, String bookIsbn) {
        
        System.out.println("==================================================");
        System.out.println("EMAIL TO: " + recipientEmail);
        System.out.println("Subject: Book Available - LibraryPlus");
        System.out.println("Body: The book (ISBN: " + bookIsbn + ") you were waiting for is now available!");
        System.out.println("==================================================");
    }
}
