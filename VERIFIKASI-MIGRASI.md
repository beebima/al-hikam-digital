# Al-Hikam Digital — Compose V1.1.1 (lanjutan migrasi)

## Yang telah diimplementasikan pada sumber Kotlin
- Reader 127 hikmah, Arab, terjemah ID/EN, per frasa, per kata, syarah, kultum, tausiah.
- Beranda, daftar dan pencarian, favorit, bookmark, baca, pengaturan.
- Teleprompter native: play/pause, kontrol kecepatan, ukuran teks, mode cermin, kembali ke pembaca, gulir otomatis.
- Pilihan bahasa dan ukuran teks disimpan; mode gelap, favorit, bookmark, dan progres disimpan di SharedPreferences.
- Salin teks dan bagikan via Android Sharesheet.
- Satu banner native di bagian bawah, tidak dirender pada teleprompter atau jika Ads-Free aktif.
- Interstitial native: ID uji Google, jeda global minimal 180 detik sejak startup / penayangan terakhir, preload ulang setelah ditutup.
- Play Billing produk INAPP non-consumable `ads_free_lifetime`, query produk, pembelian, restore, acknowledge, entitlement dari query Google Play, iklan dihentikan ketika dimiliki.

## Pemeriksaan statis
- Konten JSON: 127 hikmah dan 127 set konten panjang.
- Manifest: AdMob app ID publisher terpasang, bukan ad-unit; ad-unit masih TEST.
- Build belum dapat dijalankan di lingkungan ini karena Android SDK/Gradle tidak tersedia. APK, tampilan nyata, iklan nyata, dan pembelian sungguhan belum terverifikasi.

## Penting sebelum rilis
1. Jalankan GitHub Actions debug build dan periksa compile errors.
2. Jalankan di perangkat untuk memastikan teleprompter, pengingat, navigasi, dan iklan.
3. Buat/aktifkan produk sekali beli `ads_free_lifetime` di Play Console, lakukan pengujian lisensi di track internal; jangan menilai produk unavailable dari sideload APK.
4. Verifikasi purchase, restore, refund/revocation dan acknowledge; untuk produksi disarankan verifikasi purchase token di backend aman.
5. Ganti ID iklan uji dengan unit produksi hanya saat siap publish dan sesuai kebijakan AdMob.
6. Fitur dan tampilan WebView lama belum diaudit tuntas satu per satu: tidak ada klaim 100% parity. Pengingat lama tetap melalui kelas Java yang dipertahankan; layar pengaturan pengingat Compose belum dibuat.
7. Penyimpanan WebView localStorage lama tidak otomatis bermigrasi ke SharedPreferences Compose.


## Hotfix kompilasi GitHub Actions (24 Oktober 2026)
- Mengoreksi callback Billing 7.1.1: parameter kedua `queryProductDetailsAsync` merupakan `List<ProductDetails>`, sehingga gunakan `products.firstOrNull()` dan bukan `products.productDetailsList.firstOrNull()`.
- Memastikan hanya `MainActivity.kt` yang mendeklarasikan `MainActivity` dalam paket utama.
- Belum dilakukan kompilasi lokal; verifikasi berikutnya melalui GitHub Actions.


## Hotfix 3 (kompilasi Java)
- `ReminderReceiver.java` kini menggunakan `ComposeMainActivity.class`, bukan `MainActivity.class`.
- Workflow memeriksa referensi Activity lama sebelum menjalankan build.
- Build APK belum diuji lokal; verifikasi akhir dilakukan oleh GitHub Actions.
