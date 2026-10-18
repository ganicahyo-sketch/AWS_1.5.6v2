# Release Lint Fix — STASIUN CUACA v1.5.7

## Masalah yang ditemukan
Run GitHub Actions #57 berhasil menyelesaikan `:app:assembleDebug`, tetapi `:app:lintVitalRelease` gagal karena `root_bg`, `panel_stroke`, dan `edit_stroke` hanya tersedia pada `values-night/colors.xml` di repositori dan tidak memiliki deklarasi fallback pada folder dasar `values`.

## Perbaikan minimal
Menambahkan deklarasi default untuk ketiga warna tersebut ke `app/src/main/res/values/colors.xml`, dengan nilai yang sama dengan palet aplikasi yang ada:
- `root_bg`: `#07131F`
- `panel_stroke`: `#214457`
- `edit_stroke`: `#31566A`

Perbaikan ini tidak menonaktifkan lint dan tidak mengubah nilai versi malam. Kode aplikasi, USB Soil Meter 8-in-1, perbaikan Java, GPS, NPK, serta pilihan provider AI tidak diubah oleh patch ini.

## Verifikasi
- Struktur ZIP diperiksa dengan `unzip -t`.
- Deklarasi XML diverifikasi secara statis.
- Build Android penuh belum dijalankan secara lokal; build final tetap perlu diverifikasi di GitHub Actions.
