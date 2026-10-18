# FIX12 — Logo aplikasi untuk launcher HP

- Menggunakan **logo kedua** yang dipilih pengguna (awan, matahari, tetes air, termometer, Wi-Fi, bibit dan lahan; tulisan STASIUN CUACA v1.5.7).
- Mengatur `android:icon` dan `android:roundIcon` pada `AndroidManifest.xml`.
- Menambahkan adaptive icon untuk Android 8+ dan fallback PNG launcher untuk mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi.
- Menyimpan `APP_LOGO_PREVIEW.png` agar logo akhir mudah ditinjau.
- Tidak mengubah `applicationId`, `versionCode` (157), `versionName` (1.5.7), atau fungsionalitas aplikasi lain.
- Mempertahankan FIX10 (import `SharedPreferences` dan `isExcludedPalm`) serta FIX11 (fallback warna untuk Lint).

Validasi statis: semua file XML di `res` dan AndroidManifest berhasil diparse; setiap density memiliki kedua PNG launcher; referensi icon di manifest menunjuk resource yang tersedia. Build APK/release harus dikonfirmasi lagi lewat GitHub Actions.
