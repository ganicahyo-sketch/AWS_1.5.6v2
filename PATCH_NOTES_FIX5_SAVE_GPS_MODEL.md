# STASIUN CUACA v1.5.7 — FIX5: Simpan Catatan Lapangan, GPS, dan Model Matematika

## Perbaikan penyimpanan
- `appendHistory()` sekarang menulis ke `SharedPreferences` (sumber kompatibilitas yang dipakai tampilan/riwayat/PDF/CSV) terlebih dahulu, lalu membuat salinan JSON internal sebagai recovery mirror.
- Kegagalan menulis salinan cadangan file tidak lagi dianggap sebagai kegagalan catatan jika catatan benar-benar telah ditemukan pada `SharedPreferences`.
- Hasil penulisan diverifikasi berdasarkan ID unik catatan; item pupuk/OPT lama diverifikasi berdasarkan timestamp, komoditas, dan tanggal. Percobaan simpan ulang tidak menggandakan catatan yang memiliki ID yang sama.
- Penyimpanan histori JSON yang rusak tetap dicadangkan sebelum pemulihan; detail exception dan tahap kegagalan dicatat di Logcat. Pesan di layar membedakan gagal simpan sungguhan dari kegagalan memperbarui setelan/analisis setelah catatan tersimpan.
- Catatan disimpan sebelum kolom pengaturan formulir dan perhitungan analisis diperbarui, sehingga kegagalan di bagian berikutnya tidak menghapus keberhasilan simpan.

## Perbaikan tombol GUNAKAN GPS HP
- Tombol pada dashboard dan Pengaturan segera menampilkan lokasi Android terakhir yang valid jika tersedia, sambil meminta fix terbaru melalui provider GPS dan/atau jaringan.
- Cache lokasi tidak dibuang hanya karena umurnya lebih dari 15 menit. UI menyebutkan umur cache dengan jelas; jika Android memberi fix baru, koordinat baru menggantikan cache.
- Algoritme pemilihan lokasi memberi prioritas pada fix yang jauh lebih baru daripada cache, meskipun akurasinya tidak sedikit lebih baik. Ini mencegah koordinat lama yang berakurasi tinggi mengunci pembaruan setelah perangkat berpindah tempat.
- `SettingsActivity` langsung mengaplikasikan koordinat ke `SharedPreferences` dengan `apply()`; tombol tidak bergantung pada pengguna menekan tombol terpisah “Simpan Pengaturan”. Dashboard mendeteksi `location_updated_epoch` dan memuat ulang Open-Meteo saat kembali.
- Apabila Android tidak mengirim fix baru, aplikasi mempertahankan koordinat yang tersimpan dan memberi pesan status yang spesifik, bukan mengklaim lokasi baru telah didapat. Jika cache tersedia, lokasi tersebut tetap dipakai sementara dan jelas ditandai.

## Model matematika
- Menyertakan `MODEL_MATEMATIKA_DAN_ALUR_v1.5.7.md` yang menjelaskan umur/fase, target produksi petak kg dan luas m², baseline hara per fase, penyesuaian target hasil, screening kelas tanah, kredit pupuk dan efisiensi pemulihan, konversi label pupuk, serta batas kalibrasi sensor 8-in-1.
- Model saat ini adalah screening berbasis aturan/neraca sederhana, bukan machine learning terlatih, QUEFTS penuh, atau STCR spesifik lokasi yang sudah dikalibrasi.

## Yang dipertahankan
- Versi aplikasi tetap STASIUN CUACA 1.5.7 (versionCode 157).
- USB Soil Meter 8-in-1, ThingSpeak, Open-Meteo, GPS, pilihan AI, Google Search, OpenAlex, CSV, PDF, riwayat, catatan lapangan, dan profil tanaman lain tetap dipertahankan.
- Tidak mengubah proyek menjadi SIM Sawit.

## Validasi
- Perlu menjalankan GitHub Actions untuk membuktikan build Android penuh. Build penuh belum dapat dijalankan di lingkungan ini karena Gradle distribution belum tersedia secara lokal dan koneksi unduhan Gradle gagal.
- Setelah build, uji pada perangkat: simpan catatan kosong/minimal dan catatan lengkap, periksa riwayat sesudah aplikasi dibuka ulang, uji GPS dengan precise location ON/OFF, uji jaringan/Wi-Fi dan GPS satelit, lalu pastikan Open-Meteo memuat koordinat terbaru.
