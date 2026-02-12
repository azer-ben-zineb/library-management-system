package com.libraryplus.dao;

import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.model.Book;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BookDaoJdbcTest {

    @Test
    public void testCreateAndFindBook() throws Exception {
        BookDao dao = new BookDaoJdbc();
        String isbn = "TEST-ISBN-12345";

        
        try { dao.deleteByIsbn(isbn); } catch (Exception ignored) {}

        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle("Test Driven Development");
        b.setAuthor("Kent Beck");
        b.setCategory("Software");
        b.setAvailabilityStatus("AVAILABLE");
        b.setCoverImagePath(null);
        b.setDescription("A classic book about TDD.");

        int inserted = dao.createBook(b);
        assertTrue(inserted > 0, "createBook should insert a row");

        Optional<Book> found = dao.findByIsbn(isbn);
        assertTrue(found.isPresent(), "Book should be found by ISBN");
        Book fetched = found.get();
        assertEquals(b.getTitle(), fetched.getTitle());
        assertEquals(b.getAuthor(), fetched.getAuthor());

        
        dao.deleteByIsbn(isbn);
    }
}

