# Catatan keamanan

Versi ZIP ini memasukkan default Read API Key ThingSpeak yang diminta untuk memudahkan koneksi channel. Read key tetap dapat diganti pada Pengaturan.

OpenAI API key default dimasukkan sesuai permintaan pengguna dan field-nya menggunakan tipe password agar tersembunyi di UI. **Jangan mengunggah kembali source ZIP ini ke repository publik tanpa menghapus/merotasi key tersebut.** Untuk distribusi produksi, panggilan OpenAI sebaiknya melalui backend/proxy agar secret tidak tertanam di aplikasi klien.
