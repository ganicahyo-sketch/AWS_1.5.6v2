# FIX13 — Database komoditas, input manual, dan Tbase

## Perbaikan
- Memulihkan pilihan komoditas/tanaman pada Pengaturan dan Catatan Lapangan.
- Selector dibuat editable (`AutoCompleteTextView`), sehingga pengguna dapat memilih dari database atau mengetik sendiri.
- Opsi `Lainnya / isi manual` tersedia di bagian paling bawah. Memilihnya mengosongkan field agar pengguna langsung dapat mengetik nama komoditas.
- Database terpusat pada `CropDatabase.java` agar pilihan UI tidak lagi terpisah dari profil `AgronomyEngine`.
- Kelapa sawit tidak dimasukkan ke katalog STASIUN CUACA v1.5.7 karena sudah dipisahkan dari aplikasi ini.
- Tbase/GDD ditampilkan pada analisis agronomi dan basis sumbernya dijelaskan.

## Tbase database (°C)
Padi 10; Jagung 10; Kedelai 10; Cabai 10; Tomat 7; Singkong 10; Ubi jalar 12; Kentang 2; Bawang merah 4,5; Kacang tanah 10; Kacang hijau 8; Kacang panjang 10; Kubis 4,5; Pakcoy 4,5; Sawi 5; Selada 4; Brokoli 4,5; Kembang kol 4,5; Kangkung 10 (proxy warm-season); Bayam 7 (proxy amaranth; spinach berbeda); Wortel 6; Mentimun 10; Terong 10; Melon 10; Semangka 10; Kale/kailan 4,5 (proxy Brassica/cabbage).

Tbase adalah parameter screening untuk GDD, bukan nilai universal varietas. Nilai dapat berbeda menurut kultivar, fase, metode penentuan, dan lingkungan.

## Referensi utama
- Pereira et al./FAO56rev 2025: tabel indikatif Tbase/Tupper untuk banyak sayuran dan tanaman lapang.
- Paredes et al. 2025, Agricultural Water Management: review 2025 atas studi threshold temperatur.
- MRCC/Purdue: tabel Tbase GDD untuk banyak sayuran dan tanaman lapang.
- WSU PNW Gardener's Handbook: Tbase GDD sayuran.
- Studi mustard: Tbase 5°C.
- Studi mungbean: Tbase genotype-specific sekitar 8,6–9,8°C; aplikasi memakai 8°C sebagai nilai model indikatif.

## Validasi
- Semua XML resource berhasil diparse.
- Referensi `fnCrop`/`crop` konsisten dengan `AutoCompleteTextView`.
- Katalog tidak berisi kelapa sawit.
- Kompilasi Gradle penuh belum dapat dijalankan di lingkungan ini karena `services.gradle.org` tidak dapat diakses; GitHub Actions perlu digunakan untuk build final.
