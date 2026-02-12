package com.libraryplus.db;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SeedRunner {
    public static void main(String[] args) {
        String mode = (args != null && args.length > 0) ? args[0].toLowerCase() : "upsert"; 
        System.out.println("Seed mode: " + mode);
        StringBuilder logBuf = new StringBuilder();
        java.util.function.Consumer<String> log = s -> {
            System.out.println(s);
            try { logBuf.append(s).append(System.lineSeparator()); } catch (Exception ignore) {}
        };
        
        java.util.function.Consumer<Void> writeLogFile = v -> {
            try {
                String ts = String.valueOf(System.currentTimeMillis());
                java.nio.file.Path tmp = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "seedrunner-" + ts + ".log");
                java.nio.file.Files.writeString(tmp, logBuf.toString());
                System.out.println("SeedRunner log written to: " + tmp.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Failed to write seed log file: " + e.getMessage());
            }
        };
        String url = System.getenv().getOrDefault("H2_FILE_URL", "jdbc:h2:file:./data/libraryplus;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        String user = "sa";
        String pass = "";
        Path script = Paths.get("src/main/resources/db/seed_books.sql");
        System.out.println("Using H2 URL: " + url);
        if (!Files.exists(script)) {
            System.err.println("Seed script not found: " + script.toAbsolutePath());
            System.exit(2);
        }
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            
            try (PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS books (isbn VARCHAR(20) PRIMARY KEY, title VARCHAR(255) NOT NULL, author VARCHAR(255), category VARCHAR(50), availability_status VARCHAR(50) DEFAULT 'AVAILABLE', cover_image_path VARCHAR(1024), description CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")) {
                ps.execute();
            }

            String raw = Files.readString(script, StandardCharsets.UTF_8);
            List<Map<String, String>> rows = parseMergeStatements(raw);
            int created = 0, updated = 0, errors = 0;
            List<String> errorMessages = new ArrayList<>();

            String updateSql = "UPDATE books SET title = ?, author = ?, description = ?, category = ? WHERE isbn = ?";
            String insertSql = "INSERT INTO books (isbn, title, author, category, availability_status, cover_image_path, description) VALUES (?, ?, ?, ?, ?, ?, ?)";

            for (Map<String, String> row : rows) {
                String isbn = row.getOrDefault("isbn", null);
                String title = row.getOrDefault("title", null);
                String author = row.getOrDefault("author", null);
                String description = row.getOrDefault("description", null);
                String category = row.getOrDefault("category", null);
                try {
                    if ("replace".equals(mode)) {
                        
                        try (PreparedStatement del = c.prepareStatement("DELETE FROM books WHERE isbn = ?")) {
                            del.setString(1, isbn);
                            del.executeUpdate();
                        }
                        try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                            ins.setString(1, isbn);
                            ins.setString(2, title);
                            ins.setString(3, author);
                            ins.setString(4, category);
                            ins.setString(5, "AVAILABLE");
                            ins.setString(6, null);
                            ins.setString(7, description);
                            ins.executeUpdate();
                            created++;
                            log.accept("Replaced/Created: " + isbn + " -> " + title);
                        }
                    } else if ("insert".equals(mode)) {
                        
                        try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                            ins.setString(1, isbn);
                            ins.setString(2, title);
                            ins.setString(3, author);
                            ins.setString(4, category);
                            ins.setString(5, "AVAILABLE");
                            ins.setString(6, null);
                            ins.setString(7, description);
                            ins.executeUpdate();
                            created++;
                            log.accept("Inserted: " + isbn + " -> " + title);
                        } catch (Exception insEx) {
                            
                            log.accept("Skipped existing (insert-only): " + isbn);
                        }
                    } else { 
                        try (PreparedStatement upd = c.prepareStatement(updateSql)) {
                            upd.setString(1, title);
                            upd.setString(2, author);
                            upd.setString(3, description);
                            upd.setString(4, category);
                            upd.setString(5, isbn);
                            int rc = upd.executeUpdate();
                            if (rc > 0) {
                                updated++;
                                log.accept("Updated: " + isbn + " -> " + title);
                                continue;
                            }
                        }
                        try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                            ins.setString(1, isbn);
                            ins.setString(2, title);
                            ins.setString(3, author);
                            ins.setString(4, category);
                            ins.setString(5, "AVAILABLE");
                            ins.setString(6, null);
                            ins.setString(7, description);
                            ins.executeUpdate();
                            created++;
                            log.accept("Created: " + isbn + " -> " + title);
                        }
                    }
                } catch (Exception ex) {
                    errors++;
                    String msg = "Failed row isbn=" + isbn + " : " + ex.getMessage();
                    errorMessages.add(msg);
                    System.err.println(msg);
                    log.accept("ERROR: " + msg);
                }
            }

            int total = rows.size();
            log.accept("\nSeed summary: total=" + total + ", created=" + created + ", updated=" + updated + ", errors=" + errors);
            if (!errorMessages.isEmpty()) {
                log.accept("Errors:");
                for (String m : errorMessages) log.accept(" - " + m);
            }

            
            try (ResultSet rs = c.createStatement().executeQuery("SELECT isbn, title FROM books ORDER BY created_at DESC LIMIT 10")) {
                log.accept("\nLatest books:");
                while (rs.next()) {
                    log.accept(rs.getString("isbn") + " | " + rs.getString("title"));
                }
            }
            
            writeLogFile.accept(null);

        } catch (Exception e) {
            System.err.println("Failed to run seed: " + e.getMessage());
            e.printStackTrace();
            try { log.accept("FAILED: " + e.getMessage()); writeLogFile.accept(null); } catch (Exception ignore) {}
            System.exit(1);
        }
    }

    
    public static List<Map<String, String>> parseMergeStatements(String sql) {
        List<Map<String, String>> out = new ArrayList<>();
        if (sql == null || sql.isBlank()) return out;
        String s = sql;
        int idx = 0;
        while (true) {
            int mi = indexOfIgnoreCase(s, "merge into", idx);
            if (mi < 0) break;
            
            int booksPos = indexOfIgnoreCase(s, "books", mi);
            if (booksPos < 0) {
                idx = mi + 1; continue;
            }
            
            int colOpen = s.indexOf('(', booksPos);
            if (colOpen < 0) { idx = booksPos + 1; continue; }
            int colClose = findMatchingParen(s, colOpen);
            if (colClose < 0) { idx = colOpen + 1; continue; }
            String colsPart = s.substring(colOpen + 1, colClose).trim();
            
            int valuesIdx = indexOfIgnoreCase(s, "values", colClose);
            if (valuesIdx < 0) { idx = colClose + 1; continue; }
            int valOpen = s.indexOf('(', valuesIdx);
            if (valOpen < 0) { idx = valuesIdx + 1; continue; }
            int valClose = findMatchingParen(s, valOpen);
            if (valClose < 0) { idx = valOpen + 1; continue; }
            String valsPart = s.substring(valOpen + 1, valClose).trim();

            String[] cols = colsPart.split("\\s*,\\s*");
            List<String> vals = splitSqlValues(valsPart);
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < Math.min(cols.length, vals.size()); i++) {
                String col = cols[i].trim();
                String val = unquoteSqlString(vals.get(i));
                map.put(col, val);
            }
            out.add(map);
            idx = valClose + 1;
        }
        return out;
    }

    private static int indexOfIgnoreCase(String s, String needle, int from) {
        String low = s.toLowerCase();
        String nlow = needle.toLowerCase();
        return low.indexOf(nlow, from);
    }

    private static int findMatchingParen(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    
    private static List<String> splitSqlValues(String vals) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < vals.length(); i++) {
            char c = vals.charAt(i);
            if (c == '\'') {
                
                if (i + 1 < vals.length() && vals.charAt(i + 1) == '\'') {
                    cur.append('\'');
                    i++;
                } else {
                    inQuote = !inQuote;
                    
                }
            } else if (c == ',' && !inQuote) {
                res.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) res.add(cur.toString().trim());
        return res;
    }

    private static String unquoteSqlString(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.equalsIgnoreCase("null")) return null;
        if (s.startsWith("'") && s.endsWith("'")) {
            s = s.substring(1, s.length() - 1);
            
            s = s.replace("''", "'");
            return s;
        }
        return s;
    }
}
