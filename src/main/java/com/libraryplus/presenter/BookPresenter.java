package com.libraryplus.presenter;

import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

 
public class BookPresenter {
    private static final Logger logger = LoggerFactory.getLogger(BookPresenter.class);
    private final BookDao bookDao;

    public BookPresenter() {
        this.bookDao = new BookDaoJdbc();
    }

    public BookPresenter(BookDao bookDao) {
        this.bookDao = bookDao;
    }

     
    public List<Book> searchBooks(String query, int offset, int limit) {
        try {
            return bookDao.search(query == null ? "" : query, offset, limit);
        } catch (Exception ex) {
            logger.error("Error searching books", ex);
            return List.of();
        }
    }

     
    public Optional<Book> getBookDetails(String isbn) {
        try {
            return bookDao.findByIsbn(isbn);
        } catch (Exception ex) {
            logger.error("Error fetching book details for isbn={}", isbn, ex);
            return Optional.empty();
        }
    }

     
    public String addBook(Book book) {
        try {
            if (book.getIsbn() == null || book.getIsbn().isBlank()) {
                return "ISBN is required.";
            }
            if (book.getTitle() == null || book.getTitle().isBlank()) {
                return "Title is required.";
            }

            
            Optional<Book> existing = bookDao.findByIsbn(book.getIsbn());
            if (existing.isPresent()) {
                return "A book with this ISBN already exists.";
            }

            int result = bookDao.createBook(book);
            if (result > 0) {
                logger.info("Book added: isbn={}, title={}", book.getIsbn(), book.getTitle());
                return "Book added successfully.";
            } else {
                return "Failed to add book.";
            }

        } catch (Exception ex) {
            logger.error("Error adding book", ex);
            return "An error occurred: " + ex.getMessage();
        }
    }

     
    public String updateBook(Book book) {
        try {
            if (book.getIsbn() == null || book.getIsbn().isBlank()) {
                return "ISBN is required.";
            }

            int result = bookDao.updateBook(book);
            if (result > 0) {
                logger.info("Book updated: isbn={}", book.getIsbn());
                return "Book updated successfully.";
            } else {
                return "Book not found or update failed.";
            }

        } catch (Exception ex) {
            logger.error("Error updating book", ex);
            return "An error occurred: " + ex.getMessage();
        }
    }

     
    public String deleteBook(String isbn) {
        try {
            if (isbn == null || isbn.isBlank()) {
                return "ISBN is required.";
            }

            int result = bookDao.deleteByIsbn(isbn);
            if (result > 0) {
                logger.info("Book deleted: isbn={}", isbn);
                return "Book deleted successfully.";
            } else {
                return "Book not found.";
            }

        } catch (Exception ex) {
            logger.error("Error deleting book", ex);
            return "An error occurred: " + ex.getMessage();
        }
    }

     
    public List<Book> getFeaturedBooks(int limit) {
        try {
            return bookDao.findFeatured(limit);
        } catch (Exception ex) {
            logger.error("Error fetching featured books", ex);
            return List.of();
        }
    }
}

