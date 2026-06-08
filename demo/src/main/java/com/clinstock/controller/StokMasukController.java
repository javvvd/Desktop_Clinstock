package com.clinstock.controller;

import com.clinstock.dao.ItemDAO;
import com.clinstock.dao.StockInDAO;
import com.clinstock.model.Item;
import com.clinstock.model.StockIn;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller untuk StokMasukView.fxml.
 *
 * Mengelola:
 * - Form input barang masuk (item, batch, expired, qty, supplier)
 * - Validasi input
 * - Riwayat stok masuk
 */
public class StokMasukController {

    @FXML private ComboBox<Item> comboItem;
    @FXML private TextField txtBatch;
    @FXML private DatePicker dateExpired;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtSupplier;
    @FXML private TextArea txtNotes;
    @FXML private Label lblStatus;

    @FXML private TableView<StockIn> tableStockIn;
    @FXML private TableColumn<StockIn, String> colDate;
    @FXML private TableColumn<StockIn, String> colItem;
    @FXML private TableColumn<StockIn, String> colBatch;
    @FXML private TableColumn<StockIn, Integer> colQty;
    @FXML private TableColumn<StockIn, Integer> colRemaining;
    @FXML private TableColumn<StockIn, String> colExpired;
    @FXML private TableColumn<StockIn, String> colSupplier;
    @FXML private TableColumn<StockIn, Void> colAction;

    private final ItemDAO itemDAO;
    private final StockInDAO stockInDAO;

    public StokMasukController() {
        this.itemDAO = new ItemDAO();
        this.stockInDAO = new StockInDAO();
    }

    @FXML
    public void initialize() {
        // Setup ComboBox item
        List<Item> items = itemDAO.getAllItems();
        comboItem.setItems(FXCollections.observableArrayList(items));

        // Setup tabel riwayat
        colDate.setCellValueFactory(new PropertyValueFactory<>("inputDate"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colBatch.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colRemaining.setCellValueFactory(new PropertyValueFactory<>("remainingQty"));
        colExpired.setCellValueFactory(new PropertyValueFactory<>("expiredDate"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        setupActionColumn();

        // Load riwayat
        refreshHistory();
    }

    @FXML
    private void handleSave() {
        // Validasi input
        if (comboItem.getValue() == null) {
            showStatus("Pilih item terlebih dahulu.", false); return;
        }
        if (txtBatch.getText().trim().isEmpty()) {
            showStatus("Nomor batch wajib diisi.", false); return;
        }
        if (dateExpired.getValue() == null) {
            showStatus("Tanggal kadaluarsa wajib diisi.", false); return;
        }
        if (txtQuantity.getText().trim().isEmpty()) {
            showStatus("Jumlah wajib diisi.", false); return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(txtQuantity.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showStatus("Jumlah harus berupa angka positif.", false); return;
        }

        // Buat objek StockIn
        StockIn stockIn = new StockIn(
                comboItem.getValue().getId(),
                txtBatch.getText().trim(),
                dateExpired.getValue().toString(),
                quantity,
                txtSupplier.getText().trim(),
                txtNotes.getText().trim()
        );

        // Simpan ke database
        if (stockInDAO.insert(stockIn)) {
            showStatus("Stok masuk berhasil disimpan! (" + quantity + " "
                    + comboItem.getValue().getUnit() + " " + comboItem.getValue().getName() + ")", true);
            handleReset();
            refreshHistory();
        } else {
            showStatus("Gagal menyimpan stok masuk.", false);
        }
    }

    @FXML
    private void handleReset() {
        comboItem.setValue(null);
        txtBatch.clear();
        dateExpired.setValue(null);
        txtQuantity.clear();
        txtSupplier.clear();
        txtNotes.clear();
    }

    private void refreshHistory() {
        tableStockIn.setItems(FXCollections.observableArrayList(stockInDAO.getAll()));
    }

    private void setupActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Hapus");
            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(e -> {
                    StockIn stockIn = getTableView().getItems().get(getIndex());
                    handleDelete(stockIn);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void handleDelete(StockIn stockIn) {
        if (stockIn.getRemainingQty() < stockIn.getQuantity()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Gagal Menghapus");
            alert.setHeaderText("Batch ini tidak dapat dihapus!");
            alert.setContentText("Beberapa unit dari batch ini sudah digunakan/dikeluarkan melalui transaksi Stok Keluar.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Hapus transaksi masuk batch: " + stockIn.getBatchNumber() + "?");
        confirm.setContentText("Stok dari batch ini akan dihapus permanen dari inventori.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (stockInDAO.delete(stockIn.getId())) {
                    showStatus("Transaksi stok masuk berhasil dihapus.", true);
                    refreshHistory();
                    
                    // Juga refresh combobox item agar jumlah stok terupdate jika ada
                    List<Item> items = itemDAO.getAllItems();
                    comboItem.setItems(FXCollections.observableArrayList(items));
                } else {
                    showStatus("Gagal menghapus transaksi stok masuk.", false);
                }
            }
        });
    }

    private void showStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("status-success", "status-error");
        lblStatus.getStyleClass().add(success ? "status-success" : "status-error");
    }
}
