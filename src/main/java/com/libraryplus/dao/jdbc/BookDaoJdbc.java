package com.libraryplus.dao.jdbc;

import com.libraryplus.dao.BookDao;
import com.libraryplus.db.DataSourceConfig;
import com.libraryplus.model.Book;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDaoJdbc implements BookDao {
    private final DataSource ds;

    public BookDaoJdbc() {
        this.ds = DataSourceConfig.getDataSource();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) throws Exception {
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "SELECT isbn, title, author, category, availability_status, cover_image_path, description, stock, price FROM books WHERE isbn = ?"
                : "SELECT isbn, title, author, category, availability_status, cover_image_path, description, stock FROM books WHERE isbn = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book b = new Book();
                    b.setIsbn(rs.getString("isbn"));
                    b.setTitle(rs.getString("title"));
                    b.setAuthor(rs.getString("author"));
                    b.setCategory(rs.getString("category"));
                    b.setAvailabilityStatus(rs.getString("availability_status"));
                    b.setCoverImagePath(rs.getString("cover_image_path"));
                    b.setDescription(rs.getString("description"));
                    b.setStock(rs.getInt("stock"));
                    if (hasPriceColumn) {
                        try {
                            double price = rs.getDouble("price");
                            if (rs.wasNull() || price <= 0) {
                                b.setPrice(1.00); 
                            } else {
                                b.setPrice(price);
                            }
                        } catch (Exception e) {
                            b.setPrice(1.00);
                        }
                    } else {
                        b.setPrice(1.00); 
                    }
                    return Optional.of(b);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Book> search(String query) throws Exception {
        return search(query, 0, 100);
    }

    @Override
    public List<Book> search(String query, int offset, int limit) throws Exception {
        
        if (query == null || query.isBlank()) {
            
            boolean hasPriceColumn = checkColumnExists("books", "price");
            String sql = hasPriceColumn
                    ? "SELECT isbn, title, author, category, availability_status, stock, price FROM books LIMIT ? OFFSET ?"
                    : "SELECT isbn, title, author, category, availability_status, stock FROM books LIMIT ? OFFSET ?";
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Book> results = new ArrayList<>();
                    while (rs.next()) {
                        Book b = new Book();
                        b.setIsbn(rs.getString("isbn"));
                        b.setTitle(rs.getString("title"));
                        b.setAuthor(rs.getString("author"));
                        b.setCategory(rs.getString("category"));
                        b.setAvailabilityStatus(rs.getString("availability_status"));
                        b.setStock(rs.getInt("stock"));
                        if (hasPriceColumn) {
                            try {
                                double price = rs.getDouble("price");
                                if (rs.wasNull() || price <= 0) {
                                    b.setPrice(1.00); 
                                } else {
                                    b.setPrice(price);
                                }
                            } catch (Exception e) {
                                b.setPrice(1.00);
                            }
                        } else {
                            b.setPrice(1.00); 
                        }
                        results.add(b);
                    }
                    return results;
                }
            }
        }

        
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "SELECT isbn, title, author, category, availability_status, stock, price FROM books WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? LIMIT ? OFFSET ?"
                : "SELECT isbn, title, author, category, availability_status, stock FROM books WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt(4, limit);
            ps.setInt(5, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> results = new ArrayList<>();
                while (rs.next()) {
                    Book b = new Book();
                    b.setIsbn(rs.getString("isbn"));
                    b.setTitle(rs.getString("title"));
                    b.setAuthor(rs.getString("author"));
                    b.setCategory(rs.getString("category"));
                    b.setAvailabilityStatus(rs.getString("availability_status"));
                    b.setStock(rs.getInt("stock"));
                    if (hasPriceColumn) {
                        try {
                            double price = rs.getDouble("price");
                            if (rs.wasNull() || price <= 0) {
                                b.setPrice(1.00); 
                            } else {
                                b.setPrice(price);
                            }
                        } catch (Exception e) {
                            b.setPrice(1.00);
                        }
                    } else {
                        b.setPrice(1.00); 
                    }
                    results.add(b);
                }
                return results;
            }
        }
    }

    @Override
    public int createBook(Book book) throws Exception {
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "INSERT INTO books (isbn, title, author, category, availability_status, cover_image_path, description, stock, price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO books (isbn, title, author, category, availability_status, cover_image_path, description, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getCategory());
            ps.setString(5, book.getAvailabilityStatus());
            ps.setString(6, book.getCoverImagePath());
            ps.setString(7, book.getDescription());
            ps.setInt(8, book.getStock());
            if (hasPriceColumn) {
                ps.setDouble(9, book.getPrice());
            }
            return ps.executeUpdate();
        }
    }

    @Override
    public int updateBook(Book book) throws Exception {
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "UPDATE books SET title = ?, author = ?, category = ?, availability_status = ?, cover_image_path = ?, description = ?, stock = ?, price = ? WHERE isbn = ?"
                : "UPDATE books SET title = ?, author = ?, category = ?, availability_status = ?, cover_image_path = ?, description = ?, stock = ? WHERE isbn = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getCategory());
            ps.setString(4, book.getAvailabilityStatus());
            ps.setString(5, book.getCoverImagePath());
            ps.setString(6, book.getDescription());
            ps.setInt(7, book.getStock());
            if (hasPriceColumn) {
                ps.setDouble(8, book.getPrice());
                ps.setString(9, book.getIsbn());
            } else {
                ps.setString(8, book.getIsbn());
            }
            return ps.executeUpdate();
        }
    }

     
    private boolean checkColumnExists(String tableName, String columnName) {
        try (Connection c = ds.getConnection()) {
            String dbProduct = c.getMetaData().getDatabaseProductName().toLowerCase();
            String sql;

            if (dbProduct.contains("mysql")) {
                
                sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            } else {
                
                sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            }

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, tableName.toUpperCase());
                ps.setString(2, columnName.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (Exception e) {
            
            return false;
        }
        return false;
    }

    @Override
    public int deleteByIsbn(String isbn) throws Exception {
        String sql = "DELETE FROM books WHERE isbn = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            return ps.executeUpdate();
        }
    }

    @Override
    public List<Book> findFeatured(int limit) throws Exception {
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "SELECT b.isbn, b.title, b.author, b.category, b.availability_status, b.description, b.stock, b.price, "
                        +
                        "COALESCE(avg_r.avg_rating,0) AS avg_rating, COALESCE(avg_r.count_ratings,0) AS ratings_count, "
                        +
                        "COALESCE(p.purchases_count,0) AS purchases_count, COALESCE(l.loans_count,0) AS loans_count " +
                        "FROM books b " +
                        "LEFT JOIN (SELECT book_isbn, AVG(rating) AS avg_rating, COUNT(*) AS count_ratings FROM ratings GROUP BY book_isbn) avg_r ON avg_r.book_isbn = b.isbn "
                        +
                        "LEFT JOIN (SELECT book_isbn, COUNT(*) AS purchases_count FROM purchases GROUP BY book_isbn) p ON p.book_isbn = b.isbn "
                        +
                        "LEFT JOIN (SELECT book_isbn, COUNT(*) AS loans_count FROM loans GROUP BY book_isbn) l ON l.book_isbn = b.isbn "
                        +
                        "ORDER BY (COALESCE(avg_r.avg_rating,0) * 2 + COALESCE(avg_r.count_ratings,0) * 0.5 + COALESCE(p.purchases_count,0) * 0.3 + COALESCE(l.loans_count,0) * 0.4) DESC "
                        +
                        "LIMIT ?"
                : "SELECT b.isbn, b.title, b.author, b.category, b.availability_status, b.description, b.stock, " +
                        "COALESCE(avg_r.avg_rating,0) AS avg_rating, COALESCE(avg_r.count_ratings,0) AS ratings_count, "
                        +
                        "COALESCE(p.purchases_count,0) AS purchases_count, COALESCE(l.loans_count,0) AS loans_count " +
                        "FROM books b " +
                        "LEFT JOIN (SELECT book_isbn, AVG(rating) AS avg_rating, COUNT(*) AS count_ratings FROM ratings GROUP BY book_isbn) avg_r ON avg_r.book_isbn = b.isbn "
                        +
                        "LEFT JOIN (SELECT book_isbn, COUNT(*) AS purchases_count FROM purchases GROUP BY book_isbn) p ON p.book_isbn = b.isbn "
                        +
                        "LEFT JOIN (SELECT book_isbn, COUNT(*) AS loans_count FROM loans GROUP BY book_isbn) l ON l.book_isbn = b.isbn "
                        +
                        "ORDER BY (COALESCE(avg_r.avg_rating,0) * 2 + COALESCE(avg_r.count_ratings,0) * 0.5 + COALESCE(p.purchases_count,0) * 0.3 + COALESCE(l.loans_count,0) * 0.4) DESC "
                        +
                        "LIMIT ?";

        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> results = new ArrayList<>();
                while (rs.next()) {
                    Book b = new Book();
                    b.setIsbn(rs.getString("isbn"));
                    b.setTitle(rs.getString("title"));
                    b.setAuthor(rs.getString("author"));
                    b.setCategory(rs.getString("category"));
                    b.setAvailabilityStatus(rs.getString("availability_status"));
                    b.setDescription(rs.getString("description"));
                    b.setStock(rs.getInt("stock"));
                    
                    System.out.println("Loaded featured book: " + b.getTitle() + ", Stock: " + b.getStock());

                    if (hasPriceColumn) {
                        try {
                            double price = rs.getDouble("price");
                            System.out.println("  Raw price from DB: " + price);
                            if (rs.wasNull() || price <= 0) {
                                b.setPrice(1.00);
                                System.out.println("  Price was null or <= 0, set to 1.00");
                            } else {
                                b.setPrice(price);
                                System.out.println("  Price set to: " + price);
                            }
                        } catch (Exception e) {
                            b.setPrice(1.00);
                            System.out.println("  Error reading price: " + e.getMessage());
                        }
                    } else {
                        b.setPrice(1.00);
                        System.out.println("  No price column, set to 1.00");
                    }

                    
                    try {
                        b.setAvgRating(rs.getDouble("avg_rating"));
                    } catch (Exception ignored) {
                    }
                    try {
                        b.setRatingsCount(rs.getInt("ratings_count"));
                    } catch (Exception ignored) {
                    }
                    try {
                        b.setPurchasesCount(rs.getInt("purchases_count"));
                    } catch (Exception ignored) {
                    }
                    try {
                        b.setLoansCount(rs.getInt("loans_count"));
                    } catch (Exception ignored) {
                    }
                    results.add(b);
                }
                return results;
            }
        }
    }

    @Override
    public List<String> findAllCategories() throws Exception {
        String sql = "SELECT DISTINCT category FROM books WHERE category IS NOT NULL ORDER BY category";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<String> cats = new ArrayList<>();
                while (rs.next()) {
                    String cat = rs.getString(1);
                    if (cat != null && !cat.isBlank())
                        cats.add(cat);
                }
                return cats;
            }
        }
    }

    @Override
    public List<Book> findByCategory(String category, int offset, int limit) throws Exception {
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "SELECT isbn, title, author, category, availability_status, stock, price FROM books WHERE category = ? LIMIT ? OFFSET ?"
                : "SELECT isbn, title, author, category, availability_status, stock FROM books WHERE category = ? LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> results = new ArrayList<>();
                while (rs.next()) {
                    Book b = new Book();
                    b.setIsbn(rs.getString("isbn"));
                    b.setTitle(rs.getString("title"));
                    b.setAuthor(rs.getString("author"));
                    b.setCategory(rs.getString("category"));
                    b.setAvailabilityStatus(rs.getString("availability_status"));
                    b.setStock(rs.getInt("stock"));
                    if (hasPriceColumn) {
                        try {
                            double price = rs.getDouble("price");
                            if (rs.wasNull() || price <= 0) {
                                b.setPrice(1.00);
                            } else {
                                b.setPrice(price);
                            }
                        } catch (Exception e) {
                            b.setPrice(1.00);
                        }
                    } else {
                        b.setPrice(1.00);
                    }
                    results.add(b);
                }
                return results;
            }
        }
    }

    @Override
    public List<Book> findAll() throws Exception {
        boolean hasPriceColumn = checkColumnExists("books", "price");
        String sql = hasPriceColumn
                ? "SELECT isbn, title, author, category, availability_status, stock, price FROM books ORDER BY created_at DESC"
                : "SELECT isbn, title, author, category, availability_status, stock FROM books ORDER BY created_at DESC";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> results = new ArrayList<>();
                while (rs.next()) {
                    Book b = new Book();
                    b.setIsbn(rs.getString("isbn"));
                    b.setTitle(rs.getString("title"));
                    b.setAuthor(rs.getString("author"));
                    b.setCategory(rs.getString("category"));
                    b.setAvailabilityStatus(rs.getString("availability_status"));
                    b.setStock(rs.getInt("stock"));
                    if (hasPriceColumn) {
                        try {
                            double price = rs.getDouble("price");
                            if (rs.wasNull() || price <= 0) {
                                b.setPrice(1.00);
                            } else {
                                b.setPrice(price);
                            }
                        } catch (Exception e) {
                            b.setPrice(1.00);
                        }
                    } else {
                        b.setPrice(1.00);
                    }
                    results.add(b);
                }
                return results;
            }
        }
    }

     
    public void updateDefaultPrices() {
        try (Connection c = ds.getConnection()) {
            boolean hasPriceColumn = checkColumnExists("books", "price");
            if (!hasPriceColumn) {
                System.out.println("Price column does not exist, attempting to add it...");
                try (Statement stmt = c.createStatement()) {
                    String dbProduct = c.getMetaData().getDatabaseProductName().toLowerCase();
                    try {
                        if (dbProduct.contains("mysql")) {
                            stmt.execute("ALTER TABLE books ADD COLUMN price DECIMAL(10,2) DEFAULT 1.00");
                        } else {
                            
                            stmt.execute("ALTER TABLE books ADD COLUMN price DECIMAL(10,2) DEFAULT 1.00");
                        }
                        System.out.println("Price column added successfully");
                        hasPriceColumn = true;
                    } catch (Exception addEx) {
                        
                        if (checkColumnExists("books", "price")) {
                            System.out.println("Price column exists after all");
                            hasPriceColumn = true;
                        } else {
                            System.err.println("Failed to add price column: " + addEx.getMessage());
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to add price column: " + e.getMessage());
                    return;
                }
            }

            try (Statement stmt = c.createStatement()) {
                
                int updated1 = stmt.executeUpdate(
                        "UPDATE books SET price = 12.00 WHERE (category LIKE '%Fiction%' OR category LIKE '%Novel%') AND (price = 1.00 OR price IS NULL)");
                int updated2 = stmt.executeUpdate(
                        "UPDATE books SET price = 34.99 WHERE (category LIKE '%Science%' OR category LIKE '%Programming%' OR category LIKE '%Computer%') AND (price = 1.00 OR price IS NULL)");
                int updated3 = stmt.executeUpdate(
                        "UPDATE books SET price = 52.97 WHERE (category LIKE '%History%' OR category LIKE '%Biography%') AND (price = 1.00 OR price IS NULL)");
                int updated4 = stmt.executeUpdate(
                        "UPDATE books SET price = 353.67 WHERE (category LIKE '%Business%' OR category LIKE '%Self-Help%') AND (price = 1.00 OR price IS NULL)");
                int updated5 = stmt.executeUpdate(
                        "UPDATE books SET price = 36.63 WHERE (category LIKE '%Art%' OR category LIKE '%Philosophy%') AND (price = 1.00 OR price IS NULL)");
                int updated6 = stmt.executeUpdate(
                        "UPDATE books SET price = 76.49 WHERE (category LIKE '%Children%' OR category LIKE '%Young Adult%') AND (price = 1.00 OR price IS NULL)");
                int updated7 = stmt.executeUpdate(
                        "UPDATE books SET price = 26.99 WHERE (category LIKE '%Poetry%' OR category LIKE '%Drama%') AND (price = 1.00 OR price IS NULL)");

                
                int updated8 = stmt.executeUpdate(
                        "UPDATE books SET price = 353.67 WHERE (category LIKE '%Psychology%' OR category LIKE '%Personal Development%' OR category LIKE '%Self-Help%') AND (price = 1.00 OR price IS NULL)");

                
                int updated9 = stmt.executeUpdate("UPDATE books SET price = 25.00 WHERE price = 1.00 OR price IS NULL");

                int totalUpdated = updated1 + updated2 + updated3 + updated4 + updated5 + updated6 + updated7 + updated8
                        + updated9;
                System.out.println("Updated prices for " + totalUpdated + " books");
            } catch (Exception e) {
                System.err.println("Failed to update default prices: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Failed to connect for price update: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
