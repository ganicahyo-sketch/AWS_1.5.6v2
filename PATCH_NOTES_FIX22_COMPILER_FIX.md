# FIX22 — Perbaikan Kompilasi Kalkulator Dolomit

Perbaikan minimal berdasarkan kegagalan `:app:compileDebugJavaWithJavac` pada build FIX21:

- Menambahkan impor `org.json.JSONArray` dan `org.json.JSONObject` di `DolomiteCalculatorDialog.java`, karena kode membaca histori catatan lapangan dalam format JSON.
- Menjadikan `FieldNotesActivity.historyFileName(String)` sebagai metode `static`, karena metode ini juga dipanggil dari `latestFieldNotePhValue(...)` yang bersifat static. Fungsi pembentukan nama file tetap sama.
- Tidak mengubah rumus kalkulator dolomit, struktur data histori, UI, integrasi cuaca, atau fitur agronomi lain.

Validasi statis: deklarasi impor dan signature metode telah diperiksa terhadap sumber. Build Android penuh harus tetap dijalankan pada lingkungan yang memiliki Android SDK dan dependensi Gradle.
