package com.clinstock.dao;

import com.clinstock.model.StockIn;
import com.clinstock.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk operasi pada tabel 'stock_in'.
 * Menangani pencatatan barang masuk per-batch dan query untuk FEFO.
 */
public class StockInDAO {

    private final Connection connection;

    public StockInDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Menambahkan record stok masuk baru.
     * remaining_qty diset sama dengan quantity (stok penuh).
     */
    public boolean insert(StockIn stockIn) {
        String sql = "INSERT INTO stock_in (item_id, batch_number, expired_date, "
                   + "quantity, remaining_qty, supplier, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, stockIn.getItemId());
            pstmt.setString(2, stockIn.getBatchNumber());
            pstmt.setString(3, stockIn.getExpiredDate());
            pstmt.setInt(4, stockIn.getQuantity());
            pstmt.setInt(5, stockIn.getQuantity()); // remaining = quantity awal
            pstmt.setString(6, stockIn.getSupplier() != null ? stockIn.getSupplier() : "");
            pstmt.setString(7, stockIn.getNotes() != null ? stockIn.getNotes() : "");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mendapatkan semua riwayat stok masuk (JOIN dengan items untuk nama).
     */
    public List<StockIn> getAll() {
        List<StockIn> list = new ArrayList<>();
        String sql = "SELECT si.*, i.name AS item_name, i.item_code "
                   + "FROM stock_in si "
                   + "JOIN items i ON si.item_id = i.id "
                   + "ORDER BY si.input_date DESC, si.id DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error getAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Mendapatkan batch-batch yang masih punya sisa stok untuk item tertentu.
     * Diurutkan berdasarkan expired_date ASC (untuk algoritma FEFO).
     */
    public List<StockIn> getAvailableBatchesFEFO(int itemId) {
        List<StockIn> list = new ArrayList<>();
        String sql = "SELECT si.*, i.name AS item_name, i.item_code "
                   + "FROM stock_in si "
                   + "JOIN items i ON si.item_id = i.id "
                   + "WHERE si.item_id = ? AND si.remaining_qty > 0 "
                   + "ORDER BY si.expired_date ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error getAvailableBatchesFEFO: " + e.getMessage());
        }
        return list;
    }

    /**
     * Mengurangi remaining_qty pada batch tertentu.
     * Digunakan oleh algoritma FEFO saat stok keluar.
     */
    public boolean reduceRemainingQty(int stockInId, int reduceBy) {
        String sql = "UPDATE stock_in SET remaining_qty = remaining_qty - ? WHERE id = ? AND remaining_qty >= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, reduceBy);
            pstmt.setInt(2, stockInId);
            pstmt.setInt(3, reduceBy);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error reduceRemainingQty: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mendapatkan total stok tersedia untuk item tertentu.
     */
    public int getTotalAvailableStock(int itemId) {
        String sql = "SELECT COALESCE(SUM(remaining_qty), 0) FROM stock_in WHERE item_id = ? AND remaining_qty > 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error getTotalAvailableStock: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Mendapatkan batch-batch yang akan kadaluarsa dalam N hari ke depan.
     */
    public List<StockIn> getExpiringSoon(int days) {
        List<StockIn> list = new ArrayList<>();
        String sql = "SELECT si.*, i.name AS item_name, i.item_code "
                   + "FROM stock_in si "
                   + "JOIN items i ON si.item_id = i.id "
                   + "WHERE si.remaining_qty > 0 "
                   + "AND date(si.expired_date) <= date('now', '+" + days + " days') "
                   + "AND date(si.expired_date) >= date('now') "
                   + "ORDER BY si.expired_date ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error getExpiringSoon: " + e.getMessage());
        }
        return list;
    }

    /**
     * Mendapatkan batch-batch yang sudah kadaluarsa (expired).
     */
    public List<StockIn> getExpiredBatches() {
        List<StockIn> list = new ArrayList<>();
        String sql = "SELECT si.*, i.name AS item_name, i.item_code "
                   + "FROM stock_in si "
                   + "JOIN items i ON si.item_id = i.id "
                   + "WHERE si.remaining_qty > 0 "
                   + "AND date(si.expired_date) < date('now') "
                   + "ORDER BY si.expired_date ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error getExpiredBatches: " + e.getMessage());
        }
        return list;
    }

    /**
     * Menghapus record stok masuk (batch).
     * Hanya boleh didelete jika sisa stok sama dengan kuantitas awal (belum pernah dikeluarkan).
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM stock_in WHERE id = ? AND remaining_qty = quantity";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[StockInDAO] Error delete: " + e.getMessage());
            return false;
        }
    }

    /** Helper: map ResultSet ke StockIn */
    private StockIn mapResultSet(ResultSet rs) throws SQLException {
        StockIn si = new StockIn();
        si.setId(rs.getInt("id"));
        si.setItemId(rs.getInt("item_id"));
        si.setBatchNumber(rs.getString("batch_number"));
        si.setExpiredDate(rs.getString("expired_date"));
        si.setQuantity(rs.getInt("quantity"));
        si.setRemainingQty(rs.getInt("remaining_qty"));
        si.setSupplier(rs.getString("supplier"));
        si.setInputDate(rs.getString("input_date"));
        si.setNotes(rs.getString("notes"));
        si.setCreatedAt(rs.getString("created_at"));
        try {
            si.setItemName(rs.getString("item_name"));
            si.setItemCode(rs.getString("item_code"));
        } catch (SQLException ignored) {}
        return si;
    }
}
