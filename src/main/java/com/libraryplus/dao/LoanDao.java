package com.libraryplus.dao;

import com.libraryplus.model.Loan;

import java.util.List;
import java.util.Optional;

public interface LoanDao {
    int createLoan(Loan loan) throws Exception;

    Optional<Loan> findActiveLoanByBookAndClient(String isbn, int clientId) throws Exception;

    List<Loan> findLoansByClient(int clientId) throws Exception;

    List<Loan> findActiveLoans() throws Exception;

    List<Loan> findActiveByClientId(int clientId) throws Exception;

    void updateLoan(Loan loan) throws Exception;
}
