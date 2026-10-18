# STASIUN CUACA v1.5.7 — FIX4 Catatan Lapangan, Sunshine, dan Konversi Pupuk

Patch lokal berbasis sumber `ganicahyo-sketch/AWS_1.5.6v2` / basis proyek v1.5.7. Versi aplikasi tetap 1.5.7 (versionCode 157); ini bukan SIM Sawit.

## Perbaikan catatan lapangan
- Memasang adaptor Spinner HST/MST yang sebelumnya tidak terpasang. Nilai pilihan tidak lagi `null` saat proses simpan membaca `ageUnit`, sumber potensial pesan gagal setelah histori ditambahkan.
- Penyimpanan histori menggunakan `commit()` dan membaca ulang JSON untuk memeriksa apakah item terbaru benar-benar tersimpan. Kerusakan histori lama tetap dicadangkan sebagai `<key>_corrupt_backup`.
- Pemutakhiran analisis/tampilan dipisahkan dari hasil simpan; bila refresh analisis gagal setelah penyimpanan berhasil, pengguna diberi informasi bahwa catatan sudah tersimpan.
- Pemilih kalender tersedia untuk tanggal catatan dan tanggal tanam. Umur dihitung dari selisih tanggal tanam dan tanggal catatan, dengan HST/MST; jika tanggal tanam tidak tersedia umur manual masih dapat digunakan.
- Fase tanaman diprediksi dari umur. Kolom fase manual bersifat override opsional.
- Kolom varietas/galur/klon dan target produksi total (kg) ditambahkan secara opsional. Luas lahan memakai m²; kompatibilitas preferensi lama ha dan t/ha dipertahankan.
- Satuan numerik memakai suffix visual yang tetap terlihat saat nilai diisi.
- Kelapa sawit dikeluarkan dari alur komoditas ini; profil komoditas lain tetap tersedia dan beberapa profil sayuran/kacang ditambah.

## Data cuaca dan cahaya
- Urutan sumber otomatis tetap ThingSpeak → Open-Meteo; jika suatu parameter ThingSpeak tidak tersedia, nilai Open-Meteo digunakan sebagai fallback.
- Pemetaan ThingSpeak ditambah untuk lux/illuminance. Nilai PPFD/PAR ThingSpeak diprioritaskan atas estimasi dari Open-Meteo jika field cocok ditemukan.
- Cache otomatis menyimpan shortwave, lux perkiraan, PPFD, dan sunshine_duration agar dapat dipakai ulang oleh Catatan Lapangan.
- Dashboard menampilkan `sunshine_duration` dengan label yang jelas dan lux estimasi yang diberi peringatan bahwa nilainya dihitung dari shortwave, bukan pengukuran lux langsung.
- Permintaan Open-Meteo secara eksplisit meminta `wind_speed_unit=ms` agar angka yang ditampilkan/disimpan konsisten dengan label m/s.

## Model pupuk dan konversi produk
- Baseline screening ditambahkan untuk kubis, sawi, selada, brokoli, kembang kol, kangkung, bayam, wortel, timun/mentimun, terong, melon, semangka, kale/kailan, kacang tanah, kacang hijau dan kacang panjang. Nilai baseline bukan dosis resmi; rujukan dan batas interpretasi dijelaskan di `SCIENTIFIC_SOURCES.md`.
- Pengguna dapat mencatat varietas dan target produksi total. Model mengubah target total menjadi t/ha berdasarkan luas lahan m²; target tetap opsional.
- Tombol konversi pupuk menghitung kombinasi hingga lima produk berdasarkan target N, P₂O₅, K₂O dan persentase yang diketik dari label pupuk (majemuk maupun tunggal). Hasil menampilkan dosis kg/ha, taksiran berat untuk petak, dan selisih target unsur jika tidak dapat dipenuhi sekaligus.
- Sumber FAO/Indonesia, rekomendasi spesifik lokasi Kementan/BRMP, PUTS dan modul hortikultura dicatat di `SCIENTIFIC_SOURCES.md`. Rekomendasi dosis tetap screening; sensor USB 8-in-1 bukan pengganti analisis tanah/laboratorium.

## Yang dipertahankan
ThingSpeak, Open-Meteo, OpenAlex, opsi AI, Google Search, ekspor CSV/PDF, riwayat, GPS, USB soil meter 8-in-1, dashboard cuaca, dan fitur agronomi lain tetap berada pada proyek. Tidak ada push/commit otomatis ke GitHub dari proses ini.

## Verifikasi
XML layout divalidasi. Kode sumber dan ID view diperiksa secara statis. Helper pembacaan respons Open-Meteo yang ada di branch utama juga dipertahankan pada CSV downloader. Build Android penuh harus dikonfirmasi setelah ZIP diunggah ke GitHub Actions karena toolchain/SDK Android tidak tersedia pada lingkungan lokal ini.
- Konverter sekarang memungkinkan petani mencentang hanya pupuk yang benar-benar ada, mengganti nama dan komposisi hingga lima pupuk, serta menyimpan komposisi itu untuk penggunaan berikutnya. Unit PAR yang berasal dari ThingSpeak dengan label W/m² dikonversi ke PPFD sebelum masuk kolom foton.
- Bila ThingSpeak menyimpan placeholder kosong/`--` atau nilai nonnumerik, parameter itu tidak memblokir fallback Open-Meteo; arah angin berbentuk label mata angin juga dikenali sebagai data valid.
- Referensi model komoditas dan hasil acuan didokumentasikan di `CROP_PROFILES_SAYURAN.md`. Referensi resmi Kementerian Pertanian/BRMP untuk pangan dan hortikultura edisi 2024 ditambahkan ke sumber ilmiah.
