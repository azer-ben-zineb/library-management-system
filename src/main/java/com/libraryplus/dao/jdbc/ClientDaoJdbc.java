package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.ClientDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Client;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientDaoJdbc implements ClientDao {
    private final DataSource ds;

    public ClientDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public Optional<Client> findByUserId(int userId) throws Exception {
        String sql = "SELECT id, user_id, phone, first_name, last_name, date_of_birth, membership_type FROM clients WHERE user_id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Client cl = new Client();
                    cl.setId(rs.getInt("id"));
                    cl.setUserId(rs.getInt("user_id"));
                    cl.setPhone(rs.getString("phone"));
                    cl.setFirstName(rs.getString("first_name"));
                    cl.setLastName(rs.getString("last_name"));
                    
                    cl.setMembershipType(rs.getString("membership_type"));
                    return Optional.of(cl);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public int createClient(Client client) throws Exception {
        String sql = "INSERT INTO clients (user_id, phone, first_name, last_name, date_of_birth, membership_type) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, client.getUserId());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getFirstName());
            ps.setString(4, client.getLastName());
            ps.setObject(5, client.getDateOfBirth());
            ps.setString(6, client.getMembershipType());
            int affected = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            return affected;
        }
    }

    @Override
    public Optional<Client> findById(int id) throws Exception {
        String sql = "SELECT id, user_id, phone, first_name, last_name, date_of_birth, membership_type FROM clients WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Client cl = new Client();
                    cl.setId(rs.getInt("id"));
                    cl.setUserId(rs.getInt("user_id"));
                    cl.setPhone(rs.getString("phone"));
                    cl.setFirstName(rs.getString("first_name"));
                    cl.setLastName(rs.getString("last_name"));
                    
                    cl.setMembershipType(rs.getString("membership_type"));
                    return Optional.of(cl);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Client> findAll() throws Exception {
        String sql = "SELECT id, user_id, phone, first_name, last_name, date_of_birth, membership_type FROM clients";
        List<Client> clients = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Client client = new Client();
                    client.setId(rs.getInt("id"));
                    client.setUserId(rs.getInt("user_id"));
                    client.setPhone(rs.getString("phone"));
                    client.setFirstName(rs.getString("first_name"));
                    client.setLastName(rs.getString("last_name"));
                    java.sql.Date dob = rs.getDate("date_of_birth");
                    if (dob != null)
                        client.setDateOfBirth(dob.toLocalDate());
                    client.setMembershipType(rs.getString("membership_type"));
                    clients.add(client);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clients;
    }
}
