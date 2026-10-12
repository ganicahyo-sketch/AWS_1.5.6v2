## Perapian hasil analisis untuk petani
- Hasil analisis di layar disusun ulang agar saran tindakan dibaca lebih dahulu, diikuti sumber data, kondisi tanah, perkiraan pupuk, cuaca, peringatan OPT, catatan kebun, dan rincian tambahan.
- Antarbagian diberi jarak, judul dibuat lebih jelas, ukuran teks hasil analisis diperbesar, dan istilah teknis utama diberi penjelasan yang lebih mudah dipahami.
- Dasar data dan ketidakpastian dijabarkan dengan bahasa sehari-hari; data asli tidak ditimpa asumsi/estimasi. Hasil mentah teknis tetap disimpan terpisah untuk menjaga jejak perhitungan.
- PDF mempertahankan pergantian paragraf untuk analisis agronomi sehingga laporan tidak menjadi satu blok teks rapat.
- Perubahan ini hanya menyentuh penyajian hasil; mesin hitung, nilai data, integrasi, dan fitur lainnya tidak sengaja diubah.

# STASIUN CUACA • GANI CAHYO-UNS v1.5.7 — Open-Meteo + Agronomi

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
- Arah angin ditampilkan sebagai 16 mata angin + Tenang/Variabel, tanpa derajat.

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

## Tambahan database kelapa sawit
- Menambahkan `PalmOilAdvisor` sebagai lapisan basis pengetahuan spesifik kelapa sawit, TBM awal/TBM lanjut/TM.
- Membaca N tersedia, P tersedia, K tersedia (mg/kg), EC sensor (µS/cm), pH, kelembapan, hujan dan RH dari input yang sudah ada.
- Memberi prioritas pemeriksaan hara dan tindakan teknis kebun tanpa menyamakan pembacaan sensor murah dengan analisis laboratorium.
- AI prompt meminta pencarian web wajib untuk sumber resmi PPKS/IOPRI dan OPT; melarang mengarang dosis resmi atau konversi langsung NPK sensor menjadi kg pupuk/pokok.
- Menambahkan sumber resmi PPKS/IOPRI dan catatan keterbatasan sensor ke `SCIENTIFIC_SOURCES.md`.
- Dosis final tetap memerlukan data tambahan (analisis daun/tanah, umur, jumlah pokok/ha, jenis tanah, produktivitas dan riwayat pemupukan), selaras dengan cakupan layanan rekomendasi PPKS.


## Patch lama penyinaran + refresh GPS perangkat
- Dashboard Open-Meteo kini menampilkan `sunshine_duration` sebagai lama penyinaran matahari dalam jam/hari; nilainya tetap disimpan di `om_sun_hours` dan dipakai analisis agronomi.
- CSV Open-Meteo hourly menyertakan `sunshine_hours` (durasi per jam, dikonversi dari detik); CSV daily menyertakan `sunshine_hours` dan `daylight_hours` (dua besaran berbeda).
- Tombol GUNAKAN GPS HP kini meminta fix lokasi baru dari provider GPS dan/atau jaringan, bukan langsung memakai `getLastKnownLocation()` yang mungkin sudah lama. Koordinat terbaru yang diterima disimpan ke pengaturan, ditampilkan, lalu memicu pembaruan Open-Meteo. Ada batas tunggu 20 detik dan pesan jika perangkat tidak mendapat fix.
- Tombol GPS pada Pengaturan meneruskan permintaan fix terbaru ke dashboard.
- Fitur ThingSpeak, riwayat, catatan lapangan, analisis agronomi, dan ekspor CSV lain tidak dihapus atau sengaja diubah.


## Rilis 1.5.7 — lama penyinaran + GPS terbaru
- Versi aplikasi dinaikkan menjadi 1.5.7 (versionCode 157).
- Dashboard menampilkan lama penyinaran harian Open-Meteo (`sunshine_duration` → `om_sun_hours`).
- CSV Open-Meteo hourly dan daily tetap menyertakan `sunshine_hours`; daily juga membedakan `daylight_hours`.
- Tombol GUNAKAN GPS HP mengabaikan fix lokasi yang sudah kedaluwarsa saat listener baru dipasang, lalu menyimpan koordinat fix baru dan langsung memuat ulang data Open-Meteo. GPS diprioritaskan dengan batas akurasi 50 m; fix jaringan yang akurat (≤25 m) tetap dapat menjadi fallback. Batas tunggu tetap 20 detik.
- Perubahan terisolasi; ekspor CSV ThingSpeak dan fitur aplikasi lainnya dipertahankan.


## v1.5.7 — alur rekomendasi pemupukan adaptif dan asal data
- Analisis Catatan Lapangan menghitung ulang otomatis setelah input agronomi/cuaca berubah, pilihan metode uji atau budidaya berubah, data otomatis diperbarui, pembacaan USB masuk, atau riwayat pemupukan berubah. Perhitungan dibatasi debounce singkat agar tidak menghitung ulang pada setiap karakter secara berlebihan.
- Laporan analisis membedakan nilai yang tersedia dari input/sensor/API/riwayat, parameter yang belum tersedia, dan nilai default/asumsi model. Tingkat ketidakpastian ditampilkan berdasarkan kelengkapan data.
- Nilai estimasi hanya digunakan secara internal dalam perhitungan dan tidak ditulis ke kolom data asli. Data N/P/K yang hilang tidak dibuat seolah-olah hasil sensor/laboratorium; mesin memakai baseline kebutuhan komoditas/fase dan asumsi konservatif tanpa kredit kontribusi tanah untuk unsur yang belum diuji.
- Ketika nilai asli baru masuk atau diperbarui, nilai itu langsung dipakai untuk analisis ulang; estimasi sementara tidak mengunci atau menimpa input pengguna, USB/RS485, ThingSpeak/Open-Meteo, atau riwayat.
- Rekomendasi numerik tetap diberi label screening/STCR-style dan ketidakpastian, bukan dosis resmi spesifik lokasi. Data penting yang belum ada tetap dicantumkan agar pengguna tahu pengukuran yang perlu dilengkapi.
- Fitur lama Open-Meteo, pemetaan ThingSpeak, OpenAlex, riwayat, analisis OPT, CSV, PDF, dan deteksi USB Soil Meter tetap dipertahankan.
