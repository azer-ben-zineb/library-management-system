package com.libraryplus.util;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static final Path PREF_PATH = Paths.get(System.getProperty("user.home"), ".libraryplus", "theme.pref");

    public static void applyTheme(Scene scene, String name) {
        if (scene == null) return;
        try {
            scene.getStylesheets().removeIf(s -> s.contains("catppuccin-mocha.css") || s.contains("tokyo-night.css") || s.contains("mayor-touch.css"));
            String resource = "/css/tokyo-night.css";
            if ("Catppuccin".equalsIgnoreCase(name)) {
                resource = "/css/catppuccin-mocha.css";
            } else if ("Mayor Touch".equalsIgnoreCase(name)) {
                resource = "/css/mayor-touch.css";
            }
            var url = ThemeManager.class.getResource(resource);
            if (url != null) scene.getStylesheets().add(url.toExternalForm());
        } catch (Exception e) {
            logger.warn("Failed to apply theme {}: {}", name, e.getMessage());
        }
    }

    public static void applyThemeWithCrossfade(Scene scene, String name) {
        if (scene == null) return;
        Parent root = scene.getRoot();
        if (root == null) return;
        
        Platform.runLater(() -> {
            try {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(220), root);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> {
                    applyTheme(scene, name);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(220), root);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                    
                    try {
                        String icon = "🌙";
                        if ("Catppuccin".equalsIgnoreCase(name)) {
                            icon = "☀️";
                        } else if ("Mayor Touch".equalsIgnoreCase(name)) {
                            icon = "⭐";
                        }
                        Popup popup = new Popup();
                        Label lbl = new Label(icon);
                        lbl.setStyle("-fx-font-size:24px; -fx-background-color: rgba(0,0,0,0.45); -fx-text-fill: white; -fx-padding:10; -fx-background-radius:8;");
                        popup.getContent().add(lbl);
                        
                        var wind = scene.getWindow();
                        if (wind != null) {
                            double x = wind.getX() + wind.getWidth() - 220;
                            double y = wind.getY() + wind.getHeight() - 120;
                            popup.show(wind, x, y);
                            FadeTransition pfade = new FadeTransition(Duration.millis(900), lbl);
                            pfade.setFromValue(0.0);
                            pfade.setToValue(1.0);
                            pfade.setOnFinished(e2 -> {
                                
                                FadeTransition phide = new FadeTransition(Duration.millis(500), lbl);
                                phide.setFromValue(1.0);
                                phide.setToValue(0.0);
                                phide.setDelay(Duration.millis(700));
                                phide.setOnFinished(e3 -> popup.hide());
                                phide.play();
                            });
                            pfade.play();
                        }
                    } catch (Exception ex) {
                        logger.debug("Failed to show theme popup icon", ex);
                    }
                });
                fadeOut.play();
            } catch (Exception e) {
                logger.warn("Crossfade theme error: {}", e.getMessage());
                applyTheme(scene, name);
            }
        });
    }

    public static void saveThemePreference(String name) {
        try {
            if (name == null) return;
            Files.createDirectories(PREF_PATH.getParent());
            Files.writeString(PREF_PATH, name);
        } catch (Exception e) {
            logger.warn("Failed to save theme preference", e);
        }
    }

    public static String loadThemePreference() {
        try {
            if (Files.exists(PREF_PATH)) return Files.readString(PREF_PATH).strip();
        } catch (Exception e) {
            logger.warn("Failed to load theme preference", e);
        }
        
        return "Catppuccin";
    }
}
