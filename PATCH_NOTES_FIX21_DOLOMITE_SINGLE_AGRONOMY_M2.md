# FIX21 — Kalkulator Dolomit Terintegrasi Analisis Agronomi

Perubahan:
- Hanya ada satu akses Kalkulator Dolomit: **Analisis Agronomi**.
- Tombol Kalkulator Dolomit di **Catatan Lapangan** dihapus, tanpa menghapus field input pH, data tanah, atau histori catatan lapangan.
- pH kalkulator memakai pH live (`soil_ph`) bila tersedia. Jika kosong, aplikasi mengambil pH valid terbaru dari histori Catatan Lapangan berdasarkan timestamp `created`.
- Luas lahan pada kalkulator diubah menjadi **m²**. Nilai hektare tetap dipertahankan sebagai kompatibilitas internal, tetapi tidak menjadi satuan input/tampilan luas kalkulator.
- Total dolomit dihitung dari luas m² yang dikonversi ke hektare secara internal agar rumus dosis kg/ha tetap konsisten.
- Analisis Agronomi dan PDF laporan menampilkan luas kalkulator dalam m².
- Fitur lain dipertahankan.

Catatan: faktor kebutuhan CaCO3-ekuivalen tetap dinyatakan t/ha/unit pH karena itu adalah basis model perhitungan; yang diubah adalah satuan **luas lahan yang dimasukkan pengguna**.
