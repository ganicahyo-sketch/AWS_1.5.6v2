# FIX11 — Release Lint MissingDefaultResource

## Run yang diperiksa
GitHub Actions run `39155582051` untuk `STASIUN CUACA v1.5.7`:
https://github.com/ganicahyo-sketch/AWS_1.5.6v2/actions/runs/39155582051

## Diagnosa
- `:app:assembleDebug` berhasil.
- Build release gagal pada `:app:lintVitalRelease` karena tiga error `MissingDefaultResource`.
- `values-night/colors.xml` mendeklarasikan `root_bg`, `panel_stroke`, dan `edit_stroke`, tetapi `values/colors.xml` (resource dasar) tidak mendeklarasikannya.

## Perbaikan
Memulihkan tiga resource dasar di `app/src/main/res/values/colors.xml`, disamakan dengan nilai night yang sudah ada:
- `root_bg = #07131F`
- `panel_stroke = #214457`
- `edit_stroke = #31566A`

Ini memperbaiki fallback resource untuk konfigurasi non-night tanpa menonaktifkan lint. Warna mode malam tetap tidak berubah. Tidak mengubah ID, versi aplikasi, atau fitur lain.

## Validasi
- XML resource dasar dan night diparse.
- Nama resource tersebut kini tersedia pada base values serta values-night.
- ZIP diuji dengan `unzip -t`.
- Build penuh setelah patch belum dijalankan di lingkungan lokal; konfirmasi akhir dilakukan melalui GitHub Actions setelah patch diunggah.
