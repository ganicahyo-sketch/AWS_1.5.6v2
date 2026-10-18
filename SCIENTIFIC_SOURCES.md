# Pustaka ilmiah dan sumber teknis — v1.5.7

## Evapotranspirasi, air tanah, dan cuaca
- Allen, R.G. et al. (1998), FAO Irrigation and Drainage Paper 56 (FAO-56): Penman–Monteith reference ET0, crop coefficient Kc, crop ET, TAW/RAW/available soil water.
  https://www.fao.org/land-water/land/land-governance-and-planning/land-resources-planning-toolbox/detail/crop-evapotranspiration-%28crop-et0%29/en
- Open-Meteo documentation: current/daily weather, ET0 FAO, VPD, radiation, sunshine, wind, soil variables, forecast.
  https://open-meteo.com/en/docs

## pH, kebutuhan kapur, dan pembenah
- FAO, *Plant Nutrition for Food Security*: pH alone does not quantify reserve acidity; lime requirement depends on buffering/soil characteristics.
  https://www.fao.org/4/a0443e/a0443e03.pdf
- FAO, *Agricultural Management*, liming section: reports context-specific examples of lime response in tropical peat, including about 5 t/ha for an approximately one-unit pH change in particular sites, and warns that crop/peat/soil properties vary widely.
  https://www.fao.org/4/x5872e/x5872e0a.htm
- University of Minnesota Extension: lime requirement needs soil-test information beyond pH; buffer pH is used to estimate lime requirement.
  https://extension.umn.edu/ph-and-lime/lime-needs
- Al-Hamdan et al., Journal of Environmental Management 345 (2023) 118531: meta-analysis of soil amendments and crop response.
  DOI: 10.1016/j.jenvman.2023.118531

## N, P, K dan rekomendasi spesifik lokasi
- Anwar et al., *Evaluation of nitrogen status of agricultural soils in Java, Indonesia*, Soil Science and Plant Nutrition (2014): in the sampled Java agricultural soils, extractable inorganic N averaged 25.8 mg/kg or 1.3% of total N; mineralizable N averaged about 5.0%. The 2% relationship used in the app is therefore a deliberate screening assumption with uncertainty, not a universal constant.
  https://www.tandfonline.com/doi/full/10.1080/00380768.2014.891925
- Kementerian Pertanian RI, Rekomendasi Pupuk N, P, K Tanaman Pangan Edisi 2 (2024): status hara tanah dan rekomendasi spesifik lokasi.
  https://repository.pertanian.go.id/items/69eba091-7cb8-4d9b-8338-1314be912c7c
- Permentan No. 40/2007: rekomendasi P dan K padi sawah spesifik lokasi.
  https://psp.pertanian.go.id/storage/543/Permentan-No.-40-Th.-2007-ttg-Rekomendasi-Pemupukan-N-P-Dan-K-Pada-Padi-Sawah-Spesifik-Lokasi.pdf
- Mehlich-3 method-specific P/K interpretation: the app only applies a category when the selected extraction method provides a usable interpretation; it does not impose a universal category on unknown methods.

## Salinitas / EC
- USDA-NRCS soil salinity guidance: ECe classes are based on saturation-extract electrical conductivity; thresholds used in the engine are <2, 2–<4, 4–<8, 8–<16, and ≥16 dS/m.
  https://directives.nrcs.usda.gov/sites/default/files2/1712930958/20304.pdf
- FAO Water Quality / crop salt tolerance: ECe is the standard root-zone salinity reference and crop tolerance differs by crop.
  https://www.fao.org/4/Y4263E/y4263e0e.htm
- EC from a field sensor is retained as a screening/trend signal and is not automatically converted to ECe.

## VPD dan stres tanaman
- Review physiology literature: VPD around 0.5–1.5 kPa is commonly suitable for many crops, while higher VPD can increase water stress, especially when soil water is limited.
  https://pmc.ncbi.nlm.nih.gov/articles/PMC10422931/

## Prediksi OPT — screening, bukan diagnosis
- Rice blast / panicle blast forecasting: temperature, humidity, rainfall and wetness conditions are used as risk factors.
  https://apsjournals.apsnet.org/doi/10.1094/PHYTO-08-22-0311-R
- Brown planthopper and rice IPM references: weather can inform monitoring, but population and field thresholds remain necessary.
  https://ipm.ucanr.edu/
- Tomato late blight forecasting: cool/wet weather and leaf wetness are important drivers; weather alone does not diagnose disease.
  https://www.apsnet.org/edcenter/pdlessons/Pages/LateBlight.aspx
- Spodoptera frugiperda development / degree-day literature:
  https://pmc.ncbi.nlm.nih.gov/articles/PMC9782183/

## Organic production
- SNI 6729:2016 — Sistem Pertanian Organik Indonesia. Mode organik in the app is intended to respect applicable certification/input requirements; it does not simply rename synthetic inputs as organic.

## Model limitations
- Soil stock conversion used by the app: stock (kg/ha) = concentration (mg/kg) × bulk density (g/cm³) × depth (cm) × 0.10.
- Stock is not equivalent to plant uptake. Uptake depends on availability, root distribution, timing, water, crop demand, and losses.
- Fertilizer requirement is an interpretable STCR-style screening model. It is not a full QUEFTS model and not a locally calibrated STCR/PUTS equation.
- N is classified only when user supplies a valid method-specific lower/upper threshold. P/K are classified only when a method supported by the engine is selected.
- EC sensor is not treated as ECe without a validated conversion for the sensor, soil, and measurement method.
- OPT outputs are risk screening and never a definitive diagnosis.


## Pemupukan berdasarkan komoditas, produktivitas, dan status hara
- FAO, *Fertilizer use by crop in Indonesia* (2005), bab 3. Rentang dosis N, P₂O₅, K₂O dihimpun dari rekomendasi nasional lama; sumber ini sendiri memperingatkan rekomendasi tersebut sudah lama dan hanya layak sebagai pedoman umum, bukan SOP mutakhir spesifik lahan. Acuan contoh: kubis N 60–110, P₂O₅ 40–80, K₂O 5–35 kg/ha/musim; sawi N 70–120, P₂O₅ 30–50, K₂O 0–20; padi sawah N 65–95, P₂O₅ 40–50, K₂O 5–25; jagung N 65–95, P₂O₅ 30–50, K₂O 10–30; tomat N 65–110, P₂O₅ 45–75, K₂O 10–50; kedelai N 20–30, P₂O₅ 35–45, K₂O 0–20 kg/ha/musim. Nilai ini hanya untuk rentang acuan umum, bukan jaminan hasil varietas.
  https://www.fao.org/4/y7063e/y7063e07.htm
- Kementerian Pertanian / Badan Litbang Pertanian, Husnain et al. (2020), *Rekomendasi Pupuk N, P, dan K Spesifik Lokasi untuk Tanaman Padi, Jagung dan Kedelai pada Lahan Sawah (Per Kecamatan)*. Pendekatan berbasis status P/K tanah dan produktivitas setempat; membahas formulasi NPK 15-10-12 untuk efisiensi pupuk. Ini contoh mengapa dosis lokal dan status tanah lebih tepat daripada satu angka universal.
  https://repository.pertanian.go.id/items/6585310f-c22b-4574-9b6e-70898d9a9272/full
- Badan Standardisasi Instrumen Pertanian (BRMP), *Dosis Pupuk N, P, K untuk Tanaman Padi, Jagung dan Kedelai pada Lahan Sawah (Per Kecamatan)* (2023/2024). Digunakan sebagai rujukan pendekatan pemupukan berimbang spesifik lokasi pada lahan sawah.
  https://repository.pertanian.go.id/handle/123456789/25768
- Permentan No. 40/Permentan/OT.140/4/2007, *Rekomendasi Pemupukan N, P, dan K pada Padi Sawah Spesifik Lokasi*. Peraturan/acuan ini menguatkan penggunaan rekomendasi P dan K berbasis lokasi; jangan dianggap sebagai dosis universal bagi semua komoditas.
  https://psp.pertanian.go.id/storage/543/Permentan-No.-40-Th.-2007-ttg-Rekomendasi-Pemupukan-N-P-Dan-K-Pada-Padi-Sawah-Spesifik-Lokasi.pdf
- BSIP/BRMP Sulawesi Barat (2024), *PUTS: Cara Cepat untuk Menentukan Rekomendasi Pemupukan Padi Sawah*. Contoh lapangan menunjukkan hasil PUTS dan kondisi setempat dipakai untuk memilih formulasi dan dosis; contoh lokal tidak boleh digeneralisasi ke daerah lain.
  https://sulbar.brmp.pertanian.go.id/berita/puts-cara-cepat-untuk-menentukan-rekomendasi-pemupukan-padi-sawah
- VegIMPACT, modul pelatihan budidaya hortikultura. Berisi contoh strategi dan pembagian dosis untuk cabai dan bawang merah; harus disesuaikan dengan varietas, hasil uji tanah, target hasil dan sistem budidaya.
  https://repository.pertanian.go.id/server/api/core/bitstreams/429a5be4-e188-417e-8294-40a6f2287835/content
- FAO, *Plant nutrition and fertilizer use in protected crops*. Data serapan hara untuk tomat, paprika, terung, melon, mentimun, dan selada dapat membantu memahami pengambilan hara pada target hasil tertentu, tetapi angka serapan tanaman bukan dosis pupuk langsung: efisiensi pemulihan, simpanan tanah, residu, air dan kehilangan hara perlu dihitung.
  https://www.fao.org/4/S8630S/s8630s07.htm
- Informasi label untuk contoh produk pupuk di Indonesia: Urea, ZA, SP-36, KCl, dan NPK memiliki kadar hara berbeda. Konverter dalam aplikasi memakai persentase yang diinput pengguna dari label karung (bukan nama produk saja) dan menyelesaikan neraca N, P₂O₅, K₂O untuk kombinasi hingga lima produk.
  https://www.fao.org/4/y7063e/y7063e08.htm
- Contoh spesifikasi pengukur tanah USB multi-parameter. Spesifikasi, metode kalibrasi dan konversi meter komersial tidak seragam. Karena itu pembacaan N/P/K dari soil meter 8-in-1 tetap berstatus screening dan bukan ekuivalen otomatis hasil lab.
  https://www.niubol.com/id/sensor-pertanian/Portable-Type-C-USB-Soil-Multi-Parameter-Tester-15.html

- Kementerian Pertanian RI / Pertanian Press, *Rekomendasi Pupuk N, P, dan K untuk Tanaman Hortikultura per Kabupaten — Edisi 2* (November 2024). Metodenya memakai status P/K tanah, jenis tanaman dan target hasil; cakupan yang tercantum antara lain bayam, kangkung, caisim, pakcoy, kacang panjang, brokoli, kembang kol, sawi putih, wortel, cabai rawit, mentimun dan tomat. Ini rujukan prioritas jika kecamatan/kabupaten dan komoditas cocok; jangan mentransfer dosis kabupaten lain tanpa pemeriksaan.
  https://repository.pertanian.go.id/items/6940256f-5460-468f-ac86-8eebadae5a55
- Kementerian Pertanian RI / Pertanian Press, *Rekomendasi Pupuk N, P, dan K untuk Tanaman Pangan Edisi 2* (November 2024). Dosis dan formulasi NPK didasarkan pada status P/K tanah dan C-organik per wilayah; sumber menegaskan pupuk NPK saja tidak selalu memenuhi kebutuhan N, sehingga formulasi dan urea tambahan perlu disesuaikan.
  https://repository.pertanian.go.id/items/69eba091-7cb8-4d9b-8338-1314be912c7c
- Kementerian Pertanian RI / BRMP Tanah dan Pupuk, *Rekomendasi Pupuk N, P, dan K untuk Tanaman Pangan Edisi 2* (November 2024). Dosis N, P, K dan formulasi NPK dibedakan menurut status P/K, C-organik, tipe lahan dan kecamatan; NPK majemuk saja tidak selalu mencukupi kebutuhan N. Prioritaskan sumber lokal ini untuk padi/jagung/kedelai ketika lokasi dan status hara cocok.
  https://repository.pertanian.go.id/items/69eba091-7cb8-4d9b-8338-1314be912c7c
- Deskripsi produktivitas varietas terdokumentasi di publikasi varietas/percobaan hasil dapat dipakai sebagai target hasil hanya bila nama varietas, tipe hasil (gabah/pipilan/biji segar), kondisi lahan dan musim sesuai. Contoh lama uji varietas pada lahan pasang surut menyebut hasil jagung Kalingga/Arjuna 3,8–5,0 t/ha dan kedelai Wilis/Rinjani/Lompobatang sekitar 1,75–1,89 t/ha; ini bukan potensi hasil nasional modern dan tidak dipakai otomatis sebagai angka varietas dalam kode.
  https://repository.pertanian.go.id/server/api/core/bitstreams/2dd9f2ac-7673-452a-a25f-19a3c4f89fcc/content

## Implementasi model pemupukan pada aplikasi
- Mesin memakai baseline N, P₂O₅, K₂O per komoditas dan tahap pertumbuhan; bila pengguna memasukkan target produksi total (kg) dan luas lahan (m²), target dikonversi ke t/ha untuk mengubah skala kebutuhan fase. Faktor skala dibatasi agar nilai target tidak menghasilkan lonjakan dosis tanpa batas.
- Data soil meter 8-in-1 (N/P/K, pH, EC, kelembapan, suhu) hanya memengaruhi klasifikasi/dukungan screening. Karena metode ekstraksi dan kalibrasi alat mungkin tidak diketahui, konsentrasi mg/kg tidak langsung dianggap sebagai kg/ha pupuk yang harus ditebar.
- Nilai profil sayur yang tidak memiliki SOP yang langsung sebanding ditandai sebagai baseline screening berbasis kelompok tanaman, bukan hasil uji lapang atau dosis resmi. Untuk kubis dan sawi, rentang acuan umum FAO dipakai sebagai rentang pembatas; untuk sayuran lain baseline masih perlu dikalibrasi dengan SOP setempat.
- Deskripsi produktivitas varietas tidak diperlakukan sebagai hasil yang pasti. Catatan lapangan menerima nama varietas dan target produksi total (kg) sebagai masukan eksplisit. Bila target kosong, model memakai baseline hasil tingkat komoditas, bukan klaim potensi varietas bernama; AI boleh mencari deskripsi varietas terkini tetapi harus mencantumkan sumber, menyatakan ketidakpastian dan tidak boleh mengarang angka.
- Konverter pupuk menyelesaikan kebutuhan unsur hara dengan memilih kombinasi hingga lima produk yang kadar N/P₂O₅/K₂O-nya ditulis sesuai label. Jika komposisi produk tidak dapat memenuhi target secara bersamaan, layar menunjukkan kekurangan/kelebihan hara; hasil bukan instruksi aplikasi yang menggantikan label, SOP lokal atau saran penyuluh.

## Klasifikasi rentang P, K, pH dan EC

- Staf/Pusat Penelitian Tanah (1983): kriteria sifat kimia tanah mencakup lima kelas untuk N, P2O5 HCl, K2O HCl, K-total, dan beberapa sifat lain; pH H2O menggunakan kelas sangat masam, masam, agak masam, netral, agak alkalis, dan alkalis.
- Petunjuk teknis analisis tanah yang dirujuk dalam literatur Indonesia juga menampilkan lima kelas untuk salinitas/DHL: <1, 1–2, 2–3, 3–4, >4 dS/m. Dalam aplikasi, ini dipakai sebagai screening EC sensor dalam µS/cm dengan pembagian 1000 µS/cm = 1 dS/m, bukan sebagai pengganti ECe laboratorium.
- Karena CWT-7in1 menghasilkan nilai sensor yang tidak otomatis identik dengan metode ekstraksi laboratorium, klasifikasi P/K diberi label screening/metode dan tidak diperlakukan sebagai diagnosis laboratorium final.
- Referensi berbasis publikasi: Sufardi et al. (2025), *Soil Quality Index and Water Stress Vulnerability...*, Applied and Environmental Soil Science, yang merangkum kriteria Indonesia untuk Total N, P tersedia, K tertukar, dan pH.


## Analisis PPFD dan seluruh parameter
- Open-Meteo menyediakan shortwave radiation sebagai besaran radiasi gelombang pendek; aplikasi mengubahnya menjadi PAR energi lalu estimasi PPFD melalui helper `LightConversion`.
- Klasifikasi PPFD di aplikasi adalah **screening operasional**, bukan ambang universal untuk semua komoditas. Untuk keputusan fisiologis yang lebih kuat, gunakan kebutuhan foton spesifik komoditas/fase dan, bila memungkinkan, DLI serta pengukuran PPFD lapang yang terkalibrasi.
- Kelembapan tanah juga dianalisis lebih dulu. Nilai sensor dalam % diperlakukan sebagai screening; bila FC dan PWP tersedia, status air dihitung relatif terhadap batas tersebut. Nilai Open-Meteo `soil_moisture_0_to_10cm` dibaca sebagai fraksi volumetrik m³/m³ dan tidak dipaksakan ke kelas universal tanpa FC/PWP.
- Klasifikasi cuaca seperti tekanan absolut, ET0, cahaya dan angin bersifat kontekstual; aplikasi menampilkan status screening dan tetap mengutamakan tren, ketinggian, fase tanaman, dan kondisi lapangan.


## Kelas N-total tanah dan inferensi N tersedia (FIX7)
- SNI 6250:2021, Lampiran A (informatif), Tabel A.1 memuat contoh kriteria N-total: <0,1%; 0,1–0,2%; 0,21–0,5%; 0,51–0,75%; >0,75%. Standar tersebut mencatat N-total laboratorium menggunakan Kjeldahl. SNI ini khusus kualitas tanah lahan terganggu/revegetasi; tabel dipakai sebagai contoh kriteria tanah umum, bukan klaim ambang yang wajib untuk semua komoditas. Salinan yang bisa ditelusuri: https://id.scribd.com/document/746880622/SNI-6250-2021-Tata-Cara-Penentuan-Kualitas-Tanah-Untuk-Revegetasi.
- Lalenoh et al. (2023), *Analisis Kesuburan Tanah untuk Lahan Pertanian di Desa Balunijuk, Kecamatan Merawang, Kabupaten Bangka*, Zoning: Journal of Urban and Regional Planning, menyajikan kelas N-total yang sama dan menyebut metode Kjeldahl: https://www.jpplt.ubb.ac.id/index.php/zoning/en/article/download/19/6.
- Inferensi aplikasi: N-total titik (%) = N tersedia (mg/kg) / 200 berdasarkan asumsi utama N tersedia adalah 2% dari N-total. Sensitivitas asumsi 1–3% menghasilkan rentang (N tersedia/300) sampai (N tersedia/100). Ini merupakan asumsi eksplisit untuk screening, bukan hubungan empiris universal ataupun nilai N-total terukur.
