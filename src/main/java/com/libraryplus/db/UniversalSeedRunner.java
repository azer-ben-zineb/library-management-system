package com.libraryplus.db;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class UniversalSeedRunner {
    public static void main(String[] args) {
        Path script = Paths.get("src/main/resources/db/seed_books.sql");
        if (!Files.exists(script)) {
            System.err.println("Seed script not found: " + script.toAbsolutePath());
            System.exit(2);
        }
        try {
            DataSource ds = DataSourceConfig.getDataSource();
            List<Map<String, String>> rows = parseMergeStatements(new String(Files.readAllBytes(script), StandardCharsets.UTF_8));
            int created = 0, updated = 0, errors = 0;
            try (Connection c = ds.getConnection()) {
                String updateSql = "UPDATE books SET title = ?, author = ?, description = ?, category = ? WHERE isbn = ?";
                String insertSql = "INSERT INTO books (isbn, title, author, category, availability_status, cover_image_path, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
                for (Map<String, String> row : rows) {
                    String isbn = row.get("isbn");
                    String title = row.get("title");
                    String author = row.get("author");
                    String description = row.get("description");
                    String category = row.get("category");
                    try (PreparedStatement upd = c.prepareStatement(updateSql)) {
                        upd.setString(1, title);
                        upd.setString(2, author);
                        upd.setString(3, description);
                        upd.setString(4, category);
                        upd.setString(5, isbn);
                        int rc = upd.executeUpdate();
                        if (rc > 0) {
                            updated++;
                            System.out.println("Updated: " + isbn + " -> " + title);
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
                        System.out.println("Created: " + isbn + " -> " + title);
                    }
                }
                try (ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM books")) {
                    int after = 0;
                    if (rs.next()) after = rs.getInt("cnt");
                    System.out.println("Books in DB after seeding: " + after);
                }
            }
            System.out.println("Summary: created=" + created + ", updated=" + updated + ", errors=" + errors);
        } catch (Exception e) {
            System.err.println("Failed to run universal seed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    
    private static List<Map<String, String>> parseMergeStatements(String sql) {
        List<Map<String, String>> out = new ArrayList<>();
        String s = sql;
        int idx = 0;
        while (true) {
            int mi = indexOfIgnoreCase(s, "MERGE INTO books(", idx);
            if (mi < 0) break;
            int colsStart = mi + "MERGE INTO books(".length();
            int colsEnd = s.indexOf(')', colsStart);
            if (colsEnd < 0) break;
            String colsPart = s.substring(colsStart, colsEnd).trim();
            String[] cols = colsPart.split("\\s*,\\s*");
            int valuesIdx = indexOfIgnoreCase(s, "VALUES", colsEnd);
            if (valuesIdx < 0) break;
            int parenStart = s.indexOf('(', valuesIdx);
            if (parenStart < 0) break;
            int parenEnd = findMatchingParen(s, parenStart);
            if (parenEnd < 0) break;
            String valsPart = s.substring(parenStart + 1, parenEnd).trim();
            List<String> vals = splitSqlValues(valsPart);
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < Math.min(cols.length, vals.size()); i++) {
                String col = cols[i].trim();
                String val = unquoteSqlString(vals.get(i));
                map.put(col, val);
            }
            out.add(map);
            idx = parenEnd + 1;
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
        if (s.startsWith("'") && s.endsWith("'")) {
            s = s.substring(1, s.length() - 1);
            s = s.replace("''", "'");
            return s;
        }
        if (s.equalsIgnoreCase("null")) return null;
        return s;
    }
}

