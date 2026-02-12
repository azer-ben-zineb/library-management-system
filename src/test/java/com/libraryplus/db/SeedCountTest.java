package com.libraryplus.db;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeedCountTest {

    @Test
    public void testSeededRowCount() throws Exception {
        Path script = Paths.get("src/main/resources/db/seed_books.sql");
        
        java.nio.file.Files.exists(script);

        
        SeedRunner.main(new String[0]);

        String url = System.getenv().getOrDefault("H2_FILE_URL", "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM books")) {
                if (rs.next()) {
                    int cnt = rs.getInt("cnt");
                    System.out.println("Seeded books count: " + cnt);
                    
                    assertTrue(cnt >= 60, "Expected at least 60 seeded books, found=" + cnt);
                } else {
                    throw new IllegalStateException("COUNT(*) query did not return a row");
                }
            }
        }
    }
}

