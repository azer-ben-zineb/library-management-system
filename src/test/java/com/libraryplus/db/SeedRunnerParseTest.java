package com.libraryplus.db;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SeedRunnerParseTest {

    @Test
    public void parseSimpleMergeStatement() {
        String sql = "MERGE INTO books(isbn, title, author, description, category) VALUES ('9780061120084', 'To Kill a Mockingbird', 'Harper Lee', 'A lawyer defends a Black man.', 'Classic');";
        List<Map<String, String>> rows = SeedRunner.parseMergeStatements(sql);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map<String, String> r = rows.get(0);
        assertEquals("9780061120084", r.get("isbn"));
        assertEquals("To Kill a Mockingbird", r.get("title"));
        assertEquals("Harper Lee", r.get("author"));
        assertEquals("A lawyer defends a Black man.", r.get("description"));
        assertEquals("Classic", r.get("category"));
    }

    @Test
    public void parseMergeWithEscapedQuotesAndNull() {
        
        String sql = "MERGE INTO books(isbn, title, author, description, category) VALUES ('9780141439518', 'Pride and Prejudice', 'Jane Austen', NULL, 'Classic / Romance');\n" +
                "MERGE INTO books(isbn, title, author, description, category) VALUES ('9780307352156', 'Quiet: The Power of Introverts in a World That can''t Stop Talking', 'Susan Cain', 'Examines introversion', 'Psychology');";
        List<Map<String, String>> rows = SeedRunner.parseMergeStatements(sql);
        assertNotNull(rows);
        assertEquals(2, rows.size());
        Map<String, String> r1 = rows.get(0);
        assertEquals("9780141439518", r1.get("isbn"));
        assertEquals("Pride and Prejudice", r1.get("title"));
        assertNull(r1.get("description"));

        Map<String, String> r2 = rows.get(1);
        assertEquals("9780307352156", r2.get("isbn"));
        assertEquals("Quiet: The Power of Introverts in a World That can't Stop Talking", r2.get("title"));
        assertEquals("Examines introversion", r2.get("description"));
    }

    @Test
    public void parseValuesWithCommasInQuotedStrings() {
        String sql = "MERGE INTO books(isbn, title, author, description) VALUES ('9781234567890', 'Title with, comma', 'Author, Jr.', 'Desc, contains, commas');";
        List<Map<String, String>> rows = SeedRunner.parseMergeStatements(sql);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map<String, String> r = rows.get(0);
        assertEquals("9781234567890", r.get("isbn"));
        assertEquals("Title with, comma", r.get("title"));
        assertEquals("Author, Jr.", r.get("author"));
        assertEquals("Desc, contains, commas", r.get("description"));
    }

    @Test
    public void parseMultipleMergeGroupsAndWhitespace() {
        String sql = "  MERGE INTO   books( isbn , title )   VALUES  ('1','A');  \n\nMERGE INTO books(isbn,title)VALUES('2','B');";
        List<Map<String, String>> rows = SeedRunner.parseMergeStatements(sql);
        assertNotNull(rows);
        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).get("isbn"));
        assertEquals("A", rows.get(0).get("title"));
        assertEquals("2", rows.get(1).get("isbn"));
        assertEquals("B", rows.get(1).get("title"));
    }
}
