# STASIUN CUACA • GANI CAHYO-UNS v1.5.6 — Open-Meteo + Agronomi

Patch ini memperluas v1.5.3 dengan mesin analisis agronomi terpadu.

## Perubahan utama
- Catatan Lapangan menjadi timeline terpadu untuk pengamatan, pemupukan, OPT, tanah, air, cuaca, tindakan, dan hasil.
- Pilihan budidaya: Konvensional/PHT atau Organik.
- Tanggal tanam + HST otomatis memprediksi fase tanaman menggunakan crop profile.
- Input CWT-7in1 RS485/USB: pH, kelembapan, suhu tanah, EC µS/cm, N, P, K.
- Interpretasi pH: aman/masam/alkalis + tindakan; dosis kapur tidak ditebak dari pH saja, dianjurkan pH-buffer/kemasaman/Al-dd.
- Interpretasi N/P/K: rendah/sedang/tinggi + kecukupan screening terhadap komoditas/fase.
- EC: screening rendah/cukup/tinggi dengan konversi µS/cm → dS/m dan peringatan bahwa ECe laboratorium tidak identik dengan EC sensor.
- Kelembapan tanah: kering/cenderung kering/aman/lembap tinggi/terlalu lembap.
- VPD: umumnya aman/waspada/tinggi berdasarkan fisiologi tanaman.
- Cuaca: suhu, RH, hujan, ET0, tekanan, cahaya, PAR, lama penyinaran, angin dan kecocokan ekologi komoditas.
- Prediksi OPT berbasis cuaca + histori dengan nama organisme, antara lain rice blast, wereng batang cokelat, penggerek batang, fall armyworm, bulai, antraknosa, thrips, late blight, early blight, karat kedelai.
- Model degree-day untuk fase/serangga serta STCR-style nutrient balance untuk screening kebutuhan pupuk fase.
- Catatan satu paragraf dengan kisi-kisi pengisian agar NLP/AI dapat membaca pola kejadian.
- Konteks AI diperkuat dengan evidence brief dan histori Catatan Lapangan.
- Arah angin ditampilkan sebagai 8 arah mata angin + Tenang/Variabel, tanpa derajat; format yang sama dipakai Field Notes, dashboard, dan CSV Open-Meteo.

## Batas penting
Ambang N/P/K tersedia tidak universal karena metode ekstraksi, tanah, dan komoditas berbeda. Ambang di mesin adalah screening. Rekomendasi final mengutamakan PUTS, rekomendasi P/K spesifik lokasi, petak omisi, analisis laboratorium, atau persamaan STCR yang telah dikalibrasi.

EC sensor lapang juga bukan pengganti ECe saturation paste. Gunakan terutama untuk screening dan tren.

Dosis kapur/dolomit tidak boleh ditentukan dari pH saja. Uji buffer/kemasaman/Al-dd/CEC tetap prioritas.

Prediksi OPT bukan diagnosis. Verifikasi dengan gejala, populasi, luas serangan, dan musuh alami.


### Tambahan klasifikasi tanah
- P dan K sekarang memakai lima kelas: Sangat Rendah, Rendah, Sedang, Tinggi, Sangat Tinggi.
- pH ditampilkan berdasarkan rentang Pusat Penelitian Tanah sebelum tindakan koreksi.
- EC sensor ditampilkan dalam lima kelas DHL/EC sebelum rekomendasi; ECe laboratorium tetap dipisahkan.
- Urutan analisis menjadi: data terukur → klasifikasi rentang → faktor pembatas → rekomendasi AI menyeluruh.


## Analisis seluruh parameter — patch lanjutan
- Tahap analisis awal sekarang berjalan untuk seluruh data lingkungan dan agronomi yang tersedia, bukan hanya P/K/pH/EC.
- PPFD dianalisis setelah nilai PPFD tersedia; statusnya dipisahkan dari PAR energi (W/m²) dan Shortwave.
- Kelembapan tanah sensor dianalisis dengan 5 status screening dan, bila FC/PWP tersedia, dibandingkan relatif terhadap kapasitas lapang dan titik layu permanen.
- Kelembapan tanah Open-Meteo 0–10 cm (m³/m³) juga dibaca; bila FC/PWP tersedia, nilainya dibandingkan pada skala volume tanah.
- Parameter cuaca yang dianalisis: suhu udara, suhu terasa, titik embun, RH, tekanan, hujan, ET0, angin, gust, tutupan awan, UV, visibilitas, Shortwave, PAR energi, PPFD, dan lama penyinaran.
- Parameter tanah tambahan yang tersedia: suhu tanah, bahan organik, CEC/KTK, bulk density, dan kedalaman. Parameter metode-spesifik seperti pH-buffer/Al-dd/H-dd tetap dicantumkan tanpa membuat ambang universal palsu.
- AI diwajibkan mengikuti urutan: FAKTA TERUKUR → KELAS/RENTANG SEMUA PARAMETER → HUBUNGAN/FAKTOR PEMBATAS → REKOMENDASI MENYELURUH 0–24 jam dan 1–7 hari.
- Semua status parameter diberi label screening bila ambangnya bergantung komoditas, metode, tekstur, ketinggian, atau kondisi lokasi.


## Agronomy auto-input / history / notes patch
- Automatic source selector added to Field Notes: ThingSpeak, Open-Meteo, or priority ThingSpeak → Open-Meteo.
- Manual editing remains available after automatic fill; automatic sources do not replace unsupported fields.
- ThingSpeak field names/values are cached for name-aware agronomy mapping.
- History manager supports deleting selected or all items for field notes, fertilizer and OPT history.
- Bulk density presets: mineral 1.30 g/cm³, peat reference 0.30 g/cm³, custom input.
- EC sensor is treated as a valid primary project measurement. The app only converts units (µS/cm ↔ dS/m) and does not require ECe.
- Method/formula/scientific notes are moved to clickable NOTE dialogs; the main analysis remains focused on values, statuses and recommendations.


### CSV Open-Meteo + AI recommendation pass
- Added Open-Meteo Historical Weather CSV export by date range, hourly/daily.
- Preserved existing ThingSpeak range/all-history export.
- Hid metadata/formula explanations from the main agronomy report and moved them to the ⓘ information dialog.
- AI output budget increased to 5000 tokens and prompt changed to recommendation-first.
- AI can use the built-in web search tool for OPT discovery beyond the local crop database; `tool_choice=required` makes a tool call mandatory for the advisor request.
- Removed embedded default OpenAI API key literals from AgronomyActivity and SettingsActivity.

### Histori terpadu — tampilan layar vs laporan
- Bagian histori terpadu dihapus dari hasil analisis agronomi yang tampil di layar.
- Data histori tidak dihapus dari penyimpanan dan tetap digunakan untuk konteks AI serta laporan.
- Saat Print/Save PDF, histori lapangan + pemupukan + OPT ditampilkan dalam format tabel kolom Tanggal, Jenis, Kegiatan/OPT, dan Detail.
- Format JSON mentah dan tanda kutip ganda tidak ditampilkan pada tabel laporan.
- Tabel otomatis membungkus teks dan mengulang header ketika berganti halaman.
- Histori tetap dimasukkan pada laporan ringkas maupun lengkap karena Print/Save adalah tempat tampilnya histori.

## Build/UI correction - 2026-10-06
- Fixed `CsvDownloadActivity.readAll(InputStream)` compile error reported by GitHub Actions run #25.
- Added optional Light / Dark / System appearance selection in Settings.
- Added theme-qualified colors and a theme-aware Activity base class only; weather, ThingSpeak, agronomy, history, CSV and PDF logic remain intact.
- OpenAI API key remains blank by default.


## Multi-provider AI
- OpenAI tetap dipertahankan sebagai provider default dan tidak diubah endpoint/logikanya.
- Ditambahkan Google Gemini melalui Gemini API resmi dengan Google Search grounding.
- Ditambahkan OpenRouter Free sebagai alternatif untuk model-model gratis.
- API key OpenAI/Gemini/OpenRouter tidak ditanam ke source; semua dimasukkan pengguna dan disimpan pada pengaturan lokal.
- Tidak ada field ThingSpeak, data cuaca, analisis agronomi, histori, CSV, PDF, GPS, atau OpenAlex yang dihapus.

## CSV filename follows application title — 2026-10-07
- Semua nama file CSV ekspor sekarang mengambil judul aplikasi dari `app_title` pada Pengaturan/Konfigurasi.
- Berlaku untuk ekspor ThingSpeak berdasarkan rentang tanggal, seluruh histori ThingSpeak, dan histori Open-Meteo.
- Judul hanya disanitasi pada nama file (karakter terlarang diganti dan spasi dirapikan); judul yang tampil di aplikasi tidak diubah.


## Sinkronisasi & CSV Open-Meteo — 2026-10-07
- Memperbaiki compile blocker `fetchOpenAlex(...)` yang dipanggil oleh `AgronomyActivity` tetapi sebelumnya belum memiliki implementasi.
- `AgronomyActivity` dirender ulang pada `onResume()` agar perubahan dari Field Notes/Settings langsung terlihat saat kembali.
- Urutan sumber analisis pada PDF memprioritaskan `last_agronomy_analysis`, lalu fallback ke `last_field_analysis`.
- Memperbaiki rekomendasi P/K agar kelas `Tinggi` dan `Sangat Tinggi` tidak terlewat ketika klasifikasi membawa keterangan basis/metode.
- Cache Open-Meteo di Field Notes diperluas agar suhu terasa, titik embun, angin/gust, awan, visibilitas, UV, Shortwave, PAR, PPFD, sunshine, dan VPD sinkron dengan dashboard.
- CSV Open-Meteo menjadi sumber utama pada halaman CSV, tetap mempertahankan ekspor ThingSpeak sebagai opsi.
- CSV hourly menambahkan kolom PAR energi, PPFD dan VPD; arah angin diekspor sebagai nama mata angin, bukan derajat.
- CSV daily menambahkan PPFD rata-rata/maksimum dan VPD rata-rata/maksimum yang dihitung dari data hourly Open-Meteo pada tanggal yang sama; arah angin dominan juga dikonversi ke mata angin.
- Arah angin aplikasi diseragamkan menjadi 8 sektor: Utara, Timur Laut, Timur, Tenggara, Selatan, Barat Daya, Barat, Barat Laut. Label legacy 16 sektor tetap dapat dibaca saat migrasi data tersimpan.


## Definisi ilmiah dan sinkronisasi akhir — 2026-10-07
- `fetchOpenAlex()` dibuat robust: kegagalan OpenAlex tidak lagi menggagalkan analisis AI utama; OpenAlex diposisikan sebagai sumber evidence pendukung.
- P/K tidak lagi diuji dengan `equals()` terhadap string kelas mentah; engine sekarang membaca prefiks kelas sehingga keterangan metode seperti `Tinggi (P Olsen screening)` tetap diproses benar.
- `FieldNotesActivity` kini memperbarui cache cuaca global `om_*` saat catatan manual disimpan, termasuk suhu, RH, tekanan, curah hujan harian, ET₀, PPFD, VPD dan arah angin. Nilai Open-Meteo yang tidak diinput manual tidak dipertahankan sebagai data terkini sehingga laporan tidak mencampur timestamp sumber yang berbeda.
- Analisis terakhir diberi timestamp terpisah (`last_agronomy_analysis_epoch` dan `last_field_analysis_epoch`). PDF memilih analisis yang benar-benar paling baru, kemudian memakai fallback bila salah satu belum tersedia.
- Terminologi cahaya diperjelas: PAR energi 400–700 nm, PPFD 400–700 nm dalam µmol m⁻² s⁻¹. PPFD dari Open-Meteo diberi label estimasi broadband; PPFD manual tidak dibalik menjadi PAR energi.
- Istilah `Hujan 24 jam` diganti menjadi `Curah hujan harian (00:00–24:00 waktu lokal)` karena `precipitation_sum` Open-Meteo adalah agregasi hari kalender, bukan rolling 24 jam.
- CSV Open-Meteo tetap mempertahankan PPFD/VPD dan arah angin berbasis mata angin tanpa kolom derajat.
