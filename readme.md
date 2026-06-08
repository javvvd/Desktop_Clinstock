# ClinStock — Sistem Manajemen Inventori Klinik

ClinStock adalah aplikasi manajemen inventori klinik berbasis **Desktop JavaFX** dengan basis data **SQLite**. Aplikasi ini dirancang khusus untuk mempermudah petugas medis dan apoteker dalam mengelola stok obat serta alat kesehatan secara terorganisir dengan memanfaatkan algoritma otomatis **FEFO (First Expired, First Out)** untuk mencegah kerugian akibat obat kedaluwarsa.


## 🛠️ Spesifikasi Teknologi

* **Bahasa Pemrograman:** Java (JDK 17)
* **Framework GUI:** JavaFX 17 (dengan FXML & Custom CSS Styling)
* **Basis Data:** SQLite (Embedded DB, tidak memerlukan server eksternal/XAMPP)
* **Koneksi Database:** JDBC Driver
* **Library Tambahan:**
  * Apache POI (untuk pembuatan file Excel)
  * iText 5 (untuk pembuatan file PDF)

---

## 🏃 Cara Menjalankan Aplikasi

### Prasyarat
Pastikan komputer Anda sudah terinstal:
1. **Java Development Kit (JDK) 17** atau versi di atasnya.
2. **Apache Maven** (opsional, jika ingin menjalankan lewat command line).
3. **IDE (VS Code / IntelliJ IDEA / Eclipse)** dengan ekstensi dukungan Java.

### Menjalankan lewat Terminal / Command Line
1. Buka terminal atau Command Prompt pada folder root proyek.
2. Masuk ke direktori `demo`:
   ```bash
   cd demo
   ```
3. Jalankan perintah Maven berikut:
   ```bash
   mvn clean compile javafx:run
   ```

---

## 🔐 Kredensial Akun Default (Testing)

Anda dapat menggunakan akun-akun default berikut untuk menguji fitur multi-role pada aplikasi:

| Peran (Role) | Username | Password | Hak Akses Utama |
| :--- | :--- | :--- | :--- |
| **Administrator Farmasi** | `admin` | `admin123` | Data Master, Stok Masuk, Stok Keluar |
| **Petugas Klinik** | `petugas` | `petugas123` | Data Master, Stok Keluar |
| **Manajer Klinik** | `manajer` | `manajer123` | Dashboard, Laporan & Ekspor (Excel/PDF) |

---

## 📂 Struktur Folder Proyek

* `demo/src/main/java/com/clinstock/model/` — Class Entity/Model objek.
* `demo/src/main/java/com/clinstock/dao/` — Data Access Object (Kueri SQL).
* `demo/src/main/java/com/clinstock/controller/` — Logika pengendali halaman FXML.
* `demo/src/main/resources/com/clinstock/view/` — Desain antarmuka (FXML).
* `demo/src/main/resources/com/clinstock/css/` — Desain layout & styling visual (CSS).
* `demo/src/main/resources/sql/` — DDL inisialisasi skema tabel & data seed.
