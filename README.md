## Pembaruan tampilan hasil analisis
Hasil analisis ditampilkan dalam kartu terpisah dengan saran tindakan di bagian awal. Istilah teknis disederhanakan dan laporan teknis asli tetap dipertahankan untuk konteks AI/laporan.

## Pembaruan tampilan hasil analisis
Hasil analisis ditampilkan dalam kartu terpisah dengan saran tindakan di bagian awal. Istilah teknis disederhanakan dan laporan teknis asli tetap dipertahankan untuk konteks AI/laporan.

## Hasil analisis yang mudah dibaca
Bagian hasil analisis dibuat lebih ramah bagi petani: saran praktis diletakkan di awal, isi dipisahkan menjadi bagian pendek, istilah teknis disederhanakan, serta data yang belum lengkap dan tingkat ketidakpastian dijelaskan dengan bahasa sehari-hari. Rincian teknis tetap disertakan dan tersimpan terpisah.

# STASIUN CUACA • GANI CAHYO-UNS v1.5.7

Android project lengkap dengan Open-Meteo + Agronomi + Catatan Lapangan.

## Fitur
- Open-Meteo tanpa API key/akun: cuaca saat ini, ET0 FAO, VPD, hujan, radiasi, mata angin.
- ThingSpeak feed terakhir + ekspor seluruh histori CSV.
- Sensor CWT-7in1 RS485 dan USB Soil Meter 8-in-1: N, P, K tersedia, pH, EC, kelembapan dan suhu tanah; kedua sumber tetap dibedakan.
- Catatan Lapangan terpadu: pengamatan, pupuk, OPT, pengendalian, irigasi, pertumbuhan; tanggal catatan dan tanam memakai kalender, umur otomatis dihitung dari selisih tanggal dan dapat ditampilkan sebagai HST atau MST.
- Analisis awal seluruh parameter: pH, N/N-total, P, K, EC/ECe, kelembapan tanah, suhu tanah, OM, CEC/KTK, suhu udara, RH, tekanan, hujan, ET0, angin, UV, visibilitas, Shortwave, PAR energi, PPFD, sunshine, VPD, serta parameter lingkungan lain yang tersedia.
- Analisis pH, N/P/K, EC, kelembapan, VPD, fase tanaman, kebutuhan hara screening, dan risiko OPT per komoditas yang didukung; kelapa sawit tidak termasuk dalam komoditas aplikasi ini.
- Mode Konvensional/PHT atau Organik.
- Arah angin selalu ditampilkan sebagai mata angin; tidak menampilkan derajat.

## Build lokal
Workflow CI menggunakan Eclipse Temurin JDK 25; source compatibility Android tetap Java 17. Gradle 9.4.1 + Android Gradle Plugin 9.2.0.

## GitHub Actions
File `.github/workflows/build-apk.yml` membuild debug dan release, lalu mengunggah APK sebagai artifact. Jalankan dari tab Actions > Build APK.

## Catatan ilmiah
Profil baseline per komoditas dan referensi hasil model dirangkum di `CROP_PROFILES_SAYURAN.md`; sumber teknis lebih lengkap berada di `SCIENTIFIC_SOURCES.md`. Target hasil spesifik varietas harus diverifikasi dari deskripsi resmi dan dimasukkan sesuai satuan/jenis hasil yang sama.

Mesin agronomi membedakan keputusan deterministik, screening berbasis ambang, dan rekomendasi yang memerlukan kalibrasi lokal. Dosis pupuk tidak boleh dianggap sebagai resep mutlak ketika persamaan STCR spesifik tanah/komoditas tidak tersedia.

## Laporan PDF
Versi ini menyediakan `LAPORAN PDF - SIMPAN / PRINT` dari dashboard. Pengguna dapat memilih laporan ringkas/lengkap dan menyimpan PDF melalui pemilih file Android atau langsung membuka dialog Print Android.
Isi laporan mencakup cuaca Open-Meteo, ET0, VPD, arah angin berbasis mata angin, tanah CWT-7in1, histori catatan/pupuk/OPT, analisis agronomi, rekomendasi teknis, rekomendasi AI terakhir (jika ada), serta catatan dasar ilmiah.


## v1.5.7 agronomi
- Interpretasi pH berbasis profil komoditas dan kebutuhan kapur berbasis pH-buffer/Al-dd/H-dd/CEC/lab.
- N: default klasifikasi memakai estimasi N-total dari N tersedia dengan asumsi utama 2% (sensitivitas 1–3%). Batas N rendah/tinggi hanya dipakai jika pengguna secara eksplisit memilih basis ambang N-spesifik dan kedua batas valid; angka atau metode P/K generik tidak otomatis dipakai untuk N. Inferensi bukan pengganti uji laboratorium. P/K memakai kategori berbasis metode.
- EC sensor dipisahkan dari ECe; kelembapan memakai FC/PWP/FTSW bila tersedia.
- Risk screening OPT menyebut nama organisme; bukan diagnosis.
- Mode konvensional/PHT atau organik.
- Laporan PDF menyertakan data, histori, analisis, rekomendasi, dan sumber.


## Pembaruan CSV Open-Meteo & Analisis AI
- Menu **UNDUH DATA CSV** sekarang memiliki sumber **ThingSpeak** dan **Open-Meteo**.
- Open-Meteo mendukung ekspor historis berdasarkan rentang tanggal pada resolusi **per jam** atau **harian**, termasuk cuaca, hujan, tekanan, angin, radiasi, ET₀, VPD dan parameter tanah yang tersedia. Data diberi penanda `source=OPEN-METEO` dan `data_type=HISTORICAL_REANALYSIS`.
- Tampilan analisis agronomi menyembunyikan uraian metodologi/rentang/rumus dari layar utama. Detail ilmiah dipindahkan ke ikon **ⓘ**.
- AI menggunakan ruang output hingga **5000 token** dan diarahkan menghasilkan rekomendasi 0–24 jam, 1–7 hari, air, pemupukan, PHT, prioritas tindakan, serta pencarian web untuk OPT di luar database lokal.
- API key OpenAI tidak lagi ditanam sebagai default di source code. Masukkan key melalui Pengaturan.

USB Soil Meter 8-in-1: automatic USB-OTG detection for CH340/CH34x adapters (VID 0x1A86; PID 0x7523/0x5523/0x5512/0xE010), Modbus RTU 9600 8N1. Initial map: 0x0000 moisture/10, 0x0001 signed temperature/10, 0x0002 EC, 0x0003 pH/10, 0x0004 N, 0x0005 P, 0x0006 K, 0x0007 fertility. This is a generic candidate map derived from the Soil Detector APK and must be confirmed against the meter model/manual. RS485 and manual entry remain available.


## Alur rekomendasi pemupukan adaptif (v1.5.7)
- Data lengkap: gunakan data yang diisi/diukur sebagai dasar perhitungan.
- Data sebagian: gunakan nilai asli yang tersedia; komponen yang belum ada ditangani dengan asumsi model yang dinyatakan secara eksplisit.
- Data kosong: tetap tampilkan estimasi awal berbasis profil komoditas, fase, dan baseline kebutuhan; ketidakpastian diberi label tinggi dan data asli yang perlu diukur dicantumkan.
- Perubahan input, sensor USB, sumber cuaca, metode uji, budidaya, atau riwayat memicu hitung ulang otomatis.
- Nilai asli tidak pernah diganti dengan angka estimasi. Estimasi hanya dipakai untuk perhitungan sementara dan digantikan otomatis ketika data asli tersedia.

### Tampilan analisis yang lebih mudah dibaca
Hasil analisis agronomi sekarang dipisah menjadi kartu-kartu per topik di Catatan Lapangan dan halaman Analisis Agronomi. Judul dan isi mempunyai jarak lebih longgar, saran tindakan lebih menonjol, dan istilah teknis umum dijelaskan dengan bahasa yang lebih sederhana. Isi mentah analisis tetap disimpan untuk konteks AI serta laporan; perubahan tampilan tidak mengganti data masukan atau rumus perhitungan.


### Perbaikan build USB Soil Meter

Jika membangun proyek ini, `settings.gradle` sudah menyertakan JitPack karena dependensi USB serial `com.github.mik3y:usb-serial-for-android:3.9.0` diambil dari repositori tersebut.

## FIX5 — penyimpanan dan GPS
Detail perbaikan terbaru serta batas uji ada di `PATCH_NOTES_FIX5_SAVE_GPS_MODEL.md`. Rumus dan alur hitung agronomi dijabarkan di `MODEL_MATEMATIKA_DAN_ALUR_v1.5.7.md`.


## FIX6 — penyusunan laporan agronomi
Laporan cetak/PDF versi lengkap dan ringkas memakai alur bab yang konsisten, ringkasan eksekutif, tabel riwayat, nomor halaman, dan pemenggalan teks lintas halaman. Target tipografi: Arial, judul laporan 14 pt, judul bab/subbab 12 pt, isi 11 pt. Laporan ringkas menampilkan hingga 8 riwayat terbaru; laporan lengkap hingga 30 ditambah bagian AI dan sumber. Rincian implementasi dan batas pengujian ada di `PATCH_NOTES_FIX6_PDF_REPORT.md`.


## FIX7 — klasifikasi N tersedia dan estimasi N-total
Jika basis ambang N-spesifik tidak dipilih atau batas tidak valid, aplikasi mengonversi N tersedia (mg/kg) menjadi N-total estimasi (%) menggunakan `((100/2) × N tersedia) / 10000` (setara `N tersedia / 200`), lalu menentukan kelas N-total. Asumsi 2% adalah screening dengan sensitivitas 1–3%, bukan hasil lab. Lihat `PATCH_NOTES_FIX7_NITROGEN_CLASS_CONVERSION.md`.


## FIX8 — audit nitrogen dan kalkulator dolomit
- Rumus konversi N tersedia ke N-total tetap `N-total (%) = N tersedia (mg/kg)/200` untuk asumsi utama 2%; batas ambang N hanya dipakai jika pengguna memilih basis ambang N-spesifik dan dua ambang valid.
- Model dosis pupuk diperbaiki agar baseline hara pupuk tidak dibagi lagi dengan faktor pemulihan. Fraksi 0,50/0,30/0,60 hanya mengurangi kredit dari aplikasi pupuk terdahulu sehingga formula tidak menggelembungkan dosis.
- Kalkulator dolomit menerima pH awal, pH target, luas hektare, tekstur/faktor asumsi dan efektivitas netralisasi dolomit; hasil tersimpan untuk laporan agronomi dan PDF. Faktor bawaan hanya estimasi awal; pH saja tidak cukup untuk kebutuhan kapur definitif. Lihat `PATCH_NOTES_FIX8_NITROGEN_DOLOMITE.md`.


## FIX9 — ekspor CSV ThingSpeak dan Open-Meteo
- Menggunakan fallback Channel ID/API key yang sama dengan dashboard untuk menghindari pesan “Channel ID belum diatur” ketika pengaturan default tampil tetapi belum pernah tersimpan.
- Mendukung alias key preferensi lama saat migrasi.
- Memperbaiki ekspor Open-Meteo Historical API karena `visibility` dan `uv_index` tidak tersedia pada endpoint historis; dua kolom tetap ada di CSV namun kosong.
- Detail: `PATCH_NOTES_FIX9_CSV_EXPORT.md`.

## Build fix (FIX10)
Run GitHub Actions 39154602530 gagal di kompilasi Java karena import `SharedPreferences` dan helper `isExcludedPalm(String)` yang belum ada. FIX10 menambahkan keduanya tanpa mengubah ID versi aplikasi. Lihat `PATCH_NOTES_FIX10_BUILD_39154602530.md`.
