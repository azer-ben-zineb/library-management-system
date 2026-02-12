package com.libraryplus.ui;

import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.model.Book;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddBookController {
    @FXML
    private TextField isbnField;
    @FXML
    private TextField titleField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField categoryField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField stockField;
    @FXML
    private TextField priceField;
    @FXML
    private Button saveButton;
    @FXML
    private Label messageLabel;

    private final BookDao bookDao = new BookDaoJdbc();

    
    private Book existingBook = null;

    private static final Logger logger = LoggerFactory.getLogger(AddBookController.class);

     
    public void setBook(Book book) {
        this.existingBook = book;
        if (book != null) {
            isbnField.setText(book.getIsbn());
            isbnField.setDisable(true); 
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            categoryField.setText(book.getCategory());
            descriptionArea.setText(book.getDescription());
            stockField.setText(String.valueOf(book.getStock()));
            priceField.setText(String.format("%.2f", book.getPrice()));
            saveButton.setText("Update");
        }
    }

    @FXML
    protected void onSave(ActionEvent event) {
        String isbn = isbnField.getText();
        String title = titleField.getText();
        String author = authorField.getText();
        String category = categoryField.getText();
        String desc = descriptionArea.getText();

        if (isbn == null || isbn.isBlank() || title == null || title.isBlank()) {
            messageLabel.setText("ISBN and Title are required.");
            return;
        }

        
        String normalizedIsbn = isbn.replaceAll("[^0-9Xx]", "");
        if (!isValidIsbn(normalizedIsbn)) {
            messageLabel.setText("Please enter a valid ISBN-10 or ISBN-13.");
            return;
        }

        try {
            if (existingBook == null) {
                
                if (bookDao.findByIsbn(normalizedIsbn).isPresent()) {
                    messageLabel.setText("A book with this ISBN already exists.");
                    return;
                }
            }

            Book b = (existingBook != null) ? existingBook : new Book();
            b.setIsbn(normalizedIsbn);
            b.setTitle(title.trim());
            b.setAuthor(author == null ? null : author.trim());
            b.setCategory(category == null ? null : category.trim());
            b.setDescription(desc == null ? null : desc.trim());
            b.setAvailabilityStatus("AVAILABLE");

            
            int stock = 1; 
            try {
                if (stockField.getText() != null && !stockField.getText().isBlank()) {
                    stock = Integer.parseInt(stockField.getText());
                    if (stock < 0)
                        stock = 1;
                }
            } catch (NumberFormatException e) {
                messageLabel.setText("Stock must be a valid number.");
                return;
            }
            b.setStock(stock);

            
            double price = 1.00; 
            try {
                if (priceField.getText() != null && !priceField.getText().isBlank()) {
                    price = Double.parseDouble(priceField.getText());
                    if (price < 0)
                        price = 1.00;
                }
            } catch (NumberFormatException e) {
                messageLabel.setText("Price must be a valid number.");
                return;
            }
            b.setPrice(price);

            int affected;
            if (existingBook == null) {
                affected = bookDao.createBook(b);
            } else {
                affected = bookDao.updateBook(b);
            }

            if (affected > 0) {
                
                Stage s = (Stage) saveButton.getScene().getWindow();
                Toast.show(s, existingBook == null ? "Book added." : "Book updated.");
                s.close();
            } else {
                messageLabel.setText("Failed to save book.");
            }
        } catch (Exception e) {
            logger.error("Failed to save book", e);
            messageLabel.setText("Error: " + e.getMessage());
            Stage owner = (Stage) (saveButton.getScene() != null ? saveButton.getScene().getWindow() : null);
            if (owner != null)
                Toast.show(owner, "Error saving book: " + e.getMessage());
        }
    }

    @FXML
    protected void onCancel(ActionEvent event) {
        Stage s = (Stage) messageLabel.getScene().getWindow();
        s.close();
    }

    
    private boolean isValidIsbn(String isbn) {
        if (isbn == null)
            return false;
        if (isbn.length() == 10)
            return isValidIsbn10(isbn);
        if (isbn.length() == 13)
            return isValidIsbn13(isbn);
        return false;
    }

    private boolean isValidIsbn10(String s) {
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                char c = s.charAt(i);
                if (!Character.isDigit(c))
                    return false;
                sum += (10 - i) * (c - '0');
            }
            char last = s.charAt(9);
            int check = (last == 'X' || last == 'x') ? 10 : (Character.isDigit(last) ? (last - '0') : -1);
            if (check < 0)
                return false;
            sum += check;
            return (sum % 11) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidIsbn13(String s) {
        try {
            int sum = 0;
            for (int i = 0; i < 13; i++) {
                char c = s.charAt(i);
                if (!Character.isDigit(c))
                    return false;
                int digit = c - '0';
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            return (sum % 10) == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
