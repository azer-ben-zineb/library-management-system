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
    private Button musicToggleButton;
    @FXML
    private Slider volumeSlider;

    
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
    private TextField newReviewField;
    @FXML
    private TextField unitPriceField;
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

            
            User u = Session.getCurrentUser();
            if (u != null) {
                String role = u.getRoleId() == 1 ? "ADMIN" : "CLIENT";
                welcomeLabel.setText("Welcome, " + u.getFullName() + " (" + role + ")");
            } else {
                welcomeLabel.setText("Welcome (no user)");
            }

            
            if (u != null && u.getRoleId() == 2) { 
                if (chatButton != null)
                    chatButton.setVisible(true);
                if (inboxButton != null)
                    inboxButton.setVisible(false);
                if (requestBookButton != null)
                    requestBookButton.setVisible(true);
                if (subscribeButton != null)
                    subscribeButton.setVisible(true);
                if (viewSubscriptionsButton != null)
                    viewSubscriptionsButton.setVisible(false);
                if (usersButton != null) {
                    usersButton.setVisible(false);
                    usersButton.setManaged(false);
                }
            } else { 
                if (chatButton != null)
                    chatButton.setVisible(false);
                if (inboxButton != null)
                    inboxButton.setVisible(true);
                if (requestBookButton != null)
                    requestBookButton.setVisible(false);
                if (subscribeButton != null)
                    subscribeButton.setVisible(false);
                if (viewSubscriptionsButton != null)
                    viewSubscriptionsButton.setVisible(true);
                if (usersButton != null) {
                    usersButton.setVisible(true);
                    usersButton.setManaged(true);
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
            unitPriceField.setText("");
            return;
        }
        detailTitle.setText(b.getTitle());
        detailAuthor.setText(b.getAuthor() == null ? "" : b.getAuthor());
        detailCategory.setText(b.getCategory() == null ? "" : b.getCategory());
        detailIsbn.setText(b.getIsbn() == null ? "" : b.getIsbn());
        detailStock.setText("📦 In Stock: " + b.getStock() + " copies");
        detailPrice.setText("💰 Price: " + String.format("%.2f", b.getPrice()) + " dt");
        detailDescription.setText(b.getDescription() == null ? "" : b.getDescription());
        
        loadReviews(b.getIsbn());
        
        unitPriceField.setText("9.99");
    }

    private void loadReviews(String isbn) {
        try {
            List<Comment> comments = commentDao.findByBook(isbn);
            ObservableList<String> items = FXCollections.observableArrayList();
            for (Comment c : comments) {
                items.add(c.getComment());
            }
            reviewsListView.setItems(items);
        } catch (Exception e) {
            logger.warn("Failed to load comments", e);
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
            });
            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open Add Book dialog", e);
            Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
            if (owner != null) {
                Toast.show(owner, "Unable to open Add Book window.", 2200, "error");
            }
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

    private int ensureClientIdForCurrentUser() throws Exception {
        User u = Session.getCurrentUser();
        if (u == null) {
            throw new IllegalStateException("No logged-in user");
        }
        
        Optional<Client> oc = clientDao.findByUserId(u.getId());
        if (oc.isPresent()) {
            return oc.get().getId();
        }
        
        Client c = new Client();
        c.setUserId(u.getId());
        c.setPhone(u.getPhone());
        c.setFirstName(u.getFullName());
        c.setLastName("");
        c.setMembershipType("STANDARD");
        int created = clientDao.createClient(c);
        if (created > 0) {
            return created;
        }
        throw new IllegalStateException("Unable to create client record");
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
            Loan loan = new Loan();
            loan.setBookIsbn(b.getIsbn());
            loan.setClientId(clientId);
            loan.setBorrowDate(LocalDateTime.now());
            loan.setExpectedReturnDate(LocalDateTime.now().plusWeeks(3));
            int id = loanDao.createLoan(loan);
            if (owner != null) {
                Toast.show(owner, "Borrow recorded (id=" + id + ") for: " + b.getTitle(), 2200, "success");
            }
            refreshBooks();
        } catch (Exception e) {
            logger.error("Failed to create loan", e);
            if (owner != null) {
                Toast.show(owner, "Borrow failed: " + e.getMessage(), 2200, "error");
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
        try {
            int clientId = ensureClientIdForCurrentUser();
            Purchase p = new Purchase();
            p.setClientId(clientId);
            p.setBookIsbn(b.getIsbn());
            p.setQuantity(1);
            p.setUnitPrice(9.99); 

            com.libraryplus.service.PurchaseService purchaseService = new com.libraryplus.service.PurchaseService();
            int id = purchaseService.processPurchase(p);

            if (owner != null) {
                Toast.show(owner, "Purchase recorded (id=" + id + ") for: " + b.getTitle(), 2200, "success");
            }
            refreshBooks();
        } catch (Exception e) {
            logger.error("Failed to create purchase", e);
            if (owner != null) {
                Toast.show(owner, "Purchase failed: " + e.getMessage(), 2200, "error");
            }
        }
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
    protected void onSubmitReview(ActionEvent event) {
        Book b = booksListView.getSelectionModel().getSelectedItem();
        Stage owner = (Stage) (logoutButton.getScene() != null ? logoutButton.getScene().getWindow() : null);
        if (b == null) {
            if (owner != null) {
                Toast.show(owner, "Select a book to review.", 1800, "info");
            }
            return;
        }
        String text = newReviewField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            int clientId = ensureClientIdForCurrentUser();
            Comment c = new Comment();
            c.setBookIsbn(b.getIsbn());
            c.setClientId(clientId);
            c.setComment(text.trim());
            commentDao.createComment(c);
            newReviewField.clear();
            loadReviews(b.getIsbn());
            if (owner != null) {
                Toast.show(owner, "Review submitted.", 1600, "success");
            }
        } catch (Exception e) {
            logger.error("Failed to submit review", e);
            if (owner != null) {
                Toast.show(owner, "Failed to submit review: " + e.getMessage(), 2200, "error");
            }
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
                        u.setCardBalance(u.getCardBalance() - 20.0);
                        userDao.updateUser(u);

                        
                        subDao.createSubscription(clientId, java.time.LocalDate.now(),
                                java.time.LocalDate.now().plusMonths(3));
                        if (owner != null)
                            Toast.show(owner, "Subscribed successfully!", 2000, "success");
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
