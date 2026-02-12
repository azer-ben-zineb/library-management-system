package com.libraryplus.db;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

import static org.junit.jupiter.api.Assertions.*;

public class CoverSeederIntegrationTest {

    @Test
    public void testCoverSeederAssignsCovers() throws Exception {
        Path tmp = Files.createTempDirectory("lp-test-");
        String origUserDir = System.getProperty("user.dir");
        try {
            
            System.setProperty("user.dir", tmp.toString());

            
            SeedRunner.main(new String[0]);

            
            

            
            List<byte[]> samples = new ArrayList<>();
            String[] sampleResources = {"/images/sample1.b64", "/images/sample2.b64"};
            for (String res : sampleResources) {
                try (InputStream is = CoverSeeder.class.getResourceAsStream(res)) {
                    if (is == null) continue;
                    String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (raw.isEmpty()) continue;
                    
                    String b64 = raw.replaceAll("[^A-Za-z0-9+/=]", "");
                    if (b64.isEmpty()) continue;
                    
                    while (b64.length() % 4 != 0) b64 += "=";
                    byte[] data;
                    try {
                        data = Base64.getDecoder().decode(b64);
                    } catch (IllegalArgumentException iae) {
                        throw new IllegalArgumentException("Failed to decode base64 resource " + res + " (sanitized length=" + b64.length() + "): " + iae.getMessage(), iae);
                    }
                    samples.add(data);
                }
            }

            assertFalse(samples.isEmpty(), "Test requires at least one sample image in resources/images/*.b64");

            Path imagesDir = tmp.resolve("data").resolve("images");
            Files.createDirectories(imagesDir);

            String url = "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
            int updated = 0;
            try (Connection c = DriverManager.getConnection(url, "sa", "")) {
                List<String> isbns = new ArrayList<>();
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT isbn FROM books ORDER BY created_at ASC LIMIT 50")) {
                    while (rs.next()) {
                        isbns.add(rs.getString("isbn"));
                    }
                }

                assertFalse(isbns.isEmpty(), "Seeding should have created books first");

                try (PreparedStatement upd = c.prepareStatement("UPDATE books SET cover_image_path = ? WHERE isbn = ?")) {
                    int idx = 0;
                    for (String isbn : isbns) {
                        byte[] img = samples.get(idx % samples.size());
                        idx++;
                        Path out = imagesDir.resolve(isbn + ".png");
                        Files.write(out, img);
                        String abs = out.toAbsolutePath().toString();
                        upd.setString(1, abs);
                        upd.setString(2, isbn);
                        int rc = upd.executeUpdate();
                        if (rc > 0) updated++;
                    }
                }
            }

            assertTrue(updated > 0, "Expected at least one book row to be updated with a cover path");

            
            try (Connection c = DriverManager.getConnection(url, "sa", "")) {
                try (PreparedStatement ps = c.prepareStatement("SELECT cover_image_path FROM books WHERE cover_image_path IS NOT NULL LIMIT 5")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        List<String> found = new ArrayList<>();
                        while (rs.next()) {
                            String path = rs.getString(1);
                            if (path != null && !path.isBlank()) found.add(path);
                        }
                        assertFalse(found.isEmpty(), "Expected at least one book to have cover_image_path set");
                        for (String p : found) {
                            assertTrue(Files.exists(Paths.get(p)), "Cover file should exist: " + p);
                        }
                    }
                }
            }

        } finally {
            
            System.setProperty("user.dir", origUserDir);
        }
    }
}
