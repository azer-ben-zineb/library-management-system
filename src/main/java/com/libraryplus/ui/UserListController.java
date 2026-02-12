package com.libraryplus.ui;

import com.libraryplus.dao.UserDao;
import com.libraryplus.dao.jdbc.UserDaoJdbc;
import com.libraryplus.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserListController {

    private static final Logger logger = LoggerFactory.getLogger(UserListController.class);

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, Integer> idColumn;
    @FXML
    private TableColumn<User, String> nameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> phoneColumn;

    private UserDao userDao;

    @FXML
    public void initialize() {
        try {
            userDao = new UserDaoJdbc();
            setupTable();
            loadUsers();
        } catch (Exception e) {
            logger.error("Failed to initialize UserListController", e);
        }
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(cellData -> {
            int roleId = cellData.getValue().getRoleId();
            return new SimpleStringProperty(roleId == 1 ? "ADMIN" : "CLIENT");
        });
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
    }

    private void loadUsers() {
        try {
            List<User> users = userDao.findAll();
            usersTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            logger.error("Failed to load users", e);
        }
    }
}
