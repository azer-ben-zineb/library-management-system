package com.libraryplus.ui;
import com.libraryplus.app.Session;
import com.libraryplus.model.User;
import com.libraryplus.presenter.LoginPresenter;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.io.PrintWriter;
import java.io.StringWriter;
 import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private javafx.scene.control.ToggleButton loginThemeToggle;
    @FXML private Label messageLabel; 
    private javafx.stage.Popup loginPreviewPopup;
    private final LoginPresenter presenter = new LoginPresenter();
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    @FXML
    public void initialize() {
        
        try {
            String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
            final String initialTheme = (pref == null) ? "Tokyo Night" : pref;
            updateThemeButtonText(initialTheme);
            if (loginThemeToggle != null) {
                loginThemeToggle.setOnAction(ev -> {
                    String current = com.libraryplus.util.ThemeManager.loadThemePreference();
                    if (current == null) current = "Tokyo Night";
                    String next = getNextTheme(current);
                    updateThemeButtonText(next);
                    Scene s = getSceneSafely();
                    com.libraryplus.util.ThemeManager.applyThemeWithCrossfade(s, next);
                    com.libraryplus.util.ThemeManager.saveThemePreference(next);
                });
                
                loginThemeToggle.setOnMouseEntered(ev -> showLoginPreview());
                loginThemeToggle.setOnMouseExited(ev -> hideLoginPreview());
                
                javafx.application.Platform.runLater(() -> {
                    Scene s = getSceneSafely();
                    if (s != null) com.libraryplus.util.ThemeManager.applyTheme(s, initialTheme);
                });
            }
        } catch (Exception ex) {
            logger.warn("Failed to initialize login theme toggle", ex);
        }
    }
    
    private String getNextTheme(String current) {
        if ("Tokyo Night".equalsIgnoreCase(current)) {
            return "Catppuccin";
        } else if ("Catppuccin".equalsIgnoreCase(current)) {
            return "Mayor Touch";
        } else {
            return "Tokyo Night";
        }
    }
    
    private void updateThemeButtonText(String theme) {
        if (loginThemeToggle == null) return;
        if ("Catppuccin".equalsIgnoreCase(theme)) {
            loginThemeToggle.setText("☀️");
        } else if ("Mayor Touch".equalsIgnoreCase(theme)) {
            loginThemeToggle.setText("⭐");
        } else {
            loginThemeToggle.setText("🌙");
        }
    }
    
    private Scene getSceneSafely() {
        if (loginButton != null && loginButton.getScene() != null) return loginButton.getScene();
        if (emailField != null && emailField.getScene() != null) return emailField.getScene();
        if (passwordField != null && passwordField.getScene() != null) return passwordField.getScene();
        return null;
    }
    @FXML
    protected void onLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        Optional<User> user = presenter.login(email, password);
        if (user.isPresent()) {
            User u = user.get();
            Session.setCurrentUser(u);
            messageLabel.setText("Login successful. Welcome " + u.getFullName());
            javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(javafx.util.Duration.millis(600), loginButton);
            rt.setByAngle(360);
            rt.setOnFinished(ev -> {
                try {
                    java.net.URL fxmlUrl = getClass().getResource("/fxml/dashboard.fxml");
                    if (fxmlUrl == null) {
                        throw new IOException("Dashboard FXML resource '/fxml/dashboard.fxml' not found on classpath.");
                    }
                    FXMLLoader loader = new FXMLLoader(fxmlUrl);
                    Parent root = loader.load();
                    Stage stage = new Stage();
                    stage.setTitle("LibraryPlus - Dashboard");
                    Scene scene = new Scene(root, 800, 600);
                    
                    try {
                        scene.setFill(javafx.scene.paint.Color.web("#1e1e2e"));
                        if (!root.getStyleClass().contains("dashboard-root")) root.getStyleClass().add("dashboard-root");
                        
                        root.setStyle("-fx-background-color: #1e1e2e;");
                    } catch (Exception ex) {
                        logger.warn("Failed to set dashboard root background: {}", ex.getMessage());
                    }
                    
                    try {
                        String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
                        if (pref == null) pref = "Catppuccin";
                        com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
                        URL dashCssUrl = getClass().getResource("/css/dashboard.css");
                        if (dashCssUrl != null) scene.getStylesheets().add(dashCssUrl.toExternalForm());
                    } catch (Exception ex) {
                        logger.warn("Failed to load dashboard CSS or theme: {}", ex.getMessage());
                    }
                     stage.setScene(scene);
                     stage.initModality(Modality.NONE);
                     stage.show();
                    
                    Stage s = (Stage) loginButton.getScene().getWindow();
                    s.close();
                } catch (Exception e) {
                    
                    logger.error("Unable to open dashboard", e);
                    messageLabel.setText("Unable to open dashboard: " + e.getMessage());
                    Stage owner = (Stage) (loginButton.getScene() != null ? loginButton.getScene().getWindow() : null);
                    if (owner != null) Toast.show(owner, "Unable to open dashboard: " + e.getMessage());
                    
                    e.printStackTrace();
                    
                    try {
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        java.nio.file.Path tmp = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "dashboard-error-" + timestamp + ".log");
                        try (java.io.PrintWriter pw = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(tmp))) {
                            e.printStackTrace(pw);
                        }
                        String msg = "Saved full error to: " + tmp.toAbsolutePath();
                        logger.info(msg);
                        messageLabel.setText(messageLabel.getText() + " — details: " + tmp.toAbsolutePath());
                        if (owner != null) Toast.show(owner, msg, 4000, "info");
                    } catch (Exception ex3) {
                        logger.error("Failed to write error log", ex3);
                    }
                    
                    try {
                        java.nio.file.Path projectLog = java.nio.file.Paths.get(System.getProperty("user.dir"), "dashboard-load-error.log");
                        try (java.io.PrintWriter pw = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(projectLog))) {
                            e.printStackTrace(pw);
                        }
                        String msg2 = "Saved full error to project file: " + projectLog.toAbsolutePath();
                        System.err.println("=== DASHBOARD LOAD ERROR (project log) ===");
                        System.err.println(msg2);
                        if (owner != null) Toast.show(owner, msg2, 4000, "info");
                    } catch (Exception ex4) {
                        logger.error("Failed to write project error log", ex4);
                    }
                    
                    try {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Dashboard load error");
                        alert.setHeaderText("Unable to open dashboard: " + e.getClass().getSimpleName());
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String exceptionText = sw.toString();

                        TextArea textArea = new TextArea(exceptionText);
                        textArea.setEditable(false);
                        textArea.setWrapText(true);
                        textArea.setMaxWidth(Double.MAX_VALUE);
                        textArea.setMaxHeight(Double.MAX_VALUE);
                        alert.getDialogPane().setExpandableContent(textArea);
                        
                        Platform.runLater(() -> {
                            try {
                                alert.showAndWait();
                            } catch (Exception ignore) {}
                        });
                    } catch (Exception ex2) {
                        logger.error("Failed to show exception dialog", ex2);
                    }
                }
            });
            rt.play();
        } else {
            messageLabel.setText("Invalid credentials.");
            playShake(loginButton);
        }
    }
    private void playShake(javafx.scene.Node node) {
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), node);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }
    @FXML
    protected void onRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Register");
            Scene scene = new Scene(root);
            
            String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
            if (pref == null) pref = "Tokyo Night";
            com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Unable to open registration window", e);
            messageLabel.setText("Unable to open registration window.");
            Stage owner = (Stage) (loginButton.getScene() != null ? loginButton.getScene().getWindow() : null);
            if (owner != null) Toast.show(owner, "Unable to open registration window.");
        }
    }
    private void showLoginPreview() {
        try {
            String current = com.libraryplus.util.ThemeManager.loadThemePreference();
            if (current == null) current = "Tokyo Night";
            String preview = getNextTheme(current);
            if (loginPreviewPopup != null && loginPreviewPopup.isShowing()) return;
            loginPreviewPopup = new javafx.stage.Popup();
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(6);
            box.setStyle("-fx-padding:10; -fx-background-radius:8; -fx-border-radius:8; -fx-border-color: rgba(255,255,255,0.06);");
            javafx.scene.control.Label title = new javafx.scene.control.Label(preview + " preview");
            javafx.scene.control.Label sample = new javafx.scene.control.Label("LibraryPlus sample title");
            javafx.scene.control.Button btn = new javafx.scene.control.Button("Primary button");
            
            if ("Catppuccin".equalsIgnoreCase(preview)) {
                box.setStyle(box.getStyle() + " -fx-background-color: #1f1d2e;");
                title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                sample.setStyle("-fx-text-fill: white;");
                btn.setStyle("-fx-background-color: linear-gradient(#2b2330,#1d1a29); -fx-text-fill: white; -fx-background-radius:6;");
            } else if ("Mayor Touch".equalsIgnoreCase(preview)) {
                box.setStyle(box.getStyle() + " -fx-background-color: #172F4F; -fx-border-color: #00E6FF; -fx-border-width: 1;");
                title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                sample.setStyle("-fx-text-fill: white;");
                btn.setStyle("-fx-background-color: linear-gradient(#233D66, #11233C); -fx-text-fill: white; -fx-border-color: #00E6FF; -fx-border-width: 1; -fx-background-radius:6;");
            } else {
                box.setStyle(box.getStyle() + " -fx-background-color: #0b1220;");
                title.setStyle("-fx-text-fill: #e6eef8; -fx-font-weight: bold;");
                sample.setStyle("-fx-text-fill: #e6eef8;");
                btn.setStyle("-fx-background-color: linear-gradient(#0f1724,#071028); -fx-text-fill: #e6eef8; -fx-background-radius:6;");
            }
            box.getChildren().addAll(title, sample, btn);
            loginPreviewPopup.getContent().add(box);
            
            javafx.geometry.Bounds b = loginThemeToggle.localToScreen(loginThemeToggle.getBoundsInLocal());
            if (b != null) {
                double x = b.getMinX() - 10;
                double y = b.getMaxY() + 6;
                loginPreviewPopup.show(loginThemeToggle.getScene().getWindow(), x, y);
            }
        } catch (Exception e) {
            logger.debug("Failed to show login theme preview", e);
        }
    }
    private void hideLoginPreview() {
        try {
            if (loginPreviewPopup != null) {
                loginPreviewPopup.hide();
                loginPreviewPopup = null;
            }
        } catch (Exception ignored) {}
    }
}
