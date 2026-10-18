# FIX10 — Perbaikan build GitHub Actions 39154602530

Repositori: `ganicahyo-sketch/AWS_1.5.6v2`  
Versi aplikasi dipertahankan: STASIUN CUACA v1.5.7 (`versionCode 157`).

## Akar kegagalan

Run `https://github.com/ganicahyo-sketch/AWS_1.5.6v2/actions/runs/39154602530` gagal pada `:app:compileDebugJavaWithJavac` dengan tepat tiga error:

1. `SettingsActivity.java:276`: `package SharedPreferences does not exist` karena import `android.content.SharedPreferences` hilang, padahal tipe `SharedPreferences.Editor` digunakan untuk menyimpan lokasi GPS.
2. `FieldNotesActivity.java:559`: method `isExcludedPalm(String)` dipanggil tetapi belum didefinisikan.
3. `FieldNotesActivity.java:1592`: pemanggilan method yang sama menyebabkan error kedua.

## Perbaikan

- Menambahkan `import android.content.SharedPreferences;` pada `SettingsActivity.java`.
- Menambahkan `isExcludedPalm(String)` di `FieldNotesActivity.java`. Helper menormalkan huruf, spasi non-breaking, dan tanda baca; mengenali `sawit`, `kelapa sawit`, `oil palm`, serta `Elaeis guineensis`. Ini memenuhi validasi yang memang sudah dipakai pada penyimpanan catatan dan saat analisis. Pesan validasi menegaskan bahwa rekomendasi sawit dikhususkan pada SIM-TANI, bukan modul komoditas umum STASIUN CUACA v1.5.7.
- Tidak mengubah model perhitungan pupuk, ekspor CSV, data, atau ID versi aplikasi.

## Validasi dan batas

- Kedua deklarasi/pemanggilan diperiksa statis.
- XML dan referensi resource tidak diubah dalam FIX10.
- Build Gradle lokal dicoba, tetapi wrapper tidak dapat mengunduh Gradle 9.4.1 karena DNS ke `services.gradle.org` gagal. Oleh sebab itu, build Android lengkap harus dikonfirmasi dengan GitHub Actions setelah arsip ini diunggah.
- Upaya pembaruan sumber secara langsung melalui konektor GitHub ditolak dengan HTTP 403 (`Resource not accessible by integration`); perubahan disertakan dalam ZIP siap unggah, bukan diklaim telah diterapkan ke `main`.
