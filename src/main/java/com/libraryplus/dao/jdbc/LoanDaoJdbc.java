package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.LoanDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Loan;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanDaoJdbc implements LoanDao {
    private final DataSource ds;

    public LoanDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public int createLoan(Loan loan) throws Exception {
        String sql = "INSERT INTO loans (book_isbn, client_id, borrow_date, expected_return_date) VALUES (?, ?, ?, ?)";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, loan.getBookIsbn());
            ps.setInt(2, loan.getClientId());
            ps.setObject(3, loan.getBorrowDate());
            ps.setObject(4, loan.getExpectedReturnDate());
            int affected = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            return affected;
        }
    }

    @Override
    public Optional<Loan> findActiveLoanByBookAndClient(String isbn, int clientId) throws Exception {
        String sql = "SELECT id, book_isbn, client_id, borrow_date, expected_return_date, actual_return_date, fine_amount FROM loans WHERE book_isbn = ? AND client_id = ? AND actual_return_date IS NULL";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            ps.setInt(2, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Loan l = new Loan();
                    l.setId(rs.getInt("id"));
                    l.setBookIsbn(rs.getString("book_isbn"));
                    l.setClientId(rs.getInt("client_id"));
                    if (rs.getTimestamp("borrow_date") != null)
                        l.setBorrowDate(rs.getTimestamp("borrow_date").toLocalDateTime());
                    if (rs.getTimestamp("expected_return_date") != null)
                        l.setExpectedReturnDate(rs.getTimestamp("expected_return_date").toLocalDateTime());
                    l.setFineAmount(rs.getDouble("fine_amount"));
                    return Optional.of(l);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Loan> findLoansByClient(int clientId) throws Exception {
        String sql = "SELECT id, book_isbn, client_id, borrow_date, expected_return_date, actual_return_date, fine_amount FROM loans WHERE client_id = ? ORDER BY borrow_date DESC";
        List<Loan> out = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan l = new Loan();
                    l.setId(rs.getInt("id"));
                    l.setBookIsbn(rs.getString("book_isbn"));
                    l.setClientId(rs.getInt("client_id"));
                    if (rs.getTimestamp("borrow_date") != null)
                        l.setBorrowDate(rs.getTimestamp("borrow_date").toLocalDateTime());
                    if (rs.getTimestamp("expected_return_date") != null)
                        l.setExpectedReturnDate(rs.getTimestamp("expected_return_date").toLocalDateTime());
                    l.setFineAmount(rs.getDouble("fine_amount"));
                    out.add(l);
                }
            }
        }
        return out;
    }

    @Override
    public List<Loan> findActiveLoans() throws Exception {
        String sql = "SELECT id, book_isbn, client_id, borrow_date, expected_return_date, actual_return_date, fine_amount FROM loans WHERE actual_return_date IS NULL";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            List<Loan> loans = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan l = new Loan();
                    l.setId(rs.getInt("id"));
                    l.setBookIsbn(rs.getString("book_isbn"));
                    l.setClientId(rs.getInt("client_id"));
                    if (rs.getTimestamp("borrow_date") != null)
                        l.setBorrowDate(rs.getTimestamp("borrow_date").toLocalDateTime());
                    if (rs.getTimestamp("expected_return_date") != null)
                        l.setExpectedReturnDate(rs.getTimestamp("expected_return_date").toLocalDateTime());
                    l.setFineAmount(rs.getDouble("fine_amount"));
                    loans.add(l);
                }
            }
            return loans;
        }
    }

    @Override
    public List<Loan> findActiveByClientId(int clientId) throws Exception {
        String sql = "SELECT id, book_isbn, client_id, borrow_date, expected_return_date, actual_return_date, fine_amount FROM loans WHERE client_id = ? AND actual_return_date IS NULL";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            List<Loan> loans = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan l = new Loan();
                    l.setId(rs.getInt("id"));
                    l.setBookIsbn(rs.getString("book_isbn"));
                    l.setClientId(rs.getInt("client_id"));
                    if (rs.getTimestamp("borrow_date") != null)
                        l.setBorrowDate(rs.getTimestamp("borrow_date").toLocalDateTime());
                    if (rs.getTimestamp("expected_return_date") != null)
                        l.setExpectedReturnDate(rs.getTimestamp("expected_return_date").toLocalDateTime());
                    l.setFineAmount(rs.getDouble("fine_amount"));
                    loans.add(l);
                }
            }
            return loans;
        }
    }

    @Override
    public void updateLoan(Loan loan) throws Exception {
        String sql = "UPDATE loans SET fine_amount = ?, actual_return_date = ? WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, loan.getFineAmount());
            ps.setObject(2, loan.getActualReturnDate());
            ps.setInt(3, loan.getId());
            ps.executeUpdate();
        }
    }
}
