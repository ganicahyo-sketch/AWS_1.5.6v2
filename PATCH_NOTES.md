# STASIUN CUACA • GANI CAHYO-UNS v1.5.6 — Open-Meteo + Agronomi

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
