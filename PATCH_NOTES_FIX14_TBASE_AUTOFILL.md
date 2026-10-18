# FIX14 — Auto-fill Tbase dari database komoditas

## Perubahan
- Menambahkan field Tbase (°C) pada Pengaturan dan Catatan Lapangan.
- Memilih tanaman pada dropdown database langsung mengisi Tbase beserta keterangan sumbernya.
- Bila nama tanaman diketik manual dan tidak cocok dengan database, Tbase tidak ditebak; pengguna dapat mengisinya sendiri.
- Nilai Tbase disimpan berpasangan dengan kunci nama tanaman supaya nilai tanaman sebelumnya tidak salah dipakai untuk komoditas berbeda.
- Catatan lapangan menyimpan Tbase yang digunakan. Analisis Agronomi menggunakan Tbase tersimpan bila memang terikat pada komoditas yang sedang dipilih; jika tidak, gunakan nilai database lalu fallback profil.
- Nilai Tbase dapat diedit setelah terisi otomatis untuk penyesuaian varietas/lokasi. Rentang input yang diterima -20 sampai 60 °C.

## Validasi
- Tbase database diuji untuk semua nama tanaman di CropDatabase.CROPS.
- XML parsing dan pemeriksaan resource ID dilakukan pada seluruh proyek.
- Build Android penuh harus dikonfirmasi lewat GitHub Actions.
