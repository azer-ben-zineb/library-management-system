package com.libraryplus.service;

import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.model.Book;

import java.util.List;
import java.util.stream.Collectors;

public class ChatbotService {

    private final BookDao bookDao;

    public ChatbotService() {
        this.bookDao = new BookDaoJdbc();
    }

    
    public ChatbotService(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    public String processQuery(String query) {
        if (query == null || query.isBlank()) {
            return "Hello! How can I help you today?";
        }

        String lowerQuery = query.toLowerCase().trim();

        if (lowerQuery.contains("hello") || lowerQuery.contains("hi")) {
            return "Hello! I am the LibraryPlus AI. Ask me about books!";
        }

        if (lowerQuery.contains("help")) {
            return "I can help you find books. Try asking 'Do you have Harry Potter?' or 'Show me fantasy books'.";
        }

        if (lowerQuery.contains("do you have") || lowerQuery.contains("find book")
                || lowerQuery.contains("search for")) {
            String searchTerm = extractSearchTerm(query);
            if (searchTerm.isEmpty()) {
                return "What book are you looking for?";
            }
            return searchBooks(searchTerm);
        }

        if (lowerQuery.contains("show me") && lowerQuery.contains("books")) {
            
            String category = extractCategory(lowerQuery);
            if (category != null) {
                return findBooksByCategory(category);
            }
            return "I can show you books by category. Try 'Show me fantasy books'.";
        }

        return "I'm sorry, I didn't understand that. Try asking about a specific book or category.";
    }

    private String extractSearchTerm(String query) {
        
        String lower = query.toLowerCase();
        String[] prefixes = { "do you have", "find book", "search for", "looking for" };
        for (String prefix : prefixes) {
            if (lower.contains(prefix)) {
                int index = lower.indexOf(prefix) + prefix.length();
                return query.substring(index).trim().replaceAll("[?.]", "");
            }
        }
        return "";
    }

    private String extractCategory(String lowerQuery) {
        
        if (lowerQuery.contains("fantasy"))
            return "Fantasy";
        if (lowerQuery.contains("fiction"))
            return "Fiction";
        if (lowerQuery.contains("science"))
            return "Science";
        if (lowerQuery.contains("history"))
            return "History";
        if (lowerQuery.contains("romance"))
            return "Romance";
        return null;
    }

    private String searchBooks(String term) {
        try {
            List<Book> books = bookDao.search(term);
            if (books.isEmpty()) {
                return "I couldn't find any books matching '" + term + "'.";
            }
            String response = "Here is what I found:\n";
            response += books.stream()
                    .limit(3)
                    .map(b -> "- " + b.getTitle() + " by " + b.getAuthor() + " (" + b.getAvailabilityStatus() + ")")
                    .collect(Collectors.joining("\n"));
            if (books.size() > 3) {
                response += "\n...and " + (books.size() - 3) + " more.";
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, I encountered an error searching for books.";
        }
    }

    private String findBooksByCategory(String category) {
        try {
            List<Book> books = bookDao.findByCategory(category, 0, 5);
            if (books.isEmpty()) {
                return "I couldn't find any books in the " + category + " category.";
            }
            return "Here are some " + category + " books:\n" +
                    books.stream()
                            .map(b -> "- " + b.getTitle() + " by " + b.getAuthor())
                            .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, I encountered an error searching for books.";
        }
    }
}
