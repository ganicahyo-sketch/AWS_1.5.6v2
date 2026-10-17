# STASIUN CUACA v1.5.7 — Audit dan perbaikan terisolasi

Perbaikan pada basis `AWS-1.5.7-NPK-INDICATOR-FERTILIZER-CALC-FIX.zip`.

## Audit temuan
- USB Soil Meter: referensi sumber USB menggunakan `SOIL_SOURCE[1]` (CWT-7in1 USB); tidak ditemukan `SOIL_SOURCE[3]`.
- Opsi AI: pengaturan OpenAI, Google Gemini, dan OpenRouter tersedia. Toggle Google Search memakai web_search OpenAI, google_search Gemini, dan suffix `:online` OpenRouter. OpenAlex tetap digunakan.
- Simpan Catatan Lapangan: sebelumnya penyimpanan dan pembaruan tampilan/analisis berada dalam satu blok try; exception dari refresh/analisis bisa menampilkan pesan gagal walau data sudah tersimpan. Kini dipisahkan. Jika histori JSON rusak, teks lama dicadangkan ke `<key>_corrupt_backup` lalu catatan baru dapat disimpan.
- GPS: MainActivity sudah meminta fix lokasi baru, tetapi SettingsActivity tidak menyegarkan field koordinat ketika kembali; menyimpan Pengaturan dapat menimpa koordinat baru dengan nilai lama. Kini latitude/longitude disegarkan dari preferences setelah kembali dari aksi GPS.

## Batas verifikasi
ZIP diperiksa integritasnya. Build Android penuh dan uji perangkat langsung belum dilakukan di lingkungan ini. Jalankan GitHub Actions dan uji USB, simpan Catatan Lapangan, masing-masing penyedia AI, serta GPS pada perangkat nyata.
