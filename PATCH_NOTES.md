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

## Tampilan analisis ramah petani — perbaikan keterbacaan lanjutan
- Hasil di Catatan Lapangan tidak lagi berupa satu blok teks panjang; hasil dirender dalam kartu per bagian dengan jarak antarkartu, judul lebih besar, dan isi tiap poin dipisahkan.
- Bagian saran tindakan diberi penekanan visual agar pembaca melihat langkah yang dapat dilakukan terlebih dahulu. Profil tanaman/cara budidaya/tahap tanaman dipisahkan dari uraian.
- Halaman Analisis Agronomi juga dibagi menjadi bagian kondisi tanah, pemeriksaan parameter, perkiraan pupuk, cuaca/air, peringatan hama-penyakit, saran tindakan, serta catatan metode.
- Istilah teknis umum diganti atau dijelaskan dengan bahasa Indonesia yang lebih sehari-hari. Catatan bahwa perkiraan bukan hasil laboratorium tetap ditampilkan.
- Jawaban AI mendapat jarak antarbaris dan penekanan visual pada judul bagian; isi jawaban aslinya tetap disimpan.
- Laporan PDF menggunakan teks isi yang lebih besar dan jarak baris/paragraf yang lebih longgar agar lebih nyaman dibaca/ dicetak.
- Perhitungan, input asli, estimasi, sejarah, Open-Meteo, OpenWeather, OpenAlex, analisis OPT, CSV, serta dukungan USB Soil Meter tidak sengaja diubah.
- Pemeriksaan XML dan integritas ZIP dilakukan. Build APK belum diverifikasi di lingkungan ini karena Gradle/Android build dependencies tidak tersedia secara lokal dan unduhan Gradle memerlukan jaringan.

## Penyajian hasil analisis — pembacaan petani
- Hasil pada halaman Analisis Agronomi kini dipisahkan menjadi kartu dengan judul, jarak, dan ukuran tulisan yang lebih jelas.
- Bagian saran tindakan ditampilkan terlebih dahulu; pemeriksaan tanah, kebutuhan pupuk, cuaca, peringatan OPT, rincian tambahan, dan catatan teknis diletakkan setelahnya.
- Istilah seperti N/P/K, EC, pH, VPD, dan kg/ha dijelaskan dengan kata yang lebih umum.
- Laporan teknis asli tetap disimpan untuk konteks AI/PDF; perubahan ini hanya mengatur cara hasil ditampilkan.


## Perbaikan build Actions — 12 Oktober 2026

- Menambahkan repositori JitPack ke `dependencyResolutionManagement.repositories` pada `settings.gradle`.
- Perubahan ini memperbaiki resolusi dependensi `com.github.mik3y:usb-serial-for-android:3.9.0` yang menyebabkan task `:app:processDebugNavigationResources` gagal di GitHub Actions run 38205864030.
- Versi dependensi dan kode USB Soil Meter tidak diubah. Fitur lain tetap dipertahankan.
- Build setelah patch perlu dikonfirmasi melalui GitHub Actions setelah ZIP diunggah.

## FIX5 — penyimpanan Catatan Lapangan dan tombol GPS HP
- Penulisan riwayat menggunakan SharedPreferences sebagai kompatibilitas utama, dan file JSON internal sebagai recovery mirror best-effort. Salinan cadangan yang gagal ditulis tidak membatalkan catatan yang sudah berhasil tersimpan. Verifikasi memakai ID unik mencegah laporan sukses/gagal keliru serta duplikasi saat retry.
- Tombol GPS mengisi cache Android yang valid segera sambil meminta fix GPS/jaringan terbaru. Cache lebih tua tidak diam-diam dianggap fix baru; umur cache ditampilkan. Fix yang jelas lebih baru dapat menggantikan cache walau akurasi nominal sedikit lebih rendah.
- Koordinat dari Pengaturan langsung diterapkan ke preferences agar dashboard memuat ulang Open-Meteo tanpa perlu navigasi kembali untuk mengirim hasil.
- Tambahan `MODEL_MATEMATIKA_DAN_ALUR_v1.5.7.md` menjelaskan rumus model, alur data, konversi target hasil kg/m² lahan, estimasi hara per fase, kontribusi screening tanah, efisiensi pemulihan, kredit pemupukan dan konverter produk.
- Build Android penuh tetap harus dikonfirmasi melalui GitHub Actions; perangkat fisik diperlukan untuk mengonfirmasi fix GPS baru dan persistensi catatan pada Android.


## FIX6 — format laporan PDF/cetak
- Memperbaiki logika pagination yang menyebabkan hampir setiap elemen membuat halaman baru: konten sekarang dibandingkan terhadap tinggi halaman dikurangi margin bawah.
- Menata laporan lengkap dan ringkas dengan ringkasan eksekutif, susunan bab yang konsisten, tipografi Arial (dengan fallback font sistem di perangkat yang tidak menyediakan Arial), judul utama 14 pt, judul bab/subbab 12 pt, isi/tabel 11 pt, margin A4, footer nomor halaman, dan tabel riwayat berbaris selang-seling.
- Paragraf terlipat sesuai lebar halaman; header tabel diulang hanya saat tabel berlanjut; judul bab disimpan bersama konten awalnya untuk mengurangi judul yang terpisah di akhir halaman.
- Laporan ringkas berisi maksimal 8 riwayat terbaru. Laporan lengkap berisi maksimal 30 riwayat dan bagian AI/sumber ilmiah. Data aplikasi tidak diubah; pemotongan catatan panjang hanya pada sel tabel PDF.
- Penanganan stream saat menyimpan PDF diperkuat. Build APK penuh belum dapat dibuktikan di lingkungan ini karena Gradle wrapper tidak dapat mengunduh Gradle; lihat `PATCH_NOTES_FIX6_PDF_REPORT.md`.


## FIX7 — inferensi kelas N dari N tersedia
- Bila ambang N tersedia spesifik metode kosong/tidak valid/generik, N tersedia (mg/kg) dikonversi ke perkiraan N-total menggunakan asumsi utama 2%: N-total (%) = N tersedia/200.
- Sensitivitas 1–3% dilaporkan terpisah; kelas utama menggunakan 2%. Lima kelas N-total sekarang menjadi dasar screening N ketika ambang yang sesuai tidak tersedia.
- Kelas hasil inferensi diteruskan ke faktor dukungan tanah dalam `nutrientNeed`, sehingga data N tersedia yang valid tidak otomatis mendapat dukungan tanah 0 hanya karena ambang tidak diisi. Bila N tersedia hilang/tidak valid, model tidak mengarang kredit tanah.
- Ambang metode-spesifik yang valid tetap mengambil prioritas. Dasbor, analisis agronomi, catatan lapangan, analisis rentang, PDF dan dokumen rumus menjelaskan asal klasifikasi serta batasannya.


## FIX7 — klasifikasi N tersedia → N-total
- Konversi ditulis eksplisit: N-total (%) = ((100/2) × N tersedia mg/kg) / 10000 = N tersedia / 200.
- Jika ambang metode tidak lengkap/generik, kelas N dihitung dari N-total estimasi, bukan menganggap batas kosong sebagai pengukuran N nol.
- Jalur klasifikasi disatukan untuk mengurangi perbedaan antara analisis, kebutuhan pupuk, dan laporan.
- Uji javac dan enam contoh konversi/kelas lulus; build Android penuh masih perlu konfirmasi GitHub Actions.


## FIX8 — audit nitrogen, rumus dosis pupuk, dan kalkulator dolomit
- Rumus `N-total (%) = N tersedia (mg/kg)/200` tetap dipakai sebagai estimasi utama 2%; klasifikasi berdasarkan ambang low/high hanya aktif setelah pengguna memilih basis ambang N-spesifik/lab dan keduanya valid. Ambang generik metode P/K tidak lagi diam-diam dipakai untuk klasifikasi N.
- Model dosis pupuk dibenahi: baseline profil sudah berupa dosis hara pupuk kg/ha, sehingga tidak lagi dibagi dengan efisiensi pemulihan. Kredit riwayat 90 hari dikurangi secara konservatif berdasarkan fraksi ketersediaan asumsi N 50%, P2O5 30%, K2O 60%.
- Ditambahkan kalkulator dolomit berbasis pH awal/target, luas hektare, tekstur/faktor CaCO3-equivalent, dan efektivitas netralisasi dolomit. Formula dan hasil tersimpan muncul di analisis agronomi serta PDF. Faktor preset adalah asumsi editable, bukan pengganti uji buffer/Al-dd/CEC atau kebutuhan kapur laboratorium.
- Bukti teknis: penelitian fraksi N di tanah pertanian Jawa melaporkan rata-rata N anorganik ekstrak 1,3% dari N-total (https://www.tandfonline.com/doi/full/10.1080/00380768.2014.891925); FAO menyatakan kebutuhan kapur tidak dapat ditentukan tepat dari pH saja (https://www.fao.org/4/a0443e/a0443e03.pdf).


## FIX8 — Audit nitrogen dan kalkulator dolomit pH
- Memastikan konversi estimasi `N-total (%) = N tersedia (mg/kg) / 200` hanya dinyatakan benar di bawah asumsi `N tersedia = 2% N-total`; interval sensitivitas 1–3% tetap ditampilkan. Ambang N tersedia dari metode hanya aktif ketika pengguna memilih basis metode/lab spesifik dan kedua batas valid.
- Menyamakan jalur klasifikasi N di Catatan Lapangan, dashboard, Analisis Agronomi, laporan PDF, dan dosis hara. Nilai ambang kosong/generik tidak lagi dianggap kandungan N nol.
- Memperbaiki persamaan dosis fase: baseline profil sudah berupa rekomendasi dosis hara pupuk (kg/ha), sehingga tidak dibagi lagi dengan efisiensi pemulihan. Pupuk sebelumnya hanya memberi kredit melalui fraksi ketersediaan asumsi (N 0,50; P₂O₅ 0,30; K₂O 0,60). Koefisien tetap screening yang harus divalidasi lokal.
- Menjelaskan bahwa konversi stok tanah dari nilai N yang dimasukkan adalah stok teoritis berbasis konsentrasi input; untuk N tersedia, itu bukan N-total dan bukan kredit pupuk.
- Menyediakan kalkulator dolomit berdasarkan pH awal/target, luas hektare, faktor asumsi editable menurut tekstur, dan efektivitas netralisasi produk. Menampilkan dosis t/ha, kg/ha dan total kg; semua hasil ditandai estimasi, bukan kebutuhan kapur laboratorium.
- Pemeriksaan ilmiah: pH tunggal tidak cukup untuk menghitung kebutuhan kapur dengan presisi karena daya sangga/kemasaman cadangan bervariasi; prioritaskan pH-buffer, Al-dd/CEC, kedalaman olah dan rekomendasi laboratorium. Rujukan: FAO `https://www.fao.org/4/a0443e/a0443e03.pdf`, FAO `https://www.fao.org/4/x5872e/x5872e0a.htm`, dan studi fraksi N pada tanah pertanian Jawa `https://www.tandfonline.com/doi/full/10.1080/00380768.2014.891925`.


## FIX9 — perbaikan ekspor CSV ThingSpeak dan Open-Meteo
- Ekspor CSV ThingSpeak sekarang membaca Channel ID dan Read API Key dengan fallback yang sama seperti dashboard/Pengaturan, termasuk kunci preferensi lama. ID yang dipakai terlihat di status proses; ID kosong pada preferensi tidak lagi menjadi kegagalan palsu jika aplikasi memiliki default.
- Memperbaiki permintaan Open-Meteo Historical API dengan menghapus parameter `visibility` dan `uv_index` yang tidak disediakan endpoint arsip. Kolom CSV lama tetap tersedia namun kosong untuk dua variabel tersebut, dengan pemberitahuan di layar.
- Pesan error Open-Meteo kini menampilkan `reason` dari JSON respons bila tersedia.
- Sumber: Open-Meteo Historical Weather API (https://open-meteo.com/en/docs/historical-weather-api) dan ThingSpeak Read Data API (https://www.mathworks.com/help/thingspeak/readdata.html).
