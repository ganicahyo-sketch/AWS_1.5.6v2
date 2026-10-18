# Profil komoditas — STASIUN CUACA v1.5.7

Dokumen ini mencerminkan profil screening yang dipakai `AgronomyEngine.java`. Nilai N / P₂O₅ / K₂O adalah baseline unsur hara musim (kg/ha) untuk pembagian fase di dalam model, bukan dosis satu kali aplikasi, bukan rekomendasi resmi untuk semua lokasi, dan bukan jaminan potensi hasil varietas. Nilai referensi produktivitas dipakai hanya bila target produksi petak tidak diisi; input target kg + luas m² akan menggantikannya dalam penskalaan model.

## Baseline model (bukan dosis final)

| Komoditas | Siklus acuan (hari) | pH screening | Suhu acuan (°C) | N / P₂O₅ / K₂O (kg/ha/musim) | Hasil referensi model (t/ha) |
|---|---:|---:|---:|---:|---:|
| Padi | 115 | 5,0–7,0 | 20–32 | 80 / 45 / 20 | 6 |
| Jagung | 110 | 5,5–7,5 | 18–33 | 80 / 40 / 20 | 7 |
| Kedelai | 95 | 5,5–7,0 | 20–30 | 25 / 40 / 15 | 2,5 |
| Cabai | 130 | 5,5–7,0 | 20–32 | 90 / 55 / 35 | 8 |
| Tomat | 110 | 5,5–7,0 | 18–30 | 90 / 60 / 40 | 40 |
| Singkong | 300 | 4,5–7,0 | 20–32 | 70 / 35 / 35 | 25 |
| Ubi jalar | 120 | 5,5–6,8 | 20–30 | 60 / 30 / 30 | 15 |
| Kentang | 100 | 5,0–6,5 | 15–25 | 105 / 70 / 35 | 20 |
| Bawang merah | 90 | 5,5–6,8 | 15–30 | 100 / 55 / 35 | 12 |
| Kacang tanah | 100 | 5,0–7,0 | 20–32 | 25 / 40 / 10 | 2 |
| Kacang hijau | 65 | 5,0–7,0 | 22–32 | 25 / 35 / 8 | 1,5 |
| Kacang panjang | 65 | 5,5–7,0 | 20–32 | 50 / 50 / 20 | 12 |
| Kubis | 100 | 5,5–6,8 | 15–25 | 90 / 60 / 20 | 35 |
| Pakcoy | 45 | 5,5–6,8 | 18–28 | 90 / 40 / 80 | 25 |
| Sawi | 45 | 5,5–7,0 | 18–30 | 95 / 40 / 10 | 20 |
| Selada | 55 | 5,5–6,8 | 12–24 | 100 / 60 / 90 | 25 |
| Brokoli | 85 | 5,5–7,0 | 15–24 | 110 / 70 / 80 | 20 |
| Kembang kol | 85 | 5,5–7,0 | 15–24 | 110 / 70 / 80 | 20 |
| Kangkung | 35 | 5,0–7,0 | 20–32 | 80 / 35 / 30 | 15 |
| Bayam | 35 | 5,5–7,0 | 20–32 | 80 / 35 / 30 | 15 |
| Wortel | 100 | 5,5–7,0 | 15–25 | 85 / 50 / 90 | 30 |
| Mentimun | 70 | 5,5–7,0 | 20–30 | 100 / 55 / 100 | 35 |
| Terong | 120 | 5,5–7,0 | 22–32 | 100 / 70 / 100 | 30 |
| Melon | 90 | 5,5–7,0 | 22–32 | 100 / 70 / 120 | 30 |
| Semangka | 90 | 5,5–7,0 | 22–32 | 100 / 60 / 100 | 30 |
| Kale / kailan | 65 | 5,5–7,0 | 15–26 | 95 / 55 / 60 | 20 |

## Cara pakai dengan varietas dan soil meter 8-in-1

1. Isi komoditas dan nama varietas/galur/klon. Angka hasil pada deskripsi benih harus diperiksa jenis hasilnya (misalnya gabah kering giling, jagung pipilan kering, buah segar, atau hasil jual), kondisi uji, musim, dan kepadatan tanaman sebelum dibandingkan.
2. Isi target produksi total petak dalam kg dan luas dalam m² jika ingin target kebun memengaruhi penskalaan kebutuhan hara. Bila target kosong, aplikasi memakai angka referensi komoditas pada tabel di atas; angka itu bukan klaim hasil varietas bernama.
3. N/P/K, pH, EC, suhu dan kelembapan dari soil meter USB 8-in-1 merupakan petunjuk screening sampai model alat, register, skala dan metode kalibrasinya terverifikasi. Jangan menyamakan otomatis pembacaan itu dengan uji laboratorium atau mengubah konsentrasi mg/kg menjadi dosis pupuk secara langsung.
4. Jika hasil analisis tanah, metode ekstraksi serta wilayah tersedia, utamakan rujukan rekomendasi spesifik lokasi. Untuk pangan, lihat rekomendasi Kementerian Pertanian/BRMP Edisi 2 tahun 2024. Untuk sayuran, edisi hortikultura per kabupaten tahun 2024 mencakup tanaman seperti bayam, kangkung, caisim, pakcoy, kacang panjang, brokoli, kembang kol, sawi putih, wortel, cabai rawit, mentimun dan tomat.
5. Jika target hara dari model telah dihitung, konverter pupuk dapat mengubahnya menjadi kombinasi produk yang tersedia berdasarkan persen N, P₂O₅ dan K₂O pada label. Konversi produk tidak menggantikan rekomendasi agronomi, label, uji tanah, maupun SOP setempat.

## Sumber rujukan dan batasan

- Kementerian Pertanian RI / Pertanian Press (2024), *Rekomendasi Pupuk N, P, dan K untuk Tanaman Hortikultura per Kabupaten — Edisi 2*. Berbasis komoditas, status P/K lahan dan target hasil per wilayah: https://repository.pertanian.go.id/items/6940256f-5460-468f-ac86-8eebadae5a55
- Kementerian Pertanian RI / Pertanian Press (2024), *Rekomendasi Pupuk N, P, dan K untuk Tanaman Pangan Edisi 2*: https://repository.pertanian.go.id/items/69eba091-7cb8-4d9b-8338-1314be912c7c
- FAO (2005), *Fertilizer Use by Crop in Indonesia*, rentang historis beberapa tanaman pangan/hortikultura; dokumen secara eksplisit memperingatkan bahwa rekomendasi nasional yang dikutip sudah tua dan hanya merupakan pedoman umum: https://www.fao.org/4/y7063e/y7063e07.htm
- FAO-56 (Allen et al., 1998) untuk ET₀, kebutuhan air dan koefisien tanaman; bukan sumber dosis pupuk: https://www.fao.org/land-water/land/land-governance-and-planning/land-resources-planning-toolbox/detail/crop-evapotranspiration-%28crop-et0%29/en
- Daftar lebih luas mengenai klasifikasi hara, pembenah tanah, EC, cahaya, SOP dan sumber teknis tersedia di `SCIENTIFIC_SOURCES.md`.

Data produktivitas per kabupaten atau deskripsi varietas perlu dicocokkan dengan lokasi, varietas, bentuk produk yang dipanen dan cara budidaya. Bila belum cocok, hasil di aplikasi tetap disebut estimasi screening dengan ketidakpastian, bukan dosis resmi atau prediksi hasil terjamin.
