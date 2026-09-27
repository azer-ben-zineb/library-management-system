package com.libraryplus.ui;

import com.libraryplus.db.DataSourceConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class StatisticsController {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @FXML private Label totalBooksLabel;
    @FXML private Label totalStockLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label usersBreakdownLabel;
    @FXML private Label totalLoansLabel;
    @FXML private Label activeLoansLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label revenueDetailsLabel;

    @FXML private PieChart categoryPieChart;
    @FXML private PieChart membershipPieChart;
    @FXML private BarChart<String, Number> activityBarChart;
    @FXML private CategoryAxis activityXAxis;
    @FXML private NumberAxis activityYAxis;

    @FXML
    public void initialize() {
        loadStatistics();
    }

    @FXML
    public void onRefresh(ActionEvent event) {
        loadStatistics();
    }

    @FXML
    public void onClose(ActionEvent event) {
        if (totalBooksLabel != null && totalBooksLabel.getScene() != null) {
            Stage stage = (Stage) totalBooksLabel.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    public void loadStatistics() {
        new Thread(() -> {
            try {
                DataSource ds = DataSourceConfig.getDataSource();
                try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {

                    // 1. Books & Stock
                    int totalBooks = 0;
                    int totalStock = 0;
                    try (ResultSet rs = s.executeQuery("SELECT count(*), coalesce(sum(stock), 0) FROM books")) {
                        if (rs.next()) {
                            totalBooks = rs.getInt(1);
                            totalStock = rs.getInt(2);
                        }
                    }

                    // 2. Users
                    int totalUsers = 0;
                    int admins = 0;
                    int members = 0;
                    try (ResultSet rs = s.executeQuery("SELECT count(*), coalesce(sum(case when role_id = 1 then 1 else 0 end), 0), coalesce(sum(case when role_id = 2 then 1 else 0 end), 0) FROM users")) {
                        if (rs.next()) {
                            totalUsers = rs.getInt(1);
                            admins = rs.getInt(2);
                            members = rs.getInt(3);
                        }
                    }

                    // 3. Loans
                    int totalLoans = 0;
                    int activeLoans = 0;
                    try (ResultSet rs = s.executeQuery("SELECT count(*), coalesce(sum(case when actual_return_date IS NULL then 1 else 0 end), 0) FROM loans")) {
                        if (rs.next()) {
                            totalLoans = rs.getInt(1);
                            activeLoans = rs.getInt(2);
                        }
                    }

                    // 4. Revenue
                    double purchaseRev = 0.0;
                    try (ResultSet rs = s.executeQuery("SELECT coalesce(sum(unit_price * quantity), 0.0) FROM purchases")) {
                        if (rs.next()) purchaseRev = rs.getDouble(1);
                    }

                    double subscriptionRev = 0.0;
                    int subscriptionCount = 0;
                    try (ResultSet rs = s.executeQuery("SELECT count(*) FROM subscriptions")) {
                        if (rs.next()) {
                            subscriptionCount = rs.getInt(1);
                            subscriptionRev = subscriptionCount * 20.0;
                        }
                    }

                    double finesRev = 0.0;
                    try (ResultSet rs = s.executeQuery("SELECT coalesce(sum(fine_amount), 0.0) FROM loans")) {
                        if (rs.next()) finesRev = rs.getDouble(1);
                    }

                    double totalRevenue = purchaseRev + subscriptionRev + finesRev;

                    // 5. Category distribution
                    ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList();
                    try (ResultSet rs = s.executeQuery("SELECT coalesce(category, 'General') as cat, count(*) as cnt FROM books GROUP BY cat ORDER BY cnt DESC LIMIT 8")) {
                        while (rs.next()) {
                            categoryData.add(new PieChart.Data(rs.getString("cat") + " (" + rs.getInt("cnt") + ")", rs.getInt("cnt")));
                        }
                    }

                    // 6. Membership breakdown
                    ObservableList<PieChart.Data> membershipData = FXCollections.observableArrayList();
                    try (ResultSet rs = s.executeQuery("SELECT coalesce(membership_type, 'STANDARD') as mtype, count(*) as cnt FROM clients GROUP BY mtype")) {
                        while (rs.next()) {
                            membershipData.add(new PieChart.Data(rs.getString("mtype") + " (" + rs.getInt("cnt") + ")", rs.getInt("cnt")));
                        }
                    }
                    if (membershipData.isEmpty()) {
                        membershipData.add(new PieChart.Data("Standard Members", Math.max(1, members)));
                    }

                    // 7. Activity Bar Chart
                    XYChart.Series<String, Number> activitySeries = new XYChart.Series<>();
                    activitySeries.setName("Metrics");
                    activitySeries.getData().add(new XYChart.Data<>("Catalog Books", totalBooks));
                    activitySeries.getData().add(new XYChart.Data<>("Available Stock", totalStock));
                    activitySeries.getData().add(new XYChart.Data<>("Active Loans", activeLoans));
                    activitySeries.getData().add(new XYChart.Data<>("Total Borrowed", totalLoans));
                    activitySeries.getData().add(new XYChart.Data<>("Subscriptions", subscriptionCount));

                    int finalTotalBooks = totalBooks;
                    int finalTotalStock = totalStock;
                    int finalTotalUsers = totalUsers;
                    int finalAdmins = admins;
                    int finalMembers = members;
                    int finalTotalLoans = totalLoans;
                    int finalActiveLoans = activeLoans;
                    double finalTotalRevenue = totalRevenue;
                    double finalPurchaseRev = purchaseRev;
                    double finalSubRev = subscriptionRev;

                    Platform.runLater(() -> {
                        if (totalBooksLabel != null) totalBooksLabel.setText(String.valueOf(finalTotalBooks));
                        if (totalStockLabel != null) totalStockLabel.setText(finalTotalStock + " copies in stock");
                        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(finalTotalUsers));
                        if (usersBreakdownLabel != null) usersBreakdownLabel.setText(finalMembers + " Members • " + finalAdmins + " Admins");
                        if (totalLoansLabel != null) totalLoansLabel.setText(String.valueOf(finalTotalLoans));
                        if (activeLoansLabel != null) activeLoansLabel.setText(finalActiveLoans + " active / borrowed");
                        if (totalRevenueLabel != null) totalRevenueLabel.setText(String.format("%.2f DT", finalTotalRevenue));
                        if (revenueDetailsLabel != null) revenueDetailsLabel.setText(String.format("Purchases: %.1f DT • Subs: %.1f DT", finalPurchaseRev, finalSubRev));

                        if (categoryPieChart != null) categoryPieChart.setData(categoryData);
                        if (membershipPieChart != null) membershipPieChart.setData(membershipData);
                        if (activityBarChart != null) {
                            activityBarChart.getData().clear();
                            activityBarChart.getData().add(activitySeries);
                        }
                    });
                }
            } catch (Exception ex) {
                logger.error("Failed to load statistics", ex);
            }
        }).start();
    }
}
