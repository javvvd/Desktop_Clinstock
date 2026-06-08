package com.clinstock.controller;

import com.clinstock.dao.DashboardDAO;
import com.clinstock.dao.ItemDAO;
import com.clinstock.dao.StockInDAO;
import com.clinstock.model.Item;
import com.clinstock.model.StockIn;
import com.clinstock.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.Map;

/**
 * Controller untuk DashboardView.fxml.
 *
 * Menyajikan:
 * - Ringkasan stok (summary cards)
 * - PieChart distribusi stok per kategori
 * - BarChart pergerakan stok bulanan
 * - Panel notifikasi (early warning) dalam bentuk tabel berwarna
 */
public class DashboardController {

    @FXML private Label lblWelcome;
    @FXML private Label lblWelcomeSub;
    @FXML private Label lblLowStock;
    @FXML private Label lblExpiring;
    @FXML private Label lblExpiringThreeMonths;
    @FXML private Label lblTotalStock;
    @FXML private PieChart pieChartCategory;
    @FXML private BarChart<String, Number> barChartMonthly;

    // Notification TableView
    @FXML private TableView<NotificationRow> tableNotifications;
    @FXML private TableColumn<NotificationRow, String> colNotifType;
    @FXML private TableColumn<NotificationRow, String> colNotifItem;
    @FXML private TableColumn<NotificationRow, String> colNotifDetail;
    @FXML private TableColumn<NotificationRow, String> colNotifStatus;

    private final DashboardDAO dashboardDAO;
    private final ItemDAO itemDAO;
    private final StockInDAO stockInDAO;

    public DashboardController() {
        this.dashboardDAO = new DashboardDAO();
        this.itemDAO = new ItemDAO();
        this.stockInDAO = new StockInDAO();
    }

    @FXML
    public void initialize() {
        // Welcome message
        if (SessionManager.getInstance().getCurrentUser() != null) {
            String name = SessionManager.getInstance().getCurrentUser().getFullName();
            lblWelcome.setText("Selamat Datang, " + name + "!");
        }

        // Load summary cards
        loadSummaryCards();

        // Load charts
        loadPieChart();
        loadBarChart();

        // Load notifications
        loadNotifications();
    }

    /**
     * Memuat data untuk summary cards.
     */
    private void loadSummaryCards() {
        lblLowStock.setText(String.valueOf(dashboardDAO.getLowStockCount()));
        int expiring = dashboardDAO.getExpiringSoonCount();
        int expired = dashboardDAO.getExpiredCount();
        lblExpiring.setText(String.valueOf(expiring + expired));
        lblExpiringThreeMonths.setText(String.valueOf(dashboardDAO.getExpiringInThreeMonthsCount()));
        lblTotalStock.setText(String.valueOf(dashboardDAO.getTotalCurrentStock()));
    }

    /**
     * Memuat PieChart: distribusi stok per kategori.
     */
    private void loadPieChart() {
        Map<String, Integer> data = dashboardDAO.getStockByCategory();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            pieData.add(new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")",
                    entry.getValue()));
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("Belum ada data", 1));
        }

        pieChartCategory.setData(pieData);
        pieChartCategory.setLabelsVisible(true);
    }

    /**
     * Memuat BarChart: pergerakan stok masuk vs keluar per bulan.
     * Warna: Stok Masuk = hijau, Stok Keluar = merah.
     */
    private void loadBarChart() {
        Map<String, int[]> data = dashboardDAO.getMonthlyMovement();

        XYChart.Series<String, Number> seriesIn = new XYChart.Series<>();
        seriesIn.setName("Stok Masuk");

        XYChart.Series<String, Number> seriesOut = new XYChart.Series<>();
        seriesOut.setName("Stok Keluar");

        for (Map.Entry<String, int[]> entry : data.entrySet()) {
            seriesIn.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
            seriesOut.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
        }

        barChartMonthly.getData().clear();
        barChartMonthly.getData().add(seriesIn);
        barChartMonthly.getData().add(seriesOut);
    }

    /**
     * Memuat notifikasi peringatan dini ke tabel notifikasi.
     * Menggunakan TableView dengan kolom berwarna berdasarkan jenis notifikasi.
     */
    private void loadNotifications() {
        ObservableList<NotificationRow> notifications = FXCollections.observableArrayList();

        // Setup kolom tabel
        colNotifType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().type));
        colNotifItem.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().itemName));
        colNotifDetail.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().detail));
        colNotifStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().status));

        // Custom cell factory untuk kolom Tipe — badge berwarna
        colNotifType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Circle dot = new Circle(5);
                    Label label = new Label(item);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");

                    switch (item) {
                        case "KADALUARSA":
                            dot.setStyle("-fx-fill: #ef4444;");
                            label.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11;");
                            break;
                        case "MENDEKATI EXP":
                            dot.setStyle("-fx-fill: #f59e0b;");
                            label.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11;");
                            break;
                        case "STOK RENDAH":
                            dot.setStyle("-fx-fill: #fb923c;");
                            label.setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold; -fx-font-size: 11;");
                            break;
                        default:
                            dot.setStyle("-fx-fill: #64748b;");
                            label.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 11;");
                            break;
                    }

                    HBox box = new HBox(6, dot, label);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // Item stok menipis
        List<Item> lowStockItems = itemDAO.getLowStockItems();
        for (Item item : lowStockItems) {
            notifications.add(new NotificationRow(
                    "STOK RENDAH",
                    item.getName(),
                    "Sisa: " + item.getCurrentStock() + " " + item.getUnit()
                            + " (Min: " + item.getMinStock() + ")",
                    "Perlu restock segera"
            ));
        }

        // Batch kadaluarsa
        List<StockIn> expiredBatches = stockInDAO.getExpiredBatches();
        for (StockIn batch : expiredBatches) {
            notifications.add(new NotificationRow(
                    "KADALUARSA",
                    batch.getItemName(),
                    "Batch: " + batch.getBatchNumber()
                            + " | Exp: " + batch.getExpiredDate(),
                    "Sisa: " + batch.getRemainingQty() + " unit"
            ));
        }

        // Batch akan kadaluarsa (3 bulan / 90 hari)
        List<StockIn> expiringSoon = stockInDAO.getExpiringSoon(90);
        for (StockIn batch : expiringSoon) {
            notifications.add(new NotificationRow(
                    "MENDEKATI EXP",
                    batch.getItemName(),
                    "Batch: " + batch.getBatchNumber()
                            + " | Exp: " + batch.getExpiredDate(),
                    "Sisa: " + batch.getRemainingQty() + " unit"
            ));
        }

        tableNotifications.setItems(notifications);
    }

    // ===================== Inner Data Class =====================

    /**
     * Representasi satu baris notifikasi di tabel dashboard.
     */
    public static class NotificationRow {
        public final String type;
        public final String itemName;
        public final String detail;
        public final String status;

        public NotificationRow(String type, String itemName, String detail, String status) {
            this.type = type;
            this.itemName = itemName;
            this.detail = detail;
            this.status = status;
        }
    }
}
