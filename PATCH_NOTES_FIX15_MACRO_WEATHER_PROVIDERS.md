> **Catatan historis:** catatan ini mendeskripsikan implementasi FIX15. Sejak FIX20, integrasi OpenWeather beralih ke Current Weather dan Forecast 5 Day / 3 Hour API standar; endpoint One Call/Day Summary tidak lagi dipanggil dan ekspor historis OpenWeather dinonaktifkan.

# FIX15 — Cuaca Makro Multi-Provider, CSV Historis, Catatan Lapangan dan Analisis Agronomi

Basis: STASIUN CUACA v1.5.7 — FIX14. Versi aplikasi tetap 1.5.7 / versionCode 157.

## 1. Pengaturan provider cuaca makro

- Tambah/pastikan pilihan provider pusat: Open-Meteo atau OpenWeather One Call API 3.0.
- API key OpenWeather tetap berada di Pengaturan. Open-Meteo tidak memerlukan key.
- Dashboard menggunakan provider yang dipilih pada Pengaturan dan memeriksa perubahan provider ketika aktivitas kembali aktif.
- OpenWeather historis memakai endpoint Day Summary: satu permintaan per tanggal; aplikasi memberi peringatan dan konfirmasi ketika rentang lebih dari 31 hari.

## 2. Konsistensi sumber di Catatan Lapangan

- Pilihan "Prioritas: ThingSpeak → Cuaca Makro (sesuai Pengaturan)" menggunakan ThingSpeak untuk nilai yang dapat dipetakan dan provider cuaca makro dari Pengaturan untuk nilai yang tidak tersedia.
- Pilihan eksplisit ThingSpeak, Open-Meteo, atau OpenWeather tetap memakai provider yang dipilih dalam formulir; pilihan tersebut tidak ditimpa oleh preferensi provider global.
- Mengganti sumber pada formulir langsung memicu pengambilan data dari provider baru.
- Cache menyimpan nama provider yang benar-benar dipakai dan tidak disalah-labeli sebagai Open-Meteo ketika data berasal dari OpenWeather.
- Objek JSON cache menangani parameter tidak tersedia sebagai null/kosong agar satu nilai NaN tidak membatalkan seluruh data cache.
- Suhu dan kelembapan tanah dari CWT/USB/ThingSpeak/manual dipisahkan dari suhu/kelembapan tanah model makro. Open-Meteo grid temperature/moisture hanya muncul pada ringkasan makro dan Agronomi; OpenWeather tidak menyediakan suhu tanah sehingga ditandai tidak tersedia, bukan memakai sensor petak.
- Lux dicantumkan di Catatan Lapangan bersama metode estimasinya.

## 3. Dashboard dan laporan agronomi

- Bagian duplikat "CWT-7IN1 • STATUS TANAH" disembunyikan dari dashboard; data tanah tetap berada di Catatan Lapangan dan Analisis Agronomi.
- Ringkasan rekomendasi dashboard diberi judul "ANALISIS STATUS PARAMETER CUACA" dan hanya menjelaskan implikasi praktis parameter cuaca untuk komoditas. Pemeriksaan risiko OPT tetap tersedia terpisah di fitur Prediksi OPT.
- Tombol Analisis Agronomi pada Catatan Lapangan menyimpan input saat ini dan membuka layar Analisis Agronomi. Hasil tidak digandakan di bawah formulir.
- Urutan laporan Analisis Agronomi: (1) Parameter Tanah; (2) Parameter Cuaca termasuk air dan ekologi; (3) Pemupukan NPK dan prediksi kebutuhan fase; (4) Risk Screening Prediksi Potensi OPT; (5) Database & Rekomendasi Teknis termasuk waktu pemupukan; (6) Catatan.
- Dashboard tetap menampilkan ET₀, sunshine duration, kelembapan tanah makro, shortwave/radiasi, PAR, PPFD dan lux.

## 4. CSV histori cuaca makro

### Open-Meteo
- Resolusi hourly dan daily dipertahankan.
- Ekspor hourly/daily mencakup ET₀, sunshine duration, kelembapan tanah grid, suhu tanah grid, shortwave/radiasi, PAR, PPFD dan lux estimasi.
- Header variabel membedakan nilai hourly, daily mean, dan nilai harian; kolom metodologi menerangkan apakah nilai berasal langsung dari Open-Meteo atau turunan model.
- Kolom visibility dan UV yang tidak didukung endpoint historis tetap kompatibel dan kosong, bukan diminta sebagai variabel historis yang dapat menggagalkan seluruh permintaan.

### OpenWeather One Call API 3.0
- Ekspor historis harian memakai Day Summary, dimulai 2 Januari 1979 sampai tanggal histori yang diizinkan provider.
- ET₀ (FAO-56 Penman–Monteith dengan radiasi estimasi Hargreaves–Samani), radiasi/shortwave (Hargreaves–Samani), sunshine duration (inverse Angström–Prescott), PAR, PPFD, lux serta kelembapan tanah (bucket neraca air dengan FC/PWP/kedalaman dan kondisi awal yang tercatat) dimasukkan sebagai estimasi.
- Data soil moisture bukan observasi OpenWeather; ia sensitif pada FC/PWP, kedalaman akar, koefisien tanaman dan kondisi awal. Semua kolom diberi label estimasi/metode.
- Rentang lebih dari 31 hari membutuhkan konfirmasi. Jumlah permintaan diperkirakan satu per tanggal, sesuai kuota/subscription key.
- Tanggal akhir default ekspor cuaca diatur ke hari selesai terakhir (kemarin) agar rentang default historis tidak tergantung data hari yang masih berjalan.

## 5. Formula dan interpretasi turunan

- ET₀ Open-Meteo: nilai API berbasis FAO-56 Penman–Monteith.
- ET₀ OpenWeather: model FAO-56 Penman–Monteith dengan input cuaca harian dan radiasi estimasi; bukan ET₀ observasi.
- OpenWeather shortwave: Hargreaves–Samani `Rs = 0.16 × sqrt(Tmax − Tmin) × Ra`, dengan pembatas terhadap radiasi ekstraterestrial.
- OpenWeather sunshine: inversi hubungan Angström–Prescott standar; sensitivitas tergantung koefisien lokal.
- PAR estimasi: `PAR ≈ 0.45 × shortwave`.
- PPFD estimasi: `PPFD ≈ PAR × 4.57`.
- Lux estimasi kasar: `lux ≈ shortwave (W/m²) × 120`; bukan pengukuran lux langsung dan dapat menyimpang akibat spektrum cahaya/awan.
- Soil moisture Open-Meteo: data grid reanalysis (m³/m³ × 100 ke %vol), bukan kelembapan pengukuran di petak.
- Soil moisture OpenWeather: model bucket zona akar; bukan data dari OpenWeather.

Semua hasil model makro wajib dilabeli estimasi/model dan tidak boleh menimpa pembacaan soil meter atau data manual. Untuk keputusan irigasi, pemupukan atau penyakit, gunakan sensor/lab dan SOP lokal sebagai konfirmasi.

## 6. Sumber teknis

- Open-Meteo Historical Weather API: https://open-meteo.com/en/docs/historical-weather-api
- OpenWeather One Call API 3.0 / Day Summary: https://docs.openweather.co.uk/api/one-call-3
- FAO-56 Penman–Monteith: https://www.fao.org/4/X0490E/x0490e06.htm
- FAO-56 estimasi radiasi dari lama penyinaran / metode Ångström: https://www.fao.org/4/X0490E/x0490e07.htm
- Hargreaves–Samani (1985), DOI: https://doi.org/10.13031/2013.26773

## 7. Verifikasi

- Pemeriksaan statis XML, referensi Android resource, struktur kurung Java, jumlah kolom CSV header/baris, dan kompilasi smoke-test untuk model matematika dilakukan sebelum ZIP disiapkan.
- Build Android penuh perlu dikonfirmasi via GitHub Actions. Percobaan lokal di lingkungan ini gagal mengambil Gradle 9.4.1 karena DNS tidak dapat me-resolve `services.gradle.org`; hal ini bukan bukti bahwa proyek berhasil/ gagal dikompilasi oleh runner Android sebenarnya.
