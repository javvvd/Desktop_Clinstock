package com.clinstock.model;

/**
 * Model class yang merepresentasikan entitas Item (obat/alat kesehatan).
 * Sesuai dengan tabel 'items' di database SQLite.
 */
public class Item {

    private int id;
    private String itemCode;
    private String name;
    private String category;
    private String unit;
    private int minStock;
    private String description;
    private String createdAt;
    private String updatedAt;

    /** Field tambahan — stok saat ini (dihitung dari SUM remaining_qty di stock_in) */
    private int currentStock;

    // ===================== Constructors =====================

    public Item() {
    }

    public Item(int id, String itemCode, String name, String category,
                String unit, int minStock) {
        this.id = id;
        this.itemCode = itemCode;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.minStock = minStock;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    /**
     * Mengecek apakah stok item ini di bawah batas minimum.
     * @return true jika stok saat ini < min_stock
     */
    public boolean isLowStock() {
        return currentStock < minStock;
    }

    @Override
    public String toString() {
        return itemCode + " — " + name;
    }
}
