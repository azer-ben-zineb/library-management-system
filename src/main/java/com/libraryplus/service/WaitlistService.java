package com.libraryplus.service;

import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.WaitlistEntry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class WaitlistService {
    private final DataSource ds;

    public WaitlistService() {
        this.ds = DataSourceConfig.getDataSource();
    }

    public void joinWaitlist(String isbn, int clientId) throws Exception {
        
        String posSql = "SELECT COALESCE(MAX(position), 0) + 1 AS next_pos FROM book_waitlist WHERE book_isbn = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(posSql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int nextPos = rs.getInt("next_pos");
                    String insert = "INSERT INTO book_waitlist (book_isbn, client_id, position, created_at) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ins = c.prepareStatement(insert)) {
                        ins.setString(1, isbn);
                        ins.setInt(2, clientId);
                        ins.setInt(3, nextPos);
                        ins.setObject(4, LocalDateTime.now());
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    public WaitlistEntry popNext(String isbn) throws Exception {
        String firstSql = "SELECT id, client_id, position, created_at FROM book_waitlist WHERE book_isbn = ? ORDER BY position ASC, created_at ASC LIMIT 1";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(firstSql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int clientId = rs.getInt("client_id");
                    int position = rs.getInt("position");
                    String del = "DELETE FROM book_waitlist WHERE id = ?";
                    try (PreparedStatement delps = c.prepareStatement(del)) {
                        delps.setInt(1, id);
                        delps.executeUpdate();
                    }
                    WaitlistEntry e = new WaitlistEntry();
                    e.setId(id);
                    e.setBookIsbn(isbn);
                    e.setClientId(clientId);
                    e.setPosition(position);
                    e.setCreatedAt(java.time.LocalDateTime.now());
                    return e;
                }
            }
        }
        return null;
    }

    public List<WaitlistEntry> listForBook(String isbn) throws Exception {
        String sql = "SELECT id, client_id, position, created_at FROM book_waitlist WHERE book_isbn = ? ORDER BY position ASC, created_at ASC";
        List<WaitlistEntry> list = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WaitlistEntry e = new WaitlistEntry();
                    e.setId(rs.getInt("id"));
                    e.setBookIsbn(isbn);
                    e.setClientId(rs.getInt("client_id"));
                    e.setPosition(rs.getInt("position"));
                    e.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
                    list.add(e);
                }
            }
        }
        return list;
    }
}

