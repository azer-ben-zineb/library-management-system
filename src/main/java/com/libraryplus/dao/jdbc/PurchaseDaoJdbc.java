package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.PurchaseDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Purchase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDaoJdbc implements PurchaseDao {
    private final DataSource ds;

    public PurchaseDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public int createPurchase(Purchase p) throws Exception {
        String sql = "INSERT INTO purchases (client_id, book_isbn, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getClientId());
            ps.setString(2, p.getBookIsbn());
            ps.setInt(3, p.getQuantity());
            ps.setDouble(4, p.getUnitPrice());
            int affected = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return affected;
        }
    }
    @Override
    public List<Purchase> findByClient(int clientId) throws Exception {
        String sql = "SELECT id, client_id, book_isbn, quantity, unit_price, purchase_date FROM purchases WHERE client_id = ? ORDER BY purchase_date DESC";
        List<Purchase> out = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Purchase p = new Purchase();
                    p.setId(rs.getInt("id"));
                    p.setClientId(rs.getInt("client_id"));
                    p.setBookIsbn(rs.getString("book_isbn"));
                    p.setQuantity(rs.getInt("quantity"));
                    p.setUnitPrice(rs.getDouble("unit_price"));
                    
                    out.add(p);
                }
            }
        }
        return out;
    }
}

