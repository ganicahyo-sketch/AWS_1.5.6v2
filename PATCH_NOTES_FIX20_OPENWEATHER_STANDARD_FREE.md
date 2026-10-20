# FIX20 — OpenWeather Standard API (Free-compatible)

Tanggal: 2026-10-20

## Perubahan utama

- Menghapus endpoint One Call API 3.0 (`/data/3.0/onecall`) dari Dashboard dan Catatan Lapangan.
- Menggunakan endpoint Current Weather standar (`/data/2.5/weather`) dan 5 Day / 3 Hour Forecast (`/data/2.5/forecast`). Keduanya didokumentasikan OpenWeather sebagai current weather dan prakiraan 5 hari dengan interval 3 jam.
- Sunrise/sunset hari ini diambil dari `sys.sunrise` / `sys.sunset` pada Current Weather, dikonversi menggunakan offset zona waktu respons.
- Prakiraan hari-per-hari diringkas dari interval forecast berdasarkan tanggal lokal; suhu minimum/maksimum berasal dari field forecast dan hujan dari jumlah `rain.3h`. UI menyebut hujan ini sebagai prakiraan sisa hari, bukan observasi hujan harian.
- UV index, dew point, dan riwayat cuaca tidak ditampilkan sebagai data langsung karena tidak tersedia pada endpoint standar ini. Endpoint UV Index lama telah dihentikan oleh OpenWeather.
- CSV histori OpenWeather memberi pesan yang menjelaskan keterbatasan endpoint standar; pengguna diarahkan memilih Open-Meteo atau WeatherAPI untuk histori. Metode histori One Call Day Summary dihapus agar tidak memanggil layanan One Call.
- ET₀, radiation, sunshine duration, PAR, PPFD, lux dan soil moisture makro masih berupa estimasi model dan harus dilabeli demikian. Data tanah terukur atau masukan manual tetap dipisahkan.

## Dokumentasi resmi

- Current Weather: https://openweathermap.org/current
- 5 Day / 3 Hour Forecast: https://openweathermap.org/forecast5
- OpenWeather API catalog notes UV Index endpoint is retired: https://openweathermap.org/api
