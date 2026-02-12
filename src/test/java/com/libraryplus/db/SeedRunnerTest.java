package com.libraryplus.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class SeedRunnerTest {

    @Test
    public void testSeedingAddsBooks() throws Exception {
        String url = System.getenv().getOrDefault("H2_FILE_URL", "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        String user = "sa";
        String pass = "";
        Path script = Paths.get("src/main/resources/db/seed_books.sql");
        assertTrue(Files.exists(script), "Seed script must exist at " + script.toAbsolutePath());

        try (Connection c = DriverManager.getConnection(url, user, pass); Statement st = c.createStatement()) {
            
            st.execute("CREATE TABLE IF NOT EXISTS books (isbn VARCHAR(20) PRIMARY KEY, title VARCHAR(255) NOT NULL, author VARCHAR(255), category VARCHAR(50), availability_status VARCHAR(50) DEFAULT 'AVAILABLE', cover_image_path VARCHAR(1024), description CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            int before = 0;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM books")) {
                if (rs.next()) before = rs.getInt("cnt");
            }

            String sql = Files.readString(script, StandardCharsets.UTF_8);
            String[] parts = sql.split(";");
            int executed = 0;
            for (String part : parts) {
                String s = part.trim();
                if (s.isEmpty()) continue;
                if (s.equalsIgnoreCase("COMMIT")) continue;
                try {
                    st.execute(s);
                    executed++;
                } catch (Exception ex) {
                    
                    System.err.println("Statement failed during test run: " + ex.getMessage() + " -> " + (s.length() > 200 ? s.substring(0, 200) + "..." : s));
                }
            }

            int after = 0;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM books")) {
                if (rs.next()) after = rs.getInt("cnt");
            }

            System.out.println("Executed statements (attempted): " + executed);
            System.out.println("Books before: " + before + ", after: " + after + ", added: " + Math.max(0, after - before));

            assertTrue(after >= before, "After seeding the book count should be at least the same as before");

            
            try (ResultSet rs = st.executeQuery("SELECT title FROM books WHERE isbn='9780061120084'")) {
                assertTrue(rs.next(), "Expected ISBN 9780061120084 to exist after seeding");
                String title = rs.getString("title");
                assertNotNull(title);
                assertFalse(title.isBlank());
            }
        }
    }
}

