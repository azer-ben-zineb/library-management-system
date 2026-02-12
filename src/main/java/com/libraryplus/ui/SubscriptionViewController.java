package com.libraryplus.ui;

import com.libraryplus.dao.ClientDao;
import com.libraryplus.dao.SubscriptionDao;
import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.ClientDaoJdbc;
import com.libraryplus.dao.jdbc.SubscriptionDaoJdbc;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.Client;
import com.libraryplus.model.Subscription;
import com.libraryplus.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SubscriptionViewController {

    @FXML
    private TableView<SubscriptionRow> subscriptionTable;
    @FXML
    private TableColumn<SubscriptionRow, String> clientIdColumn;
    @FXML
    private TableColumn<SubscriptionRow, String> nameColumn;
    @FXML
    private TableColumn<SubscriptionRow, String> emailColumn;
    @FXML
    private TableColumn<SubscriptionRow, String> statusColumn;
    @FXML
    private TableColumn<SubscriptionRow, String> startDateColumn;
    @FXML
    private TableColumn<SubscriptionRow, String> endDateColumn;

    private final SubscriptionDao subscriptionDao = new SubscriptionDaoJdbc();
    private final ClientDao clientDao = new ClientDaoJdbc();
    private final UserDao userDao = new UserDaoJdbc();

    @FXML
    public void initialize() {
        
        clientIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clientId));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
        startDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().startDate));
        endDateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().endDate));

        loadData();
    }

    @FXML
    void onRefresh(ActionEvent event) {
        loadData();
    }

    @FXML
    void onClose(ActionEvent event) {
        Stage stage = (Stage) subscriptionTable.getScene().getWindow();
        stage.close();
    }

    private void loadData() {
        ObservableList<SubscriptionRow> rows = FXCollections.observableArrayList();

        try {
            
            List<Client> allClients = clientDao.findAll();

            for (Client client : allClients) {
                try {
                    
                    Optional<User> userOpt = userDao.findById(client.getUserId());
                    if (!userOpt.isPresent())
                        continue;
                    User user = userOpt.get();

                    
                    Optional<Subscription> subOpt = subscriptionDao.findActiveByClientId(client.getId());

                    SubscriptionRow row = new SubscriptionRow();
                    row.clientId = String.valueOf(client.getId());
                    row.name = user.getFullName();
                    row.email = user.getEmail();

                    if (subOpt.isPresent()) {
                        Subscription sub = subOpt.get();
                        LocalDate now = LocalDate.now();
                        if (sub.getEndDate() != null && sub.getEndDate().isBefore(now)) {
                            row.status = "EXPIRED";
                        } else {
                            row.status = "ACTIVE";
                        }
                        row.startDate = sub.getStartDate() != null ? sub.getStartDate().toString() : "N/A";
                        row.endDate = sub.getEndDate() != null ? sub.getEndDate().toString() : "N/A";
                    } else {
                        row.status = "NOT SUBSCRIBED";
                        row.startDate = "N/A";
                        row.endDate = "N/A";
                    }

                    rows.add(row);
                } catch (Exception e) {
                    System.err.println("Error loading client " + client.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading subscriptions: " + e.getMessage());
            e.printStackTrace();
        }

        subscriptionTable.setItems(rows);
    }

    public static class SubscriptionRow {
        public String clientId;
        public String name;
        public String email;
        public String status;
        public String startDate;
        public String endDate;
    }
}
