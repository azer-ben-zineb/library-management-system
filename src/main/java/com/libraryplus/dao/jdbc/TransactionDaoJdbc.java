package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.TransactionDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDaoJdbc implements TransactionDao {
    private final DataSource ds;

    public TransactionDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public int createTransaction(Transaction transaction) throws Exception {
        String sql = "INSERT INTO transactions (client_id, amount, reason, timestamp, resulting_balance) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, transaction.getClientId());
            ps.setDouble(2, transaction.getAmount());
            ps.setString(3, transaction.getReason());
            LocalDateTime ts = transaction.getTimestamp() != null ? transaction.getTimestamp() : LocalDateTime.now();
            ps.setTimestamp(4, Timestamp.valueOf(ts));
            ps.setDouble(5, transaction.getResultingBalance());
            int affected = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    return keys.getInt(1);
                }
            }
            return affected;
        }
    }

    @Override
    public List<Transaction> findByClientId(int clientId) throws Exception {
        String sql = "SELECT id, client_id, amount, reason, timestamp, resulting_balance FROM transactions WHERE client_id = ? ORDER BY timestamp DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction();
                    t.setId(rs.getInt("id"));
                    t.setClientId(rs.getInt("client_id"));
                    t.setAmount(rs.getDouble("amount"));
                    t.setReason(rs.getString("reason"));
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) {
                        t.setTimestamp(ts.toLocalDateTime());
                    }
                    t.setResultingBalance(rs.getDouble("resulting_balance"));
                    list.add(t);
                }
            }
        }
        return list;
    }

    @Override
    public List<Transaction> findAll() throws Exception {
        String sql = "SELECT id, client_id, amount, reason, timestamp, resulting_balance FROM transactions ORDER BY timestamp DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getInt("id"));
                t.setClientId(rs.getInt("client_id"));
                t.setAmount(rs.getDouble("amount"));
                t.setReason(rs.getString("reason"));
                Timestamp ts = rs.getTimestamp("timestamp");
                if (ts != null) {
                    t.setTimestamp(ts.toLocalDateTime());
                }
                t.setResultingBalance(rs.getDouble("resulting_balance"));
                list.add(t);
            }
        }
        return list;
    }
}
