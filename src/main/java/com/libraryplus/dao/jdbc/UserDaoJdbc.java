package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.UserDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.User;
import com.libraryplus.util.EncryptionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;


public class UserDaoJdbc implements UserDao {
    private final DataSource ds;

    public UserDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        String sql = "SELECT id, email, password_hash, role_id, full_name, phone, date_of_birth, card_number, card_balance FROM users WHERE email = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setEmail(rs.getString("email"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRoleId(rs.getInt("role_id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setPhone(rs.getString("phone"));
                    
                    String storedCard = rs.getString("card_number");
                    if (storedCard != null) {
                        String decrypted = EncryptionUtils.decrypt(storedCard);
                        u.setCardNumber(decrypted);
                    }
                    u.setCardBalance(rs.getDouble("card_balance"));
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public int createUser(User user) throws Exception {
        String sql = "INSERT INTO users (email, password_hash, role_id, full_name, phone, date_of_birth, card_number, card_balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setInt(3, user.getRoleId());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            if (user.getDateOfBirth() != null) {
                ps.setDate(6, java.sql.Date.valueOf(user.getDateOfBirth()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }
            String cardToStore = null;
            if (user.getCardNumber() != null) {
                cardToStore = com.libraryplus.util.EncryptionUtils.encrypt(user.getCardNumber());
            }
            ps.setString(7, cardToStore);
            ps.setDouble(8, user.getCardBalance());
            int affected = 0;
            try {
                affected = ps.executeUpdate();
            } catch (Exception e) {
                System.err
                        .println("Failed to execute user insert for email=" + user.getEmail() + ": " + e.getMessage());
                throw e;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    return keys.getInt(1);
                }
            }
            return affected;
        }
    }

    @Override
    public Optional<User> findById(int id) throws Exception {
        String sql = "SELECT id, email, password_hash, role_id, full_name, phone, date_of_birth, card_number, card_balance FROM users WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setEmail(rs.getString("email"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRoleId(rs.getInt("role_id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setPhone(rs.getString("phone"));
                    
                    String storedCard = rs.getString("card_number");
                    if (storedCard != null) {
                        String decrypted = EncryptionUtils.decrypt(storedCard);
                        u.setCardNumber(decrypted);
                    }
                    u.setCardBalance(rs.getDouble("card_balance"));
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void updateUser(User user) throws Exception {
        String sql = "UPDATE users SET card_balance = ? WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, user.getCardBalance());
            ps.setInt(2, user.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public java.util.List<User> findAll() throws Exception {
        String sql = "SELECT id, email, password_hash, role_id, full_name, phone, date_of_birth, card_number, card_balance FROM users";
        java.util.List<User> users = new java.util.ArrayList<>();
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setEmail(rs.getString("email"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRoleId(rs.getInt("role_id"));
                u.setFullName(rs.getString("full_name"));
                u.setPhone(rs.getString("phone"));
                
                String storedCard = rs.getString("card_number");
                if (storedCard != null) {
                    try {
                        String decrypted = EncryptionUtils.decrypt(storedCard);
                        u.setCardNumber(decrypted);
                    } catch (Exception e) {
                        
                        u.setCardNumber("****");
                    }
                }
                u.setCardBalance(rs.getDouble("card_balance"));
                users.add(u);
            }
        }
        return users;
    }
}
