package com.libraryplus.presenter;

import com.libraryplus.app.Session;
import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.LoanDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.dao.jdbc.LoanDaoJdbc;
import com.libraryplus.model.Book;
import com.libraryplus.model.Loan;
import com.libraryplus.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

 
public class ChatbotPresenter {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotPresenter.class);
    private final BookDao bookDao;
    private final LoanDao loanDao;

    public ChatbotPresenter() {
        this.bookDao = new BookDaoJdbc();
        this.loanDao = new LoanDaoJdbc();
    }

    public ChatbotPresenter(BookDao bookDao, LoanDao loanDao) {
        this.bookDao = bookDao;
        this.loanDao = loanDao;
    }

     
    public String answer(String question) {
        if (question == null || question.isBlank()) {
            return "Please ask me a question. I can help with book availability, your loans, and more.";
        }

        String lowerQuestion = question.toLowerCase();

        
        if (lowerQuestion.contains("availability") || lowerQuestion.contains("available")) {
            return handleAvailabilityQuery(question);
        }

        
        if (lowerQuestion.contains("loan") && (lowerQuestion.contains("my") || lowerQuestion.contains("current") || lowerQuestion.contains("active"))) {
            return handleMyLoansQuery();
        }

        
        if (lowerQuestion.contains("balance") || lowerQuestion.contains("money") || lowerQuestion.contains("card")) {
            return handleCardBalanceQuery();
        }

        
        if (lowerQuestion.contains("borrowed") || lowerQuestion.contains("popular")) {
            return handleMostBorrowedQuery();
        }

        
        if (lowerQuestion.contains("purchased") || lowerQuestion.contains("best seller") || lowerQuestion.contains("bestseller")) {
            return handleMostPurchasedQuery();
        }

        
        if (lowerQuestion.contains("help") || lowerQuestion.contains("can you")) {
            return getHelpText();
        }

        return "I didn't quite understand that. Try asking about:\n" +
               "- Book availability (e.g., 'Is Clean Code available?')\n" +
               "- Your current loans\n" +
               "- Your card balance\n" +
               "- Most borrowed or purchased books\n" +
               "Type 'help' for more options.";
    }

     
    private String handleAvailabilityQuery(String question) {
        try {
            
            String title = extractTitle(question);

            if (title == null || title.isBlank()) {
                return "Please specify a book title. For example: 'Is Clean Code available?'";
            }

            Optional<Book> bookOpt = bookDao.findByIsbn(title);
            if (bookOpt.isEmpty()) {
                
                List<Book> results = bookDao.search(title, 0, 1);
                if (results.isEmpty()) {
                    return "I couldn't find a book titled '" + title + "'. Please try another title or ISBN.";
                }
                bookOpt = Optional.of(results.get(0));
            }

            Book book = bookOpt.get();
            return String.format("📚 %s by %s is currently **%s**.",
                    book.getTitle(), book.getAuthor() == null ? "Unknown" : book.getAuthor(),
                    book.getAvailabilityStatus() == null ? "Unknown" : book.getAvailabilityStatus());

        } catch (Exception ex) {
            logger.error("Error handling availability query", ex);
            return "Sorry, I encountered an error checking book availability.";
        }
    }

     
    private String handleMyLoansQuery() {
        try {
            User user = Session.getCurrentUser();
            if (user == null) {
                return "You must be logged in to check your loans.";
            }

            
            
            List<Loan> loans = loanDao.findActiveByClientId(user.getId());

            if (loans.isEmpty()) {
                return "✅ You have no active loans.";
            }

            StringBuilder sb = new StringBuilder("📖 Your Active Loans:\n\n");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Loan loan : loans) {
                sb.append(String.format("- **%s** (ISBN: %s)\n", loan.getBookIsbn(), loan.getBookIsbn()));
                sb.append(String.format("  Borrowed: %s\n", loan.getBorrowDate()));
                sb.append(String.format("  Due: %s\n", loan.getExpectedReturnDate()));
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception ex) {
            logger.error("Error handling my loans query", ex);
            return "Sorry, I couldn't retrieve your loans.";
        }
    }

     
    private String handleCardBalanceQuery() {
        try {
            User user = Session.getCurrentUser();
            if (user == null) {
                return "You must be logged in to check your balance.";
            }

            double balance = user.getCardBalance();
            return String.format("💳 Your card balance is **%.2f DT**.", balance);

        } catch (Exception ex) {
            logger.error("Error handling card balance query", ex);
            return "Sorry, I couldn't retrieve your card balance.";
        }
    }

     
    private String handleMostBorrowedQuery() {
        try {
            List<Book> topBorrowed = bookDao.search("", 0, 5);  
            
            

            if (topBorrowed.isEmpty()) {
                return "No borrowing data available yet.";
            }

            StringBuilder sb = new StringBuilder("🔥 Top Borrowed Books:\n\n");
            int rank = 1;
            for (Book book : topBorrowed) {
                sb.append(String.format("%d. **%s** by %s\n", rank++, book.getTitle(), book.getAuthor() == null ? "Unknown" : book.getAuthor()));
            }

            return sb.toString();

        } catch (Exception ex) {
            logger.error("Error handling most borrowed query", ex);
            return "Sorry, I couldn't retrieve the most borrowed books.";
        }
    }

     
    private String handleMostPurchasedQuery() {
        try {
            
            return "📊 Most purchased books:\n(Feature coming soon - check back later!)";

        } catch (Exception ex) {
            logger.error("Error handling most purchased query", ex);
            return "Sorry, I couldn't retrieve the most purchased books.";
        }
    }

     
    private String extractTitle(String question) {
        
        Pattern p1 = Pattern.compile("\\bof\\s+([^?]+)", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(question);
        if (m1.find()) {
            return m1.group(1).trim();
        }

        
        Pattern p2 = Pattern.compile("\\bis\\s+([^?]+)\\s+available", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(question);
        if (m2.find()) {
            return m2.group(1).trim();
        }

        
        Pattern p3 = Pattern.compile("^([^?]+)\\?$");
        Matcher m3 = p3.matcher(question.trim());
        if (m3.find()) {
            String potential = m3.group(1).trim().toLowerCase();
            if (!potential.contains("available") && !potential.contains("loan") && !potential.contains("balance")) {
                return m3.group(1).trim();
            }
        }

        return null;
    }

     
    private String getHelpText() {
        return """
                📚 **LibraryPlus Chatbot Help**
                
                I can help you with these questions:
                
                **Book Availability:**
                - "Is Clean Code available?"
                - "What is the availability of [book title]?"
                
                **Your Loans:**
                - "What are my current loans?"
                - "Show my active loans"
                
                **Card Balance:**
                - "What is my card balance?"
                - "How much money do I have?"
                
                **Recommendations:**
                - "What are the most borrowed books?"
                - "Show me the best sellers"
                
                Just type your question in the chatbot and I'll help!
                """;
    }
}

