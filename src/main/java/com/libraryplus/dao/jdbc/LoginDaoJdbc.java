package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.LoginDao;
import com.libraryplus.db.DataSourceConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;

public class LoginDaoJdbc implements LoginDao {
    private final DataSource ds;

    public LoginDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public void recordLogin(int userId, Instant when, String ipAddress) throws Exception {
        String sql = "INSERT INTO login_events (user_id, login_time, ip_address) VALUES (?, ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setObject(2, java.sql.Timestamp.from(when));
            if (ipAddress != null) ps.setString(3, ipAddress); else ps.setNull(3, java.sql.Types.VARCHAR);
            ps.executeUpdate();
        }
    }
}

