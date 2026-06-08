package com.clinstock.dao;

import com.clinstock.util.DatabaseConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO khusus untuk menyediakan data Dashboard dan analitik.
 * Menghasilkan data agregat untuk chart (PieChart, BarChart)
 * dan ringkasan status inventori.
 */
public class DashboardDAO {

    private final Connection connection;

    public DashboardDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /** Menghitung total jumlah jenis item yang terdaftar. */
    public int getTotalItemCount() {
        return queryInt("SELECT COUNT(*) FROM items");
    }

    /** Menghitung jumlah item yang stoknya di bawah minimum. */
    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) FROM ("
                + "  SELECT i.id FROM items i "
                + "  LEFT JOIN stock_in si ON i.id = si.item_id "
                + "  GROUP BY i.id "
                + "  HAVING COALESCE(SUM(si.remaining_qty), 0) < i.min_stock"
                + ")";
        return queryInt(sql);
    }

    /** Menghitung jumlah batch yang akan kadaluarsa dalam 30 hari. */
    public int getExpiringSoonCount() {
        String sql = "SELECT COUNT(*) FROM stock_in "
                + "WHERE remaining_qty > 0 "
                + "AND date(expired_date) <= date('now', '+30 days') "
                + "AND date(expired_date) >= date('now')";
        return queryInt(sql);
    }

    /** Menghitung jumlah batch yang akan kadaluarsa dalam 3 bulan (90 hari). */
    public int getExpiringInThreeMonthsCount() {
        String sql = "SELECT COUNT(*) FROM stock_in "
                + "WHERE remaining_qty > 0 "
                + "AND date(expired_date) <= date('now', '+3 months') "
                + "AND date(expired_date) >= date('now')";
        return queryInt(sql);
    }

    /** Menghitung jumlah batch yang sudah kadaluarsa. */
    public int getExpiredCount() {
        String sql = "SELECT COUNT(*) FROM stock_in "
                + "WHERE remaining_qty > 0 "
                + "AND date(expired_date) < date('now')";
        return queryInt(sql);
    }

    /** Menghitung total seluruh stok yang tersedia. */
    public int getTotalCurrentStock() {
        return queryInt("SELECT COALESCE(SUM(remaining_qty), 0) FROM stock_in WHERE remaining_qty > 0");
    }

    /** Menghitung total transaksi stok masuk bulan ini. */
    public int getStockInThisMonth() {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_in "
                + "WHERE strftime('%Y-%m', input_date) = strftime('%Y-%m', 'now')";
        return queryInt(sql);
    }

    /** Menghitung total transaksi stok keluar bulan ini. */
    public int getStockOutThisMonth() {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_out "
                + "WHERE strftime('%Y-%m', out_date) = strftime('%Y-%m', 'now')";
        return queryInt(sql);
    }

    /**
     * Data untuk PieChart: distribusi stok per kategori.
     * 
     * @return Map<NamaKategori, JumlahStok>
     */
    public Map<String, Integer> getStockByCategory() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT i.category, COALESCE(SUM(si.remaining_qty), 0) AS total "
                + "FROM items i "
                + "LEFT JOIN stock_in si ON i.id = si.item_id "
                + "GROUP BY i.category ORDER BY total DESC";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                data.put(rs.getString("category"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error getStockByCategory: " + e.getMessage());
        }
        return data;
    }

    /**
     * Data untuk BarChart: pergerakan stok per bulan (6 bulan terakhir).
     * 
     * @return Map<Bulan, int[]> dimana int[0]=masuk, int[1]=keluar
     */
    public Map<String, int[]> getMonthlyMovement() {
        Map<String, int[]> data = new LinkedHashMap<>();

        // Inisialisasi 6 bulan terakhir
        String initSql = "WITH RECURSIVE months(m) AS ("
                + "  SELECT strftime('%Y-%m', 'now', '-5 months') "
                + "  UNION ALL "
                + "  SELECT strftime('%Y-%m', m || '-01', '+1 month') FROM months "
                + "  WHERE m < strftime('%Y-%m', 'now')"
                + ") SELECT m FROM months";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(initSql)) {
            while (rs.next()) {
                data.put(rs.getString("m"), new int[] { 0, 0 });
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error init months: " + e.getMessage());
        }

        // Stok masuk per bulan
        String inSql = "SELECT strftime('%Y-%m', input_date) AS month, "
                + "COALESCE(SUM(quantity), 0) AS total "
                + "FROM stock_in "
                + "WHERE input_date >= date('now', '-6 months') "
                + "GROUP BY month";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(inSql)) {
            while (rs.next()) {
                String m = rs.getString("month");
                if (data.containsKey(m))
                    data.get(m)[0] = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error stok masuk per bulan: " + e.getMessage());
        }

        // Stok keluar per bulan
        String outSql = "SELECT strftime('%Y-%m', out_date) AS month, "
                + "COALESCE(SUM(quantity), 0) AS total "
                + "FROM stock_out "
                + "WHERE out_date >= date('now', '-6 months') "
                + "GROUP BY month";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(outSql)) {
            while (rs.next()) {
                String m = rs.getString("month");
                if (data.containsKey(m))
                    data.get(m)[1] = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error stok keluar per bulan: " + e.getMessage());
        }

        return data;
    }

    /** Helper: menjalankan query yang mengembalikan satu integer. */
    private int queryInt(String sql) {
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error: " + e.getMessage());
        }
        return 0;
    }
}
