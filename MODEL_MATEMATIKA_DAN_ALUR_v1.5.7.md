# STASIUN CUACA v1.5.7 — Model Matematika dan Alur Perhitungan

Dokumen ini mendeskripsikan **kode yang benar-benar dipakai** oleh `AgronomyEngine`, `FieldNotesActivity`, `AgronomyActivity`, dan `FertilizerConverterDialog`. Model saat ini adalah model deterministik berbasis aturan dan neraca sederhana (screening), bukan model machine-learning yang dilatih dengan data hasil panen, bukan persamaan QUEFTS penuh, dan bukan persamaan STCR spesifik lokasi yang sudah dikalibrasi.

## 1. Alur data

1. **Pilih komoditas dan varietas/galur.** Komoditas memilih profil umum (umur siklus, pH/suhu screening, ambang EC, dan baseline kebutuhan N/P₂O₅/K₂O). Kolom varietas dicatat, tetapi belum otomatis memilih koefisien produktivitas spesifik varietas dari suatu basis data tervalidasi.
2. **Tentukan umur/fase.** Umur HST dihitung dari tanggal tanam sampai tanggal catatan, atau dibaca dari input umur manual. MST dikonversi ke hari dengan `MST × 7`. Fase manual mengungguli fase otomatis bila kolom diisi; bila kosong, fase dipilih berdasarkan umur dan panjang siklus profil.
3. **Gabungkan data aktual.** Data USB soil meter 8-in-1, input manual, ThingSpeak, dan Open-Meteo tetap dibedakan sumbernya. Data yang hilang tidak ditulis seolah-olah hasil sensor. ThingSpeak diprioritaskan; bila suatu parameter tidak valid/tidak tersedia, Open-Meteo menjadi cadangan untuk parameter yang dimilikinya.
4. **Klasifikasi screening.** pH, EC, kelembapan, N/P/K, cuaca, VPD dan parameter cahaya dibandingkan dengan ambang operasional/profil. Untuk N, pengguna harus memilih basis ambang N-spesifik secara eksplisit sebelum batas rendah/tinggi digunakan; nama metode P/K seperti HCl, Bray, Olsen, Mehlich atau NH4OAc tidak otomatis dianggap ambang N yang sah. Jika basis default dipilih, N-total diestimasi dari N tersedia memakai asumsi 2% dan hasilnya diberi label screening.
5. **Estimasi kebutuhan hara per fase.** Baseline yang ada pada profil adalah baseline dosis rekomendasi hara pupuk (kg N/P₂O₅/K₂O per ha), bukan serapan tanaman. Baseline dikalikan fraksi fase dan skala target hasil, lalu dikurangi dukungan screening kelas tanah dan fraksi kredit pupuk sebelumnya. Baseline tidak dibagi lagi oleh efisiensi pemulihan.
6. **Konversi unsur hara ke pupuk aktual.** Kebutuhan N, P₂O₅ dan K₂O dikonversi menjadi dosis produk berdasarkan persentase pada label pupuk yang dipilih petani.
7. **Laporkan sumber dan ketidakpastian.** Hasil merupakan perkiraan screening. Rekomendasi final perlu diuji terhadap panduan resmi sesuai komoditas, lokasi, kelas tanah, varietas, pola tanam, dan metode analisis yang cocok.

## 2. Umur dan fase

Jika tanggal tanam dan tanggal catatan valid:

`umur_hari = tanggal_catatan − tanggal_tanam`

`umur_MST = umur_hari / 7`

Fase otomatis menggunakan batas pada kode: establishment sampai `max(20 hari, 18% siklus)`; vegetatif sampai `max(35 hari, 45% siklus)`; pembungaan/reproduktif sampai `max(60 hari, 68% siklus)`; kemudian pengisian/pemasakan sampai umur siklus profil berakhir. Setelah umur siklus, aplikasi menyarankan verifikasi varietas/rotasi. Batas ini aturan umum, bukan fenologi terkalibrasi untuk semua varietas.

Untuk perhitungan kebutuhan pupuk, label fase dipetakan ke fraksi baseline: establishment 0,15; vegetatif 0,30; reproduktif/pembungaan 0,35; pengisian/pemasakan 0,20. Jika label fase manual tidak dikenali, aplikasi kembali ke fraksi berdasarkan umur.

## 3. Target produksi

Input target produksi di Catatan Lapangan adalah **total kg hasil untuk luas petak m²**. Model mengubahnya ke ton/ha:

`target_t_per_ha = target_total_kg × 10 / luas_m2`

Contoh: 500 kg dari 1.000 m² = `500 × 10 / 1.000 = 5 t/ha`. Jika target hasil tidak diisi, skala hasil bernilai 1,0 dan profil baseline tetap digunakan.

Skala hasil dibandingkan dengan hasil referensi generik komoditas:

`yield_scale = clamp(target_t_per_ha / hasil_referensi_t_per_ha, 0,5, 1,8)`

Artinya skala dibatasi minimum 0,5 dan maksimum 1,8; batas tersebut adalah pengaman model, bukan hukum respons hasil tanaman. Hasil referensi saat ini per komoditas, bukan klaim resmi produktivitas varietas tertentu.

## 4. Estimasi N, P₂O₅ dan K₂O

Untuk nutrien `j`:

`kebutuhan_fase_j = baseline_musim_j × fraksi_fase × yield_scale`

Model memberi faktor dukungan screening dari kelas soil-test:

| Kelas | Dukungan model |
|---|---:|
| Sangat rendah | 0% |
| Rendah | 20% |
| Sedang | 50% |
| Tinggi | 80% |
| Sangat tinggi | 90% |

`dosis_kotor_j = kebutuhan_fase_j × (1 − dukungan_soil_test)`

Baseline profil sudah berupa dosis hara pupuk yang akan diaplikasikan. Karena itu aplikasi **tidak membaginya lagi dengan efisiensi pemulihan**; pembagian tersebut sebelumnya bisa menggelembungkan dosis, misalnya menggandakan kebutuhan N saat efisiensi diasumsikan 50%.

Untuk kredit pemupukan 90 hari terakhir (`kredit_hara_nominal_j`, satuan kg/ha hara dari label), dipakai fraksi ketersediaan sementara N 0,50; P₂O₅ 0,30; K₂O 0,60:

`kredit_tersedia_j = max(0, kredit_hara_nominal_j) × fraksi_ketersediaan_j`

`dosis_pupuk_unsur_j = max(0, dosis_kotor_j − kredit_tersedia_j)`

P dan K tetap memakai rentang screening menurut metode yang dipilih. Untuk N, ambang bawah/atas hanya digunakan bila pengguna secara eksplisit memilih basis **ambang N tersedia khusus metode/lab** dan kedua batas valid (`low > 0`, `high > low`). Bila basis default, ambang kosong/generik, atau tidak valid, N-total diestimasi dari N tersedia memakai asumsi 2%, lalu kelas N-total dipakai untuk faktor dukungan tanah. Jika input N tersedia hilang/invalid, model tidak mengklaim kontribusi N tanah; dosis profil tetap menjadi baseline dengan ketidakpastian lebih tinggi.

Penting: faktor dukungan 0–90% dan fraksi kredit 50/30/60% adalah koefisien heuristik, bukan konstanta universal. Baseline masih perlu dikalibrasi menurut rekomendasi resmi, target hasil, varietas, tanah, musim, dan lokasi.


## 5. Estimasi awal kebutuhan dolomit dari pH

Fitur kalkulator menggunakan pH awal, pH target, luas lahan, faktor kebutuhan CaCO₃ ekuivalen per unit pH, dan efektivitas netralisasi dolomit (ECCE/ENP). Rumus yang dipakai:

`ΔpH = max(0, pH_target − pH_awal)`

`CaCO3_equivalent (t/ha) = ΔpH × faktor_tekstur (t/ha per unit pH)`

`dolomit (t/ha) = CaCO3_equivalent / (ECCE/100)`

`total_dolomit (kg) = dolomit (t/ha) × luas (ha) × 1000`

Nilai bawaan yang dapat diedit adalah 1,0 t CaCO₃-eq/ha/pH untuk tanah mineral ringan, 2,0 untuk mineral sedang, 4,0 untuk mineral berat/liat, dan 5,0 untuk gambut. Ini faktor asumsi operasional aplikasi, **bukan nilai universal atau dosis laboratorium**; contoh FAO pada beberapa tanah gambut tropis melaporkan sekitar 5 t/ha untuk kenaikan pH mendekati satu unit pada kondisi lokasi tertentu, tetapi variasi antar-tanah sangat besar. Bila ΔpH >1 atau dosis >5 t/ha, aplikasi memberi peringatan untuk mempertimbangkan pembagian aplikasi dan uji ulang pH.

FAO menjelaskan bahwa kebutuhan kapur tidak dapat dihitung tepat dari pH tunggal karena kemasaman cadangan tidak terlihat hanya dari pH. Gunakan hasil laboratorium/pH-buffer, Al-dd, CEC/KTK, kedalaman olah, tekstur dan mutu dolomit untuk menetapkan dosis final. Sumber: [FAO — Plant nutrition for food security](https://www.fao.org/4/a0443e/a0443e03.pdf); [FAO — Agricultural management, liming](https://www.fao.org/4/x5872e/x5872e0a.htm).

## 6. Perhitungan stok lapisan tanah dan air

Aplikasi menampilkan perkiraan stok unsur pada lapisan tanah:

`stok_kg_ha = konsentrasi_mg_kg × bulk_density_g_cm3 × kedalaman_cm × 0,10`

Persamaan tersebut adalah konversi massa tanah teoritis pada kedalaman seragam. **Stok ini belum dikurangkan langsung ke rumus kebutuhan pupuk**, karena stok teoritis tidak otomatis sama dengan hara tersedia, bisa diserap, atau setara metode soil-test. Jika input N merupakan N tersedia, hasil yang dihitung adalah stok teoritis berdasarkan konsentrasi N tersedia, bukan N-total terukur, bukan serapan dan bukan kredit pupuk. Untuk data meter USB 8-in-1, kesesuaian satuan dan kalibrasi alat tetap harus dibuktikan terhadap metode rujukan sebelum stok dapat dipakai sebagai kredit agronomi.

Jika FC dan PWP tersedia:

`FTSW = clamp((kelembapan − PWP) / (FC − PWP), 0, 1)`

`TAW_mm = (FC − PWP) × kedalaman_cm × 0,1`

`RAW_mm = TAW_mm × fraksi_RAW`

Fraksi RAW operasional saat ini dipilih dari ET₀: 0,7 ketika ET₀ ≤ 2 mm/hari; 0,3 ketika ET₀ ≥ 5 mm/hari; selain itu 0,5. Jika FC/PWP tidak tersedia, sensor kelembapan hanya dinilai dengan ambang screening.

VPD dihitung dari suhu udara `T` (°C) dan RH (%):

`e_s = 0,6108 × exp(17,27 × T / (T + 237,3))`

`VPD = max(0, e_s × (1 − RH/100))` kPa

PAR/PPFD dan lux dari radiasi Open-Meteo merupakan estimasi broadband: PAR ≈ 0,45 × shortwave (W/m²), PPFD ≈ PAR × 4,57 (µmol/m²/s), sedangkan lux pada dashboard ≈ shortwave × 120. Lux estimasi ini **bukan** pembacaan sensor lux; bila tersedia pengukuran langsung yang valid di ThingSpeak atau dari alat ukur, utamakan pengukuran tersebut.

## 7. Konversi dosis menjadi pupuk milik petani

Untuk setiap produk `i`, angka komposisi label diubah ke fraksi:

`A_Ni = persen_N_i/100`, `A_Pi = persen_P2O5_i/100`, `A_Ki = persen_K2O_i/100`

Aplikasi menyusun matriks komposisi produk `A` dan target unsur `b = [N, P₂O₅, K₂O]`, lalu mencari dosis nonnegatif `x_i` yang meminimalkan:

`minimize Σ_j (Σ_i A_ji × x_i − b_j)²`, dengan `x_i ≥ 0`.

Hingga lima produk dapat dimasukkan, tetapi algoritme saat ini menguji kombinasi dengan maksimum tiga produk aktif untuk memenuhi tiga target unsur. Output adalah dosis kg/ha dan, jika luas petak ada, jumlah produk untuk luas tersebut. Residu ditampilkan bila kombinasi stok pupuk tidak dapat memenuhi seluruh target secara bersamaan.

Konverter belum mempertimbangkan harga, jumlah stok yang tersedia dalam kg, batas maksimum dosis label, bentuk fisik, waktu/penempatan, kehilangan hara, kompatibilitas pencampuran, dan keseimbangan biaya. Karena itu hasilnya adalah konversi matematis berbasis label, **bukan** pemeriksaan keamanan dosis atau pengganti anjuran SOP.

## 8. Batas interpretasi soil meter 8-in-1

Nilai N/P/K dari sensor digunakan sebagai input sebagaimana diminta, tetapi kode saat ini belum memiliki model kalibrasi khusus untuk setiap model/firmware, jenis tanah, kadar air, suhu, metode ekstraksi, dan varietas. Rentang P/K yang dipakai adalah klasifikasi screening yang diberi label metode; itu tidak menjadikan angka sensor otomatis setara hasil laboratorium. Untuk penggunaan dosis produksi tinggi, bandingkan sensor dengan sampel tanah laboratorium dan rekomendasi spesifik lokasi.

Dokumen resmi BRMP/Kementerian Pertanian menekankan rekomendasi N/P/K yang spesifik lokasi dan bergantung status hara tanah; formula NPK majemuk tetap harus disesuaikan agar tidak menyebabkan kelebihan satu unsur dan kekurangan unsur lainnya:
- BRMP Tanah dan Pupuk, *Rekomendasi Pupuk N, P, dan K untuk Tanaman Pangan Edisi 2* (November 2024): https://repository.pertanian.go.id/items/69eba091-7cb8-4d9b-8338-1314be912c7c
- BRMP, *Dosis Pupuk N, P, K untuk Tanaman Padi, Jagung dan Kedelai pada Lahan Sawah (Per Kecamatan)* (2023): https://repository.pertanian.go.id/items/03b5bfd9-392d-4ea7-b525-336119e06e08

Jadi, keluaran aplikasi saat ini tepat diperlakukan sebagai gambaran awal dan alat bantu pencatatan. Untuk meningkatkan akurasi berikutnya, koefisien baseline, ambang kelas, faktor pemulihan, dan target varietas perlu dilatih/kalibrasi terhadap data percobaan atau SOP resmi per komoditas dan zona agroekologi.


## Perbaikan inferensi kelas nitrogen (FIX7/FIX8)
Bila batas rendah/tinggi N tersedia spesifik metode yang valid belum diisi, mesin tidak lagi membiarkan status N menjadi tidak diketahui sehingga dukungan tanah selalu 0%. Sebagai gantinya, N tersedia digunakan untuk memperkirakan N-total memakai asumsi titik 2%: `N-total (mg/kg) = N tersedia / 0,02`; `N-total (%) = N tersedia (mg/kg) / 200`. Sensitivitas rasio 1–3% ditampilkan dengan rentang `N tersedia/300` hingga `N tersedia/100` dalam persen. Kelas N-total screening: sangat rendah <0,10%; rendah 0,10–0,20%; sedang >0,20–0,50%; tinggi >0,50–0,75%; sangat tinggi >0,75%. Jika pengguna memilih basis ambang N-spesifik dan batas bawah >0 serta batas atas lebih besar, kelas N tersedia dari batas yang diinput digunakan. Memasukkan dua angka tanpa memilih basis N-spesifik tidak otomatis mengaktifkan ambang tersebut, karena metode HCl/Bray/Olsen/Mehlich/NH4OAc pada form adalah metode umum N/P/K dan tidak menjamin ambang N-spesifik. Jika nilai N tersedia tidak valid/hilang, estimasi tidak dilakukan dan kontribusi tanah untuk N tidak diklaim.

Contoh: N tersedia 20 mg/kg → N-total titik = 20/200 = 0,100% (kelas rendah); rentang sensitivitas 1–3% = 0,067–0,200% (kelas potensial sangat rendah–rendah). Studi pada tanah pertanian di Jawa mendapatkan fraksi N anorganik ekstrak rata-rata 1,3% N-total, menunjukkan bahwa asumsi 2% dapat membantu screening tetapi tidak universal. Sumber: https://www.tandfonline.com/doi/full/10.1080/00380768.2014.891925. Konfirmasi dengan laboratorium N-total (misalnya pembakaran kering/Kjeldahl sesuai laboratorium) bila keputusan presisi diperlukan.


## Audit model N dan contoh hitungan (FIX8)

Konversi yang diminta benar secara aljabar jika rasio yang diasumsikan memang N tersedia/N-total = 2%:

`N-total (%) = ((100/2) × N tersedia (mg/kg)) / 10.000 = N tersedia (mg/kg) / 200`

Contoh `N tersedia = 20 mg/kg` menghasilkan `N-total estimasi = 0,100%`, dengan rentang sensitivitas asumsi 1–3% sebesar `0,067–0,200%`. Rasio 2% bukan konstanta biogeokimia universal; hasil studi tanah pertanian di Jawa yang menemukan fraksi N anorganik ekstrak rata-rata sekitar 1,3% dari N-total mendukung perlunya memberi label estimasi dan tetap memprioritaskan uji lab jika keputusan butuh presisi.

Dosis fase untuk tiap hara dihitung dengan `dosis_kotor = baseline dosis pupuk musim × fraksi fase × skala target hasil × (1 − dukungan kelas screening)`. Lalu `kredit_tersedia = pupuk hara nominal 90 hari × fraksi ketersediaan asumsi`; dosis keluaran `max(0, dosis_kotor − kredit_tersedia)`. Untuk N fraksi kredit asumsi 0,50. Jangan membagi baseline dosis pupuk dengan 0,50 sekali lagi: baseline sudah dalam kg/ha unsur pupuk, bukan uptake crop demand. Kelas N-total estimasi juga bukan pengukuran mineral N aktual; koefisien dukungan tanah tetap heuristik, harus dikalibrasi terhadap rekomendasi lokal/petunjuk lab.

Contoh untuk profil padi (baseline screening N 80 kg/ha), fase vegetatif fraksi 0,30, skala hasil 1, dukungan kelas N rendah 0,20, dan kredit hara nominal sebelumnya 10 kg N/ha: `80 × 0,30 × 1 × (1 − 0,20) = 19,2 kg N/ha`, kredit sementara `10 × 0,50 = 5`, sehingga keluaran screening fase `14,2 kg N/ha`. Ini hanya ilustrasi perilaku rumus, bukan dosis resmi padi.

Stok lapisan dihitung dari konsentrasi input dan bulk density sebagai konversi massa teoritis. Bila konsentrasi N yang dimasukkan adalah N tersedia, nilai stok itu bukan N-total terukur, bukan serapan, dan tidak dikurangkan lagi ke dosis pupuk.
