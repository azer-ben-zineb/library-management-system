package com.libraryplus.ui;

import com.libraryplus.dao.AdminMessageDao;
import com.libraryplus.dao.jdbc.AdminMessageDaoJdbc;
import com.libraryplus.model.AdminMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;

import java.util.List;

public class AdminInboxController {

    @FXML
    private ListView<AdminMessage> messageListView;
    @FXML
    private TextField subjectField;
    @FXML
    private TextField senderField;
    @FXML
    private TextArea contentArea;
    @FXML
    private TextArea replyArea;

    private final AdminMessageDao messageDao = new AdminMessageDaoJdbc();

    @FXML
    public void initialize() {
        messageListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AdminMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText((item.getStatus().equals("UNREAD") ? "[NEW] " : "") + item.getSubject() + " ("
                            + item.getSenderEmail() + ")");
                }
            }
        });

        messageListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showDetails(newVal));
        onRefresh(null);
    }

    @FXML
    void onRefresh(ActionEvent event) {
        List<AdminMessage> messages = messageDao.findAll();
        ObservableList<AdminMessage> items = FXCollections.observableArrayList(messages);
        messageListView.setItems(items);
    }

    private void showDetails(AdminMessage msg) {
        if (msg == null) {
            subjectField.clear();
            senderField.clear();
            contentArea.clear();
            replyArea.clear();
            return;
        }
        subjectField.setText(msg.getSubject());
        senderField.setText(msg.getSenderEmail());
        contentArea.setText(msg.getContent());
        replyArea.setText(msg.getAdminReply() == null ? "" : msg.getAdminReply());
    }

    @FXML
    void onMarkRead(ActionEvent event) {
        AdminMessage selected = messageListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            messageDao.markAsRead(selected.getId());
            onRefresh(null);
            Stage owner = (Stage) messageListView.getScene().getWindow();
            Toast.show(owner, "Marked as read", 1500, "success");
        }
    }

    @FXML
    void onSendReply(ActionEvent event) {
        AdminMessage selected = messageListView.getSelectionModel().getSelectedItem();
        String reply = replyArea.getText();
        if (selected != null && reply != null && !reply.isBlank()) {
            messageDao.saveAdminReply(selected.getId(), reply, "REPLIED");
            onRefresh(null);
            Stage owner = (Stage) messageListView.getScene().getWindow();
            Toast.show(owner, "Reply sent", 1500, "success");
        }
    }
}
