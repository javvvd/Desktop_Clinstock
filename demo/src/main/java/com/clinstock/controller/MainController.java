package com.clinstock.controller;

import com.clinstock.App;
import com.clinstock.model.User;
import com.clinstock.util.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controller untuk MainView.fxml — Cangkang utama aplikasi.
 *
 * Mengelola:
 * - Navigasi bar dinamis berdasarkan role user
 * - Switching konten halaman (Dashboard, DataMaster, StokMasuk, StokKeluar, Laporan)
 * - Logout
 */
public class MainController {

    @FXML private HBox navButtonContainer;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private StackPane contentArea;

    /** Tombol navigasi yang sedang aktif */
    private Button activeNavButton;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();

        if (user != null) {
            lblUserName.setText(user.getFullName());
            lblUserRole.setText(user.getRoleDisplayName());

            // Buat menu navigasi berdasarkan role
            buildNavigationMenu(user.getRole());
        }

        // Muat Dashboard sebagai halaman default
        loadPage("DashboardView");
    }

    /**
     * Membangun tombol navigasi berdasarkan role pengguna.
     */
    private void buildNavigationMenu(String role) {
        navButtonContainer.getChildren().clear();

        // Semua role mendapat akses ke Dashboard
        addNavButton("Dashboard", "DashboardView");

        switch (role) {
            case "admin_farmasi":
                // Admin: semua kecuali Laporan
                addNavButton("Data Master", "DataMasterView");
                addNavButton("Stok Masuk", "StokMasukView");
                addNavButton("Stok Keluar", "StokKeluarView");
                break;
            case "petugas_klinik":
                // Petugas: hanya Data Master & Stok Keluar
                addNavButton("Data Master", "DataMasterView");
                addNavButton("Stok Keluar", "StokKeluarView");
                break;
            case "manajer_klinik":
                // Manajer: akses penuh
                addNavButton("Data Master", "DataMasterView");
                addNavButton("Stok Masuk", "StokMasukView");
                addNavButton("Stok Keluar", "StokKeluarView");
                addNavButton("Laporan", "LaporanView");
                break;
            default:
                break;
        }
    }

    /**
     * Menambahkan satu tombol navigasi ke nav bar.
     */
    private void addNavButton(String label, String fxmlName) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-btn");
        btn.setOnAction(e -> {
            setActiveButton(btn);
            loadPage(fxmlName);
        });

        // Set tombol pertama (Dashboard) sebagai aktif
        if (navButtonContainer.getChildren().isEmpty()) {
            setActiveButton(btn);
        }

        navButtonContainer.getChildren().add(btn);
    }

    /**
     * Mengatur tombol navigasi yang aktif (highlight).
     */
    private void setActiveButton(Button btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-btn-active");
        }
        btn.getStyleClass().add("nav-btn-active");
        activeNavButton = btn;
    }

    /**
     * Memuat halaman FXML ke dalam content area.
     */
    private void loadPage(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/clinstock/view/" + fxmlName + ".fxml"));
            Parent page = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            System.err.println("[MainController] Gagal memuat halaman: " + fxmlName);
            e.printStackTrace();
        }
    }

    /**
     * Handler tombol Logout.
     */
    @FXML
    private void handleLogout() {
        System.out.println("[LOGOUT] User " + SessionManager.getInstance().getCurrentUser().getUsername());
        SessionManager.getInstance().clearSession();

        try {
            App.setRoot("view/LoginView", 620, 620);
            App.getPrimaryStage().setResizable(false);
        } catch (IOException e) {
            System.err.println("[LOGOUT] Gagal kembali ke halaman login");
            e.printStackTrace();
        }
    }
}
