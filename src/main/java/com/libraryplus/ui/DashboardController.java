package com.libraryplus.ui;

import com.libraryplus.app.Session;
import com.libraryplus.dao.BookDao;
import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.CommentDao;
import com.libraryplus.dao.LoanDao;
import com.libraryplus.dao.PurchaseDao;
import com.libraryplus.dao.jdbc.BookDaoJdbc;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.CommentDaoJdbc;
import com.libraryplus.dao.jdbc.LoanDaoJdbc;
import com.libraryplus.dao.jdbc.PurchaseDaoJdbc;
import com.libraryplus.model.Book;
import com.libraryplus.model.Client;
import com.libraryplus.model.Comment;
import com.libraryplus.model.Loan;
import com.libraryplus.model.Purchase;
import com.libraryplus.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.dao.jdbc.TransactionDaoJdbc;
import com.libraryplus.model.Transaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Button logoutButton;
    @FXML
    private ListView<Book> booksListView;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button chatButton;
    @FXML
    private Button inboxButton;
    @FXML
    private Button requestBookButton;
    @FXML
    private Button subscribeButton;
    @FXML
    private Button viewSubscriptionsButton;
    @FXML
    private TextField searchField;
    @FXML
    private Button usersButton;
    @FXML
    private Button addBookButton;
    @FXML
    private Button statisticsButton;
    @FXML
    private Button musicToggleButton;
    @FXML
    private Slider volumeSlider;
    private final ContextMenu suggestionsMenu = new ContextMenu();

    @FXML
    private HBox featuredBox;
    @FXML
    private ListView<String> searchHistoryListView;
    @FXML
    private Label detailTitle;
    @FXML
    private Label detailAuthor;
    @FXML
    private Label detailCategory;
    @FXML
    private Label detailIsbn;
    @FXML
    private Label detailStock;
    @FXML
    private Label detailPrice;
    @FXML
    private TextArea detailDescription;
    @FXML
    private Button loadMoreButton;
    @FXML
    private ListView<String> reviewsListView;
    @FXML
    private Label ratingSummaryLabel;
    @FXML
    private ChoiceBox<String> ratingChoiceBox;
    @FXML
    private TextField newReviewField;
    @FXML
    private TextField unitPriceField;
    @FXML
    private Label purchaseDiscountInfo;
    @FXML
    private Button myLoansButton;
    @FXML
    private javafx.scene.image.ImageView carouselCover;
    @FXML
    private Label carouselTitle;

    
    
    private BookDao bookDao;
    private ClientDao clientDao;
    private PurchaseDao purchaseDao;
    private LoanDao loanDao;
    private CommentDao commentDao;

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    
    private final ObservableList<String> searchHistory = FXCollections.observableArrayList();
    private int pageOffset = 0;
    private final int pageSize = 20;
    private List<Book> featuredBooksCache = java.util.Collections.emptyList();
    private javafx.animation.Timeline carouselTimeline;
    private int carouselIndex = 0;

    public void updateUserHeaderDisplay() {
        User u = Session.getCurrentUser();
        if (u != null) {
            try {
                com.libraryplus.dao.UserDao uDao = new com.libraryplus.dao.jdbc.UserDaoJdbc();
                Optional<User> fresh = uDao.findById(u.getId());
                if (fresh.isPresent()) {
                    u = fresh.get();
                    Session.setCurrentUser(u);
                }
            } catch (Exception ignored) {}
            String role = (u.getRoleId() == 1) ? "ADMIN" : "MEMBER";
            if (welcomeLabel != null) {
                welcomeLabel.setText("👤 " + u.getFullName() + " (" + role + ")");
            }
            if (balanceLabel != null) {
                balanceLabel.setText(String.format("💳 %.2f DT", u.getCardBalance()));
            }
        }
        if (themeToggleButton != null) {
            String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
            if ("Tokyo Night".equalsIgnoreCase(pref)) {
                themeToggleButton.setText("🌙 Tokyo Night");
            } else if ("Mayor Touch".equalsIgnoreCase(pref)) {
                themeToggleButton.setText("⭐ Mayor Touch");
            } else {
                themeToggleButton.setText("☀️ Catppuccin");
            }
        }
    }

    @FXML
    public void initialize() {
        
        
        try {
            this.bookDao = new BookDaoJdbc();
            
            if (this.bookDao instanceof com.libraryplus.dao.jdbc.BookDaoJdbc) {
                ((com.libraryplus.dao.jdbc.BookDaoJdbc) this.bookDao).updateDefaultPrices();
            }
            this.clientDao = new ClientDaoJdbc();
            this.purchaseDao = new PurchaseDaoJdbc();
            this.loanDao = new LoanDaoJdbc();
            this.commentDao = new CommentDaoJdbc();
        } catch (Exception e) {
            logger.warn("Failed to initialize DAOs during Dashboard initialize(). Continuing with null DAOs.", e);
            
        }

        try {
            com.libraryplus.util.AudioManager audio = com.libraryplus.util.AudioManager.getInstance();

            if (volumeSlider != null) {
                volumeSlider.setValue(audio.getVolume());
                volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    audio.setVolume(newVal.doubleValue());
                });
            }

            if (musicToggleButton != null) {
                updateMusicButtonIcon();
            }

            if (ratingChoiceBox != null) {
                ratingChoiceBox.getItems().setAll(
                    "⭐⭐⭐⭐⭐ (5 Stars)",
                    "⭐⭐⭐⭐ (4 Stars)",
                    "⭐⭐⭐ (3 Stars)",
                    "⭐⭐ (2 Stars)",
                    "⭐ (1 Star)"
                );
                ratingChoiceBox.setValue("⭐⭐⭐⭐⭐ (5 Stars)");
            }

            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldText, newText) -> {
                    onSearchTextChanged(newText);
                });
                searchField.focusedProperty().addListener((obs, oldF, newF) -> {
                    if (!newF) {
                        suggestionsMenu.hide();
                    }
                });
            }

            updateUserHeaderDisplay();

            User u = Session.getCurrentUser();
            if (u != null && u.getRoleId() == 2) { 
                // CLIENT / MEMBER view
                if (chatButton != null) chatButton.setVisible(true);
                if (inboxButton != null) inboxButton.setVisible(false);
                if (requestBookButton != null) requestBookButton.setVisible(true);
                if (subscribeButton != null) subscribeButton.setVisible(true);
                if (viewSubscriptionsButton != null) viewSubscriptionsButton.setVisible(false);
                if (addBookButton != null) {
                    addBookButton.setVisible(false);
                    addBookButton.setManaged(false);
                }
                if (statisticsButton != null) {
                    statisticsButton.setVisible(false);
                    statisticsButton.setManaged(false);
                }
                if (usersButton != null) {
                    usersButton.setVisible(false);
                    usersButton.setManaged(false);
                }
                if (editButton != null) {
                    editButton.setVisible(false);
                    editButton.setManaged(false);
                }
                if (deleteButton != null) {
                    deleteButton.setVisible(false);
                    deleteButton.setManaged(false);
                }
            } else { 
                // ADMIN view
                if (chatButton != null) chatButton.setVisible(false);
                if (inboxButton != null) inboxButton.setVisible(true);
                if (requestBookButton != null) requestBookButton.setVisible(false);
                if (subscribeButton != null) subscribeButton.setVisible(false);
                if (viewSubscriptionsButton != null) viewSubscriptionsButton.setVisible(true);
                if (addBookButton != null) {
                    addBookButton.setVisible(true);
                    addBookButton.setManaged(true);
                }
                if (statisticsButton != null) {
                    statisticsButton.setVisible(true);
                    statisticsButton.setManaged(true);
                }
                if (usersButton != null) {
                    usersButton.setVisible(true);
                    usersButton.setManaged(true);
                }
                if (editButton != null) {
                    editButton.setVisible(true);
                    editButton.setManaged(true);
                }
                if (deleteButton != null) {
                    deleteButton.setVisible(true);
                    deleteButton.setManaged(true);
                }
            }

            
            booksListView.setCellFactory(lv -> new ListCell<>() {
                private final VBox rootBox = new VBox();
                private final Label headerLabel = new Label();
                private final VBox contentBox = new VBox();
                private final Label title = new Label();
                private final Label meta = new Label();
                {
                    headerLabel.getStyleClass().add("section-title");
                    title.getStyleClass().add("book-title");
                    meta.getStyleClass().add("book-meta");
                    contentBox.getChildren().addAll(title, meta);
                    rootBox.getChildren().addAll(headerLabel, contentBox);
                    headerLabel.setVisible(false);
                    headerLabel.setManaged(false);
                }

                @Override
                protected void updateItem(Book item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    title.setText(item.getTitle());
                    meta.setText((item.getAuthor() == null ? "Unknown author" : item.getAuthor())
                            + " • " + (item.getIsbn() == null ? "" : item.getIsbn()));

                    
                    
                    try {
                        int idx = getIndex();
                        boolean showHeader = false;
                        String cat = item.getCategory() == null ? "" : item.getCategory();
                        if (idx <= 0) {
                            showHeader = !cat.isBlank();
                        } else {
                            List<Book> items = booksListView.getItems();
                            if (items != null && idx - 1 < items.size() && idx - 1 >= 0) {
                                Book prev = items.get(idx - 1);
                                String prevCat = prev == null || prev.getCategory() == null ? "" : prev.getCategory();
                                if (!cat.equals(prevCat))
                                    showHeader = !cat.isBlank();
                            } else {
                                showHeader = !cat.isBlank();
                            }
                        }
                        if (showHeader) {
                            headerLabel.setText(cat);
                            headerLabel.setVisible(true);
                            headerLabel.setManaged(true);
                            
                            if (!headerLabel.getStyleClass().contains("clickable-category")) {
                                headerLabel.getStyleClass().add("clickable-category");
                            }
                            
                            try {
                                Tooltip.install(headerLabel, new Tooltip("Click to filter by this category"));
                            } catch (Exception exTooltip) {
                                
                            }
                            
                            headerLabel.setOnMouseClicked(me -> {
                                try {
                                    
                                    DashboardController.this.loadByCategory(cat);
                                } catch (Exception ex) {
                                    logger.debug("Category header click failed", ex);
                                }
                            });
                        } else {
                            headerLabel.setVisible(false);
                            headerLabel.setManaged(false);
                            headerLabel.setOnMouseClicked(null);
                            headerLabel.getStyleClass().remove("clickable-category");
                            try {
                                Tooltip.uninstall(headerLabel, null);
                            } catch (Exception exUninstall) {
                                
                            }
                        }
                    } catch (Exception exUpdate) {
                        headerLabel.setVisible(false);
                        headerLabel.setManaged(false);
                        headerLabel.setOnMouseClicked(null);
                        headerLabel.getStyleClass().remove("clickable-category");
                        try {
                            Tooltip.uninstall(headerLabel, null);
                        } catch (Exception exUninstall2) {
                            
                        }
                    }

                    setGraphic(rootBox);
                }
            });

            
            booksListView.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldV, newV) -> showDetails(newV));

            
            searchHistoryListView.setItems(searchHistory);
            searchHistoryListView.setOnMouseClicked(ev -> {
                String sel = searchHistoryListView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    searchField.setText(sel);
                    onSearch(null);
                }
            });

            refreshBooks();
            refreshFeatured();
            refreshCategorizedView();

        } catch (Throwable t) {
            logger.error("Dashboard initialization failed", t);
            
            try {
                String timestamp = String.valueOf(System.currentTimeMillis());
                java.nio.file.Path tmp = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"),
                        "dashboard-init-" + timestamp + ".log");
                try (PrintWriter pw = new PrintWriter(java.nio.file.Files.newBufferedWriter(tmp))) {
                    t.printStackTrace(pw);
                }
                String msg = "Dashboard init failed; details saved to: " + tmp.toAbsolutePath();
                logger.info(msg);
                
                try {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Dashboard init error");
                    a.setHeaderText("Dashboard failed to initialize");
                    TextArea ta = new TextArea();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    ta.setText(sw.toString());
                    ta.setEditable(false);
                    ta.setWrapText(true);
                    a.getDialogPane().setExpandableContent(ta);
                    a.showAndWait();
                } catch (Exception alertEx) {
                    
                }
            } catch (Exception ex) {
                logger.error("Failed to write dashboard init log", ex);
            }
            
            throw new RuntimeException(t);
        }
    }

    private void refreshFeatured() {
        featuredBox.getChildren().clear();
        try {
            List<Book> featured = bookDao.findFeatured(8);
            
            try {
                java.util.Collections.shuffle(featured);
            } catch (Exception shuffleEx) {
                
            }
            featuredBooksCache = featured; 
            for (Book b : featured) {
                VBox card = createFeaturedCard(b);
                featuredBox.getChildren().add(card);
            }
            startCarouselRotation();
        } catch (Exception e) {
            logger.warn("Failed to load featured books", e);
        }
    }

    private void startCarouselRotation() {
        try {
            if (carouselTimeline != null) {
                carouselTimeline.stop();
            }
            if (featuredBooksCache == null || featuredBooksCache.isEmpty()) {
                return;
            }
            carouselIndex = 0;
            carouselTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0), ev -> {
                        try {
                            Book b = featuredBooksCache.get(carouselIndex % featuredBooksCache.size());
                            javafx.scene.image.Image img = loadImageForBook(b);
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    if (carouselCover != null) {
                                        carouselCover.setImage(img);
                                    }
                                    if (carouselTitle != null) {
                                        carouselTitle.setText(b.getTitle());
                                    }
                                } catch (Exception platformEx) {
                                    
                                }
                            });
                            carouselIndex++;
                        } catch (Exception ex) {
                            logger.debug("Carousel rotate failed", ex);
                        }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.5)));
            carouselTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            carouselTimeline.play();
        } catch (Exception e) {
            logger.debug("Failed to start carousel rotation", e);
        }
    }

    private javafx.scene.image.Image loadImageForBook(Book b) {
        if (b == null) {
            return null;
        }
        try {
            String isbn = b.getIsbn();
            if (isbn != null && !isbn.isBlank()) {
                java.nio.file.Path coversDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "data",
                        "covers");
                java.nio.file.Path p = coversDir.resolve(isbn + ".png");
                java.io.File f = p.toFile();
                if (f.exists()) {
                    return new javafx.scene.image.Image(f.toURI().toString(), true);
                }
            }
            
            String resName = "/images/" + (b.getIsbn() == null ? "" : b.getIsbn()) + ".png";
            java.net.URL res = getClass().getResource(resName);
            if (res != null) {
                return new javafx.scene.image.Image(res.toExternalForm(), true);
            }
        } catch (Exception e) {
            
        }
        return null;
    }

    private VBox createFeaturedCard(Book b) {
        VBox card = new VBox();
        card.getStyleClass().addAll("featured-card");
        javafx.scene.image.ImageView cover = new javafx.scene.image.ImageView();
        cover.setFitHeight(110);
        cover.setPreserveRatio(true);
        javafx.scene.image.Image img = loadImageForBook(b);
        if (img != null) {
            cover.setImage(img);
        }
        Label t = new Label(b.getTitle());
        t.getStyleClass().add("book-title");
        card.getChildren().addAll(cover, t);
        card.setOnMouseClicked(ev -> {
            
            
            
            
            showDetails(b);
        });
        return card;
    }

    private void showDetails(Book b) {
        if (b == null) {
            detailTitle.setText("Select a book");
            detailAuthor.setText("");
            detailCategory.setText("");
            detailIsbn.setText("");
            detailStock.setText("");
            detailPrice.setText("");
            detailDescription.setText("");
            reviewsListView.getItems().clear();
            if (unitPriceField != null) unitPriceField.setText("");
            return;
        }
        detailTitle.setText(b.getTitle());
        detailAuthor.setText(b.getAuthor() == null ? "" : b.getAuthor());
        detailCategory.setText(b.getCategory() == null ? "" : b.getCategory());
        detailIsbn.setText(b.getIsbn() == null ? "" : b.getIsbn());
        String stockText = "📦 Stock: " + b.getStock() + " copies (" + (b.getStock() > 0 ? "In Stock" : "Out of Stock") + ")";
        try {
            int clientId = ensureClientIdForCurrentUser();
            Optional<Loan> activeLoan = loanDao.findActiveLoanByBookAndClient(b.getIsbn(), clientId);
            if (activeLoan.isPresent()) {
                LocalDateTime due = activeLoan.get().getExpectedReturnDate();
                stockText += " | 📌 Borrowed by you (Due: " + (due != null ? due.toLocalDate().toString() : "N/A") + ")";
            }
        } catch (Exception ignore) {}
        detailStock.setText(stockText);

        double price = b.getPrice() > 0 ? b.getPrice() : 15.00;
        detailPrice.setText("💰 Price: " + String.format("%.2f", price) + " DT");
        detailDescription.setText(b.getDescription() == null ? "" : b.getDescription());
        
        loadReviews(b.getIsbn());
        
        if (unitPriceField != null) {
            unitPriceField.setText(String.format("%.2f DT", price));
        }
        if (purchaseDiscountInfo != null) {
            try {
                int cId = ensureClientIdForCurrentUser();
                boolean isSub = new com.libraryplus.dao.jdbc.SubscriptionDaoJdbc().findActiveByClientId(cId).isPresent();
                if (isSub) {
                    double discPrice = Math.round((price * 0.90) * 100.0) / 100.0;
                    purchaseDiscountInfo.setText("⭐ 10% Subscriber Discount Active! You pay " + String.format("%.2f DT", discPrice) + " instead of " + String.format("%.2f DT", price));
                    purchaseDiscountInfo.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                } else {
                    purchaseDiscountInfo.setText("💡 Subscribers receive 10% off books. Click 'Subscribe' in the header to get 10% off all purchases!");
                    purchaseDiscountInfo.setStyle("-fx-text-fill: #94a3b8;");
                }
            } catch (Exception ignore) {
                purchaseDiscountInfo.setText("💡 Subscribers automatically receive a 10% discount on purchases.");
                purchaseDiscountInfo.setStyle("-fx-text-fill: #94a3b8;");
            }
        }
    }

    private void loadReviews(String isbn) {
        try {
            List<Comment> comments = commentDao.findByBook(isbn);
            ObservableList<String> items = FXCollections.observableArrayList();
            double totalStars = 0.0;
            int count = 0;
            for (Comment c : comments) {
                String comm = c.getComment();
                if (comm != null) {
                    items.add(comm);
                    int stars = 5;
                    if (comm.startsWith("★")) {
                        stars = (int) comm.chars().filter(ch -> ch == '★').count();
                    } else if (comm.startsWith("⭐")) {
                        stars = (int) comm.chars().filter(ch -> ch == '⭐').count();
                    }
                    totalStars += Math.max(1, Math.min(5, stars));
                    count++;
                }
            }
            reviewsListView.setItems(items);
            if (ratingSummaryLabel != null) {
                if (count > 0) {
                    double avg = totalStars / count;
                    ratingSummaryLabel.setText(String.format("⭐ Average Rating: %.1f / 5.0 (%d %s)", avg, count, count == 1 ? "review" : "reviews"));
                } else {
                    ratingSummaryLabel.setText("⭐ Average Rating: No reviews yet. Be the first to rate!");
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load comments", e);
        }
    }

    private void onSearchTextChanged(String newText) {
        if (newText == null || newText.trim().isEmpty()) {
            suggestionsMenu.hide();
            searchResultsPane.setVisible(false);
            categorizedPane.setVisible(true);
            return;
        }

        String q = newText.trim();
        try {
            List<Book> matches = bookDao.search(q, 0, 8);
            suggestionsMenu.getItems().clear();

            if (matches.isEmpty()) {
                MenuItem noMatch = new MenuItem("No books found matching \"" + q + "\"");
                noMatch.setDisable(true);
                suggestionsMenu.getItems().add(noMatch);
            } else {
                for (Book b : matches) {
                    String label = "📖 " + b.getTitle() + " — " + (b.getAuthor() != null ? b.getAuthor() : "Unknown");
                    MenuItem mi = new MenuItem(label);
                    mi.setOnAction(ev -> {
                        searchField.setText(b.getTitle());
                        showDetails(b);
                        onSearch(null);
                    });
                    suggestionsMenu.getItems().add(mi);
                }
            }

            // Real-time live catalog filtering as characters are typed
            ObservableList<Book> liveItems = FXCollections.observableArrayList(matches);
            booksListView.setItems(liveItems);
            searchResultsPane.setVisible(true);
            categorizedPane.setVisible(false);

            if (!suggestionsMenu.isShowing() && searchField.getScene() != null && searchField.getScene().getWindow() != null) {
                javafx.geometry.Point2D p = searchField.localToScreen(0, searchField.getHeight());
                if (p != null) {
                    suggestionsMenu.show(searchField, p.getX(), p.getY());
                }
            }
        } catch (Exception ex) {
            logger.debug("Live search error", ex);
        }
    }

    private void refreshBooks() {
        try {
            List<Book> books = bookDao == null ? java.util.Collections.emptyList() : bookDao.search("", 0, pageSize);
            ObservableList<Book> items = FXCollections.observableArrayList();
            items.addAll(books);
            booksListView.setItems(items);
            
            pageOffset = (books == null ? 0 : books.size());
            if (loadMoreButton != null) {
                loadMoreButton.setDisable(books == null || books.size() < pageSize);
            }
        } catch (Exception e) {
            logger.error("Failed to load books", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Failed to load books: " + e.getMessage(), 2500, "error");
            }
        }
    }

    @FXML
    protected void onSearch(ActionEvent event) {
        String q = searchField.getText();

        
        if (q == null || q.isBlank()) {
            searchResultsPane.setVisible(false);
            categorizedPane.setVisible(true);
            return;
        } else {
            searchResultsPane.setVisible(true);
            categorizedPane.setVisible(false);
        }

        
        pageOffset = 0;
        
        if (q != null && !q.isBlank()) {
            searchHistory.remove(q); 
            searchHistory.add(0, q);
            if (searchHistory.size() > 10) {
                searchHistory.remove(10);
            }
        }
        try {
            List<Book> books = bookDao.search(q == null ? "" : q, pageOffset, pageSize);
            ObservableList<Book> items = FXCollections.observableArrayList();
            items.addAll(books);
            booksListView.setItems(items);
            pageOffset += books.size();
            if (loadMoreButton != null) {
                loadMoreButton.setDisable(books == null || books.size() < pageSize);
            }
        } catch (Exception e) {
            logger.error("Search failed", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Search failed: " + e.getMessage(), 2200, "error");
            }
        }
    }

    @FXML
    protected void onLoadMore(ActionEvent event) {
        String q = searchField.getText();
        try {
            List<Book> books = bookDao.search(q == null ? "" : q, pageOffset, pageSize);
            ObservableList<Book> items = booksListView.getItems();
            if (items == null) {
                items = FXCollections.observableArrayList();
            }
            items.addAll(books);
            booksListView.setItems(items);
            pageOffset += books.size();
            if (books.size() < pageSize) {
                
                loadMoreButton.setDisable(true);
            }
        } catch (Exception e) {
            logger.error("Load more failed", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Load more failed: " + e.getMessage(), 2200, "error");
            }
        }
    }

    @FXML
    protected void onAddBook(ActionEvent event) {
        User u = Session.getCurrentUser();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (u == null || u.getRoleId() != 1) {
            if (owner != null) {
                Toast.show(owner, "Access Denied: Only administrators can add books.", 2500, "error");
            }
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_book.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add Book");
            Scene scene = new Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            dialog.setScene(scene);
            dialog.setOnHidden(ev -> {
                refreshBooks();
                refreshFeatured();
                refreshCategorizedView();
            });
            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open Add Book dialog", e);
            if (owner != null) {
                Toast.show(owner, "Unable to open Add Book window.", 2200, "error");
            }
        }
    }

    @FXML
    protected void onShowStatistics(ActionEvent event) {
        User u = Session.getCurrentUser();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (u == null || u.getRoleId() != 1) {
            if (owner != null) {
                Toast.show(owner, "Access Denied: Statistics are reserved for administrators.", 2500, "error");
            }
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Library Analytics & Statistics");
            Scene scene = new Scene(root, 950, 720);
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null) pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {}
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open statistics dialog", e);
            if (owner != null) {
                Toast.show(owner, "Unable to open Statistics: " + e.getMessage(), 2500, "error");
            }
        }
    }

    @FXML
    protected void onSubmitReview(ActionEvent event) {
        Book b = booksListView.getSelectionModel().getSelectedItem();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (b == null) {
            if (owner != null) Toast.show(owner, "Select a book to write a review.", 2000, "info");
            return;
        }
        String text = newReviewField != null ? newReviewField.getText() : "";
        if (text == null || text.trim().isEmpty()) {
            if (owner != null) Toast.show(owner, "Please enter your review comment.", 2000, "warning");
            return;
        }
        String starsChoice = (ratingChoiceBox != null && ratingChoiceBox.getValue() != null) 
                ? ratingChoiceBox.getValue() : "⭐⭐⭐⭐⭐ (5 Stars)";
        int stars = 5;
        if (starsChoice.startsWith("⭐ (")) stars = 1;
        else if (starsChoice.startsWith("⭐⭐ (")) stars = 2;
        else if (starsChoice.startsWith("⭐⭐⭐ (")) stars = 3;
        else if (starsChoice.startsWith("⭐⭐⭐⭐ (")) stars = 4;
        else stars = 5;

        String starSymbols = "★".repeat(stars) + "☆".repeat(5 - stars);
        String formattedComment = starSymbols + " — " + text.trim();

        try {
            int clientId = ensureClientIdForCurrentUser();
            Comment c = new Comment();
            c.setBookIsbn(b.getIsbn());
            c.setClientId(clientId);
            c.setComment(formattedComment);
            commentDao.createComment(c);

            // Recalculate average rating for this book
            List<Comment> allComments = commentDao.findByBook(b.getIsbn());
            double totalStars = 0.0;
            int reviewCount = 0;
            for (Comment comm : allComments) {
                String cText = comm.getComment();
                if (cText != null) {
                    int st = 5;
                    if (cText.startsWith("★")) {
                        st = (int) cText.chars().filter(ch -> ch == '★').count();
                    } else if (cText.startsWith("⭐")) {
                        st = (int) cText.chars().filter(ch -> ch == '⭐').count();
                    }
                    totalStars += Math.max(1, Math.min(5, st));
                    reviewCount++;
                }
            }
            double newAvg = reviewCount > 0 ? (totalStars / reviewCount) : stars;
            b.setAvgRating(newAvg);
            b.setRatingsCount(reviewCount);
            bookDao.updateBook(b);

            if (newReviewField != null) newReviewField.clear();
            loadReviews(b.getIsbn());
            if (owner != null) {
                Toast.show(owner, "Review & " + stars + "-star rating posted!", 2500, "success");
            }
        } catch (Exception e) {
            logger.error("Failed to submit review", e);
            if (owner != null) Toast.show(owner, "Failed to submit review: " + e.getMessage(), 2500, "error");
        }
    }

    @FXML
    protected void onEditBook(ActionEvent event) {
        Book selected = booksListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_book.fxml"));
            Parent root = loader.load();
            AddBookController ctrl = loader.getController();
            ctrl.setBook(selected);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Edit Book");
            Scene scene = new Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            dialog.setScene(scene);
            dialog.setOnHidden(ev -> {
                refreshBooks();
                refreshFeatured();
            });
            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open Edit Book dialog", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Unable to open Edit Book window.", 2200, "error");
            }
        }
    }

    @FXML
    protected void onDeleteBook(ActionEvent event) {
        Book selected = booksListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete book '" + selected.getTitle() + "' ?", ButtonType.YES,
                ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try {
                    bookDao.deleteByIsbn(selected.getIsbn());
                    refreshBooks();
                    refreshFeatured();
                    Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow()
                            : null);
                    if (owner != null) {
                        Toast.show(owner, "Book deleted.", 1800, "success");
                    }
                } catch (Exception e) {
                    logger.error("Failed to delete book", e);
                    Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow()
                            : null);
                    if (owner != null) {
                        Toast.show(owner, "Failed to delete book: " + e.getMessage(), 2200, "error");
                    }
                }
            }
        });
    }

    private synchronized int ensureClientIdForCurrentUser() throws Exception {
        User u = Session.getCurrentUser();
        if (u == null) {
            throw new IllegalStateException("No user is currently logged in. Please sign in.");
        }

        Optional<Client> oc = clientDao.findByUserId(u.getId());
        if (oc.isPresent()) {
            return oc.get().getId();
        }

        // Generate a safe unique phone number if user does not have one
        String phone = u.getPhone();
        if (phone == null || phone.isBlank()) {
            phone = "+216" + Math.abs((u.getId() * 10007 + 1000) % 100000000);
        }

        Client c = new Client();
        c.setUserId(u.getId());
        c.setPhone(phone.trim());
        String name = (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName().trim() : u.getEmail();
        String[] parts = name.split("\\s+", 2);
        c.setFirstName(parts[0].isBlank() ? "Member" : parts[0]);
        c.setLastName(parts.length > 1 ? parts[1] : "");
        c.setDateOfBirth(u.getDateOfBirth() != null ? u.getDateOfBirth() : java.time.LocalDate.of(2000, 1, 1));
        c.setMembershipType("STANDARD");

        try {
            int created = clientDao.createClient(c);
            if (created > 0) {
                return created;
            }
        } catch (Exception ex) {
            logger.warn("Initial client creation failed for user {} ({}), trying unique phone fallback...", u.getId(), ex.getMessage());
            // Retry with a guaranteed unique timestamp-based phone
            c.setPhone("+216" + (System.currentTimeMillis() % 100000000));
            try {
                int created = clientDao.createClient(c);
                if (created > 0) return created;
            } catch (Exception ex2) {
                logger.warn("Retry failed ({}), attempting to fetch client by userId...", ex2.getMessage());
            }
        }

        Optional<Client> recheck = clientDao.findByUserId(u.getId());
        if (recheck.isPresent()) {
            return recheck.get().getId();
        }
        throw new IllegalStateException("Unable to retrieve or create a client account for user " + u.getEmail());
    }

    @FXML
    protected void onBorrow(ActionEvent event) {
        Book b = booksListView.getSelectionModel().getSelectedItem();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (b == null) {
            if (owner != null) {
                Toast.show(owner, "Select a book to borrow.", 1800, "info");
            }
            return;
        }
        try {
            int clientId = ensureClientIdForCurrentUser();

            // Check if user already has an active loan for this book
            Optional<Loan> activeLoan = loanDao.findActiveLoanByBookAndClient(b.getIsbn(), clientId);
            if (activeLoan.isPresent()) {
                LocalDateTime due = activeLoan.get().getExpectedReturnDate();
                Alert alreadyAlert = new Alert(Alert.AlertType.CONFIRMATION,
                        "You currently have '" + b.getTitle() + "' borrowed (Due: " +
                        (due != null ? due.toLocalDate().toString() : "N/A") + ").\n\nWould you like to return this book now?",
                        ButtonType.YES, ButtonType.NO);
                alreadyAlert.setTitle("Active Loan Detected");
                alreadyAlert.setHeaderText("Book Already Borrowed");
                if (owner != null) alreadyAlert.initOwner(owner);
                Optional<ButtonType> resp = alreadyAlert.showAndWait();
                if (resp.isPresent() && resp.get() == ButtonType.YES) {
                    com.libraryplus.service.ReturnBookService returnService = new com.libraryplus.service.ReturnBookService();
                    returnService.returnBook(b.getIsbn(), clientId);
                    if (owner != null) {
                        Toast.show(owner, "Book returned successfully: " + b.getTitle(), 2500, "success");
                    }
                    refreshBooks();
                    Optional<Book> reloaded = bookDao.findByIsbn(b.getIsbn());
                    reloaded.ifPresent(this::showDetails);
                    updateUserHeaderDisplay();
                }
                return;
            }

            // Check if stock is available
            if (b.getStock() <= 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "'" + b.getTitle() + "' is currently out of stock.\nWould you like to join the waitlist to be notified when a copy is returned?",
                        ButtonType.YES, ButtonType.NO);
                alert.setTitle("Book Out of Stock");
                alert.setHeaderText("Join Waitlist");
                if (owner != null) alert.initOwner(owner);
                Optional<ButtonType> resp = alert.showAndWait();
                if (resp.isPresent() && resp.get() == ButtonType.YES) {
                    new com.libraryplus.service.WaitlistService().joinWaitlist(b.getIsbn(), clientId);
                    if (owner != null) {
                        Toast.show(owner, "Added to waitlist for: " + b.getTitle(), 2200, "info");
                    }
                }
                return;
            }

            // Check borrow limits (maximum 5 active loans)
            List<Loan> currentLoans = loanDao.findActiveByClientId(clientId);
            if (currentLoans.size() >= 5) {
                Alert limitAlert = new Alert(Alert.AlertType.WARNING,
                        "You have reached the maximum limit of 5 active borrowed books.\nPlease return a book before borrowing another.",
                        ButtonType.OK);
                limitAlert.setTitle("Borrow Limit Reached");
                limitAlert.setHeaderText("Maximum Active Loans");
                if (owner != null) limitAlert.initOwner(owner);
                limitAlert.showAndWait();
                return;
            }

            // Confirmation Dialog with loan terms
            LocalDateTime borrowTime = LocalDateTime.now();
            LocalDateTime returnDue = borrowTime.plusWeeks(2);
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Book Loan");
            confirmAlert.setHeaderText("Borrow '" + b.getTitle() + "'");
            confirmAlert.setContentText("Author: " + (b.getAuthor() != null ? b.getAuthor() : "Unknown") +
                    "\nBorrow Date: " + borrowTime.toLocalDate() +
                    "\nDue Date: " + returnDue.toLocalDate() + " (14 Days)" +
                    "\n\nReturn Policy: 14 days free loan period.\nLate returns incur a fee of 1.00 DT per day.\n\nProceed to borrow this book?");
            if (owner != null) confirmAlert.initOwner(owner);

            ButtonType borrowButtonType = new ButtonType("Confirm Borrow", ButtonBar.ButtonData.OK_DONE);
            confirmAlert.getButtonTypes().setAll(borrowButtonType, ButtonType.CANCEL);

            Optional<ButtonType> choice = confirmAlert.showAndWait();
            if (choice.isEmpty() || choice.get() != borrowButtonType) {
                return;
            }

            Loan loan = new Loan();
            loan.setBookIsbn(b.getIsbn());
            loan.setClientId(clientId);
            loan.setBorrowDate(borrowTime);
            loan.setExpectedReturnDate(returnDue);
            int id = loanDao.createLoan(loan);

            // Decrement book stock
            b.setStock(b.getStock() - 1);
            if (b.getStock() <= 0) {
                b.setAvailabilityStatus("OUT_OF_STOCK");
            }
            bookDao.updateBook(b);

            if (owner != null) {
                Toast.show(owner, "🎉 Borrowed '" + b.getTitle() + "'! Due on " + returnDue.toLocalDate(), 2500, "success");
            }
            refreshBooks();
            Optional<Book> reloaded = bookDao.findByIsbn(b.getIsbn());
            reloaded.ifPresent(this::showDetails);
            updateUserHeaderDisplay();
        } catch (Exception e) {
            logger.error("Failed to create loan", e);
            if (owner != null) {
                Toast.show(owner, "Borrow failed: " + e.getMessage(), 2500, "error");
            }
        }
    }

    @FXML
    protected void onPurchase(ActionEvent event) {
        Book b = booksListView.getSelectionModel().getSelectedItem();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (b == null) {
            if (owner != null) {
                Toast.show(owner, "Select a book to purchase.", 1800, "info");
            }
            return;
        }

        if (b.getStock() <= 0) {
            if (owner != null) {
                Toast.show(owner, "'" + b.getTitle() + "' is out of stock for purchase.", 2200, "warning");
            }
            return;
        }

        try {
            int clientId = ensureClientIdForCurrentUser();
            User u = Session.getCurrentUser();
            if (u == null) throw new IllegalStateException("No user logged in");

            // Fetch fresh balance
            UserDao uDao = new UserDaoJdbc();
            Optional<User> freshUser = uDao.findById(u.getId());
            if (freshUser.isPresent()) {
                u = freshUser.get();
                Session.setCurrentUser(u);
            }

            // Calculate price and subscriber discount
            double basePrice = b.getPrice() > 0 ? b.getPrice() : 15.00;
            com.libraryplus.dao.SubscriptionDao subDao = new com.libraryplus.dao.jdbc.SubscriptionDaoJdbc();
            Optional<com.libraryplus.model.Subscription> sub = subDao.findActiveByClientId(clientId);
            boolean isSubscriber = sub.isPresent();
            double finalUnitPrice = isSubscriber ? Math.round((basePrice * 0.90) * 100.0) / 100.0 : basePrice;

            // Purchase confirmation dialog
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Confirm Book Purchase");
            dialog.setHeaderText("Purchase '" + b.getTitle() + "'");
            if (owner != null) dialog.initOwner(owner);

            ButtonType buyButtonType = new ButtonType("Confirm Payment", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(buyButtonType, ButtonType.CANCEL);

            VBox box = new VBox(10);
            box.setPadding(new Insets(14));

            Label authorLbl = new Label("Author: " + (b.getAuthor() != null ? b.getAuthor() : "Unknown"));
            Label stockLbl = new Label("Available in Stock: " + b.getStock() + " copies");

            HBox qtyBox = new HBox(8);
            qtyBox.setAlignment(Pos.CENTER_LEFT);
            Label qtyPrompt = new Label("Select Quantity:");
            qtyPrompt.setStyle("-fx-font-weight: 600;");
            Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, b.getStock()), 1);
            qtySpinner.setPrefWidth(90);
            qtyBox.getChildren().addAll(qtyPrompt, qtySpinner);

            String discountText = isSubscriber ? " (10% Subscriber Discount: -" + String.format("%.2f", basePrice - finalUnitPrice) + " DT)" : "";
            Label priceLbl = new Label("Unit Price: " + String.format("%.2f DT", finalUnitPrice) + discountText);
            priceLbl.setStyle("-fx-font-weight: 600;");

            Label totalLbl = new Label("Total Cost: " + String.format("%.2f DT", finalUnitPrice));
            totalLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #3b82f6;");

            double currentBal = u.getCardBalance();
            Label balanceLbl = new Label("Your Card Balance: " + String.format("%.2f DT", currentBal));
            Label afterLbl = new Label("Balance After Purchase: " + String.format("%.2f DT", currentBal - finalUnitPrice));

            final double userBalanceFinal = currentBal;
            Runnable updateCalculations = () -> {
                int q = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                double total = Math.round((finalUnitPrice * q) * 100.0) / 100.0;
                totalLbl.setText("Total Cost: " + String.format("%.2f DT", total));
                double remaining = Math.round((userBalanceFinal - total) * 100.0) / 100.0;
                if (remaining >= 0) {
                    afterLbl.setText("Balance After Purchase: " + String.format("%.2f DT", remaining));
                    afterLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: 600;");
                    javafx.scene.Node btn = dialog.getDialogPane().lookupButton(buyButtonType);
                    if (btn != null) btn.setDisable(false);
                } else {
                    afterLbl.setText("⚠️ Insufficient Balance! (Need " + String.format("%.2f DT", -remaining) + " more)");
                    afterLbl.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 700;");
                    javafx.scene.Node btn = dialog.getDialogPane().lookupButton(buyButtonType);
                    if (btn != null) btn.setDisable(true);
                }
            };

            qtySpinner.valueProperty().addListener((obs, oldV, newV) -> updateCalculations.run());

            Button topUpInDialogBtn = new Button("💳 Top Up Wallet Now");
            topUpInDialogBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
            topUpInDialogBtn.setOnAction(e -> {
                dialog.close();
                onTopUpBalance(event);
            });

            box.getChildren().addAll(authorLbl, stockLbl, priceLbl, qtyBox, totalLbl, balanceLbl, afterLbl, topUpInDialogBtn);
            dialog.getDialogPane().setContent(box);

            if (owner != null && owner.getScene() != null && !owner.getScene().getStylesheets().isEmpty()) {
                dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
            }

            Platform.runLater(updateCalculations);

            dialog.setResultConverter(btn -> btn == buyButtonType ? qtySpinner.getValue() : null);

            Optional<Integer> qtyChosen = dialog.showAndWait();
            if (qtyChosen.isEmpty()) {
                return;
            }

            int qty = qtyChosen.get();
            Purchase p = new Purchase();
            p.setClientId(clientId);
            p.setBookIsbn(b.getIsbn());
            p.setQuantity(qty);
            p.setUnitPrice(finalUnitPrice);

            com.libraryplus.service.PurchaseService purchaseService = new com.libraryplus.service.PurchaseService();
            int id = purchaseService.processPurchase(p);

            // Update in-memory session user balance
            double totalSpent = finalUnitPrice * qty;
            u.setCardBalance(Math.round((u.getCardBalance() - totalSpent) * 100.0) / 100.0);
            Session.setCurrentUser(u);

            if (owner != null) {
                Toast.show(owner, "🎉 Purchase successful (Receipt #" + id + ") for " + qty + "x " + b.getTitle() + "!", 2500, "success");
            }
            refreshBooks();
            Optional<Book> reloaded = bookDao.findByIsbn(b.getIsbn());
            reloaded.ifPresent(this::showDetails);
            updateUserHeaderDisplay();
        } catch (Exception e) {
            logger.error("Failed to create purchase", e);
            if (owner != null) {
                Toast.show(owner, "Purchase failed: " + e.getMessage(), 2500, "error");
            }
        }
    }

    @FXML
    protected void onReturnBook(ActionEvent event) {
        Book b = booksListView.getSelectionModel().getSelectedItem();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (b == null) {
            if (owner != null) {
                Toast.show(owner, "Select a book to return.", 1800, "info");
            }
            return;
        }
        try {
            int clientId = ensureClientIdForCurrentUser();
            com.libraryplus.service.ReturnBookService returnService = new com.libraryplus.service.ReturnBookService();
            returnService.returnBook(b.getIsbn(), clientId);
            if (owner != null) {
                Toast.show(owner, "Book returned successfully: " + b.getTitle(), 2500, "success");
            }
            refreshBooks();
            Optional<Book> reloaded = bookDao.findByIsbn(b.getIsbn());
            reloaded.ifPresent(this::showDetails);
            updateUserHeaderDisplay();
        } catch (IllegalStateException e) {
            if (owner != null) {
                Toast.show(owner, e.getMessage(), 2500, "warning");
            }
        } catch (Exception e) {
            logger.error("Failed to return book", e);
            if (owner != null) {
                Toast.show(owner, "Return failed: " + e.getMessage(), 2200, "error");
            }
        }
    }

    @FXML
    protected void onTopUpBalance(ActionEvent event) {
        User u = Session.getCurrentUser();
        if (u == null) return;
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Top Up Wallet Balance");
        dialog.setHeaderText("Add funds to your LibraryPlus account\nCurrent Balance: " + String.format("%.2f DT", u.getCardBalance()));
        if (owner != null) dialog.initOwner(owner);

        ButtonType confirmButtonType = new ButtonType("Add Funds", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14));

        Label amountLabel = new Label("Select or enter amount to deposit (DT):");
        amountLabel.setStyle("-fx-font-weight: 600;");

        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        Button btn10 = new Button("+10 DT");
        Button btn20 = new Button("+20 DT");
        Button btn50 = new Button("+50 DT");
        Button btn100 = new Button("+100 DT");
        presetBox.getChildren().addAll(btn10, btn20, btn50, btn100);

        TextField customAmountField = new TextField("20.00");
        customAmountField.setPromptText("Enter amount in DT (e.g. 50.00)");

        btn10.setOnAction(e -> customAmountField.setText("10.00"));
        btn20.setOnAction(e -> customAmountField.setText("20.00"));
        btn50.setOnAction(e -> customAmountField.setText("50.00"));
        btn100.setOnAction(e -> customAmountField.setText("100.00"));

        Label previewLabel = new Label("New Balance will be: " + String.format("%.2f DT", u.getCardBalance() + 20.0));
        previewLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        customAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double val = Double.parseDouble(newVal.trim());
                if (val > 0) {
                    previewLabel.setText("New Balance will be: " + String.format("%.2f DT", u.getCardBalance() + val));
                    previewLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                } else {
                    previewLabel.setText("Please enter a positive amount.");
                    previewLabel.setStyle("-fx-text-fill: #ef4444;");
                }
            } catch (Exception ex) {
                previewLabel.setText("Invalid amount format.");
                previewLabel.setStyle("-fx-text-fill: #ef4444;");
            }
        });

        content.getChildren().addAll(amountLabel, presetBox, customAmountField, previewLabel);
        dialog.getDialogPane().setContent(content);

        if (owner != null && owner.getScene() != null && !owner.getScene().getStylesheets().isEmpty()) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    double amt = Double.parseDouble(customAmountField.getText().trim());
                    return amt > 0 ? amt : null;
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        });

        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                int clientId = ensureClientIdForCurrentUser();
                UserDao uDao = new UserDaoJdbc();
                Optional<User> freshOpt = uDao.findById(u.getId());
                User cur = freshOpt.orElse(u);

                double newBal = Math.round((cur.getCardBalance() + amount) * 100.0) / 100.0;
                cur.setCardBalance(newBal);
                uDao.updateUser(cur);
                Session.setCurrentUser(cur);

                // Record financial transaction
                try {
                    Transaction tx = new Transaction();
                    tx.setClientId(clientId);
                    tx.setAmount(amount);
                    tx.setReason("Wallet top-up (Deposit)");
                    tx.setResultingBalance(newBal);
                    new TransactionDaoJdbc().createTransaction(tx);
                } catch (Exception ignore) {}

                updateUserHeaderDisplay();
                Book selected = booksListView.getSelectionModel().getSelectedItem();
                if (selected != null) showDetails(selected);

                if (owner != null) {
                    Toast.show(owner, "Wallet credited with +" + String.format("%.2f", amount) + " DT! New Balance: " + String.format("%.2f", newBal) + " DT", 2500, "success");
                }
            } catch (Exception ex) {
                logger.error("Failed to top up balance", ex);
                if (owner != null) {
                    Toast.show(owner, "Top up failed: " + ex.getMessage(), 2200, "error");
                }
            }
        });
    }

    @FXML
    protected void onMyLoans(ActionEvent event) {
        User u = Session.getCurrentUser();
        if (u == null) return;
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);

        try {
            int clientId = ensureClientIdForCurrentUser();
            List<Loan> activeLoans = loanDao.findActiveByClientId(clientId);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("My Borrowed Books");
            dialog.setHeaderText("Active Loans for " + u.getFullName() + " (" + activeLoans.size() + " active)");
            if (owner != null) dialog.initOwner(owner);

            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox box = new VBox(12);
            box.setPrefWidth(660);
            box.setPrefHeight(380);
            box.setPadding(new Insets(12));

            if (activeLoans.isEmpty()) {
                Label emptyLabel = new Label("✨ You have no active loans right now. Browse books and click 'Borrow' to start reading!");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-padding: 20; -fx-opacity: 0.8;");
                box.getChildren().add(emptyLabel);
            } else {
                TableView<Loan> table = new TableView<>();
                table.setPrefHeight(320);

                TableColumn<Loan, String> titleCol = new TableColumn<>("Book Title");
                titleCol.setPrefWidth(240);
                titleCol.setCellValueFactory(cellData -> {
                    String isbn = cellData.getValue().getBookIsbn();
                    try {
                        Optional<Book> b = bookDao.findByIsbn(isbn);
                        return new SimpleStringProperty(b.map(Book::getTitle).orElse(isbn));
                    } catch (Exception ex) {
                        return new SimpleStringProperty(isbn);
                    }
                });

                TableColumn<Loan, String> borrowCol = new TableColumn<>("Borrow Date");
                borrowCol.setPrefWidth(100);
                borrowCol.setCellValueFactory(cellData -> {
                    LocalDateTime bd = cellData.getValue().getBorrowDate();
                    return new SimpleStringProperty(bd != null ? bd.toLocalDate().toString() : "N/A");
                });

                TableColumn<Loan, String> dueCol = new TableColumn<>("Due Date");
                dueCol.setPrefWidth(100);
                dueCol.setCellValueFactory(cellData -> {
                    LocalDateTime ed = cellData.getValue().getExpectedReturnDate();
                    return new SimpleStringProperty(ed != null ? ed.toLocalDate().toString() : "N/A");
                });

                TableColumn<Loan, String> statusCol = new TableColumn<>("Status");
                statusCol.setPrefWidth(110);
                statusCol.setCellValueFactory(cellData -> {
                    Loan l = cellData.getValue();
                    LocalDateTime ed = l.getExpectedReturnDate();
                    LocalDateTime now = LocalDateTime.now();
                    if (ed != null && now.isAfter(ed)) {
                        long days = java.time.Duration.between(ed, now).toDays();
                        return new SimpleStringProperty("⚠️ Overdue (" + Math.max(1, days) + "d)");
                    } else if (ed != null) {
                        long days = java.time.Duration.between(now, ed).toDays();
                        return new SimpleStringProperty("Active (" + days + "d left)");
                    }
                    return new SimpleStringProperty("Active");
                });

                TableColumn<Loan, Void> actionCol = new TableColumn<>("Action");
                actionCol.setPrefWidth(90);
                actionCol.setCellFactory(param -> new TableCell<>() {
                    private final Button returnBtn = new Button("↩️ Return");
                    {
                        returnBtn.getStyleClass().add("button-primary");
                        returnBtn.setOnAction(e -> {
                            Loan loan = getTableView().getItems().get(getIndex());
                            try {
                                com.libraryplus.service.ReturnBookService returnService = new com.libraryplus.service.ReturnBookService();
                                returnService.returnBook(loan.getBookIsbn(), clientId);
                                getTableView().getItems().remove(loan);
                                refreshBooks();
                                updateUserHeaderDisplay();
                                Book sel = booksListView.getSelectionModel().getSelectedItem();
                                if (sel != null) showDetails(sel);
                                if (owner != null) {
                                    Toast.show(owner, "Book returned successfully!", 2000, "success");
                                }
                            } catch (Exception exReturn) {
                                if (owner != null) {
                                    Toast.show(owner, "Failed to return: " + exReturn.getMessage(), 2200, "error");
                                }
                            }
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : returnBtn);
                    }
                });

                table.getColumns().addAll(titleCol, borrowCol, dueCol, statusCol, actionCol);
                table.setItems(FXCollections.observableArrayList(activeLoans));
                box.getChildren().add(table);
            }

            dialog.getDialogPane().setContent(box);
            if (owner != null && owner.getScene() != null && !owner.getScene().getStylesheets().isEmpty()) {
                dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
            }
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to show active loans", e);
            if (owner != null) {
                Toast.show(owner, "Could not load active loans: " + e.getMessage(), 2200, "error");
            }
        }
    }

    @FXML
    protected void onToggleTheme(ActionEvent event) {
        Scene scene = (logoutButton != null && logoutButton.getScene() != null) ? logoutButton.getScene() : null;
        if (scene == null && welcomeLabel != null && welcomeLabel.getScene() != null) {
            scene = welcomeLabel.getScene();
        }
        if (scene == null) return;
        String current = com.libraryplus.util.ThemeManager.loadThemePreference();
        String next;
        if ("Catppuccin".equalsIgnoreCase(current)) {
            next = "Mayor Touch";
        } else if ("Mayor Touch".equalsIgnoreCase(current)) {
            next = "Tokyo Night";
        } else {
            next = "Catppuccin";
        }
        com.libraryplus.util.ThemeManager.saveThemePreference(next);
        com.libraryplus.util.ThemeManager.applyThemeWithCrossfade(scene, next);
        updateUserHeaderDisplay();
    }

    @FXML
    private javafx.scene.layout.StackPane resultsStackPane;
    @FXML
    private ScrollPane categorizedPane;
    @FXML
    private VBox categoriesScrollContent;
    @FXML
    private VBox searchResultsPane;

    @FXML
    protected void onToggleMusic(ActionEvent event) {
        com.libraryplus.util.AudioManager audio = com.libraryplus.util.AudioManager.getInstance();
        audio.toggleMute();
        updateMusicButtonIcon();
    }

    private void updateMusicButtonIcon() {
        if (musicToggleButton != null) {
            com.libraryplus.util.AudioManager audio = com.libraryplus.util.AudioManager.getInstance();
            if (audio.isMuted()) {
                musicToggleButton.setText("🔇");
            } else {
                musicToggleButton.setText("🔊");
            }
        }
    }

    @FXML
    protected void onLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES,
                ButtonType.NO);
        alert.setHeaderText(null);
        alert.setTitle("Logout Confirmation");
        
        try {
            String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
            if (pref == null)
                pref = "Catppuccin";
            DialogPane dialogPane = alert.getDialogPane();
            if (dialogPane.getScene() != null) {
                com.libraryplus.util.ThemeManager.applyTheme(dialogPane.getScene(), pref);
            }
        } catch (Exception ignored) {
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                        javafx.util.Duration.millis(600),
                        logoutButton);
                rt.setByAngle(360);
                rt.setOnFinished(ev -> {
                    Session.clear();
                    Stage s = (Stage) logoutButton.getScene().getWindow();
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root, 800, 600);

                        
                        String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                        if (pref == null)
                            pref = "Catppuccin";
                        com.libraryplus.util.ThemeManager.applyTheme(scene, pref);

                        s.setScene(scene);
                        s.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        s.close(); 
                    }
                });
                rt.play();
            }
        });
    }

    @FXML
    protected void onShowUsers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_list.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("User List");
            Scene scene = new Scene(root, 600, 400);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to open User List", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Unable to open User List.", 2200, "error");
            }
        }
    }

    private void refreshCategorizedView() {
        categoriesScrollContent.getChildren().clear();
        try {
            List<String> categories = bookDao.findAllCategories();
            for (String cat : categories) {
                
                List<Book> books = bookDao.findByCategory(cat, 0, 6);
                if (books.isEmpty())
                    continue;

                VBox catSection = new VBox(8);
                Label catTitle = new Label(cat);
                catTitle.getStyleClass().add("section-title");
                catTitle.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 4 0;");

                HBox booksRow = new HBox(12);
                booksRow.setStyle("-fx-padding: 0 0 12 0;");

                for (Book b : books) {
                    VBox card = createFeaturedCard(b); 
                    
                    booksRow.getChildren().add(card);
                }

                catSection.getChildren().addAll(catTitle, booksRow);
                categoriesScrollContent.getChildren().add(catSection);
            }
        } catch (Exception e) {
            logger.error("Failed to load categorized view", e);
        }
    }



    @FXML
    protected void onChat(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/libraryplus/ui/chatbot_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("LibraryPlus AI Assistant");
            Scene scene = new Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            stage.setScene(scene);
            stage.initModality(Modality.NONE); 
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to open Chatbot", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Unable to open Chatbot.", 2200, "error");
            }
        }
    }

    @FXML
    protected void onInbox(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_inbox.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Admin Inbox");
            Scene scene = new Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to open Inbox", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Unable to open Inbox.", 2200, "error");
            }
        }
    }

    @FXML
    protected void onRequestBook(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Request Book");
        dialog.setHeaderText("Request a book");
        dialog.setContentText("Enter book title/author:");
        dialog.showAndWait().ifPresent(text -> {
            if (!text.isBlank()) {
                try {
                    com.libraryplus.dao.AdminMessageDao dao = new com.libraryplus.dao.jdbc.AdminMessageDaoJdbc();
                    com.libraryplus.model.AdminMessage msg = new com.libraryplus.model.AdminMessage();
                    User u = Session.getCurrentUser();
                    msg.setClientId(ensureClientIdForCurrentUser());
                    msg.setSenderEmail(u.getEmail());
                    msg.setSubject("Book Request");
                    msg.setContent("I would like to request the book: " + text);
                    msg.setStatus("UNREAD");
                    dao.create(msg);
                    Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow()
                            : null);
                    if (owner != null)
                        Toast.show(owner, "Request sent!", 1500, "success");
                } catch (Exception e) {
                    logger.error("Failed to send request", e);
                }
            }
        });
    }

    @FXML
    protected void onSubscribe(ActionEvent event) {
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        try {
            int clientId = ensureClientIdForCurrentUser();
            com.libraryplus.dao.SubscriptionDao subDao = new com.libraryplus.dao.jdbc.SubscriptionDaoJdbc();
            Optional<com.libraryplus.model.Subscription> active = subDao.findActiveByClientId(clientId);
            if (active.isPresent()) {
                if (owner != null)
                    Toast.show(owner, "You already have an active subscription.", 2000, "info");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Subscribe for 3 months for 20 DT?", ButtonType.YES,
                    ButtonType.NO);
            confirm.setHeaderText("Confirm Subscription");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    try {
                        
                        com.libraryplus.dao.UserDao userDao = new com.libraryplus.dao.jdbc.UserDaoJdbc();
                        User u = Session.getCurrentUser();
                        if (u.getCardBalance() < 20.0) {
                            if (owner != null)
                                Toast.show(owner, "Insufficient balance.", 2000, "error");
                            return;
                        }
                        double newBalance = Math.round((u.getCardBalance() - 20.0) * 100.0) / 100.0;
                        u.setCardBalance(newBalance);
                        userDao.updateUser(u);

                        // Record financial transaction
                        try {
                            com.libraryplus.model.Transaction tx = new com.libraryplus.model.Transaction();
                            tx.setClientId(clientId);
                            tx.setAmount(20.0);
                            tx.setReason("3-Month Premium Membership Subscription");
                            tx.setResultingBalance(newBalance);
                            new com.libraryplus.dao.jdbc.TransactionDaoJdbc().createTransaction(tx);
                        } catch (Exception exTx) {
                            logger.warn("Could not log subscription transaction: {}", exTx.getMessage());
                        }

                        // Upgrade client status
                        try {
                            clientDao.updateMembershipType(clientId, "PREMIUM");
                        } catch (Exception exClient) {
                            logger.warn("Could not update client membership type: {}", exClient.getMessage());
                        }

                        subDao.createSubscription(clientId, java.time.LocalDate.now(),
                                java.time.LocalDate.now().plusMonths(3));
                        if (owner != null)
                            Toast.show(owner, "Subscribed to Premium successfully!", 2200, "success");
                        updateUserHeaderDisplay();
                    } catch (Exception e) {
                        logger.error("Subscription failed", e);
                        if (owner != null)
                            Toast.show(owner, "Subscription failed: " + e.getMessage(), 2000, "error");
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to check subscription", e);
        }
    }

    @FXML
    protected void onViewSubscriptions(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/subscription_view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Subscription Status");
            Scene scene = new Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to open Subscription View", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Unable to open Subscription View.", 2200, "error");
            }
        }
    }

    @FXML
    public void onShowAllClicked(ActionEvent event) {
        try {
            
            if (searchField != null) {
                searchField.setText("");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/all_books.fxml"));
            Parent root = loader.load();
            AllBooksController ctrl = loader.getController();
            if (ctrl == null) {
                logger.error("AllBooksController is null after loading FXML");
                Stage owner = getOwnerStage(event);
                if (owner != null) {
                    Toast.show(owner, "Unable to open All Books window (controller missing).", 2200, "error");
                }
                return;
            }
            ctrl.setBookDao(bookDao); 

            Stage dialog = new Stage();
            
            Stage owner = getOwnerStage(event);
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("All Books");
            Scene scene = new Scene(root);
            
            try {
                String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                if (pref == null)
                    pref = "Catppuccin";
                com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            } catch (Exception ignored) {
            }
            dialog.setScene(scene);
            dialog.setOnHidden(ev -> refreshBooks()); 
            dialog.showAndWait();
        } catch (Exception e) {
            
            
            logger.error("Failed to open All Books window", e);
            Stage owner = getOwnerStage(event);
            if (owner != null) {
                Toast.show(owner, "Unable to open All Books window: " + e.getMessage(), 2200, "error");
            }
        }
    }

    
    public void loadByCategory(String category) {
        try {
            pageOffset = 0;
            String cat = category;
            if (cat == null) {
                cat = "";
            }
            final String finalCat = cat;
            List<Book> books = bookDao == null ? java.util.Collections.emptyList()
                    : bookDao.findByCategory(cat, 0, pageSize);
            ObservableList<Book> items = FXCollections.observableArrayList();
            items.addAll(books);
            
            if (searchField != null) {
                searchField.setText(cat);
            }
            pageOffset += (books == null ? 0 : books.size());
            
            if (loadMoreButton != null) {
                loadMoreButton.setDisable(books == null || books.size() < pageSize);
            }

            
            try {
                javafx.application.Platform.runLater(() -> {
                    try {
                        if (booksListView == null) {
                            
                            return;
                        }
                        javafx.animation.FadeTransition ftOut = new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(120), booksListView);
                        ftOut.setFromValue(1.0);
                        ftOut.setToValue(0.0);
                        ftOut.setOnFinished(ev -> {
                            try {
                                booksListView.setItems(items);
                                
                                booksListView.getSelectionModel().clearSelection();
                                javafx.animation.FadeTransition ftIn = new javafx.animation.FadeTransition(
                                        javafx.util.Duration.millis(280), booksListView);
                                ftIn.setFromValue(0.0);
                                ftIn.setToValue(1.0);
                                ftIn.setOnFinished(ev2 -> {
                                    
                                    try {
                                        Stage owner = getOwnerStage(null);
                                        if (owner != null) {
                                            Toast.show(owner, "Filtered by: " + finalCat, 1400, "info");
                                        }
                                    } catch (Exception toastEx) {
                                        
                                    }
                                });
                                ftIn.play();
                            } catch (Exception ex) {
                                
                                try {
                                    booksListView.setItems(items);
                                } catch (Exception setEx) {
                                    
                                }
                            }
                        });
                        ftOut.play();
                    } catch (Exception ex) {
                        
                        try {
                            booksListView.setItems(items);
                            Stage owner = getOwnerStage(null);
                            if (owner != null) {
                                Toast.show(owner, "Filtered by: " + finalCat, 1400, "info");
                            }
                        } catch (Exception fallbackEx) {
                            
                        }
                    }
                });
            } catch (Exception ex) {
                
                try {
                    booksListView.setItems(items);
                    Stage owner = getOwnerStage(null);
                    if (owner != null) {
                        Toast.show(owner, "Filtered by: " + finalCat, 1400, "info");
                    }
                } catch (Exception lastEx) {
                    
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load books by category", e);
        }
    }

    
    private Stage getOwnerStage(javafx.event.ActionEvent event) {
        try {
            if (event != null && event.getSource() instanceof javafx.scene.Node) {
                javafx.scene.Node n = (javafx.scene.Node) event.getSource();
                if (n.getScene() != null && n.getScene().getWindow() instanceof Stage) {
                    return (Stage) n.getScene().getWindow();
                }
            }
        } catch (Exception ignored) {
            
        }
        try {
            if (logoutButton != null && logoutButton.getScene() != null
                    && logoutButton.getScene().getWindow() instanceof Stage) {
                return (Stage) logoutButton.getScene().getWindow();
            }
        } catch (Exception ignored) {
            
        }
        try {
            if (welcomeLabel != null && welcomeLabel.getScene() != null
                    && welcomeLabel.getScene().getWindow() instanceof Stage) {
                return (Stage) welcomeLabel.getScene().getWindow();
            }
        } catch (Exception ignored) {
            
        }
        return null;
    }

}
