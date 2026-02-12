package com.libraryplus.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.stage.Window;
import java.util.ArrayList;
import java.util.List;

public class Toast {
    private static final List<Stage> ACTIVE_TOASTS = new ArrayList<>();

    public static void show(Stage owner, String message) {
        show(owner, message, 2200, "");
    }

    public static void show(Stage owner, String message, int durationMs) {
        show(owner, message, durationMs, "");
    }

    public static void show(Stage owner, String message, int durationMs, String typeClass) {
        if (owner == null) return;
        Stage t = new Stage();
        t.initOwner(owner);
        t.initStyle(StageStyle.TRANSPARENT);
        t.initModality(Modality.NONE);

        Label lbl = new Label(message);
        
        lbl.setFont(new Font(13));
        lbl.setPadding(new Insets(10, 14, 10, 14));
        lbl.getStyleClass().add("toast-label");

        StackPane root = new StackPane(lbl);
        root.getStyleClass().add("toast-root");
        if (typeClass != null && !typeClass.isBlank()) root.getStyleClass().add(typeClass);
        root.setOpacity(0);
        root.setPickOnBounds(false);
        root.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.6)));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        
        try {
            String pref = com.libraryplus.util.ThemeManager.loadThemePreference();
            if (pref == null) pref = "Catppuccin";
            com.libraryplus.util.ThemeManager.applyTheme(scene, pref);
            String css = Toast.class.getResource("/css/toast.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        t.setScene(scene);
        t.setAlwaysOnTop(true);

        
        double toastWidth = Math.min(420, Math.max(240, lbl.getText().length() * 7));
        root.setPrefWidth(toastWidth);

        
        double margin = 20.0;
        double x = owner.getX() + Math.max(0, owner.getWidth() - toastWidth - margin);
        double y = owner.getY() + Math.max(0, owner.getHeight() - 80 - margin); 
        t.setX(x);
        t.setY(y + 20); 

        
        synchronized (ACTIVE_TOASTS) {
            double offset = 0;
            for (Stage s : ACTIVE_TOASTS) {
                s.setY(s.getY() - (root.getPrefHeight() + 12));
                offset += (root.getPrefHeight() + 12);
            }
            ACTIVE_TOASTS.add(t);
        }

        
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(root.opacityProperty(), 0), new KeyValue(root.translateYProperty(), 10)),
                new KeyFrame(Duration.millis(220), new KeyValue(root.opacityProperty(), 1.0), new KeyValue(root.translateYProperty(), 0))
        );

        Timeline wait = new Timeline(new KeyFrame(Duration.millis(durationMs)));

        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(root.opacityProperty(), 1.0), new KeyValue(root.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(root.opacityProperty(), 0), new KeyValue(root.translateYProperty(), -10))
        );

        fadeIn.setOnFinished(e -> wait.play());
        wait.setOnFinished(e -> fadeOut.play());
        fadeOut.setOnFinished(e -> {
            t.close();
            synchronized (ACTIVE_TOASTS) {
                ACTIVE_TOASTS.remove(t);
            }
        });

        t.show();
        fadeIn.play();
    }
}
