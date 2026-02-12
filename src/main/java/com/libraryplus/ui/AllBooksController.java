package com.libraryplus.ui;

import com.libraryplus.dao.BookDao;
import com.libraryplus.model.Book;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.List;

public class AllBooksController {
    @FXML
    private TilePane coversPane;
    @FXML
    private TextField filterField;
    @FXML
    private Button refreshButton;
    @FXML
    private Label bookCountLabel;
    @FXML
    private Label stockCountLabel;
    @FXML
    private Button chatbotButton;

    private BookDao bookDao;

    public void setBookDao(BookDao dao) {
        this.bookDao = dao;
        
        if (dao instanceof com.libraryplus.dao.jdbc.BookDaoJdbc) {
            ((com.libraryplus.dao.jdbc.BookDaoJdbc) dao).updateDefaultPrices();
        }
        
        try {
            loadAllBooks();
        } catch (Exception ignore) {
        }
    }

    @FXML
    public void initialize() {
        try {
            if (refreshButton != null)
                refreshButton.setOnAction(ev -> loadAllBooks());
            if (filterField != null)
                filterField.setOnAction(ev -> applyFilter());
            loadAllBooks();
        } catch (Exception ignore) {
        }
    }

    private void applyFilter() {
        String q = filterField == null ? "" : filterField.getText();
        loadAllBooksFiltered(q == null ? "" : q);
    }

    private void loadAllBooks() {
        loadAllBooksFiltered("");
    }

    private void loadAllBooksFiltered(String q) {
        try {
            if (bookDao == null) {
                System.err.println("BookDao is null, cannot load books");
                return;
            }

            if (coversPane == null) {
                System.err.println("CoversPane is null, cannot display books");
                return;
            }

            
            if (bookDao instanceof com.libraryplus.dao.jdbc.BookDaoJdbc) {
                ((com.libraryplus.dao.jdbc.BookDaoJdbc) bookDao).updateDefaultPrices();
            }

            List<Book> books = bookDao.search(q, 0, 10000);
            System.out.println("Loaded " + books.size() + " books");

            
            int totalBooks = books.size();
            int totalStock = books.stream().mapToInt(Book::getStock).sum();
            if (bookCountLabel != null) {
                bookCountLabel.setText("Total Books: " + totalBooks);
            }
            if (stockCountLabel != null) {
                stockCountLabel.setText("Total Stock: " + totalStock);
            }

            coversPane.getChildren().clear();

            if (books.isEmpty()) {
                
                Label noBooks = new Label("No books found in database");
                noBooks.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 16px; -fx-padding: 20;");
                coversPane.getChildren().add(noBooks);
                return;
            }

            for (Book b : books) {
                VBox bookCard = createBookCard(b);
                coversPane.getChildren().add(bookCard);
            }
        } catch (Exception e) {
            System.err.println("Error loading books: " + e.getMessage());
            e.printStackTrace();
            try {
                coversPane.getChildren().clear();
                Label error = new Label("Error loading books: " + e.getMessage());
                error.setStyle("-fx-text-fill: #f38ba8; -fx-padding: 20;");
                coversPane.getChildren().add(error);
            } catch (Exception ignore) {
            }
        }
    }

    private VBox createBookCard(Book b) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-padding: 10; -fx-background-color: #1e1e2e; -fx-background-radius: 8; -fx-cursor: hand;");
        card.setPrefWidth(140);

        
        ImageView iv = new ImageView();
        Image img = loadImageForBook(b);
        if (img != null) {
            iv.setImage(img);
        } else {
            
            Label placeholder = new Label(
                    b.getTitle() != null && !b.getTitle().isEmpty() ? b.getTitle().substring(0, 1).toUpperCase() : "?");
            placeholder.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: white; " +
                    "-fx-font-size: 48px; -fx-font-weight: bold; " +
                    "-fx-alignment: center; -fx-pref-width: 100; -fx-pref-height: 150;");
            card.getChildren().add(placeholder);
        }

        if (img != null) {
            iv.setFitWidth(100);
            iv.setFitHeight(150);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            card.getChildren().add(iv);
        }

        
        Label titleLabel = new Label(b.getTitle());
        titleLabel.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold; -fx-font-size: 11px; -fx-wrap-text: true;");
        titleLabel.setMaxWidth(120);
        titleLabel.setWrapText(true);

        Label authorLabel = new Label(b.getAuthor() != null ? b.getAuthor() : "Unknown");
        authorLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 10px;");

        Label stockLabel = new Label("📦 " + b.getStock() + " in stock");
        stockLabel.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 10px;");

        card.getChildren().addAll(titleLabel, authorLabel, stockLabel);

        
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-padding: 10; -fx-background-color: #313244; -fx-background-radius: 8; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-padding: 10; -fx-background-color: #1e1e2e; -fx-background-radius: 8; -fx-cursor: hand;"));

        
        card.setOnMouseClicked(ev -> showBookDetail(b));

        return card;
    }

    private Image loadImageForBook(Book b) {
        if (b == null)
            return null;
        try {
            String isbn = b.getIsbn();
            if (isbn != null && !isbn.isBlank()) {
                java.nio.file.Path coversDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "data",
                        "covers");
                java.nio.file.Path p = coversDir.resolve(isbn + ".png");
                java.io.File f = p.toFile();
                if (f.exists()) {
                    return new Image(f.toURI().toString(), 100, 150, true, true);
                }
            }
            
            String resName = "/images/" + (b.getIsbn() == null ? "" : b.getIsbn()) + ".png";
            java.net.URL res = getClass().getResource(resName);
            if (res != null) {
                return new Image(res.toExternalForm(), 100, 150, true, true);
            }
        } catch (Exception e) {
            
        }
        return null;
    }

    private void showBookDetail(Book b) {
        try {
            
            Book bookToShow = b;
            if (bookDao != null && b.getIsbn() != null) {
                try {
                    java.util.Optional<Book> reloaded = bookDao.findByIsbn(b.getIsbn());
                    if (reloaded.isPresent()) {
                        bookToShow = reloaded.get();
                    }
                } catch (Exception e) {
                    
                    bookToShow = b;
                }
            }
            
            VBox root = new VBox(10);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setStyle("-fx-padding: 20; -fx-background-color: #1e1e2e;");

            HBox top = new HBox(15);
            top.setAlignment(Pos.TOP_LEFT);

            
            Image img = loadImageForBook(bookToShow);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(120);
                iv.setFitHeight(180);
                iv.setPreserveRatio(true);
                top.getChildren().add(iv);
            } else {
                
                Label placeholder = new Label(
                        bookToShow.getTitle() != null && !bookToShow.getTitle().isEmpty() ? bookToShow.getTitle().substring(0, 1).toUpperCase()
                                : "?");
                placeholder.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: white; " +
                        "-fx-font-size: 60px; -fx-font-weight: bold; " +
                        "-fx-alignment: center; -fx-pref-width: 120; -fx-pref-height: 180;");
                top.getChildren().add(placeholder);
            }

            
            VBox info = new VBox(8);

            Label title = new Label(bookToShow.getTitle());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");
            title.setWrapText(true);
            title.setMaxWidth(320);

            Label author = new Label("Author: " + (bookToShow.getAuthor() == null ? "Unknown" : bookToShow.getAuthor()));
            author.setStyle("-fx-text-fill: #b4befe; -fx-font-size: 13px;");

            Label category = new Label("Category: " + (bookToShow.getCategory() == null ? "N/A" : bookToShow.getCategory()));
            category.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 12px;");

            Label isbn = new Label("ISBN: " + (bookToShow.getIsbn() == null ? "N/A" : bookToShow.getIsbn()));
            isbn.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 12px;");

            Label stock = new Label("📦 Stock: " + bookToShow.getStock() + " copies available");
            stock.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px; -fx-font-weight: bold;");

            Label price = new Label("💰 Price: " + String.format("%.2f", bookToShow.getPrice()) + " dt");
            price.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px; -fx-font-weight: bold;");

            Label status = new Label(
                    "Status: " + (bookToShow.getAvailabilityStatus() == null ? "Unknown" : bookToShow.getAvailabilityStatus()));
            status.setStyle("-fx-text-fill: #f9e2af; -fx-font-size: 12px;");

            info.getChildren().addAll(title, author, category, isbn, stock, price, status);
            top.getChildren().add(info);

            root.getChildren().add(top);

            
            if (bookToShow.getDescription() != null && !bookToShow.getDescription().isBlank()) {
                Label descLabel = new Label("Description:");
                descLabel.setStyle(
                        "-fx-font-weight: bold; -fx-text-fill: #cdd6f4; -fx-font-size: 13px; -fx-padding: 10 0 5 0;");

                Text desc = new Text(bookToShow.getDescription());
                desc.setStyle("-fx-fill: #a6adc8;");
                desc.setWrappingWidth(450);

                root.getChildren().addAll(descLabel, desc);
            }

            Stage dialog = new Stage();
            dialog.initOwner(coversPane.getScene() == null ? null : (Stage) coversPane.getScene().getWindow());
            dialog.setTitle(bookToShow.getTitle());
            Scene s = new Scene(root, 500, 400);

            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null) pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(s, pref);
            } catch (Exception ignored) {
            }

            dialog.setScene(s);
            dialog.show();
        } catch (Exception e) {
            System.err.println("Error showing book detail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void onClose() {
        try {
            Stage s = (Stage) coversPane.getScene().getWindow();
            if (s != null)
                s.close();
        } catch (Exception ignore) {
        }
    }

    @FXML
    protected void onChatbot() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/libraryplus/ui/chatbot_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("LibraryPlus AI Assistant");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null) pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.NONE); 
            stage.show();
        } catch (Exception e) {
            System.err.println("Failed to open Chatbot: " + e.getMessage());
            e.printStackTrace();
            try {
                Stage owner = (Stage) (coversPane.getScene() != null ? coversPane.getScene().getWindow() : null);
                if (owner != null) {
                    Toast.show(owner, "Unable to open Chatbot.", 2200, "error");
                }
            } catch (Exception ignored) {}
        }
    }
}
