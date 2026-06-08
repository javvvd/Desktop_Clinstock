package com.clinstock.controller;

import com.clinstock.dao.ItemDAO;
import com.clinstock.dao.StockInDAO;
import com.clinstock.dao.StockOutDAO;
import com.clinstock.model.Item;
import com.clinstock.model.StockIn;
import com.clinstock.model.StockOut;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller untuk LaporanView.fxml.
 *
 * Mengelola:
 * - Filter laporan (tanggal, kategori, tipe transaksi)
 * - Tabel laporan gabungan stok masuk & keluar
 * - Export ke Excel (.xlsx) dan PDF
 */
public class LaporanController {

    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboType;
    @FXML private Label lblResultInfo;

    @FXML private TableView<ReportRow> tableReport;
    @FXML private TableColumn<ReportRow, Integer> colNo;
    @FXML private TableColumn<ReportRow, String> colDate;
    @FXML private TableColumn<ReportRow, String> colType;
    @FXML private TableColumn<ReportRow, String> colItemCode;
    @FXML private TableColumn<ReportRow, String> colItemName;
    @FXML private TableColumn<ReportRow, String> colCategory;
    @FXML private TableColumn<ReportRow, String> colBatch;
    @FXML private TableColumn<ReportRow, String> colExpiredDate;
    @FXML private TableColumn<ReportRow, Integer> colQty;
    @FXML private TableColumn<ReportRow, String> colDetail;

    private final StockInDAO stockInDAO;
    private final StockOutDAO stockOutDAO;
    private final ItemDAO itemDAO;

    /** Data laporan yang sedang ditampilkan */
    private ObservableList<ReportRow> reportData = FXCollections.observableArrayList();

    public LaporanController() {
        this.stockInDAO = new StockInDAO();
        this.stockOutDAO = new StockOutDAO();
        this.itemDAO = new ItemDAO();
    }

    @FXML
    public void initialize() {
        // Setup tipe filter
        comboType.setItems(FXCollections.observableArrayList(
                "Semua", "Stok Masuk", "Stok Keluar"));
        comboType.setValue("Semua");

        // Setup kategori filter dari database
        List<String> categories = new ArrayList<>();
        categories.add("Semua");
        for (Item item : itemDAO.getAllItems()) {
            if (!categories.contains(item.getCategory())) {
                categories.add(item.getCategory());
            }
        }
        comboCategory.setItems(FXCollections.observableArrayList(categories));
        comboCategory.setValue("Semua");

        // Setup kolom tabel
        colNo.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(reportData.indexOf(cellData.getValue()) + 1).asObject());
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().date));
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().type));
        colItemCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().itemCode));
        colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().itemName));
        colCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category));
        colBatch.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().batch));
        colExpiredDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().expiredDate));
        colQty.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().qty).asObject());
        colDetail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().detail));

        // Style kolom tipe berdasarkan isi
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("MASUK")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Default: tampilkan semua data
        handleQuickAll();
    }

    // ===================== Filter Handlers =====================

    @FXML
    private void handleFilter() {
        loadReportData();
    }

    @FXML
    private void handleQuickToday() {
        dateFrom.setValue(LocalDate.now());
        dateTo.setValue(LocalDate.now());
        loadReportData();
    }

    @FXML
    private void handleQuickWeek() {
        dateTo.setValue(LocalDate.now());
        dateFrom.setValue(LocalDate.now().minusDays(7));
        loadReportData();
    }

    @FXML
    private void handleQuickMonth() {
        dateTo.setValue(LocalDate.now());
        dateFrom.setValue(LocalDate.now().withDayOfMonth(1));
        loadReportData();
    }

    @FXML
    private void handleQuickAll() {
        dateFrom.setValue(null);
        dateTo.setValue(null);
        comboCategory.setValue("Semua");
        comboType.setValue("Semua");
        loadReportData();
    }

    /**
     * Memuat data laporan berdasarkan filter yang dipilih.
     */
    private void loadReportData() {
        reportData.clear();

        String filterType = comboType.getValue();
        String filterCategory = comboCategory.getValue();
        LocalDate from = dateFrom.getValue();
        LocalDate to = dateTo.getValue();

        // Ambil data stok masuk
        if (filterType == null || filterType.equals("Semua") || filterType.equals("Stok Masuk")) {
            List<StockIn> stockInList = stockInDAO.getAll();
            for (StockIn si : stockInList) {
                // Filter tanggal
                if (from != null && si.getInputDate() != null) {
                    LocalDate siDate = LocalDate.parse(si.getInputDate().substring(0, 10));
                    if (siDate.isBefore(from)) continue;
                }
                if (to != null && si.getInputDate() != null) {
                    LocalDate siDate = LocalDate.parse(si.getInputDate().substring(0, 10));
                    if (siDate.isAfter(to)) continue;
                }

                // Filter kategori
                if (filterCategory != null && !filterCategory.equals("Semua")) {
                    Item item = itemDAO.getItemById(si.getItemId());
                    if (item != null && !item.getCategory().equals(filterCategory)) continue;
                }

                String cat = "";
                Item item = itemDAO.getItemById(si.getItemId());
                if (item != null) cat = item.getCategory();

                reportData.add(new ReportRow(
                        si.getInputDate(),
                        "MASUK",
                        si.getItemCode(),
                        si.getItemName(),
                        cat,
                        si.getBatchNumber(),
                        si.getQuantity(),
                        si.getExpiredDate(),
                        "Supplier: " + (si.getSupplier() != null && !si.getSupplier().isEmpty()
                                ? si.getSupplier() : "-")
                ));
            }
        }

        // Ambil data stok keluar
        if (filterType == null || filterType.equals("Semua") || filterType.equals("Stok Keluar")) {
            List<StockOut> stockOutList = stockOutDAO.getAll();
            for (StockOut so : stockOutList) {
                // Filter tanggal
                if (from != null && so.getOutDate() != null) {
                    LocalDate soDate = LocalDate.parse(so.getOutDate().substring(0, 10));
                    if (soDate.isBefore(from)) continue;
                }
                if (to != null && so.getOutDate() != null) {
                    LocalDate soDate = LocalDate.parse(so.getOutDate().substring(0, 10));
                    if (soDate.isAfter(to)) continue;
                }

                // Filter kategori
                if (filterCategory != null && !filterCategory.equals("Semua")) {
                    Item item = itemDAO.getItemById(so.getItemId());
                    if (item != null && !item.getCategory().equals(filterCategory)) continue;
                }

                String cat = "";
                Item item = itemDAO.getItemById(so.getItemId());
                if (item != null) cat = item.getCategory();

                reportData.add(new ReportRow(
                        so.getOutDate(),
                        "KELUAR",
                        so.getItemCode(),
                        so.getItemName(),
                        cat,
                        so.getBatchNumber(),
                        so.getQuantity(),
                        so.getExpiredDate() != null ? so.getExpiredDate() : "-",
                        "Tujuan: " + (so.getDestination() != null && !so.getDestination().isEmpty()
                                ? so.getDestination() : "-")
                ));
            }
        }

        // Urutkan berdasarkan tanggal (terbaru dulu)
        reportData.sort((a, b) -> {
            if (a.date == null || b.date == null) return 0;
            return b.date.compareTo(a.date);
        });

        tableReport.setItems(reportData);
        lblResultInfo.setText("Menampilkan " + reportData.size() + " data transaksi");
    }

    // ===================== Export Handlers =====================

    /**
     * Export laporan ke file Excel (.xlsx) dengan format tabel rapih.
     */
    @FXML
    private void handleExportExcel() {
        if (reportData.isEmpty()) {
            showAlert("Tidak ada data untuk di-export. Terapkan filter terlebih dahulu.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Laporan Excel");
        fileChooser.setInitialFileName("laporan_clinstock.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = fileChooser.showSaveDialog(tableReport.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Laporan Inventori");

                // === Style Definitions ===
                // Title style
                CellStyle titleStyle = workbook.createCellStyle();
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 16);
                titleFont.setFontName("Segoe UI");
                titleStyle.setFont(titleFont);

                // Subtitle style
                CellStyle subtitleStyle = workbook.createCellStyle();
                Font subtitleFont = workbook.createFont();
                subtitleFont.setFontHeightInPoints((short) 10);
                subtitleFont.setFontName("Segoe UI");
                subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
                subtitleStyle.setFont(subtitleFont);

                // Header style
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

                // Data cell style
                CellStyle dataStyle = workbook.createCellStyle();
                Font dataFont = workbook.createFont();
                dataFont.setFontHeightInPoints((short) 10);
                dataFont.setFontName("Segoe UI");
                dataStyle.setFont(dataFont);
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);

                // MASUK style (green text)
                CellStyle masukStyle = workbook.createCellStyle();
                masukStyle.cloneStyleFrom(dataStyle);
                Font masukFont = workbook.createFont();
                masukFont.setFontHeightInPoints((short) 10);
                masukFont.setFontName("Segoe UI");
                masukFont.setBold(true);
                masukFont.setColor(IndexedColors.GREEN.getIndex());
                masukStyle.setFont(masukFont);
                masukStyle.setAlignment(HorizontalAlignment.CENTER);

                // KELUAR style (red text)
                CellStyle keluarStyle = workbook.createCellStyle();
                keluarStyle.cloneStyleFrom(dataStyle);
                Font keluarFont = workbook.createFont();
                keluarFont.setFontHeightInPoints((short) 10);
                keluarFont.setFontName("Segoe UI");
                keluarFont.setBold(true);
                keluarFont.setColor(IndexedColors.RED.getIndex());
                keluarStyle.setFont(keluarFont);
                keluarStyle.setAlignment(HorizontalAlignment.CENTER);

                // Number center style
                CellStyle numStyle = workbook.createCellStyle();
                numStyle.cloneStyleFrom(dataStyle);
                numStyle.setAlignment(HorizontalAlignment.CENTER);

                // === Row 0: Title ===
                int rowNum = 0;
                Row titleRow = sheet.createRow(rowNum++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("LAPORAN INVENTORI CLINSTOCK");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

                // === Row 1: Filter info ===
                Row filterRow = sheet.createRow(rowNum++);
                StringBuilder filterInfo = new StringBuilder("Filter: ");
                filterInfo.append("Tanggal: ")
                        .append(dateFrom.getValue() != null ? dateFrom.getValue().toString() : "Semua")
                        .append(" s/d ")
                        .append(dateTo.getValue() != null ? dateTo.getValue().toString() : "Semua");
                filterInfo.append(" | Kategori: ").append(comboCategory.getValue());
                filterInfo.append(" | Tipe: ").append(comboType.getValue());
                Cell filterCell = filterRow.createCell(0);
                filterCell.setCellValue(filterInfo.toString());
                filterCell.setCellStyle(subtitleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

                // === Row 2: Date printed ===
                Row dateRow = sheet.createRow(rowNum++);
                Cell dateCell = dateRow.createCell(0);
                dateCell.setCellValue("Tanggal cetak: " + LocalDate.now() + " | Total: " + reportData.size() + " data");
                dateCell.setCellStyle(subtitleStyle);
                sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 9));

                // Empty row
                rowNum++;

                // === Row 4: Headers ===
                String[] headers = {"No", "Tanggal", "Tipe", "Kode", "Nama Barang",
                        "Kategori", "Batch", "Kadaluarsa", "Jumlah", "Keterangan"};
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // === Data Rows ===
                for (int i = 0; i < reportData.size(); i++) {
                    ReportRow r = reportData.get(i);
                    Row row = sheet.createRow(rowNum++);

                    Cell noCell = row.createCell(0);
                    noCell.setCellValue(i + 1);
                    noCell.setCellStyle(numStyle);

                    Cell dateDataCell = row.createCell(1);
                    dateDataCell.setCellValue(safe(r.date));
                    dateDataCell.setCellStyle(dataStyle);

                    Cell typeCell = row.createCell(2);
                    typeCell.setCellValue(r.type);
                    typeCell.setCellStyle(r.type.equals("MASUK") ? masukStyle : keluarStyle);

                    Cell codeCell = row.createCell(3);
                    codeCell.setCellValue(safe(r.itemCode));
                    codeCell.setCellStyle(dataStyle);

                    Cell nameCell = row.createCell(4);
                    nameCell.setCellValue(safe(r.itemName));
                    nameCell.setCellStyle(dataStyle);

                    Cell catCell = row.createCell(5);
                    catCell.setCellValue(safe(r.category));
                    catCell.setCellStyle(dataStyle);

                    Cell batchCell = row.createCell(6);
                    batchCell.setCellValue(safe(r.batch));
                    batchCell.setCellStyle(dataStyle);

                    Cell expiredCell = row.createCell(7);
                    expiredCell.setCellValue(safe(r.expiredDate));
                    expiredCell.setCellStyle(dataStyle);

                    Cell qtyCell = row.createCell(8);
                    qtyCell.setCellValue(r.qty);
                    qtyCell.setCellStyle(numStyle);

                    Cell detailCell = row.createCell(9);
                    detailCell.setCellValue(safe(r.detail));
                    detailCell.setCellStyle(dataStyle);
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                    // Set minimum width
                    if (sheet.getColumnWidth(i) < 3000) {
                        sheet.setColumnWidth(i, 3000);
                    }
                }

                // Write to file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }

                showAlert("Laporan berhasil di-export ke Excel:\n" + file.getAbsolutePath());

            } catch (IOException e) {
                showAlert("Gagal menyimpan file Excel: " + e.getMessage());
            }
        }
    }

    /**
     * Export laporan ke file PDF dengan format tabel rapih.
     */
    @FXML
    private void handleExportPDF() {
        if (reportData.isEmpty()) {
            showAlert("Tidak ada data untuk di-export. Terapkan filter terlebih dahulu.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Laporan PDF");
        fileChooser.setInitialFileName("laporan_clinstock.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(tableReport.getScene().getWindow());

        if (file != null) {
            try {
                Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // === Fonts ===
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
                com.itextpdf.text.Font masukFont = new com.itextpdf.text.Font(bfBold, 8,
                        com.itextpdf.text.Font.BOLD, new BaseColor(34, 197, 94));
                com.itextpdf.text.Font keluarFont = new com.itextpdf.text.Font(bfBold, 8,
                        com.itextpdf.text.Font.BOLD, new BaseColor(239, 68, 68));
                com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(bf, 8,
                        com.itextpdf.text.Font.ITALIC, new BaseColor(100, 116, 139));

                // === Title ===
                Paragraph title = new Paragraph("LAPORAN INVENTORI CLINSTOCK", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(6);
                document.add(title);

                // === Filter info ===
                StringBuilder filterInfo = new StringBuilder();
                filterInfo.append("Tanggal: ")
                        .append(dateFrom.getValue() != null ? dateFrom.getValue().toString() : "Semua")
                        .append(" s/d ")
                        .append(dateTo.getValue() != null ? dateTo.getValue().toString() : "Semua");
                filterInfo.append("  |  Kategori: ").append(comboCategory.getValue());
                filterInfo.append("  |  Tipe: ").append(comboType.getValue());

                Paragraph filterPara = new Paragraph(filterInfo.toString(), subtitleFont);
                filterPara.setAlignment(Element.ALIGN_CENTER);
                filterPara.setSpacingAfter(4);
                document.add(filterPara);

                Paragraph datePara = new Paragraph("Tanggal cetak: " + LocalDate.now()
                        + "  |  Total: " + reportData.size() + " data transaksi", subtitleFont);
                datePara.setAlignment(Element.ALIGN_CENTER);
                datePara.setSpacingAfter(16);
                document.add(datePara);

                // === Table ===
                float[] columnWidths = {30f, 80f, 50f, 60f, 120f, 80f, 80f, 80f, 45f, 140f};
                PdfPTable table = new PdfPTable(columnWidths);
                table.setWidthPercentage(100);

                // Header row
                BaseColor headerBg = new BaseColor(13, 148, 136);
                String[] headers = {"No", "Tanggal", "Tipe", "Kode", "Nama Barang",
                        "Kategori", "Batch", "Kadaluarsa", "Jumlah", "Keterangan"};

                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setBackgroundColor(headerBg);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(6);
                    cell.setBorderColor(new BaseColor(51, 65, 85));
                    table.addCell(cell);
                }

                // Data rows
                BaseColor altRowColor = new BaseColor(241, 245, 249);
                for (int i = 0; i < reportData.size(); i++) {
                    ReportRow r = reportData.get(i);
                    BaseColor rowBg = (i % 2 == 1) ? altRowColor : BaseColor.WHITE;

                    // No
                    addPdfCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                    // Tanggal
                    addPdfCell(table, safe(r.date), dataFont, rowBg, Element.ALIGN_LEFT);
                    // Tipe — colored
                    com.itextpdf.text.Font typeFont = r.type.equals("MASUK") ? masukFont : keluarFont;
                    addPdfCell(table, r.type, typeFont, rowBg, Element.ALIGN_CENTER);
                    // Kode
                    addPdfCell(table, safe(r.itemCode), dataFont, rowBg, Element.ALIGN_LEFT);
                    // Nama
                    addPdfCell(table, safe(r.itemName), dataFont, rowBg, Element.ALIGN_LEFT);
                    // Kategori
                    addPdfCell(table, safe(r.category), dataFont, rowBg, Element.ALIGN_LEFT);
                    // Batch
                    addPdfCell(table, safe(r.batch), dataFont, rowBg, Element.ALIGN_LEFT);
                    // Kadaluarsa
                    addPdfCell(table, safe(r.expiredDate), dataFont, rowBg, Element.ALIGN_CENTER);
                    // Jumlah
                    addPdfCell(table, String.valueOf(r.qty), dataFont, rowBg, Element.ALIGN_CENTER);
                    // Keterangan
                    addPdfCell(table, safe(r.detail), dataFont, rowBg, Element.ALIGN_LEFT);
                }

                document.add(table);

                // === Footer ===
                document.add(new Paragraph(" "));
                Paragraph footer = new Paragraph("Dicetak oleh sistem ClinStock v1.0 — "
                        + LocalDate.now(), footerFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);

                document.close();

                showAlert("Laporan berhasil di-export ke PDF:\n" + file.getAbsolutePath());

            } catch (Exception e) {
                showAlert("Gagal menyimpan file PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper: tambah cell ke PdfPTable dengan styling.
     */
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

    // ===================== Helpers =====================

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ClinStock — Export");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== Inner Data Class =====================

    /** Representasi satu baris di tabel laporan. */
    public static class ReportRow {
        public final String date;
        public final String type;
        public final String itemCode;
        public final String itemName;
        public final String category;
        public final String batch;
        public final int qty;
        public final String expiredDate;
        public final String detail;

        public ReportRow(String date, String type, String itemCode, String itemName,
                         String category, String batch, int qty, String expiredDate, String detail) {
            this.date = date;
            this.type = type;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.category = category;
            this.batch = batch;
            this.qty = qty;
            this.expiredDate = expiredDate;
            this.detail = detail;
        }
    }
}
