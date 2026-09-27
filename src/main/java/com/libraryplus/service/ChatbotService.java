package com.libraryplus.service;

import com.libraryplus.app.Session;
import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.LoanDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.LoanDaoJdbc;
import com.libraryplus.model.Book;
import com.libraryplus.model.Client;
import com.libraryplus.model.Loan;
import com.libraryplus.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private final BookDao bookDao;
    private final LoanDao loanDao;
    private final ClientDao clientDao;

    public ChatbotService() {
        this.bookDao = new BookDaoJdbc();
        this.loanDao = new LoanDaoJdbc();
        this.clientDao = new ClientDaoJdbc();
    }

    public ChatbotService(BookDao bookDao, LoanDao loanDao, ClientDao clientDao) {
        this.bookDao = bookDao;
        this.loanDao = loanDao;
        this.clientDao = clientDao;
    }

    public String processQuery(String query) {
        if (query == null || query.isBlank()) {
            return "👋 Hello! I am your LibraryPlus AI Assistant. You can ask me to search for books, check availability, view your loans, check your wallet balance, or explain library rules.";
        }

        String lower = query.toLowerCase().trim();

        // 1. Greetings
        if (lower.matches("^(hi|hello|hey|greetings|bonjour|salut).*") || lower.equals("hi") || lower.equals("hello")) {
            User u = Session.getCurrentUser();
            String name = u != null ? u.getFullName() : "there";
            return "👋 Hello, " + name + "! How can I assist you with your reading today?\n" +
                    "Try asking:\n" +
                    "• 'Find book Clean Code'\n" +
                    "• 'Show me Science books'\n" +
                    "• 'What is my card balance?'\n" +
                    "• 'Show my active loans'\n" +
                    "• 'How do subscriptions work?'";
        }

        // 2. Help
        if (lower.contains("help") || lower.contains("commands") || lower.contains("what can you do")) {
            return "📚 Here is what I can do for you:\n\n" +
                    "🔍 Search Books: 'Find [title/author]' or 'Do you have [book]?'\n" +
                    "🏷️ By Genre: 'Show me [fiction/science/history/programming] books'\n" +
                    "📦 Availability: 'Is [book] in stock?'\n" +
                    "💳 Balance: 'What is my balance?' or 'My card'\n" +
                    "📖 Your Loans: 'Show my loans' or 'What books do I have?'\n" +
                    "⭐ Membership: 'Subscription info' or 'Discounts'\n" +
                    "⏳ Rules: 'What are the loan rules and fines?'";
        }

        // 3. User Balance
        if (lower.contains("balance") || lower.contains("money") || lower.contains("wallet") || lower.contains("card balance")) {
            User u = Session.getCurrentUser();
            if (u == null) {
                return "🔒 You are currently not logged in. Please sign in to check your card balance.";
            }
            return String.format("💳 Your current card balance is: **%.2f DT**.\n" +
                    "You can use this balance to borrow, purchase books (with 10%% subscriber discount), or renew memberships.", u.getCardBalance());
        }

        // 4. User Active Loans
        if (lower.contains("my loan") || lower.contains("my books") || lower.contains("borrowed") || lower.contains("active loan")) {
            User u = Session.getCurrentUser();
            if (u == null) {
                return "🔒 Please log in to check your active loans.";
            }
            try {
                Optional<Client> cOpt = clientDao.findByUserId(u.getId());
                if (cOpt.isEmpty()) {
                    return "You do not have any registered client record yet.";
                }
                List<Loan> active = loanDao.findActiveByClientId(cOpt.get().getId());
                if (active.isEmpty()) {
                    return "📖 You have no active loans right now. Browse our catalog to borrow a great book!";
                }
                StringBuilder sb = new StringBuilder("📖 You currently have " + active.size() + " active loan(s):\n");
                for (Loan l : active) {
                    Optional<Book> bOpt = bookDao.findByIsbn(l.getBookIsbn());
                    String title = bOpt.isPresent() ? bOpt.get().getTitle() : l.getBookIsbn();
                    sb.append("• ").append(title)
                            .append(" (Due: ").append(l.getExpectedReturnDate().toLocalDate()).append(")");
                    if (l.getFineAmount() > 0) {
                        sb.append(" [Late Fine: ").append(l.getFineAmount()).append(" DT]");
                    }
                    sb.append("\n");
                }
                return sb.toString();
            } catch (Exception e) {
                logger.error("Error retrieving user loans for chatbot", e);
                return "Sorry, I couldn't look up your loans right now.";
            }
        }

        // 5. Subscription & Membership
        if (lower.contains("subscri") || lower.contains("membership") || lower.contains("discount") || lower.contains("premium")) {
            return "⭐ **LibraryPlus Premium Subscription**:\n" +
                    "• Cost: **20.00 DT** for 3 months.\n" +
                    "• Benefits: Automatic **10% discount** on all book purchases!\n" +
                    "• Priority queue on all book waitlists.\n" +
                    "• Click the 'Subscribe (20DT)' button on the dashboard to activate instantly!";
        }

        // 6. Library Rules, Loan Period & Fines
        if (lower.contains("rule") || lower.contains("fine") || lower.contains("period") || lower.contains("overdue") || lower.contains("duration")) {
            return "📋 **LibraryPlus Lending Rules**:\n" +
                    "• Standard loan duration is **14 days** (2 weeks).\n" +
                    "• Books must be returned before the due date to avoid fines.\n" +
                    "• Overdue fine rate: **1.00 DT per day** overdue.\n" +
                    "• You can return books any time from the dashboard.";
        }

        // 7. Category Inquiries
        String detectedCategory = extractCategory(lower);
        if (detectedCategory != null) {
            return findBooksByCategory(detectedCategory);
        }

        // 8. Book search by Title or Author
        String term = extractSearchTerm(query);
        if (!term.isBlank()) {
            return searchBooks(term);
        }

        // 9. Recommendations / Popular
        if (lower.contains("recommend") || lower.contains("popular") || lower.contains("best") || lower.contains("suggest")) {
            try {
                List<Book> featured = bookDao.findFeatured(4);
                if (!featured.isEmpty()) {
                    StringBuilder sb = new StringBuilder("✨ Here are our top recommended books today:\n\n");
                    for (Book b : featured) {
                        sb.append("📚 **").append(b.getTitle()).append("** by ").append(b.getAuthor() != null ? b.getAuthor() : "Unknown")
                                .append("\n   Category: ").append(b.getCategory())
                                .append(" | Price: ").append(b.getPrice() > 0 ? b.getPrice() : 15.0).append(" DT")
                                .append(" | Copies: ").append(b.getStock()).append("\n\n");
                    }
                    return sb.toString().trim();
                }
            } catch (Exception ignored) {}
        }

        return "🤔 I couldn't find a direct match for that. Try asking:\n" +
                "• 'Do you have Clean Code?'\n" +
                "• 'Show me Science books'\n" +
                "• 'What is my balance?'\n" +
                "• 'What are the overdue fines?'";
    }

    private String extractSearchTerm(String query) {
        String lower = query.toLowerCase();
        String[] prefixes = { "do you have", "find book", "search for", "looking for", "find", "is", "where is", "info on" };
        for (String prefix : prefixes) {
            if (lower.contains(prefix)) {
                int index = lower.indexOf(prefix) + prefix.length();
                String raw = query.substring(index).trim().replaceAll("[?.!]", "");
                if (raw.endsWith(" available") || raw.endsWith(" in stock")) {
                    raw = raw.replace(" available", "").replace(" in stock", "").trim();
                }
                return raw;
            }
        }
        return "";
    }

    private String extractCategory(String lowerQuery) {
        if (lowerQuery.contains("fiction") || lowerQuery.contains("novel")) return "Fiction";
        if (lowerQuery.contains("science") || lowerQuery.contains("physics") || lowerQuery.contains("biology")) return "Science";
        if (lowerQuery.contains("program") || lowerQuery.contains("code") || lowerQuery.contains("software") || lowerQuery.contains("computer")) return "Programming";
        if (lowerQuery.contains("history") || lowerQuery.contains("biography")) return "History";
        if (lowerQuery.contains("business") || lowerQuery.contains("finance") || lowerQuery.contains("invest")) return "Business";
        if (lowerQuery.contains("philosophy") || lowerQuery.contains("art")) return "Philosophy";
        if (lowerQuery.contains("poetry") || lowerQuery.contains("drama")) return "Poetry";
        if (lowerQuery.contains("children") || lowerQuery.contains("young adult")) return "Children";
        return null;
    }

    private String findBooksByCategory(String category) {
        try {
            List<Book> books = bookDao.findByCategory(category, 0, 5);
            if (books.isEmpty()) {
                return "I couldn't find any books in the '" + category + "' category right now.";
            }
            StringBuilder sb = new StringBuilder("📚 Here are books in **" + category + "**:\n\n");
            for (Book b : books) {
                sb.append("• **").append(b.getTitle()).append("** by ").append(b.getAuthor() != null ? b.getAuthor() : "Unknown")
                        .append(" — ").append(b.getPrice() > 0 ? b.getPrice() : 15.0).append(" DT")
                        .append(" (").append(b.getStock() > 0 ? b.getStock() + " available" : "Out of stock").append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Category search failed", e);
            return "Failed to load books for category: " + category;
        }
    }

    private String searchBooks(String term) {
        try {
            List<Book> books = bookDao.search(term, 0, 5);
            if (books.isEmpty()) {
                return "🔍 I couldn't find any books matching '" + term + "'. Try searching by partial title, author, or category.";
            }
            StringBuilder sb = new StringBuilder("🔍 Found matching books for '" + term + "':\n\n");
            for (Book b : books) {
                sb.append("• **").append(b.getTitle()).append("** by ").append(b.getAuthor() != null ? b.getAuthor() : "Unknown")
                        .append("\n  Category: ").append(b.getCategory() != null ? b.getCategory() : "General")
                        .append(" | Price: ").append(b.getPrice() > 0 ? b.getPrice() : 15.0).append(" DT")
                        .append(" | Status: ").append(b.getStock() > 0 ? "✅ " + b.getStock() + " copies available" : "❌ Out of stock (join waitlist)")
                        .append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            logger.error("Search books query failed", e);
            return "Error searching for book: " + term;
        }
    }
}
