package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.SubscriptionDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Subscription;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubscriptionDaoJdbc implements SubscriptionDao {

    private final DataSource ds;

    public SubscriptionDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    private Subscription mapRow(ResultSet rs) throws java.sql.SQLException {
        Subscription s = new Subscription();
        s.setId(rs.getInt("id"));
        s.setClientId(rs.getInt("client_id"));
        Date start = rs.getDate("start_date");
        if (start != null) s.setStartDate(start.toLocalDate());
        Date end = rs.getDate("end_date");
        if (end != null) s.setEndDate(end.toLocalDate());
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }

    @Override
    public Optional<Subscription> findActiveByClientId(int clientId) {
        String sql = "SELECT * FROM subscriptions WHERE client_id = ? AND is_active = TRUE AND end_date >= CURRENT_DATE LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Subscription> findByClientId(int clientId) {
        String sql = "SELECT * FROM subscriptions WHERE client_id = ? ORDER BY start_date DESC";
        List<Subscription> subs = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subs.add(mapRow(rs));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return subs;
    }

    @Override
    public void createSubscription(int clientId, LocalDate startDate, LocalDate endDate) {
        String sql = "INSERT INTO subscriptions (client_id, start_date, end_date, is_active) VALUES (?, ?, ?, TRUE)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int countActiveSubscriptions() {
        String sql = "SELECT COUNT(*) FROM subscriptions WHERE is_active = TRUE AND end_date >= CURRENT_DATE";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }
}
