package com.clinstock.model;

/**
 * Model class untuk transaksi Stok Masuk (per-batch).
 * Sesuai dengan tabel 'stock_in' di database SQLite.
 *
 * Kolom remaining_qty melacak sisa stok di batch ini
 * untuk mendukung algoritma FEFO (First Expired, First Out).
 */
public class StockIn {

    private int id;
    private int itemId;
    private String batchNumber;
    private String expiredDate;
    private int quantity;
    private int remainingQty;
    private String supplier;
    private String inputDate;
    private String notes;
    private String createdAt;

    /** Field tambahan untuk display di tabel */
    private String itemName;
    private String itemCode;

    // ===================== Constructors =====================

    public StockIn() {
    }

    public StockIn(int itemId, String batchNumber, String expiredDate,
                   int quantity, String supplier, String notes) {
        this.itemId = itemId;
        this.batchNumber = batchNumber;
        this.expiredDate = expiredDate;
        this.quantity = quantity;
        this.remainingQty = quantity; // Awalnya remaining = quantity
        this.supplier = supplier;
        this.notes = notes;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getExpiredDate() { return expiredDate; }
    public void setExpiredDate(String expiredDate) { this.expiredDate = expiredDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getRemainingQty() { return remainingQty; }
    public void setRemainingQty(int remainingQty) { this.remainingQty = remainingQty; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getInputDate() { return inputDate; }
    public void setInputDate(String inputDate) { this.inputDate = inputDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    @Override
    public String toString() {
        return "StockIn{batch=" + batchNumber + ", qty=" + quantity
                + ", remaining=" + remainingQty + ", exp=" + expiredDate + "}";
    }
}
