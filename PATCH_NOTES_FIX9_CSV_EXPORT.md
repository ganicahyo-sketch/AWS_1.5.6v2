# FIX9 — perbaikan ekspor CSV ThingSpeak dan Open-Meteo

## Perubahan

### ThingSpeak
- Menyamakan pembacaan Channel ID dan Read API Key pada ekspor CSV dengan perilaku dashboard dan Pengaturan.
- Bila pengaturan belum pernah tersimpan, ekspor memakai default aplikasi yang sama (`2981880`) alih-alih menyatakan Channel ID kosong.
- Mendukung nama kunci preferensi lama/alias (`channel_id`, `channelId`, `thingspeak_channel_id`, serta alias Read API Key) untuk migrasi konfigurasi.
- Mengonversi nilai preferensi dengan aman menjadi teks, merapikan whitespace pada ID, dan memberi validasi bahwa Channel ID numerik.
- Status proses menampilkan Channel ID yang benar-benar dipakai agar konfigurasi yang dibaca lebih mudah diverifikasi.

### Open-Meteo
- Menghapus `visibility` dan `uv_index` dari parameter permintaan Historical Weather API. Keduanya bukan variabel pada endpoint historical archive; permintaan dengan parameter tak dikenal ditolak dan membuat seluruh ekspor gagal.
- Mempertahankan kolom `visibility_m` dan `uv_index` di CSV demi kompatibilitas struktur lama; nilainya kosong karena endpoint historis tidak menyediakan variabel tersebut. UI memberi keterangan eksplisit.
- Menampilkan alasan error API dari properti JSON `reason`, bukan hanya respons mentah, sehingga kegagalan jaringan/API lebih mudah didiagnosis.

## Pemeriksaan
- Daftar variabel hourly/daily di query dibandingkan dengan dokumentasi resmi Historical Weather API.
- Default Channel ID/Read API Key disamakan dengan nilai default yang digunakan dashboard dan Pengaturan.
- XML resource dan referensi `R.id` akan diperiksa dalam validasi paket.
- Build Android penuh tetap perlu dikonfirmasi melalui GitHub Actions karena lingkungan ini tidak mempunyai distribusi Gradle 9.4.1 yang sudah terpasang dan akses DNS ke distribusi dapat gagal.

## Sumber API
- Open-Meteo Historical Weather API: https://open-meteo.com/en/docs/historical-weather-api
- ThingSpeak Read Data API: https://www.mathworks.com/help/thingspeak/readdata.html
