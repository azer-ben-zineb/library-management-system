package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.AdminMessageDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.AdminMessage;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminMessageDaoJdbc implements AdminMessageDao {

    private final DataSource ds;

    public AdminMessageDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    private AdminMessage mapRow(ResultSet rs) throws SQLException {
        AdminMessage m = new AdminMessage();
        m.setId(rs.getInt("id"));
        int clientId = rs.getInt("client_id");
        if (!rs.wasNull()) {
            m.setClientId(clientId);
        }
        m.setSenderEmail(rs.getString("sender_email"));
        m.setSubject(rs.getString("subject"));
        m.setContent(rs.getString("content"));
        m.setStatus(rs.getString("status"));
        m.setAdminReply(rs.getString("admin_reply"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            m.setCreatedAt(ts.toLocalDateTime());
        }
        return m;
    }

    @Override
    public void create(AdminMessage message) {
        String sql = "INSERT INTO admin_messages (client_id, sender_email, subject, content, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (message.getClientId() != null) {
                ps.setInt(1, message.getClientId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, message.getSenderEmail());
            ps.setString(3, message.getSubject());
            ps.setString(4, message.getContent());
            ps.setString(5, message.getStatus() == null ? "UNREAD" : message.getStatus());
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<AdminMessage> findAll() {
        String sql = "SELECT * FROM admin_messages ORDER BY created_at DESC";
        List<AdminMessage> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    @Override
    public List<AdminMessage> findByStatus(String status) {
        String sql = "SELECT * FROM admin_messages WHERE status = ? ORDER BY created_at DESC";
        List<AdminMessage> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
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
    public void markAsRead(int id) {
        String sql = "UPDATE admin_messages SET status = 'READ' WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void saveAdminReply(int id, String reply, String newStatus) {
        String sql = "UPDATE admin_messages SET admin_reply = ?, status = ? WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reply);
            ps.setString(2, newStatus == null ? "REPLIED" : newStatus);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
