package com.clinstock.controller;

import com.clinstock.dao.ItemDAO;
import com.clinstock.dao.StockInDAO;
import com.clinstock.dao.StockOutDAO;
import com.clinstock.model.Item;
import com.clinstock.model.StockOut;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller untuk StokKeluarView.fxml.
 *
 * Mengelola:
 * - Form pengeluaran barang dengan algoritma FEFO
 * - Menampilkan stok tersedia saat item dipilih
 * - Riwayat stok keluar
 */
public class StokKeluarController {

    @FXML private ComboBox<Item> comboItem;
    @FXML private Label lblAvailableStock;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtDestination;
    @FXML private TextArea txtNotes;
    @FXML private Label lblStatus;

    @FXML private TableView<StockOut> tableStockOut;
    @FXML private TableColumn<StockOut, String> colDate;
    @FXML private TableColumn<StockOut, String> colItem;
    @FXML private TableColumn<StockOut, String> colBatch;
    @FXML private TableColumn<StockOut, Integer> colQty;
    @FXML private TableColumn<StockOut, String> colDestination;
    @FXML private TableColumn<StockOut, String> colNotes;
    @FXML private TableColumn<StockOut, Void> colAction;

    private final ItemDAO itemDAO;
    private final StockInDAO stockInDAO;
    private final StockOutDAO stockOutDAO;

    public StokKeluarController() {
        this.itemDAO = new ItemDAO();
        this.stockInDAO = new StockInDAO();
        this.stockOutDAO = new StockOutDAO();
    }

    @FXML
    public void initialize() {
        // Setup ComboBox item
        List<Item> items = itemDAO.getAllItems();
        comboItem.setItems(FXCollections.observableArrayList(items));

        // Listener: saat item dipilih, tampilkan stok tersedia
        comboItem.setOnAction(e -> {
            Item selected = comboItem.getValue();
            if (selected != null) {
                int available = stockInDAO.getTotalAvailableStock(selected.getId());
                lblAvailableStock.setText("Stok tersedia: " + available + " " + selected.getUnit());
            } else {
                lblAvailableStock.setText("Stok tersedia: -");
            }
        });

        // Setup tabel riwayat
        colDate.setCellValueFactory(new PropertyValueFactory<>("outDate"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colBatch.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        setupActionColumn();

        // Load riwayat
        refreshHistory();
    }

    /**
     * Handler tombol "Proses Keluar" — menjalankan algoritma FEFO.
     */
    @FXML
    private void handleProcess() {
        // Validasi input
        if (comboItem.getValue() == null) {
            showStatus("Pilih item terlebih dahulu.", false); return;
        }
        if (txtQuantity.getText().trim().isEmpty()) {
            showStatus("Jumlah keluar wajib diisi.", false); return;
        }
        if (txtDestination.getText().trim().isEmpty()) {
            showStatus("Tujuan wajib diisi.", false); return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(txtQuantity.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showStatus("Jumlah harus berupa angka positif.", false); return;
        }

        Item selectedItem = comboItem.getValue();

        // Cek stok cukup
        int available = stockInDAO.getTotalAvailableStock(selectedItem.getId());
        if (available < quantity) {
            showStatus("Stok tidak cukup! Tersedia: " + available
                    + " " + selectedItem.getUnit()
                    + ", Diminta: " + quantity, false);
            return;
        }

        // Konfirmasi
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Stok Keluar");
        confirm.setHeaderText("Proses pengeluaran " + quantity + " " + selectedItem.getUnit()
                + " " + selectedItem.getName() + "?");
        confirm.setContentText("Sistem akan otomatis menggunakan algoritma FEFO "
                + "(First Expired, First Out) untuk memotong stok dari batch "
                + "dengan tanggal kadaluarsa terdekat.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Proses FEFO
                List<StockOut> results = stockOutDAO.processStockOutFEFO(
                        selectedItem.getId(),
                        quantity,
                        txtDestination.getText().trim(),
                        txtNotes.getText().trim()
                );

                if (!results.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Berhasil! ");
                    sb.append(quantity).append(" ").append(selectedItem.getUnit())
                      .append(" dikeluarkan dari ").append(results.size()).append(" batch: ");
                    for (StockOut out : results) {
                        sb.append("[").append(out.getBatchNumber())
                          .append(":").append(out.getQuantity()).append("] ");
                    }
                    showStatus(sb.toString(), true);
                    handleReset();
                    refreshHistory();
                } else {
                    showStatus("Gagal memproses stok keluar.", false);
                }
            }
        });
    }

    @FXML
    private void handleReset() {
        comboItem.setValue(null);
        lblAvailableStock.setText("Stok tersedia: -");
        txtQuantity.clear();
        txtDestination.clear();
        txtNotes.clear();
    }

    private void refreshHistory() {
        tableStockOut.setItems(FXCollections.observableArrayList(stockOutDAO.getAll()));
    }

    private void setupActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Hapus");
            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(e -> {
                    StockOut stockOut = getTableView().getItems().get(getIndex());
                    handleDelete(stockOut);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void handleDelete(StockOut stockOut) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Hapus/Batalkan transaksi keluar batch: " + stockOut.getBatchNumber() + "?");
        confirm.setContentText("Jumlah barang (" + stockOut.getQuantity() + " unit) akan dikembalikan ke batch asal di stok masuk.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = stockOutDAO.deleteStockOut(
                        stockOut.getId(),
                        stockOut.getItemId(),
                        stockOut.getBatchNumber(),
                        stockOut.getQuantity()
                );
                if (success) {
                    showStatus("Transaksi stok keluar berhasil dibatalkan dan stok dikembalikan.", true);
                    refreshHistory();
                    
                    // Juga update stok yang sedang dipilih di combobox jika ada
                    Item selected = comboItem.getValue();
                    if (selected != null) {
                        int available = stockInDAO.getTotalAvailableStock(selected.getId());
                        lblAvailableStock.setText("Stok tersedia: " + available + " " + selected.getUnit());
                    }
                } else {
                    showStatus("Gagal membatalkan transaksi stok keluar.", false);
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
