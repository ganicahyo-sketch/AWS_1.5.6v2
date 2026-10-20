# FIX18 — Hasil hitung pupuk dan dolomit di analisis serta laporan

Basis: FIX17 WeatherAPI/OpenWeather build fix. Tidak mengubah applicationId, versionName/versionCode, atau alur data cuaca.

## Perubahan
- Konversi Dosis Pupuk sekarang menyimpan hasil perhitungan produk (produk yang dihitung, kg/ha, total kg bila luas tersedia, kadar N/P₂O₅/K₂O pada label, target dan hara yang tersuplai, komoditas serta waktu hitung).
- Analisis Agronomi menampilkan dosis N/P₂O₅/K₂O hasil skrining, hasil konversi ke produk, indikator apakah hasil konversi cocok dengan komoditas/kebutuhan/luas saat ini, dan hingga enam catatan pemakaian pupuk aktual untuk komoditas terpilih.
- Analisis Agronomi merinci hasil kalkulator dolomit: pH awal/target/ΔpH, tipe tanah, faktor CaCO₃-ekuivalen, efektivitas netralisasi, t/ha, kg/ha, luas dan total kg. Jika komoditas kalkulator berbeda dari pilihan sekarang, laporan memberi peringatan.
- PDF ringkas dan lengkap mendapat bagian hasil perhitungan pupuk/dolomit serta riwayat pemakaian pupuk yang dicatat. Laporan membedakan dosis hasil konversi matematis dari pupuk yang benar-benar tercatat telah diaplikasikan.
- Perhitungan dan riwayat lama tetap dipertahankan. Jika hasil konversi belum ada, laporan menyebutkan bahwa konversi harus dijalankan terlebih dahulu; angka dolomit tetap diberi label estimasi pH-only, bukan kebutuhan kapur laboratorium.

## Validasi
- Perlu menjalankan GitHub Actions `:app:assembleDebug` untuk memverifikasi kompilasi Android dan menghasilkan APK. Paket ini tidak memiliki `gradle-wrapper.jar`; build penuh lokal tidak dapat dijalankan melalui wrapper dalam lingkungan ini.
