package com.libraryplus.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryBooksTest {

    @Test
    public void printLatestBooks() throws Exception {
        String url = System.getenv().getOrDefault("H2_FILE_URL", "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (Statement st = c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT isbn, title FROM books ORDER BY created_at DESC LIMIT 20")) {
                    System.out.println("Latest seeded books (up to 20):");
                    int i = 0;
                    while (rs.next()) {
                        i++;
                        System.out.println(i + ". " + rs.getString("isbn") + " | " + rs.getString("title"));
                    }
                    if (i == 0) System.out.println("(no books found)");
                }
            }
        }
    }
}

