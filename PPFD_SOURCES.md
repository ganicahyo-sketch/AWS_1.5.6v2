# Dasar ilmiah PAR dan PPFD — STASIUN CUACA v1.5.6

## Definisi yang dipakai aplikasi
- **PAR (Photosynthetically Active Radiation)** adalah bagian spektrum radiasi yang secara konvensional dipakai untuk fotosintesis tanaman, **400–700 nm**. Jika dinyatakan sebagai energi, satuannya W/m².
- **PPFD (Photosynthetic Photon Flux Density)** menyatakan jumlah foton pada 400–700 nm yang mencapai luas permukaan per satuan waktu, dalam **µmol m⁻² s⁻¹**. PPFD adalah besaran foton, bukan energi.
- **Shortwave/GHI** dari Open-Meteo adalah radiasi gelombang pendek broadband, sehingga tidak identik dengan PAR.

## Estimasi broadband yang digunakan
Aplikasi menghitung:

1. `PAR energi_est ≈ 0,45 × Shortwave`
2. `PPFD_est ≈ PAR energi_est × 4,57 µmol/J`

Koefisien tersebut dipakai sebagai **pendekatan operasional**, bukan konstanta universal. Fraksi PAR terhadap shortwave dan efikasi foton bergantung pada spektrum matahari, atmosfer, awan, aerosol, dan kondisi pengukuran. Karena itu nilai estimasi tidak boleh dipresentasikan sebagai hasil pengukuran quantum sensor.

**Penting:** apabila pengguna memasukkan PPFD terukur dari quantum sensor pada Field Notes, aplikasi tidak membaliknya menjadi PAR energi. Konversi energi↔foton memerlukan asumsi spektral; menyajikan angka balik sebagai pengukuran akan memberi ketepatan semu.

## Rujukan
- McCree, K. J. (1972). *Test of current definitions of photosynthetically active radiation against leaf photosynthesis data*. Agricultural Meteorology, 10, 443–453. DOI: 10.1016/0002-1571(72)90045-3.
- Thimijan, R. W. & Heins, R. D. (1983). *Photometric, radiometric, and quantum light units of measure: A review of procedures for interconversion*. HortScience 18(6): 818–822. DOI: 10.21273/HORTSCI.18.6.818.
- Literatur dan panduan pengukuran PAR/PPFD USDA/USFS menggunakan PPFD pada rentang fotosintetik 400–700 nm.

## Implikasi analisis agronomi
PPFD satu saat hanya menggambarkan kondisi instan. Untuk evaluasi penerimaan cahaya tanaman sepanjang hari, **DLI (Daily Light Integral)** lebih informatif. Aplikasi tetap menampilkan PPFD sebagai parameter lingkungan dan tidak menggunakannya sebagai ambang universal lintas komoditas/fase.
