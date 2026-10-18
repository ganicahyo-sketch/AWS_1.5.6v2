# FIX8 — Audit nitrogen dan kalkulator dolomit pH

## Jawaban matematika: konversi N tersedia → N-total

Rumus pengguna benar secara aljabar dengan asumsi N tersedia merupakan 2% dari N-total:

`N-total (%) = ((100 / 2) × N tersedia (mg/kg)) / 10.000 = N tersedia (mg/kg) / 200`

Contoh: 20 mg/kg N tersedia menjadi 1.000 mg/kg atau 0,100% N-total. Nilai itu adalah estimasi, bukan hasil pengukuran N-total. Asumsi 2% bukan konstanta universal; kode tetap menyediakan rentang sensitivitas asumsi rasio 1–3%. Studi tanah pertanian di Jawa melaporkan fraksi N anorganik ekstrak rata-rata sekitar 1,3% N-total pada sampel yang diteliti.

## Perbaikan klasifikasi dan model kebutuhan hara

- Klasifikasi N di dashboard, Catatan Lapangan, analisis agronomi, laporan PDF, analisis rentang, dan model dosis memakai alur yang sama.
- Batas rendah/tinggi hanya digunakan bila pengguna memilih basis ambang N tersedia yang spesifik terhadap metode/laboratorium dan kedua batas valid (`low > 0`, `high > low`). Batas kosong/generik bukan nilai N tanah nol; sistem kembali ke estimasi N-total dari N tersedia.
- Jika N tersedia kosong/invalid, model tidak menghitung kredit dukungan N dari tanah; baseline dosis tetap ada dan ketidakpastian dijelaskan.
- Baseline `seasonalN/P/K` ditafsirkan sebagai dosis pupuk hara (kg/ha) per musim, bukan besaran serapan tanaman. Dosis fase = `baseline dosis × fraksi fase × skala target hasil × (1 − dukungan kelas tanah)`.
- Kredit pupuk yang tercatat dalam 90 hari dihitung dari dosis produk × persen kandungan hara; untuk screening dikreditkan menurut fraksi ketersediaan asumsi N 0,50, P₂O₅ 0,30, dan K₂O 0,60. Baseline tidak dibagi lagi dengan fraksi pemulihan karena dapat menggembungkan rekomendasi. Semua koefisien tersebut adalah asumsi screening dan perlu divalidasi dengan rekomendasi lokal.
- Tampilan stok N diubah agar tidak menyiratkan stok N-total: jika input adalah N tersedia, perhitungan massa lapisan merupakan stok teoritis berdasarkan konsentrasi input; ia tidak otomatis sama dengan serapan dan tidak digunakan sebagai kredit pupuk kedua kali.

## Fitur kalkulator dolomit

Kalkulator tersedia dari Catatan Lapangan dan Analisis Agronomi. Pengguna memasukkan pH awal, pH target, area (ha), faktor kapur ekuivalen yang dapat diubah, dan efektivitas netralisasi produk (ECCE/ENP). Rumus:

- `ΔpH = max(0, pH target − pH awal)`
- `CaCO₃-eq (t/ha) = ΔpH × faktor tekstur (t/ha per unit pH)`
- `Dolomit (t/ha) = CaCO₃-eq / (ECCE/100)`
- `Total dolomit (kg) = dolomit (t/ha) × luas (ha) × 1.000`

Preset awal yang bisa diedit: mineral ringan/berpasir 1,0; mineral sedang 2,0; mineral liat/berat 4,0; gambut 5,0 t CaCO₃-eq/ha/unit pH. Angka tersebut bukan nilai universal atau dosis final. Kalkulator memperingatkan jika target/pH-delta/dosis tinggi, menyimpan hasil, dan menampilkannya dalam analisis agronomi serta PDF.

FAO menyatakan kebutuhan kapur tidak dapat ditentukan akurat hanya dari pH karena pH tidak mewakili keseluruhan kemasaman cadangan/daya sangga tanah. Untuk keputusan akhir, utamakan kebutuhan kapur laboratorium/pH-buffer, Al-dd/H-dd, CEC/KTK, tekstur, bahan organik, kedalaman olah dan mutu dolomit. Referensi:

- FAO, *Plant Nutrition for Food Security*: https://www.fao.org/4/a0443e/a0443e03.pdf
- FAO, *Agricultural Management — Liming*: https://www.fao.org/4/x5872e/x5872e0a.htm
- Anwar et al. (2014), *Evaluation of nitrogen status of agricultural soils in Java, Indonesia*: https://www.tandfonline.com/doi/full/10.1080/00380768.2014.891925

## Uji dan batas validasi

- 14 kasus matematika N/dolomit lulus: konversi N-total %, rentang sensitivitas, kelas pada batas, fallback jika ambang kosong/0, dosis fase, kredit pupuk, dosis dolomit t/ha dan total kg, target pH ≤ pH awal, serta input invalid.
- Java murni `NitrogenInference`, `AgronomyEvidence`, `LightConversion`, dan `AgronomyEngine` berhasil dikompilasi dengan `javac`.
- Semua XML resource diparse; semua referensi `R.id` yang diperiksa memiliki deklarasi.
- Build penuh Android belum dapat dikonfirmasi di lingkungan ini karena Gradle 9.4.1 tidak dapat diunduh (`services.gradle.org` gagal resolusi DNS). Jalankan GitHub Actions setelah ZIP diunggah.
- Versi tetap 1.5.7 / versionCode 157.
