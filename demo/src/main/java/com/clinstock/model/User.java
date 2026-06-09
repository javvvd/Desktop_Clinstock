package com.clinstock.model;

/**
 * Model class yang merepresentasikan entitas User.
 * Sesuai dengan tabel 'users' di database SQLite.
 *
 * Role yang tersedia:
 * - admin_farmasi   : Mengelola data master & stok masuk
 * - petugas_klinik  : Transaksi stok keluar & cek stok
 * - manajer_klinik  : Dashboard & laporan analitik
 */
public class User {

    private int id;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private boolean isActive;
    private String createdAt;
    private String updatedAt;

    // ===================== Constructors =====================

    /** Default constructor */
    public User() {
    }

    /**
     * Constructor untuk proses login (field minimal).
     *
     * @param id       ID user dari database
     * @param username Username
     * @param fullName Nama lengkap user
     * @param role     Role / hak akses user
     */
    public User(int id, String username, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    /**
     * Full constructor untuk kebutuhan CRUD lengkap.
     */
    public User(int id, String username, String password, String fullName,
                String role, boolean isActive, String createdAt, String updatedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===================== Getters & Setters =====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ===================== Utility Methods =====================

    /**
     * Mendapatkan label role yang user-friendly (untuk ditampilkan di UI).
     *
     * @return Label role dalam Bahasa Indonesia
     */
    public String getRoleDisplayName() {
        if (role == null) return "Tidak Dikenal";
        return switch (role) {
            case "admin_farmasi"  -> "Admin Farmasi";
            case "petugas_klinik" -> "Petugas Klinik";
            case "manajer_klinik" -> "Manajer Klinik";
            default               -> "Tidak Dikenal";
        };
    }

    public String getStatusDisplayName() {
        return isActive ? "Aktif" : "Nonaktif";
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
