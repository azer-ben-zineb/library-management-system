package com.libraryplus.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DataSourceConfig {
    private static volatile HikariDataSource ds;

    public static DataSource getDataSource() {
        if (ds == null) {
            synchronized (DataSourceConfig.class) {
                if (ds == null) {
                    ds = createDataSource();
                }
            }
        }
        return ds;
    }

    public static String getActiveJdbcUrl() {
        if (ds != null) {
            try {
                return ds.getJdbcUrl();
            } catch (Exception ignored) {
            }
        }

        String envUrl = System.getenv().get("DB_URL");
        if (envUrl != null && !envUrl.isBlank())
            return envUrl;
        return System.getenv().getOrDefault("H2_FILE_URL",
                "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/libraryplus");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "");

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        try {
            HikariDataSource hikari = new HikariDataSource(config);
            try (Connection c = hikari.getConnection()) {
                return hikari;
            }
        } catch (Exception e) {
            System.err.println("Could not connect to configured DB: " + e.getMessage());
            System.err.println("Falling back to in-memory H2 database for UI/testing.");
        }

        HikariConfig h2config = new HikariConfig();

        String h2FileUrl = System.getenv().getOrDefault("H2_FILE_URL",
                "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        h2config.setJdbcUrl(h2FileUrl);
        h2config.setUsername("sa");
        h2config.setPassword("");
        HikariDataSource h2ds = new HikariDataSource(h2config);

        try (Connection c = h2ds.getConnection()) {
            String schema = readResourceAsString("/db/schema.sql");
            if (schema != null && !schema.isBlank()) {
                String sanitized = sanitizeSqlForH2(schema);
                try (Statement stmt = c.createStatement()) {
                    for (String part : sanitized.split(";")) {
                        String sql = part.trim();
                        if (sql.isEmpty())
                            continue;
                        try {
                            stmt.execute(sql);
                        } catch (SQLException ex) {
                            System.err.println("Schema statement failed: " + ex.getMessage() + " -> " + sql);
                        }
                    }

                    try {
                        stmt.execute("MERGE INTO roles(name) KEY(name) VALUES('ADMIN')");
                        stmt.execute("MERGE INTO roles(name) KEY(name) VALUES('CLIENT')");
                    } catch (SQLException ex) {
                        System.err.println("Failed to seed roles: " + ex.getMessage());
                    }

                    try {
                        stmt.executeQuery("SELECT 1 FROM users LIMIT 1");
                    } catch (SQLException ex) {
                        System.err.println("Users table missing, creating a minimal users table for H2 fallback.");
                        try {
                            stmt.execute(
                                    "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY AUTO_INCREMENT, email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, salt VARCHAR(255), role_id INT NOT NULL, full_name VARCHAR(255), phone VARCHAR(50), date_of_birth DATE, card_number VARCHAR(512), card_balance DECIMAL(10,2) DEFAULT 0.00, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                        } catch (SQLException ex2) {
                            System.err.println("Failed to create minimal users table: " + ex2.getMessage());
                        }
                    }

                    // Ensure card_number column is wide enough for encrypted data
                    try {
                        stmt.execute("ALTER TABLE users ALTER COLUMN card_number VARCHAR(512)");
                    } catch (SQLException exAlter) {
                        // Column may already be correct size or ALTER not supported
                    }

                    try {
                        try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM users")) {
                            if (rs.next() && rs.getInt("cnt") == 0) {
                                String adminHash = com.libraryplus.util.PasswordUtils.hashPassword("admin123");
                                String clientHash = com.libraryplus.util.PasswordUtils.hashPassword("client123");
                                String adminCard = com.libraryplus.util.EncryptionUtils.encrypt("4000123456789010");
                                String clientCard = com.libraryplus.util.EncryptionUtils.encrypt("4000123456789020");

                                System.out.println("[H2 Seed] Seeding default admin and client users...");

                                try (java.sql.PreparedStatement ps = c.prepareStatement(
                                        "INSERT INTO users (email, password_hash, role_id, full_name, phone, date_of_birth, card_number, card_balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                                    // Seed Admin
                                    ps.setString(1, "admin@libraryplus.com");
                                    ps.setString(2, adminHash);
                                    ps.setInt(3, 1);
                                    ps.setString(4, "Administrator");
                                    ps.setString(5, "+1234567890");
                                    ps.setDate(6, java.sql.Date.valueOf("1990-01-01"));
                                    ps.setString(7, adminCard);
                                    ps.setDouble(8, 150.00);
                                    ps.executeUpdate();
                                    System.out.println("[H2 Seed] Admin user seeded: admin@libraryplus.com");

                                    // Seed Client
                                    ps.setString(1, "client@libraryplus.com");
                                    ps.setString(2, clientHash);
                                    ps.setInt(3, 2);
                                    ps.setString(4, "John Reader");
                                    ps.setString(5, "+1987654321");
                                    ps.setDate(6, java.sql.Date.valueOf("1995-05-15"));
                                    ps.setString(7, clientCard);
                                    ps.setDouble(8, 50.00);
                                    ps.executeUpdate();
                                    System.out.println("[H2 Seed] Client user seeded: client@libraryplus.com");

                                    int clientUserId = 2;
                                    try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                                        if (keys != null && keys.next()) clientUserId = keys.getInt(1);
                                    }

                                    try (java.sql.PreparedStatement cps = c.prepareStatement(
                                            "INSERT INTO clients (user_id, phone, first_name, last_name, date_of_birth, membership_type) VALUES (?, ?, ?, ?, ?, ?)")) {
                                        cps.setInt(1, clientUserId);
                                        cps.setString(2, "+1987654321");
                                        cps.setString(3, "John");
                                        cps.setString(4, "Reader");
                                        cps.setDate(5, java.sql.Date.valueOf("1995-05-15"));
                                        cps.setString(6, "STANDARD");
                                        cps.executeUpdate();
                                    }
                                }

                                // Verify seeded users can authenticate
                                try (java.sql.ResultSet verify = stmt.executeQuery(
                                        "SELECT email, password_hash FROM users WHERE email IN ('admin@libraryplus.com', 'client@libraryplus.com')")) {
                                    int verified = 0;
                                    while (verify.next()) {
                                        String email = verify.getString("email");
                                        String hash = verify.getString("password_hash");
                                        String testPwd = email.startsWith("admin") ? "admin123" : "client123";
                                        boolean ok = com.libraryplus.util.PasswordUtils.verifyPassword(testPwd, hash);
                                        System.out.println("[H2 Seed] Verify " + email + " -> " + (ok ? "OK" : "FAILED"));
                                        if (ok) verified++;
                                    }
                                    System.out.println("[H2 Seed] Verified " + verified + "/2 seeded users.");
                                }
                            }
                        }
                    } catch (Exception exSeed) {
                        System.err.println("Could not seed default users: " + exSeed.getMessage());
                        exSeed.printStackTrace();
                    }

                    try {
                        stmt.executeQuery("SELECT 1 FROM books LIMIT 1");
                    } catch (SQLException ex) {
                        System.err.println("Books table missing, creating a minimal books table for H2 fallback.");
                        try {
                            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                                    "isbn VARCHAR(20) PRIMARY KEY, " +
                                    "title VARCHAR(255) NOT NULL, " +
                                    "author VARCHAR(255), " +
                                    "category VARCHAR(50), " +
                                    "stock INT DEFAULT 1, " +
                                    "price DECIMAL(10,2) DEFAULT 1.00, " +
                                    "availability_status VARCHAR(50) DEFAULT 'AVAILABLE', " +
                                    "cover_image_path VARCHAR(1024), " +
                                    "description CLOB, " +
                                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                        } catch (SQLException ex2) {
                            System.err.println("Failed to create minimal books table: " + ex2.getMessage());
                        }
                    }

                    try {
                        stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "client_id INT NOT NULL, " +
                                "amount DECIMAL(10,2) NOT NULL, " +
                                "reason VARCHAR(255), " +
                                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                "resulting_balance DECIMAL(10,2))");
                    } catch (SQLException exTrans) {
                        // ignore if exists
                    }

                    try {
                        try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM books")) {
                            int cnt = 0;
                            if (rs.next())
                                cnt = rs.getInt("cnt");
                            if (cnt == 0) {

                                java.nio.file.Path imagesDir = java.nio.file.Paths.get(System.getProperty("user.dir"),
                                        "data", "images");
                                try {
                                    java.nio.file.Files.createDirectories(imagesDir);
                                } catch (java.io.IOException ioex) {
                                    System.err.println("Failed to create images directory: " + ioex.getMessage());
                                    imagesDir = null;
                                }

                                final java.nio.file.Path finalImagesDir = imagesDir;

                                class ImageWriter {
                                    void write(String resPath, String outName) throws Exception {
                                        if (finalImagesDir == null)
                                            return;
                                        try (java.io.InputStream ris = DataSourceConfig.class
                                                .getResourceAsStream(resPath)) {
                                            if (ris == null)
                                                return;
                                            String b64 = new String(ris.readAllBytes(),
                                                    java.nio.charset.StandardCharsets.UTF_8).trim();
                                            byte[] bytes = java.util.Base64.getDecoder().decode(b64);
                                            java.nio.file.Files.write(finalImagesDir.resolve(outName), bytes);
                                        }
                                    }
                                }
                                ImageWriter writer = new ImageWriter();
                                if (finalImagesDir != null) {
                                    try {
                                        writer.write("/images/sample1.b64", "sample1.png");
                                        writer.write("/images/sample2.b64", "sample2.png");
                                    } catch (Exception ex) {
                                        System.err.println("Failed to write sample images: " + ex.getMessage());
                                    }
                                }

                                try {
                                    UniversalSeedRunner.main(new String[0]);
                                } catch (Throwable seedEx) {
                                    System.err.println("UniversalSeedRunner failed, fallback to basic seed: " + seedEx.getMessage());
                                }
                            }
                        }
                    } catch (SQLException ex3) {
                        System.err.println("Failed to seed sample books: " + ex3.getMessage());
                    }

                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to initialize H2 schema: " + ex.getMessage());
        }

        return h2ds;
    }

    private static String readResourceAsString(String path) {
        try (InputStream in = DataSourceConfig.class.getResourceAsStream(path)) {
            if (in == null)
                return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitizeSqlForH2(String sql) {
        StringBuilder out = new StringBuilder();
        String[] lines = sql.split("\r?\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty())
                continue;
            if (line.startsWith("--"))
                continue;
            if (line.matches("(?i)^CREATE\\s+DATABASE.*"))
                continue;
            if (line.matches("(?i)^USE\\s+.*"))
                continue;
            line = line.replaceAll("(?i)ENGINE=[^;\\s]+", "");
            line = line.replaceAll("(?i)DEFAULT\\s+CHARACTER\\s+SET\\s+[^;\\n]+", "");
            line = line.replaceAll("(?i)COLLATE\\s+[^;\\n]+", "");
            line = line.replace("`", "");
            if (line.matches("(?i)^INDEX\\s+.*") || line.matches("(?i)^KEY\\s+.*")
                    || line.matches("(?i)^UNIQUE\\s*\\(.*") || line.matches("(?i)^UNIQUE\\s+KEY\\s+.*")) {
                continue;
            }
            if (line.matches("(?i)^INSERT\\s+IGNORE\\s+.*")) {
                continue;
            }
            if (line.matches("(?i)^SET\\s+@.*") || line.matches("(?i)^PREPARE\\s+.*")
                    || line.matches("(?i)^EXECUTE\\s+.*") || line.matches("(?i)^DEALLOCATE\\s+.*")) {
                continue;
            }
            line = line.replaceAll("(?i)ENGINE=[^;\\s]+;?", "");
            out.append(line).append('\n');
        }

        String result = out.toString();
        result = result.replaceAll(",\\s*\\n\\)", "\n)");
        return result;
    }

    private DataSourceConfig() {
    }
}
