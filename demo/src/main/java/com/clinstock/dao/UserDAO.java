package com.clinstock.dao;

import com.clinstock.model.User;
import com.clinstock.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public static final String INITIAL_MANAGER_USERNAME = "manajer";

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

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, full_name, role, is_active, created_at, updated_at "
                   + "FROM users ORDER BY role, username";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat mengambil daftar user: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    public List<User> searchUsers(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, full_name, role, is_active, created_at, updated_at "
                   + "FROM users "
                   + "WHERE username LIKE ? OR full_name LIKE ? OR role LIKE ? "
                   + "ORDER BY role, username";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat mencari user: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    public boolean insertUser(User user) {
        String sql = "INSERT INTO users (username, password, full_name, role, is_active) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setInt(5, user.isActive() ? 1 : 0);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat menambahkan user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(User user, boolean updatePassword) {
        String sql;
        if (updatePassword) {
            sql = "UPDATE users SET username=?, password=?, full_name=?, role=?, is_active=?, "
                + "updated_at=datetime('now','localtime') WHERE id=?";
        } else {
            sql = "UPDATE users SET username=?, full_name=?, role=?, is_active=?, "
                + "updated_at=datetime('now','localtime') WHERE id=?";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            if (updatePassword) {
                pstmt.setString(2, user.getPassword());
                pstmt.setString(3, user.getFullName());
                pstmt.setString(4, user.getRole());
                pstmt.setInt(5, user.isActive() ? 1 : 0);
                pstmt.setInt(6, user.getId());
            } else {
                pstmt.setString(2, user.getFullName());
                pstmt.setString(3, user.getRole());
                pstmt.setInt(4, user.isActive() ? 1 : 0);
                pstmt.setInt(5, user.getId());
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat memperbarui user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ? AND username <> ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, INITIAL_MANAGER_USERNAME);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] Error saat menghapus user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getInt("is_active") == 1);
        user.setCreatedAt(rs.getString("created_at"));
        user.setUpdatedAt(rs.getString("updated_at"));
        return user;
    }
}
