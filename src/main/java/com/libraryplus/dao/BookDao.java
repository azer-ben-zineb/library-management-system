package com.libraryplus.dao;

import com.libraryplus.model.Book;

import java.util.List;
import java.util.Optional;

public interface BookDao {
    Optional<Book> findByIsbn(String isbn) throws Exception;
    List<Book> search(String query) throws Exception;
    List<Book> search(String query, int offset, int limit) throws Exception; 
    List<Book> findAll() throws Exception;
    int createBook(Book book) throws Exception;
    int updateBook(Book book) throws Exception;
    int deleteByIsbn(String isbn) throws Exception;
    List<Book> findFeatured(int limit) throws Exception; 
    List<String> findAllCategories() throws Exception;
    List<Book> findByCategory(String category, int offset, int limit) throws Exception;
}
