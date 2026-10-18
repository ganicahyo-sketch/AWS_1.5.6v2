# Agronomy notes – v1.5.7 patch

- ThingSpeak is preferred, Open-Meteo is fallback in automatic input mode, and manual entry remains editable.
- EC sensor values are treated as valid project inputs. Unit conversion only converts µS/cm to dS/m; the app does not require ECe. ECe is optional laboratory reference.
- No universal EC-sensor→ECe conversion is hard-coded because published conversion factors vary with extraction ratio, texture and soil conditions.
- Bulk-density presets: mineral 1.30 g/cm³, peat 0.30 g/cm³, or custom. Presets are reference assumptions.
- History management supports delete selected or delete all for field notes, fertilizer history and OPT history.
- Long formulas/scientific caveats are moved out of the main analysis view into clickable NOTE dialogs.

References: Seo et al. (2022), Pedosphere 32(6), DOI 10.1016/j.pedsph.2022.06.023; Journal of the Saudi Society of Agricultural Sciences 23(4), DOI 10.1016/j.jssas.2023.12.005.


## Inferensi kelas nitrogen (FIX7)
- Input yang dipakai adalah N tersedia dalam mg/kg. Bila ambang bawah/atas yang secara khusus ditetapkan sesuai metode belum tersedia, aplikasi menganggap N tersedia sebagai sekitar 2% dari N-total untuk estimasi titik: `N-total (%) = N tersedia (mg/kg) / 200`.
- Sensitivitas asumsi 1–3% ditampilkan sebagai rentang: `N-total terendah (%) = N tersedia / 300` (rasio tersedia 3%) sampai `N-total tertinggi (%) = N tersedia / 100` (rasio tersedia 1%). Kelas operasional menggunakan estimasi titik 2%, sedangkan rentang kelas menggambarkan ketidakpastian.
- Kelas N-total screening: sangat rendah <0,10%; rendah 0,10–0,20%; sedang >0,20–0,50%; tinggi >0,50–0,75%; sangat tinggi >0,75%. Kriteria ini mengikuti tabel contoh penilaian sifat kimia tanah Indonesia; N-total laboratorium lazimnya ditetapkan dengan Kjeldahl.
- Ambang yang valid dan dimasukkan untuk N tersedia tetap mengambil prioritas. Batas kosong, tidak valid, terbalik, atau batas bawah 0 dianggap belum terkonfigurasi dan memicu estimasi N-total—tidak diperlakukan sebagai kadar N tanah 0%.
- Rujukan kelas: SNI 6250:2021 Lampiran A (tabel contoh kriteria penilaian hasil analisis tanah, berbasis kriteria Pusat Penelitian Tanah 1983); contoh kajian tanah Indonesia menampilkan kriteria N-total yang sama: https://www.jpplt.ubb.ac.id/index.php/zoning/en/article/download/19/6.
- Peringatan: 2% adalah asumsi screening sesuai permintaan sistem, bukan konstanta universal. Rasio N mineral/tersedia terhadap N-total berubah menurut jenis tanah, bahan organik, kelembapan, musim, sumber analisis, dan kalibrasi meter; hasil sensor tidak menggantikan laboratorium.


## FIX8 — audit model N dan fitur kalkulator dolomit
- Rumus konversi tetap setara `N-total (%) = N tersedia (mg/kg) / 200`, di bawah asumsi utama N tersedia = 2% N-total. Studi fraksi N pada tanah pertanian di Jawa menemukan fraksi anorganik ekstrak rata-rata 1,3% N-total; jadi 2% hanya asumsi screening dan bukan konstanta universal. Sumber: https://www.tandfonline.com/doi/full/10.1080/00380768.2014.891925.
- Batas kelas N rendah/tinggi hanya aktif bila pengguna secara eksplisit memilih basis ambang N spesifik dan mengisi dua batas valid. Metode HCl/Bray/Olsen/Mehlich/NH4OAc pada pemilih metode N/P/K tidak otomatis dianggap metode ambang N.
- Baseline profil N/P2O5/K2O ditafsirkan sebagai rekomendasi dosis hara pupuk (kg/ha); model tidak membagi baseline lagi dengan efisiensi pemulihan. Fraksi 0,50/0,30/0,60 digunakan hanya untuk menghitung kredit konservatif dari aplikasi pupuk yang masih baru.
- Kalkulator dolomit: `dosis (t/ha) = max(0, pH_target−pH_awal) × faktor CaCO3-eq (t/ha/pH) ÷ (ECCE/100)`; `total kg = dosis t/ha × luas ha × 1000`. Preset faktor adalah asumsi editable, bukan rekomendasi universial; utamakan kebutuhan kapur laboratorium, terutama pada tanah dengan daya sangga/kemasaman cadangan besar. Sumber: https://www.fao.org/4/a0443e/a0443e03.pdf dan https://www.fao.org/4/x5872e/x5872e0a.htm.
