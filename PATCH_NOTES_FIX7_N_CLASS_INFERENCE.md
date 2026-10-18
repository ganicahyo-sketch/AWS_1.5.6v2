# PATCH FIX7 — Klasifikasi N dari N tersedia

## Masalah
Saat ambang rendah/tinggi N tersedia tidak tersedia atau tidak valid, `classifyN()` mengembalikan “metode/ambang belum diketahui”. Akibatnya `nutrientNeed()` menggunakan dukungan tanah N = 0 walaupun pembacaan N tersedia ada, sehingga kebutuhan pupuk N berbasis model tampak seolah tanah memberi kontribusi nol. `NitrogenInference` juga memakai rasio 1–5% dengan label titik tengah yang tidak konsisten dengan asumsi 2% yang diminta.

## Perbaikan
- Input N tersedia mg/kg dikonversi menjadi N-total dengan asumsi utama 2%: `N-total (%) = N tersedia (mg/kg) / 200`.
- Sensitivitas 1–3% ditampilkan: rentang N-total = `N tersedia/300` sampai `N tersedia/100` dalam %. Kelas utama menggunakan asumsi titik 2%.
- Kelas N-total screening: sangat rendah <0,10%; rendah 0,10–0,20%; sedang >0,20–0,50%; tinggi >0,50–0,75%; sangat tinggi >0,75%.
- Ambang rendah/tinggi khusus metode yang valid tetap diprioritaskan. Ambang kosong, terbalik, nol/generik kembali ke inferensi N-total, bukan diperlakukan sebagai kandungan N = 0.
- Model pupuk menggunakan kelas N hasil inferensi untuk faktor dukungan tanah (sangat rendah 0%; rendah 20%; sedang 50%; tinggi 80%; sangat tinggi 90%). Jika nilai N tersedia sendiri hilang/tidak valid, model tetap tidak mengarang kontribusi tanah.
- Dasbor, analisis agronomi, catatan lapangan, analisis rentang, dan laporan PDF menyatakan dengan jelas sumber kelas N dan menampilkan estimasi utama serta sensitivitas.
- Dokumen README, catatan agronomi, sumber ilmiah, dan model matematika diperbarui.

## Rujukan dan batasan
Kriteria N-total mengacu pada tabel contoh analisis tanah SNI 6250:2021 Lampiran A dan Lalenoh et al. (2023). Asumsi bahwa N tersedia merupakan 2% dari N-total adalah asumsi screening aplikasi, bukan konstanta universal. Pembacaan soil meter harus diverifikasi dengan analisis N-total laboratorium bila diperlukan keputusan presisi.

## Verifikasi yang direncanakan
Kompilasi Java unit-level dan skenario numerik untuk beberapa input N tersedia, kelas batas, fallback ambang generik, serta dampak pada kebutuhan pupuk. Build Android penuh tetap memerlukan GitHub Actions/akses distribusi Gradle.
