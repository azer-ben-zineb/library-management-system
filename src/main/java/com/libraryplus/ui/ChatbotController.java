package com.libraryplus.ui;

import com.libraryplus.service.ChatbotService;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;

public class ChatbotController {

    @FXML
    private VBox chatContainer;
    @FXML
    private TextField inputField;
    @FXML
    private ScrollPane scrollPane;

    private final ChatbotService chatbotService = new ChatbotService();

    @FXML
    public void initialize() {
        addSystemMessage("Hello! I'm your library assistant. How can I help you?");

        
        chatContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void onSend() {
        String text = inputField.getText();
        if (text == null || text.isBlank())
            return;

        addUserMessage(text);
        inputField.clear();

        
        String response = chatbotService.processQuery(text);
        addSystemMessage(response);
    }

    private void addUserMessage(String text) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(5));

        Label label = new Label(text);
        label.setStyle(
                "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-padding: 8 12; -fx-background-radius: 15; -fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(300);

        hbox.getChildren().add(label);
        chatContainer.getChildren().add(hbox);
    }

    private void addSystemMessage(String text) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5));

        Label label = new Label(text);
        label.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #cdd6f4; -fx-padding: 8 12; -fx-background-radius: 15; -fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(300);

        hbox.getChildren().add(label);
        chatContainer.getChildren().add(hbox);
    }
}
