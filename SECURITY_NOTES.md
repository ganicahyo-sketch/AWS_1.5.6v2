# Catatan keamanan

OpenAI API key **tidak lagi ditanam sebagai default di source code**. Aplikasi mengambil key dari Pengaturan (`ai_api_key`). Key yang sudah tersimpan di perangkat pengguna tetap digunakan.

Untuk distribusi produksi, panggilan OpenAI sebaiknya melalui backend/proxy agar secret tidak tertanam di aplikasi klien. Hindari memasukkan API key ke repository publik.

ThingSpeak Read API Key juga sebaiknya diisi melalui Pengaturan dan tidak dicantumkan kembali di source publik.


Multi-provider AI security: API keys are runtime settings only. No OpenAI, Gemini, or OpenRouter secret is embedded in source code. Gemini uses the x-goog-api-key request header; OpenRouter uses Bearer authentication. Users should rotate/revoke exposed keys.
