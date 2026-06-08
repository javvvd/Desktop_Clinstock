package com.clinstock.controller;

import com.clinstock.App;
import com.clinstock.dao.UserDAO;
import com.clinstock.model.User;
import com.clinstock.util.SessionManager;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller untuk halaman Login (LoginView.fxml).
 *
 * Menangani autentikasi pengguna dan navigasi ke MainView setelah login berhasil.
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    private final UserDAO userDAO;

    public LoginController() {
        this.userDAO = new UserDAO();
    }

    @FXML
    public void initialize() {
        txtUsername.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) txtUsername.requestFocus();
        });
    }

    /**
     * Handler untuk tombol "Masuk" dan Enter di PasswordField.
     */
    @FXML
    private void handleLogin() {
        hideError();

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // Validasi input
        if (username.isEmpty() && password.isEmpty()) {
            showError("Username dan password tidak boleh kosong.");
            txtUsername.requestFocus();
            return;
        }
        if (username.isEmpty()) {
            showError("Username tidak boleh kosong.");
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Password tidak boleh kosong.");
            txtPassword.requestFocus();
            return;
        }

        // Proses autentikasi
        btnLogin.setDisable(true);
        btnLogin.setText("Memproses...");

        User authenticatedUser = userDAO.authenticate(username, password);

        if (authenticatedUser != null) {
            // LOGIN BERHASIL
            System.out.println("[LOGIN] Berhasil — User: " + authenticatedUser.getUsername()
                    + " | Role: " + authenticatedUser.getRoleDisplayName());

            SessionManager.getInstance().setCurrentUser(authenticatedUser);

            // Navigasi ke MainView (Dashboard)
            try {
                App.getPrimaryStage().setResizable(true);
                App.setRoot("view/MainView", 1100, 720);
            } catch (IOException e) {
                System.err.println("[LOGIN] Gagal membuka dashboard: " + e.getMessage());
                e.printStackTrace();
                showError("Terjadi kesalahan sistem.");
                btnLogin.setDisable(false);
                btnLogin.setText("Login");
            }
        } else {
            // LOGIN GAGAL
            showError("Username atau password salah.");
            txtPassword.clear();
            txtPassword.requestFocus();
            btnLogin.setDisable(false);
            btnLogin.setText("Login");
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), lblError);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblError.setText("");
        lblError.setStyle("");
    }
}
