package com.clinstock.dao;

import com.clinstock.model.Item;
import com.clinstock.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk operasi CRUD pada tabel 'items'.
 * Mendukung pencarian, pagination, dan kalkulasi stok saat ini.
 */
public class ItemDAO {

    private final Connection connection;

    public ItemDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Mendapatkan semua item beserta stok saat ini.
     * Stok dihitung dari SUM(remaining_qty) di tabel stock_in.
     */
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT i.*, COALESCE(SUM(si.remaining_qty), 0) AS current_stock "
                   + "FROM items i "
                   + "LEFT JOIN stock_in si ON i.id = si.item_id "
                   + "GROUP BY i.id ORDER BY i.name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error getAllItems: " + e.getMessage());
        }
        return items;
    }

    /**
     * Mencari item berdasarkan keyword (nama atau kode).
     */
    public List<Item> searchItems(String keyword) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT i.*, COALESCE(SUM(si.remaining_qty), 0) AS current_stock "
                   + "FROM items i "
                   + "LEFT JOIN stock_in si ON i.id = si.item_id "
                   + "WHERE i.name LIKE ? OR i.item_code LIKE ? OR i.category LIKE ? "
                   + "GROUP BY i.id ORDER BY i.name";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error searchItems: " + e.getMessage());
        }
        return items;
    }

    /**
     * Mendapatkan item berdasarkan ID.
     */
    public Item getItemById(int id) {
        String sql = "SELECT i.*, COALESCE(SUM(si.remaining_qty), 0) AS current_stock "
                   + "FROM items i "
                   + "LEFT JOIN stock_in si ON i.id = si.item_id "
                   + "WHERE i.id = ? GROUP BY i.id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToItem(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error getItemById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Menambahkan item baru ke database.
     * @return true jika berhasil
     */
    public boolean insertItem(Item item) {
        String sql = "INSERT INTO items (item_code, name, category, unit, min_stock, description) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item.getItemCode());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getCategory());
            pstmt.setString(4, item.getUnit());
            pstmt.setInt(5, item.getMinStock());
            pstmt.setString(6, item.getDescription() != null ? item.getDescription() : "");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error insertItem: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mengupdate item yang sudah ada.
     */
    public boolean updateItem(Item item) {
        String sql = "UPDATE items SET item_code=?, name=?, category=?, unit=?, min_stock=?, "
                   + "description=?, updated_at=datetime('now','localtime') WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item.getItemCode());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getCategory());
            pstmt.setString(4, item.getUnit());
            pstmt.setInt(5, item.getMinStock());
            pstmt.setString(6, item.getDescription() != null ? item.getDescription() : "");
            pstmt.setInt(7, item.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error updateItem: " + e.getMessage());
            return false;
        }
    }

    /**
     * Menghapus item berdasarkan ID.
     */
    public boolean deleteItem(int id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error deleteItem: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mendapatkan item yang stoknya di bawah minimum.
     */
    public List<Item> getLowStockItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT i.*, COALESCE(SUM(si.remaining_qty), 0) AS current_stock "
                   + "FROM items i "
                   + "LEFT JOIN stock_in si ON i.id = si.item_id "
                   + "GROUP BY i.id HAVING current_stock < i.min_stock "
                   + "ORDER BY current_stock ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Error getLowStockItems: " + e.getMessage());
        }
        return items;
    }

    /** Helper: map ResultSet ke objek Item */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setItemCode(rs.getString("item_code"));
        item.setName(rs.getString("name"));
        item.setCategory(rs.getString("category"));
        item.setUnit(rs.getString("unit"));
        item.setMinStock(rs.getInt("min_stock"));
        item.setDescription(rs.getString("description"));
        item.setCreatedAt(rs.getString("created_at"));
        item.setUpdatedAt(rs.getString("updated_at"));
        try { item.setCurrentStock(rs.getInt("current_stock")); }
        catch (SQLException ignored) { /* kolom tidak ada di query tertentu */ }
        return item;
    }
}
