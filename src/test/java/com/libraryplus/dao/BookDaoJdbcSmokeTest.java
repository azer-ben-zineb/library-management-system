package com.libraryplus.dao;

import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.model.Book;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BookDaoJdbcSmokeTest {

    @Test
    public void findAllReturnsList() throws Exception {
        BookDao dao = new BookDaoJdbc();
        List<Book> books = dao.findAll();
        assertNotNull(books, "findAll should return a non-null list (may be empty)");
    }

}

