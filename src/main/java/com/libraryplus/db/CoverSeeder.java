package com.libraryplus.db;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CoverSeeder {
    public static void main(String[] args) {
        String url = System.getenv().getOrDefault("H2_FILE_URL", "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        String user = "sa";
        String pass = "";
        Path imagesDir = Paths.get(System.getProperty("user.dir"), "data", "images");
        try {
            Files.createDirectories(imagesDir);
        } catch (Exception e) {
            System.err.println("Failed to create images dir: " + e.getMessage());
            System.exit(2);
        }

        
        List<byte[]> samples = new ArrayList<>();
        String[] sampleResources = {"/images/sample1.b64", "/images/sample2.b64"};
        for (String res : sampleResources) {
            try (InputStream is = CoverSeeder.class.getResourceAsStream(res)) {
                if (is == null) {
                    System.err.println("Resource not found: " + res);
                    continue;
                }
                String b64 = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                byte[] data = Base64.getDecoder().decode(b64);
                samples.add(data);
            } catch (Exception ex) {
                System.err.println("Failed to read resource " + res + ": " + ex.getMessage());
            }
        }
        if (samples.isEmpty()) {
            System.err.println("No sample images available. Aborting.");
            System.exit(3);
        }

        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS books (isbn VARCHAR(20) PRIMARY KEY, title VARCHAR(255) NOT NULL, author VARCHAR(255), category VARCHAR(50), availability_status VARCHAR(50) DEFAULT 'AVAILABLE', cover_image_path VARCHAR(1024), description CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }

            List<String> isbns = new ArrayList<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT isbn FROM books ORDER BY created_at ASC")) {
                while (rs.next()) {
                    isbns.add(rs.getString("isbn"));
                }
            }

            if (isbns.isEmpty()) {
                System.out.println("No books found in DB to assign covers to.");
                return;
            }

            int idx = 0;
            int updated = 0;
            try (PreparedStatement upd = c.prepareStatement("UPDATE books SET cover_image_path = ? WHERE isbn = ?")) {
                for (String isbn : isbns) {
                    byte[] img = samples.get(idx % samples.size());
                    idx++;
                    Path out = imagesDir.resolve(isbn + ".png");
                    try {
                        Files.write(out, img);
                        String abs = out.toAbsolutePath().toString();
                        upd.setString(1, abs);
                        upd.setString(2, isbn);
                        int rc = upd.executeUpdate();
                        if (rc > 0) updated++;
                        System.out.println((rc > 0 ? "Updated" : "NoRow") + ": " + isbn + " -> " + abs);
                    } catch (Exception ex) {
                        System.err.println("Failed write/update for " + isbn + ": " + ex.getMessage());
                    }
                }
            }

            System.out.println("\nCover assignment complete. Files written: " + isbns.size() + ", DB rows updated: " + updated);

            
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT isbn, title, cover_image_path FROM books ORDER BY created_at DESC LIMIT 10")) {
                System.out.println("\nSample books with cover paths:");
                while (rs.next()) {
                    System.out.println(rs.getString("isbn") + " | " + rs.getString("title") + " | " + rs.getString("cover_image_path"));
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to assign covers: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

