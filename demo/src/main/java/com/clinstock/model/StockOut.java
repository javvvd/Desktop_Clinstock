package com.clinstock.model;

/**
 * Model class untuk transaksi Stok Keluar.
 * Sesuai dengan tabel 'stock_out' di database SQLite.
 */
public class StockOut {

    private int id;
    private int itemId;
    private String batchNumber;
    private int quantity;
    private String outDate;
    private String destination;
    private String notes;
    private String createdAt;

    /** Field tambahan untuk display di tabel */
    private String itemName;
    private String itemCode;
    private String expiredDate;

    // ===================== Constructors =====================

    public StockOut() {
    }

    public StockOut(int itemId, String batchNumber, int quantity,
                    String destination, String notes) {
        this.itemId = itemId;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.destination = destination;
        this.notes = notes;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getOutDate() { return outDate; }
    public void setOutDate(String outDate) { this.outDate = outDate; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getExpiredDate() { return expiredDate; }
    public void setExpiredDate(String expiredDate) { this.expiredDate = expiredDate; }

    @Override
    public String toString() {
        return "StockOut{batch=" + batchNumber + ", qty=" + quantity
                + ", dest=" + destination + "}";
    }
}
