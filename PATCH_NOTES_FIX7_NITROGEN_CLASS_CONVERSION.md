# FIX7 — Koreksi klasifikasi N tersedia → N-total

## Perubahan
- Rumus konversi N-total ditulis eksplisit sesuai ketentuan: `N-total (%) = ((100 / 2) × N tersedia (mg/kg)) / 10000`, setara dengan `N tersedia / 200`.
- Kelas N menggunakan batas rendah/tinggi spesifik metode hanya jika kedua batas tersedia dan valid (`low > 0` serta `high > low`). Nilai kosong, nol, terbalik, atau generik yang terwakili sebagai batas nol tidak diperlakukan sebagai pengukuran N tanah nol; sistem mengonversi N tersedia menjadi estimasi N-total lalu menentukan kelas.
- Klasifikasi inferensi dibuat melalui satu jalur (`classifyAvailableNAsTotalN`) agar UI, analisis kebutuhan pupuk, dan laporan menggunakan aturan yang sama.
- Dokumentasi formula menjelaskan bahwa rasio 2% merupakan asumsi screening (sensitivitas 1–3%), bukan konversi universal atau hasil laboratorium N-total.

## Contoh verifikasi
| N tersedia (mg/kg) | Estimasi N-total (%) | Kelas screening |
|---:|---:|---|
| 2 | 0,010 | Sangat Rendah |
| 20 | 0,100 | Rendah |
| 40 | 0,200 | Rendah |
| 60 | 0,300 | Sedang |
| 120 | 0,600 | Tinggi |
| 160 | 0,800 | Sangat Tinggi |

## Validasi
- `NitrogenInference.java` berhasil dikompilasi menggunakan `javac`.
- Pengujian formula dan enam titik kelas lulus.
- Build APK Android penuh belum dikonfirmasi dalam lingkungan ini karena unduhan Gradle sebelumnya gagal akibat kendala DNS; konfirmasi akhir melalui GitHub Actions tetap diperlukan.
- Versi proyek dipertahankan pada STASIUN CUACA 1.5.7 / versionCode 157.
