# FIX6 — perbaikan format laporan cetak dan PDF (v1.5.7)

## Perubahan utama
- Memperbaiki pemeriksaan ruang halaman PDF. Sebelumnya posisi teks dibandingkan dengan margin bawah saja; akibatnya hampir setiap elemen memicu halaman baru dan satu halaman dapat berisi hanya satu informasi. Kini batas halaman menggunakan tinggi halaman A4 dikurangi margin bawah.
- Menata laporan agar lebih menyerupai laporan agronomi: identitas laporan, judul utama, metadata kebun, ringkasan eksekutif, data cuaca, kondisi tanah, analisis agronomi, risiko OPT, rekomendasi teknis, riwayat lapangan, dan (untuk versi lengkap) catatan AI serta dasar ilmiah.
- Mengatur typeface ke Arial; judul utama 14 pt, judul bab/subbab 12 pt, isi dan tabel 11 pt. Android yang tidak menyediakan Arial dapat memakai substitusi sans-serif sistem.
- Menambahkan hierarki visual sederhana: panel judul bab, aksen warna, baris tabel selang-seling, garis pembatas, margin A4 dan footer nomor halaman.
- Paragraf panjang dilipat sesuai lebar halaman dan berlanjut ke halaman berikutnya. Tabel riwayat mengulang header ketika benar-benar berlanjut, tanpa mengulang header tabel pada bagian laporan setelah tabel selesai.
- Mencegah judul bab terpisah dari awal isinya dengan menunda gambar judul bab sampai ruang untuk konten pertama tersedia.
- Memformat judul kapital dan bernomor dalam analisis yang tersimpan sebagai subjudul 12 pt.
- Laporan ringkas membatasi riwayat menjadi 8 entri terbaru; laporan lengkap menampilkan hingga 30 entri, rekomendasi AI, serta catatan dasar ilmiah. Catatan riwayat yang terlalu panjang dipotong pada tampilan tabel agar tinggi satu baris tidak merusak layout; data sumber tidak diubah.
- Memperkuat alur penyimpanan PDF dengan memeriksa stream hasil `openOutputStream()` sebelum membungkusnya.
- Nama berkas PDF dibuat generik (`STASIUN-CUACA_LAPORAN_AGRONOMI_...`) alih-alih label lama yang memuat nama lokasi.

## Pemeriksaan
- XML sumber: 18 berkas berhasil diparse.
- Java sumber: pemeriksaan leksikal dan keseimbangan delimiter pada 12 berkas lolos.
- Pemeriksaan versi: tetap `versionName 1.5.7` dan `versionCode 157`.
- Build APK penuh belum dapat diverifikasi di lingkungan ini: Gradle 9.4.1 tidak tersedia dalam cache dan unduhan wrapper gagal karena resolver DNS tidak dapat mengakses `services.gradle.org`. Setelah ZIP diunggah, jalankan GitHub Actions untuk validasi build final.
