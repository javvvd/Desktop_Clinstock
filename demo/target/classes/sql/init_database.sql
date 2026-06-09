-- ============================================================
-- ClinStock - Sistem Manajemen Inventori Klinik
-- DDL (Data Definition Language) untuk SQLite
-- ============================================================

-- Aktifkan Foreign Key constraint (SQLite default-nya OFF)
PRAGMA foreign_keys = ON;

-- ============================================================
-- 1. Tabel USERS
-- Menyimpan data akun pengguna beserta role-nya.
-- Role: 'admin_farmasi', 'petugas_klinik', 'manajer_klinik'
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password      TEXT    NOT NULL,
    full_name     TEXT    NOT NULL DEFAULT '',
    role          TEXT    NOT NULL CHECK (role IN ('admin_farmasi', 'petugas_klinik', 'manajer_klinik')),
    is_active     INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================================
-- 2. Tabel ITEMS
-- Menyimpan data master barang (obat & alat kesehatan).
-- ============================================================
CREATE TABLE IF NOT EXISTS items (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    item_code     TEXT    NOT NULL UNIQUE,
    name          TEXT    NOT NULL,
    category      TEXT    NOT NULL DEFAULT 'Umum',
    unit          TEXT    NOT NULL DEFAULT 'pcs',
    min_stock     INTEGER NOT NULL DEFAULT 0,
    description   TEXT    DEFAULT '',
    created_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================================
-- 3. Tabel STOCK_IN
-- Mencatat setiap transaksi barang masuk (per-batch).
-- Kolom remaining_qty digunakan untuk algoritma FEFO.
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_in (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         INTEGER NOT NULL,
    batch_number    TEXT    NOT NULL,
    expired_date    TEXT    NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    remaining_qty   INTEGER NOT NULL CHECK (remaining_qty >= 0),
    supplier        TEXT    DEFAULT '',
    input_date      TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    notes           TEXT    DEFAULT '',
    created_at      TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT
);

-- ============================================================
-- 4. Tabel STOCK_OUT
-- Mencatat setiap transaksi barang keluar.
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_out (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         INTEGER NOT NULL,
    batch_number    TEXT    NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    out_date        TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    destination     TEXT    NOT NULL DEFAULT '',
    notes           TEXT    DEFAULT '',
    created_at      TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT
);

-- ============================================================
-- INDEX untuk optimasi query
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_stock_in_item_expired
    ON stock_in (item_id, expired_date);

CREATE INDEX IF NOT EXISTS idx_stock_in_remaining
    ON stock_in (item_id, remaining_qty);

CREATE INDEX IF NOT EXISTS idx_stock_out_item
    ON stock_out (item_id);

CREATE INDEX IF NOT EXISTS idx_stock_out_date
    ON stock_out (out_date);

CREATE INDEX IF NOT EXISTS idx_items_code
    ON items (item_code);

-- ============================================================
-- DATA SEED: Akun default untuk testing
-- Password: admin123 (dalam produksi harus di-hash!)
-- ============================================================
INSERT OR IGNORE INTO users (username, password, full_name, role) VALUES
    ('admin',    'admin123',    'Administrator Farmasi', 'admin_farmasi'),
    ('petugas',  'petugas123',  'Petugas Klinik',       'petugas_klinik'),
    ('manajer',  'manajer123',  'Manajer Klinik',       'manajer_klinik');

-- ============================================================
-- DATA SEED: Contoh item untuk testing
-- ============================================================
INSERT OR IGNORE INTO items (item_code, name, category, unit, min_stock) VALUES
    ('OBT-001', 'Paracetamol 500mg',    'Obat Bebas',      'Tablet',  50),
    ('OBT-002', 'Amoxicillin 500mg',    'Antibiotik',      'Kapsul',  30),
    ('OBT-003', 'Omeprazole 20mg',      'Obat Maag',       'Kapsul',  25),
    ('ALK-001', 'Sarung Tangan Latex',  'Alat Kesehatan',  'Box',     10),
    ('ALK-002', 'Masker Bedah',         'Alat Kesehatan',  'Box',     15);
