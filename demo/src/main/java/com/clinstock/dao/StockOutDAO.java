package com.clinstock.dao;

import com.clinstock.model.StockIn;
import com.clinstock.model.StockOut;
import com.clinstock.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk operasi pada tabel 'stock_out'.
 * Mengimplementasikan algoritma FEFO (First Expired, First Out)
 * untuk pengeluaran stok otomatis.
 */
public class StockOutDAO {

    private final Connection connection;
    private final StockInDAO stockInDAO;

    public StockOutDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.stockInDAO = new StockInDAO();
    }

    /**
     * Proses stok keluar menggunakan algoritma FEFO.
     *
     * <p>Algoritma:
     * 1. Ambil batch-batch item yang masih punya sisa stok, urut expired_date ASC
     * 2. Potong dari batch dengan expired date paling dekat ke masa kini
     * 3. Jika batch pertama kurang, lanjut ke batch berikutnya
     * 4. Catat setiap potongan sebagai record stock_out terpisah</p>
     *
     * @param itemId      ID item yang dikeluarkan
     * @param quantity    Jumlah yang dikeluarkan
     * @param destination Tujuan pengeluaran
     * @param notes       Catatan tambahan
     * @return List batch yang terpotong (untuk laporan), kosong jika gagal
     */
    public List<StockOut> processStockOutFEFO(int itemId, int quantity,
                                               String destination, String notes) {
        List<StockOut> processedBatches = new ArrayList<>();

        // Cek total stok tersedia
        int availableStock = stockInDAO.getTotalAvailableStock(itemId);
        if (availableStock < quantity) {
            System.err.println("[FEFO] Stok tidak cukup. Tersedia: " + availableStock
                    + ", Diminta: " + quantity);
            return processedBatches; // Kembalikan list kosong
        }

        // Ambil batch-batch FEFO (urut expired_date ASC)
        List<StockIn> batches = stockInDAO.getAvailableBatchesFEFO(itemId);

        try {
            // Mulai transaksi
            connection.setAutoCommit(false);

            int remaining = quantity;

            for (StockIn batch : batches) {
                if (remaining <= 0) break;

                // Hitung berapa yang dipotong dari batch ini
                int deduct = Math.min(remaining, batch.getRemainingQty());

                // Kurangi remaining_qty di stock_in
                if (!stockInDAO.reduceRemainingQty(batch.getId(), deduct)) {
                    throw new SQLException("Gagal mengurangi stok batch ID: " + batch.getId());
                }

                // Catat record stock_out
                String sql = "INSERT INTO stock_out (item_id, batch_number, quantity, "
                           + "destination, notes) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, itemId);
                    pstmt.setString(2, batch.getBatchNumber());
                    pstmt.setInt(3, deduct);
                    pstmt.setString(4, destination != null ? destination : "");
                    pstmt.setString(5, notes != null ? notes : "");
                    pstmt.executeUpdate();
                }

                // Catat ke result list
                StockOut outRecord = new StockOut(itemId, batch.getBatchNumber(),
                        deduct, destination, notes);
                outRecord.setItemName(batch.getItemName());
                outRecord.setExpiredDate(batch.getExpiredDate());
                processedBatches.add(outRecord);

                remaining -= deduct;
                System.out.println("[FEFO] Potong batch " + batch.getBatchNumber()
                        + " (exp: " + batch.getExpiredDate() + ") sebanyak " + deduct);
            }

            // Commit transaksi
            connection.commit();
            System.out.println("[FEFO] Stok keluar berhasil diproses. Total batch terpotong: "
                    + processedBatches.size());

        } catch (SQLException e) {
            // Rollback jika ada error
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[FEFO] Error saat proses stok keluar: " + e.getMessage());
            processedBatches.clear();
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }

        return processedBatches;
    }

    /**
     * Mendapatkan semua riwayat stok keluar.
     */
    public List<StockOut> getAll() {
        List<StockOut> list = new ArrayList<>();
        String sql = "SELECT so.*, i.name AS item_name, i.item_code, si.expired_date "
                   + "FROM stock_out so "
                   + "JOIN items i ON so.item_id = i.id "
                   + "LEFT JOIN stock_in si ON so.item_id = si.item_id AND so.batch_number = si.batch_number "
                   + "ORDER BY so.out_date DESC, so.id DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[StockOutDAO] Error getAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Membatalkan/menghapus transaksi stok keluar dan mengembalikan jumlahnya ke batch asal di stock_in.
     */
    public boolean deleteStockOut(int stockOutId, int itemId, String batchNumber, int quantity) {
        String updateStockInSql = "UPDATE stock_in SET remaining_qty = remaining_qty + ? "
                + "WHERE item_id = ? AND batch_number = ?";
        String deleteStockOutSql = "DELETE FROM stock_out WHERE id = ?";
        try {
            connection.setAutoCommit(false);

            // 1. Kembalikan stok ke stock_in
            try (PreparedStatement pstmt1 = connection.prepareStatement(updateStockInSql)) {
                pstmt1.setInt(1, quantity);
                pstmt1.setInt(2, itemId);
                pstmt1.setString(3, batchNumber);
                pstmt1.executeUpdate();
            }

            // 2. Hapus record stock_out
            try (PreparedStatement pstmt2 = connection.prepareStatement(deleteStockOutSql)) {
                pstmt2.setInt(1, stockOutId);
                int deleted = pstmt2.executeUpdate();
                if (deleted == 0) {
                    throw new SQLException("Record stock_out tidak ditemukan atau gagal dihapus.");
                }
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[StockOutDAO] Error deleteStockOut: " + e.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /** Helper: map ResultSet ke StockOut */
    private StockOut mapResultSet(ResultSet rs) throws SQLException {
        StockOut so = new StockOut();
        so.setId(rs.getInt("id"));
        so.setItemId(rs.getInt("item_id"));
        so.setBatchNumber(rs.getString("batch_number"));
        so.setQuantity(rs.getInt("quantity"));
        so.setOutDate(rs.getString("out_date"));
        so.setDestination(rs.getString("destination"));
        so.setNotes(rs.getString("notes"));
        so.setCreatedAt(rs.getString("created_at"));
        try {
            so.setItemName(rs.getString("item_name"));
            so.setItemCode(rs.getString("item_code"));
        } catch (SQLException ignored) {}
        try {
            so.setExpiredDate(rs.getString("expired_date"));
        } catch (SQLException ignored) {}
        return so;
    }
}
