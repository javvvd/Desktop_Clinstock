package com.clinstock;

import com.clinstock.util.DatabaseConnection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * ClinStock — Sistem Manajemen Inventori Klinik Berbasis Desktop.
 *
 * <p>Class utama yang menjadi entry point aplikasi JavaFX.
 * Bertanggung jawab untuk:</p>
 * <ul>
 *   <li>Inisialisasi koneksi database (SQLite)</li>
 *   <li>Memuat halaman login sebagai tampilan awal</li>
 *   <li>Menyediakan method navigasi antar halaman (scene switching)</li>
 *   <li>Menutup koneksi database saat aplikasi ditutup</li>
 * </ul>
 */
public class App extends Application {

    /** Scene utama yang digunakan untuk switching antar halaman */
    private static Scene scene;

    /** Stage utama aplikasi */
    private static Stage primaryStage;

    /**
     * Method start() — dipanggil oleh JavaFX saat aplikasi dijalankan.
     * Menginisialisasi database dan menampilkan halaman login.
     *
     * @param stage Stage utama dari JavaFX
     * @throws IOException Jika file FXML gagal dimuat
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Inisialisasi database (Singleton — akan membuat tabel jika belum ada)
        DatabaseConnection.getInstance();

        // Muat halaman Login sebagai tampilan awal
        scene = new Scene(loadFXML("view/LoginView"), 620, 620);

        // Konfigurasi window utama
        stage.setTitle("ClinStock — Sistem Manajemen Inventori Klinik");
        
        // Set App Icon
        try {
            stage.getIcons().add(new javafx.scene.image.Image(App.class.getResourceAsStream("images/logo.png")));
        } catch (Exception e) {
            System.err.println("[APP] Gagal memuat ikon aplikasi: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();

        // Event handler: tutup koneksi DB saat window ditutup
        stage.setOnCloseRequest(event -> {
            System.out.println("[APP] Aplikasi ditutup. Membersihkan resources...");
            DatabaseConnection.getInstance().closeConnection();
        });

        stage.show();
        System.out.println("[APP] ClinStock berhasil dijalankan.");
    }

    /**
     * Mengganti root scene ke halaman FXML lain (navigasi antar halaman).
     *
     * @param fxml Nama file FXML (tanpa ekstensi, relatif terhadap package view)
     * @throws IOException Jika file FXML gagal dimuat
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Mengganti root scene ke halaman FXML lain dengan ukuran window baru.
     *
     * @param fxml   Nama file FXML (tanpa ekstensi)
     * @param width  Lebar window baru
     * @param height Tinggi window baru
     * @throws IOException Jika file FXML gagal dimuat
     */
    public static void setRoot(String fxml, double width, double height) throws IOException {
        scene.setRoot(loadFXML(fxml));
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
    }

    /**
     * Mendapatkan Stage utama aplikasi.
     *
     * @return Stage utama
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Memuat file FXML dari resources.
     * Path FXML relatif terhadap package com/clinstock/ di resources.
     *
     * @param fxml Nama file FXML (tanpa ekstensi .fxml)
     * @return Parent node dari FXML yang dimuat
     * @throws IOException Jika file FXML tidak ditemukan atau gagal di-parse
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Method main() — entry point JVM.
     *
     * @param args Argumen command line
     */
    public static void main(String[] args) {
        launch();
    }
}