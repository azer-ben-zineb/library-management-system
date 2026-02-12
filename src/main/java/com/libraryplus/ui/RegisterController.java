package com.libraryplus.ui;

import com.libraryplus.presenter.RegisterPresenter;
import com.libraryplus.util.ValidationUtils;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private ChoiceBox<String> roleChoice;
    @FXML private Label messageLabel;

    
    @FXML private DatePicker dobPicker;
    @FXML private TextField ageField;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardBalanceField;
    @FXML private javafx.scene.control.Button registerButton;

    private final RegisterPresenter presenter = new RegisterPresenter();

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    public void initialize() {
        roleChoice.getItems().addAll("CLIENT", "ADMIN");
        roleChoice.setValue("CLIENT");

        
        ChangeListener<String> ageListener = (obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) return;
            try {
                int age = Integer.parseInt(newVal.trim());
                if (age <= 0) return;
                LocalDate dob = LocalDate.now().minusYears(age);
                dobPicker.setValue(dob);
            } catch (NumberFormatException ex) {
                
            }
        };
        ageField.textProperty().addListener(ageListener);

        dobPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            int years = LocalDate.now().getYear() - newVal.getYear();
            ageField.setText(String.valueOf(years));
        });
    }

    @FXML
    protected void onRegister(ActionEvent event) {
        
        String email = emailField.getText();
        String password = passwordField.getText();
        String fullName = fullNameField.getText();
        String phone = phoneField.getText();
        String role = roleChoice.getValue();
        String card = cardNumberField.getText();
        String cardBalanceText = cardBalanceField.getText();

        Double cardBalance = null;
        if (cardBalanceText != null && !cardBalanceText.isBlank()) {
            try { cardBalance = Double.parseDouble(cardBalanceText.trim()); }
            catch (NumberFormatException ex) {
                messageLabel.setText("Card balance must be a numeric value.");
                return;
            }
        }

        
        try {
            String result = presenter.register(email, password, fullName, phone, dobPicker.getValue(), card == null || card.isBlank() ? null : card.trim(), cardBalance);
            messageLabel.setText(result);
            Stage owner = (Stage) messageLabel.getScene().getWindow();
            if (result != null && result.toLowerCase().contains("success")) {
                if (owner != null) Toast.show(owner, result, 2200, "success");
                
                try {
                    javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(javafx.util.Duration.millis(500), registerButton);
                    rt.setByAngle(360);
                    rt.setOnFinished(ev -> { if (owner != null) owner.close(); });
                    rt.play();
                } catch (Exception ignore) { if (owner != null) owner.close(); }
            } else {
                if (owner != null) Toast.show(owner, result, 3400, "error");
                playShake(registerButton);
            }
        } catch (Exception e) {
            logger.error("Registration error: ", e);
            messageLabel.setText("Registration failed: " + e.getMessage());
            Stage owner = (Stage) messageLabel.getScene().getWindow();
            if (owner != null) Toast.show(owner, "Registration failed: " + e.getMessage(), 3500, "error");
            playShake(registerButton);
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
    protected void onCancel(ActionEvent event) {
        Stage s = (Stage) messageLabel.getScene().getWindow();
        s.close();
    }
}
