package com.clinstock.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Singleton class untuk mengelola koneksi ke database SQLite.
 *
 * <p>Class ini memastikan hanya ada SATU instance koneksi database
 * yang aktif di seluruh lifecycle aplikasi (Singleton Pattern).</p>
 *
 * <p>Database file akan dibuat di direktori kerja aplikasi
 * dengan nama 'clinstock.db'.</p>
 *
 * <p>Penggunaan:
 * <pre>
 *     Connection conn = DatabaseConnection.getInstance().getConnection();
 * </pre>
 * </p>
 */
public class DatabaseConnection {

    // Nama file database SQLite
    private static final String DB_NAME = "clinstock.db";

    // URL koneksi JDBC untuk SQLite
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;

    // Path ke file SQL inisialisasi di resources
    private static final String INIT_SQL_PATH = "/sql/init_database.sql";

    // Instance tunggal (Singleton)
    private static DatabaseConnection instance;

    // Objek koneksi JDBC
    private Connection connection;

    /**
     * Private constructor — mencegah instansiasi dari luar.
     * Membuka koneksi ke SQLite dan menjalankan script inisialisasi.
     */
    private DatabaseConnection() {
        try {
            // Buka koneksi ke SQLite (file akan otomatis dibuat jika belum ada)
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] Koneksi ke database '" + DB_NAME + "' berhasil.");

            // Aktifkan foreign key support
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            // Jalankan script inisialisasi tabel
            initializeDatabase();

        } catch (SQLException e) {
            System.err.println("[DB] GAGAL terhubung ke database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mendapatkan instance tunggal dari DatabaseConnection.
     * Jika belum ada, instance baru akan dibuat (lazy initialization).
     * Jika koneksi sebelumnya sudah tertutup, koneksi baru akan dibuat.
     *
     * @return Instance DatabaseConnection
     */
    public static synchronized DatabaseConnection getInstance() {
        try {
            if (instance == null || instance.getConnection() == null || instance.getConnection().isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error saat memeriksa koneksi: " + e.getMessage());
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Mendapatkan objek Connection JDBC yang aktif.
     *
     * @return Objek Connection ke database SQLite
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Menjalankan script SQL inisialisasi dari file resource.
     * Script ini membuat tabel-tabel yang diperlukan jika belum ada.
     */
    private void initializeDatabase() {
        try (InputStream inputStream = getClass().getResourceAsStream(INIT_SQL_PATH)) {
            if (inputStream == null) {
                System.err.println("[DB] File SQL inisialisasi tidak ditemukan: " + INIT_SQL_PATH);
                return;
            }

            // Baca isi file SQL dan hapus baris komentar
            String sql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                sql = reader.lines()
                            .map(String::trim)
                            .filter(line -> !line.startsWith("--"))
                            .collect(Collectors.joining("\n"));
            }

            // Eksekusi setiap statement SQL (dipisah oleh ';')
            try (Statement stmt = connection.createStatement()) {
                String[] statements = sql.split(";");
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }

            System.out.println("[DB] Inisialisasi database berhasil.");

        } catch (Exception e) {
            System.err.println("[DB] Gagal menginisialisasi database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Menutup koneksi database.
     * Dipanggil saat aplikasi ditutup / shutdown.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Koneksi database ditutup.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Gagal menutup koneksi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
