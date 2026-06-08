package com.clinstock.controller;

import com.clinstock.dao.ItemDAO;
import com.clinstock.model.Item;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller untuk DataMasterView.fxml.
 *
 * Mengelola CRUD data master barang (obat & alat kesehatan):
 * - Tambah, Edit, Hapus item
 * - Search / filter realtime
 * - Tabel dengan pagination
 */
public class DataMasterController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, String> colCode;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colUnit;
    @FXML private TableColumn<Item, Integer> colMinStock;
    @FXML private TableColumn<Item, Integer> colCurrentStock;
    @FXML private TableColumn<Item, Void> colAction;
    @FXML private Label lblStatus;
    @FXML private Label lblPageInfo;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private final ItemDAO itemDAO;
    private ObservableList<Item> allItems;

    // Pagination
    private static final int PAGE_SIZE = 15;
    private int currentPage = 0;
    private List<Item> filteredItems;

    public DataMasterController() {
        this.itemDAO = new ItemDAO();
    }

    @FXML
    public void initialize() {
        // Setup kolom tabel
        colCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colCurrentStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));

        // Kolom aksi (Edit + Hapus)
        setupActionColumn();

        // Load data
        refreshData();
    }

    /**
     * Setup kolom Aksi dengan tombol Edit dan Hapus.
     */
    private void setupActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Hapus");
            private final HBox container = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-edit");
                btnDelete.getStyleClass().add("btn-danger");

                btnEdit.setOnAction(e -> {
                    Item item = getTableView().getItems().get(getIndex());
                    handleEdit(item);
                });

                btnDelete.setOnAction(e -> {
                    Item item = getTableView().getItems().get(getIndex());
                    handleDelete(item);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    /**
     * Refresh data dari database dan tampilkan di tabel.
     */
    private void refreshData() {
        filteredItems = itemDAO.getAllItems();
        currentPage = 0;
        applyPagination();
    }

    /**
     * Terapkan pagination pada data.
     */
    private void applyPagination() {
        int totalItems = filteredItems.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        List<Item> pageData = filteredItems.subList(fromIndex, toIndex);
        tableItems.setItems(FXCollections.observableArrayList(pageData));

        lblPageInfo.setText("Halaman " + (currentPage + 1) + " dari " + totalPages
                + " (" + totalItems + " item)");
        btnPrev.setDisable(currentPage <= 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
    }

    // ===================== Event Handlers =====================

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            filteredItems = itemDAO.getAllItems();
        } else {
            filteredItems = itemDAO.searchItems(keyword);
        }
        currentPage = 0;
        applyPagination();
    }

    @FXML
    private void handlePrev() {
        if (currentPage > 0) {
            currentPage--;
            applyPagination();
        }
    }

    @FXML
    private void handleNext() {
        currentPage++;
        applyPagination();
    }

    @FXML
    private void handleAdd() {
        showItemDialog(null);
    }

    private void handleEdit(Item item) {
        showItemDialog(item);
    }

    private void handleDelete(Item item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Hapus item: " + item.getName() + "?");
        confirm.setContentText("Data yang dihapus tidak dapat dikembalikan.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (itemDAO.deleteItem(item.getId())) {
                showStatus("Item berhasil dihapus.", true);
                refreshData();
            } else {
                showStatus("Gagal menghapus. Item mungkin sedang digunakan.", false);
            }
        }
    }

    /**
     * Menampilkan dialog form untuk Tambah / Edit item.
     */
    private void showItemDialog(Item existingItem) {
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle(existingItem == null ? "Tambah Obat Baru" : "Edit Obat");

        ButtonType saveButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtCode = new TextField();
        txtCode.setPromptText("Contoh: OBT-004");
        TextField txtName = new TextField();
        txtName.setPromptText("Nama obat/alat");
        TextField txtCategory = new TextField();
        txtCategory.setPromptText("Contoh: Antibiotik");
        TextField txtUnit = new TextField();
        txtUnit.setPromptText("Contoh: Tablet, Box");
        TextField txtMinStock = new TextField();
        txtMinStock.setPromptText("Jumlah minimum");

        if (existingItem != null) {
            txtCode.setText(existingItem.getItemCode());
            txtName.setText(existingItem.getName());
            txtCategory.setText(existingItem.getCategory());
            txtUnit.setText(existingItem.getUnit());
            txtMinStock.setText(String.valueOf(existingItem.getMinStock()));
        }

        grid.add(new Label("Kode Barang:"), 0, 0);  grid.add(txtCode, 1, 0);
        grid.add(new Label("Nama Barang:"), 0, 1);   grid.add(txtName, 1, 1);
        grid.add(new Label("Kategori:"), 0, 2);       grid.add(txtCategory, 1, 2);
        grid.add(new Label("Satuan:"), 0, 3);          grid.add(txtUnit, 1, 3);
        grid.add(new Label("Stok Minimum:"), 0, 4);   grid.add(txtMinStock, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Item item = existingItem != null ? existingItem : new Item();
                    item.setItemCode(txtCode.getText().trim());
                    item.setName(txtName.getText().trim());
                    item.setCategory(txtCategory.getText().trim());
                    item.setUnit(txtUnit.getText().trim());
                    item.setMinStock(Integer.parseInt(txtMinStock.getText().trim()));
                    return item;
                } catch (NumberFormatException e) {
                    showStatus("Stok minimum harus berupa angka.", false);
                    return null;
                }
            }
            return null;
        });

        Optional<Item> result = dialog.showAndWait();
        result.ifPresent(item -> {
            if (item.getName().isEmpty() || item.getItemCode().isEmpty()) {
                showStatus("Kode dan Nama barang wajib diisi.", false);
                return;
            }
            boolean success;
            if (existingItem != null) {
                success = itemDAO.updateItem(item);
                showStatus(success ? "Item berhasil diperbarui." : "Gagal memperbarui item.", success);
            } else {
                success = itemDAO.insertItem(item);
                showStatus(success ? "Item baru berhasil ditambahkan." : "Gagal menambahkan item. Kode mungkin sudah ada.", success);
            }
            if (success) refreshData();
        });
    }

    private void showStatus(String message, boolean success) {
        lblStatus.setText(message);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
        lblStatus.getStyleClass().removeAll("status-success", "status-error");
        lblStatus.getStyleClass().add(success ? "status-success" : "status-error");
    }

    @FXML
    private void handleExportExcel() {
        if (filteredItems == null || filteredItems.isEmpty()) {
            showAlert("Tidak ada data untuk di-export.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Data Master Excel");
        fileChooser.setInitialFileName("data_master_clinstock.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = fileChooser.showSaveDialog(tableItems.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Data Master");

                // === Style Definitions ===
                CellStyle titleStyle = workbook.createCellStyle();
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 16);
                titleFont.setFontName("Segoe UI");
                titleStyle.setFont(titleFont);

                CellStyle subtitleStyle = workbook.createCellStyle();
                Font subtitleFont = workbook.createFont();
                subtitleFont.setFontHeightInPoints((short) 10);
                subtitleFont.setFontName("Segoe UI");
                subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
                subtitleStyle.setFont(subtitleFont);

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short) 11);
                headerFont.setFontName("Segoe UI");
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);

                CellStyle dataStyle = workbook.createCellStyle();
                Font dataFont = workbook.createFont();
                dataFont.setFontHeightInPoints((short) 10);
                dataFont.setFontName("Segoe UI");
                dataStyle.setFont(dataFont);
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);

                CellStyle numStyle = workbook.createCellStyle();
                numStyle.cloneStyleFrom(dataStyle);
                numStyle.setAlignment(HorizontalAlignment.CENTER);

                // === Row 0: Title ===
                int rowNum = 0;
                Row titleRow = sheet.createRow(rowNum++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("DATA MASTER BARANG - CLINSTOCK");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

                // === Row 1: Subtitle ===
                Row dateRow = sheet.createRow(rowNum++);
                Cell dateCell = dateRow.createCell(0);
                dateCell.setCellValue("Tanggal cetak: " + LocalDate.now() + " | Total: " + filteredItems.size() + " item");
                dateCell.setCellStyle(subtitleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

                rowNum++; // Empty row

                // === Row 3: Headers ===
                String[] headers = {"No", "Kode Barang", "Nama Barang", "Kategori", "Satuan", "Stok Minimum", "Stok Saat Ini"};
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // === Data Rows ===
                for (int i = 0; i < filteredItems.size(); i++) {
                    Item item = filteredItems.get(i);
                    Row row = sheet.createRow(rowNum++);

                    Cell noCell = row.createCell(0);
                    noCell.setCellValue(i + 1);
                    noCell.setCellStyle(numStyle);

                    Cell codeCell = row.createCell(1);
                    codeCell.setCellValue(safe(item.getItemCode()));
                    codeCell.setCellStyle(dataStyle);

                    Cell nameCell = row.createCell(2);
                    nameCell.setCellValue(safe(item.getName()));
                    nameCell.setCellStyle(dataStyle);

                    Cell catCell = row.createCell(3);
                    catCell.setCellValue(safe(item.getCategory()));
                    catCell.setCellStyle(dataStyle);

                    Cell unitCell = row.createCell(4);
                    unitCell.setCellValue(safe(item.getUnit()));
                    unitCell.setCellStyle(dataStyle);

                    Cell minCell = row.createCell(5);
                    minCell.setCellValue(item.getMinStock());
                    minCell.setCellStyle(numStyle);

                    Cell curCell = row.createCell(6);
                    curCell.setCellValue(item.getCurrentStock());
                    curCell.setCellStyle(numStyle);
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                    if (sheet.getColumnWidth(i) < 3000) {
                        sheet.setColumnWidth(i, 3000);
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }

                showAlert("Data Master berhasil di-export ke Excel:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);

            } catch (IOException e) {
                showAlert("Gagal menyimpan file Excel: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleExportPDF() {
        if (filteredItems == null || filteredItems.isEmpty()) {
            showAlert("Tidak ada data untuk di-export.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Data Master PDF");
        fileChooser.setInitialFileName("data_master_clinstock.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(tableItems.getScene().getWindow());

        if (file != null) {
            try {
                Document document = new Document(PageSize.A4, 30, 30, 40, 30);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(bfBold, 18,
                        com.itextpdf.text.Font.BOLD, new BaseColor(15, 23, 42));
                com.itextpdf.text.Font subtitleFont = new com.itextpdf.text.Font(bf, 10,
                        com.itextpdf.text.Font.NORMAL, new BaseColor(100, 116, 139));
                com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(bfBold, 9,
                        com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
                com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(bf, 8,
                        com.itextpdf.text.Font.NORMAL, new BaseColor(30, 41, 59));
                com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(bf, 8,
                        com.itextpdf.text.Font.ITALIC, new BaseColor(100, 116, 139));

                Paragraph title = new Paragraph("DATA MASTER BARANG - CLINSTOCK", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(6);
                document.add(title);

                Paragraph datePara = new Paragraph("Tanggal cetak: " + LocalDate.now()
                        + "  |  Total: " + filteredItems.size() + " item", subtitleFont);
                datePara.setAlignment(Element.ALIGN_CENTER);
                datePara.setSpacingAfter(16);
                document.add(datePara);

                float[] columnWidths = {30f, 80f, 150f, 100f, 80f, 70f, 70f};
                PdfPTable table = new PdfPTable(columnWidths);
                table.setWidthPercentage(100);

                BaseColor headerBg = new BaseColor(13, 148, 136); // Teal
                String[] headers = {"No", "Kode", "Nama Barang", "Kategori", "Satuan", "Stok Min", "Stok Saat Ini"};

                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setBackgroundColor(headerBg);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(6);
                    cell.setBorderColor(new BaseColor(51, 65, 85));
                    table.addCell(cell);
                }

                BaseColor altRowColor = new BaseColor(241, 245, 249);
                for (int i = 0; i < filteredItems.size(); i++) {
                    Item item = filteredItems.get(i);
                    BaseColor rowBg = (i % 2 == 1) ? altRowColor : BaseColor.WHITE;

                    addPdfCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                    addPdfCell(table, safe(item.getItemCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                    addPdfCell(table, safe(item.getName()), dataFont, rowBg, Element.ALIGN_LEFT);
                    addPdfCell(table, safe(item.getCategory()), dataFont, rowBg, Element.ALIGN_LEFT);
                    addPdfCell(table, safe(item.getUnit()), dataFont, rowBg, Element.ALIGN_LEFT);
                    addPdfCell(table, String.valueOf(item.getMinStock()), dataFont, rowBg, Element.ALIGN_CENTER);
                    addPdfCell(table, String.valueOf(item.getCurrentStock()), dataFont, rowBg, Element.ALIGN_CENTER);
                }

                document.add(table);

                document.add(new Paragraph(" "));
                Paragraph footer = new Paragraph("Dicetak oleh sistem ClinStock v1.0 — "
                        + LocalDate.now(), footerFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);

                document.close();

                showAlert("Data Master berhasil di-export ke PDF:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                showAlert("Gagal menyimpan file PDF: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void addPdfCell(PdfPTable table, String text,
                            com.itextpdf.text.Font font, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(203, 213, 225));
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("ClinStock — Export");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
