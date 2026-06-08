package com.clinstock.dao;

import com.clinstock.model.User;
import com.clinstock.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) untuk entitas User.
 *
 * <p>Class ini menangani semua operasi database yang berkaitan
 * dengan tabel 'users', termasuk autentikasi login.</p>
 *
 * <p>Pada tahap selanjutnya, class ini akan diperluas dengan
 * method CRUD lengkap (create, update, delete, getAll).</p>
 */
public class UserDAO {

    // Instance koneksi database
    private final Connection connection;

    /**
     * Constructor — mengambil koneksi dari Singleton DatabaseConnection.
     */
    public UserDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Memvalidasi kredensial login pengguna.
     *
     * <p>Mencocokkan username dan password terhadap data di tabel 'users'.
     * Hanya akun yang aktif (is_active = 1) yang dapat login.</p>
     *
     * @param username Username yang diinput
     * @param password Password yang diinput (plain text — akan di-hash pada tahap berikutnya)
     * @return Objek {@link User} jika login berhasil, {@code null} jika gagal
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT id, username, full_name, role FROM users "
                   + "WHERE username = ? AND password = ? AND is_active = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Login berhasil — buat objek User dari hasil query
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat autentikasi user: " + e.getMessage());
            e.printStackTrace();
        }

        // Login gagal — username/password salah atau akun non-aktif
        return null;
    }

    /**
     * Mencari user berdasarkan username.
     *
     * @param username Username yang dicari
     * @return Objek {@link User} jika ditemukan, {@code null} jika tidak
     */
    public User findByUsername(String username) {
        String sql = "SELECT id, username, full_name, role, is_active FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("full_name"));
                    user.setRole(rs.getString("role"));
                    user.setActive(rs.getInt("is_active") == 1);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat mencari user: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
