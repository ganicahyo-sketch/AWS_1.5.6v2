# STASIUN CUACA v1.5.7 — Audit dan Perbaikan NPK

## Temuan
1. Klasifikasi P/K menghasilkan label dengan keterangan metode, misalnya `Sangat Rendah (P tersedia screening)`, tetapi mesin kebutuhan pupuk membandingkan label dengan string persis `Rendah`, `Sedang`, atau `Tinggi`. Akibatnya kelas P/K tidak pernah memengaruhi kebutuhan pupuk.
2. `nutrientNeed()` mengklasifikasikan N tanpa meneruskan ambang N rendah/tinggi yang telah diisi pengguna. Akibatnya input ambang N tidak berpengaruh pada penghitungan pupuk.
3. Kredit riwayat pupuk sebelumnya dikurangkan 1:1 dari kebutuhan pupuk, padahal baseline kebutuhan dibagi efisiensi pemulihan. Ini tidak konsisten dan bisa mengkreditkan pupuk terlalu besar.
4. Ringkasan stok hara menuliskan faktor `0,01`, sedangkan fungsi perhitungan menggunakan faktor `0,10`; teks penjelasan tidak konsisten.
5. Analisis rekomendasi P/K menggunakan perbandingan string persis sehingga peringatan untuk kelas tinggi dengan label metode tidak muncul.
6. Dashboard agronomi sebelumnya memakai semua riwayat pupuk untuk kredit, sementara Catatan Lapangan membatasi kredit pada 90 hari. Kredit dashboard kini dibatasi pada 90 hari, hanya untuk komoditas yang sama dan catatan berdosis kg/ha dengan tanggal valid.

## Perbaikan
- Penerapan dukungan soil-test memakai awalan kelas (Sangat Rendah, Rendah, Sedang, Tinggi, Sangat Tinggi), tidak mengabaikan keterangan metode.
- Ambang N rendah/tinggi dikirim ke perhitungan pupuk. Bila belum diisi atau tidak valid, aplikasi tidak mengarang kelas N; baseline kebutuhan komoditas/fase tetap dipakai dengan peringatan.
- Kredit pupuk historis hanya dikreditkan berdasarkan fraksi pemulihan yang sama dengan model: N 50%, P2O5 30%, K2O 60% (asumsi screening, bukan koefisien universal).
- Memperjelas satuan keluaran: N dalam kg N/ha; P dan K sebagai P2O5 dan K2O sesuai konvensi label pupuk.
- Memperbaiki faktor stok hara pada teks agar sesuai dengan rumus `mg/kg × BD × kedalaman × 0,10`.
- Untuk komoditas yang belum memiliki profil khusus, hasil ditandai sebagai baseline umum dengan ketidakpastian tinggi.
- Tidak menghapus fitur lain; perubahan difokuskan pada mesin agronomi, panggilan kalkulasi, peringatan kelas, dan penjelasan.

## Batas ilmiah
Kelas P/K tetap screening karena angka ppm tidak dapat ditafsirkan universal tanpa metode ekstraksi, satuan, jenis tanah, dan kalibrasi tanaman/lokasi. Ambang N wajib diisi dari metode/lab yang relevan. Baseline komoditas dan faktor pemulihan merupakan estimasi awal, bukan rekomendasi pupuk terkalibrasi/standar resmi untuk semua tanaman. Untuk dosis final, gunakan rekomendasi setempat, hasil laboratorium, atau persamaan STCR/PUTS/kalibrasi komoditas.

## Pemeriksaan
- `AgronomyEngine.java`, `NitrogenInference.java`, dan `AgronomyEvidence.java` dikompilasi dengan `javac`.
- Tes unit sederhana lolos: label kelas P/K, dampak ambang N, dampak kelas tanah pada kebutuhan, dan kredit pupuk sesuai efisiensi.
- Build Android penuh belum diverifikasi di lingkungan ini.
