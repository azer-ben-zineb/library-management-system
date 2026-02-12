package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.WaitlistDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.WaitlistEntry;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WaitlistDaoJdbc implements WaitlistDao {

    private final DataSource ds;

    public WaitlistDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    private WaitlistEntry mapRow(ResultSet rs) throws SQLException {
        WaitlistEntry e = new WaitlistEntry();
        e.setId(rs.getInt("id"));
        e.setBookIsbn(rs.getString("book_isbn"));
        e.setClientId(rs.getInt("client_id"));
        e.setPosition(rs.getInt("position"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            e.setCreatedAt(ts.toLocalDateTime());
        }
        return e;
    }

    @Override
    public void enqueue(String bookIsbn, int clientId) {
        String getMaxSql = "SELECT COALESCE(MAX(position), 0) + 1 FROM book_waitlist WHERE book_isbn = ?";
        String insertSql = "INSERT INTO book_waitlist (book_isbn, client_id, position) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement psMax = conn.prepareStatement(getMaxSql)) {
            conn.setAutoCommit(false);
            try {
                psMax.setString(1, bookIsbn);
                int nextPos = 1;
                try (ResultSet rs = psMax.executeQuery()) {
                    if (rs.next()) {
                        nextPos = rs.getInt(1);
                    }
                }
                try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                    psIns.setString(1, bookIsbn);
                    psIns.setInt(2, clientId);
                    psIns.setInt(3, nextPos);
                    psIns.executeUpdate();
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Optional<WaitlistEntry> dequeueNext(String bookIsbn) {
        String selectSql = "SELECT * FROM book_waitlist WHERE book_isbn = ? ORDER BY position ASC LIMIT 1";
        String deleteSql = "DELETE FROM book_waitlist WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement psSel = conn.prepareStatement(selectSql)) {
            conn.setAutoCommit(false);
            try {
                psSel.setString(1, bookIsbn);
                WaitlistEntry entry = null;
                try (ResultSet rs = psSel.executeQuery()) {
                    if (rs.next()) {
                        entry = mapRow(rs);
                    }
                }
                if (entry != null) {
                    try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                        psDel.setInt(1, entry.getId());
                        psDel.executeUpdate();
                    }
                    conn.commit();
                    return Optional.of(entry);
                } else {
                    conn.rollback();
                }
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<WaitlistEntry> getQueueForBook(String bookIsbn) {
        String sql = "SELECT * FROM book_waitlist WHERE book_isbn = ? ORDER BY position ASC";
        List<WaitlistEntry> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookIsbn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean isClientInQueue(String bookIsbn, int clientId) {
        String sql = "SELECT 1 FROM book_waitlist WHERE book_isbn = ? AND client_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookIsbn);
            ps.setInt(2, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
