package com.clinstock.util;

import com.clinstock.model.User;

/**
 * Singleton class untuk mengelola sesi pengguna yang sedang login.
 *
 * <p>Menyimpan informasi user yang berhasil login agar dapat diakses
 * dari seluruh bagian aplikasi tanpa perlu passing parameter.</p>
 *
 * <p>Penggunaan:
 * <pre>
 *     // Set saat login berhasil
 *     SessionManager.getInstance().setCurrentUser(user);
 *
 *     // Ambil data user yang sedang login
 *     User current = SessionManager.getInstance().getCurrentUser();
 *
 *     // Hapus saat logout
 *     SessionManager.getInstance().clearSession();
 * </pre>
 * </p>
 */
public class SessionManager {

    private static SessionManager instance;

    // User yang sedang login
    private User currentUser;

    /** Private constructor — Singleton Pattern */
    private SessionManager() {
    }

    /**
     * Mendapatkan instance tunggal SessionManager.
     *
     * @return Instance SessionManager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Menyimpan data user yang berhasil login.
     *
     * @param user Objek User yang berhasil login
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Mendapatkan data user yang sedang login.
     *
     * @return Objek User yang sedang login, null jika belum login
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Mengecek apakah ada user yang sedang login.
     *
     * @return true jika ada user yang sedang login
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Menghapus sesi (logout).
     */
    public void clearSession() {
        currentUser = null;
    }

    /**
     * Mengecek apakah user yang login memiliki role tertentu.
     *
     * @param role Role yang dicek
     * @return true jika role user sesuai
     */
    public boolean hasRole(String role) {
        return currentUser != null && currentUser.getRole().equals(role);
    }
}
