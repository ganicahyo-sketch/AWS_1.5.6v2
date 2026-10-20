> **Catatan historis:** catatan ini mendeskripsikan implementasi FIX16. Sejak FIX20, integrasi OpenWeather beralih ke Current Weather dan Forecast 5 Day / 3 Hour API standar; UV/riwayat OpenWeather tidak dianggap tersedia pada endpoint standar dan tidak ada pemanggilan One Call API 3.0.

# FIX16 — WeatherAPI + Perbaikan Dashboard OpenWeather

Basis: `STASIUN CUACA v1.5.7 — FIX15` dari paket sumber yang tersedia. `versionName` dan `versionCode` aplikasi sengaja dipertahankan sesuai basis v1.5.7/157 agar pembaruan ini tetap merupakan patch sumber, bukan rilis versi aplikasi baru.

## 1. Provider WeatherAPI

- Menambahkan WeatherAPI sebagai pilihan sumber cuaca makro di Pengaturan bersama Open-Meteo dan OpenWeather One Call API 3.0.
- API key WeatherAPI disimpan melalui Pengaturan aplikasi dan tidak ditanam dalam source code.
- Dashboard mengambil kondisi saat ini, prakiraan WeatherAPI (meminta 7 hari bila paket mengizinkan; mencoba ulang 3 hari hanya jika respons jelas menunjukkan pembatasan paket), suhu, terasa seperti, RH, titik embun, tekanan, awan, angin, hembusan, visibilitas, hujan, UV, dan astronomi matahari. Label prakiraan netral sehingga tidak menjanjikan rentang tujuh hari jika paket memberikan kurang dari itu.
- Cuaca makro WeatherAPI dapat dipakai dari Catatan Lapangan (pilihan eksplisit atau Prioritas mengikuti Pengaturan), ringkasan Agronomi, serta CSV histori harian.
- CSV WeatherAPI menggunakan History API per tanggal agar kompatibel dengan lebih banyak tingkatan paket. Histori dimulai pada 1 Januari 2010 dan tetap bergantung pada izin/paket akun; rentang lebih dari 31 hari menampilkan konfirmasi karena tiap hari dapat membutuhkan satu permintaan.
- Data ET₀, radiasi, sunshine duration, PAR, PPFD, Lux, dan kelembapan tanah makro diberi label model/estimasi bila respons/paket WeatherAPI tidak menyediakannya secara langsung. Model kelembapan tanah terpisah dari sensor USB/RS485 dan input manual.

## 2. Dashboard OpenWeather

- Jam matahari terbit/terbenam menggunakan timestamp harian dari `daily[].sunrise` / `daily[].sunset`, dengan fallback ke current timestamp hanya jika field harian tidak tersedia.
- Timestamp Unix dikonversi menggunakan zona waktu yang dikembalikan oleh respons OpenWeather, bukan asumsi UTC atau pemotongan timestamp mentah. Tampilan jam memakai `HH:mm`.
- Hujan harian menggunakan `daily[].rain` dan tidak mencampurkan salju ke angka hujan. Karena OpenWeather dapat menghilangkan field `rain` ketika tidak ada presipitasi, field yang tidak hadir diperlakukan sebagai 0 mm pada ringkasan harian. Hujan berjalan menggunakan `current.rain.1h` bila tersedia; field yang tidak hadir menunjukkan tidak ada hujan terlapor untuk interval itu.
- UV saat ini menggunakan `current.uvi`, sementara UV maksimum harian menggunakan `daily[].uvi`. Angka nol dianggap nilai yang valid, dan jika salah satu parameter tidak ada, parameter lain tetap ditampilkan. `--` hanya digunakan jika keduanya tidak tersedia.
- Label dashboard menjelaskan rentang hujan (`hari ini` versus `1 jam terakhir`) untuk mengurangi kebingungan antara akumulasi harian dan pembacaan saat ini.

## 3. Konsistensi provider dan cache

- Perubahan provider pada Pengaturan memicu pemuatan dashboard dengan sumber baru.
- Catatan Lapangan membedakan cache provider yang benar-benar digunakan agar tidak salah memberi label data WeatherAPI sebagai Open-Meteo/OpenWeather.
- Estimasi kelembapan tanah WeatherAPI memakai state model terpisah (`weatherapi_soil_water_fraction/date`); nilai tersebut hanya ringkasan makro model dan tidak mengisi kolom pengukuran tanah lapangan.
- Ringkasan cuaca menyebut sumber provider dan metode estimasi sehingga data API langsung tidak tercampur dengan turunan matematis.

## 4. CSV histori

- Menambahkan pilihan WeatherAPI pada ekspor cuaca makro.
- CSV harian menyertakan sumber, tanggal, koordinat, cuaca, hujan, angin, UV, waktu terbit/terbenam, durasi siang, sunshine, shortwave, PAR, PPFD, Lux, ET₀, serta kelembapan tanah makro model dan kolom metode.
- Jika shortwave/ET₀ tidak tersedia dari tingkat langganan atau respons historis, file memberi label estimasi Hargreaves–Samani / FAO-56 Penman–Monteith alih-alih menganggapnya pengukuran langsung.

## 5. Validasi dan batas pengujian

- XML layout/resource berhasil diparse secara statis; semua XML dalam paket tidak memiliki kesalahan sintaks XML.
- Pemeriksaan delimiter dan pemeriksaan sintaks kasar pada file Java yang berubah tidak menemukan tanda kurung/string yang tidak berpasangan atau pesan kesalahan sintaks Java dari `javac`.
- Kompilasi Android penuh tidak dapat dijalankan pada lingkungan ini karena paket ZIP sumber tidak menyertakan `gradle-wrapper.jar`, dan Android SDK/`android.jar` tidak tersedia. Oleh karena itu build APK penuh masih perlu diverifikasi melalui GitHub Actions setelah source ZIP diunggah.

## 6. Dokumentasi API

- OpenWeather One Call 3.0: https://docs.openweather.co.uk/api/one-call-3
- WeatherAPI Forecast / History API: https://www.weatherapi.com/docs/
- WeatherAPI History API: https://www.weatherapi.com/docs/history-api/
