# Al-Hikam Digital — Baseline Teknis dan Catatan Trial & Error

Diperbarui: 24 Oktober 2026. Dokumen kerja; keputusan baru harus dicatat di sini agar tidak mengulang bug lama.

## Baseline sumber (Step 13, belum terverifikasi lulus GitHub/Play saat dokumen dibuat)

- Project: Kitab Al-Hikam Digital — Sejuta Santri.
- `applicationId` / namespace: `id.sejutasantri.alhikam` — jangan diubah.
- `versionName`: `1.1.1`; `versionCode`: `4`. **Naikkan versionCode pada unggahan AAB baru bila Play Console menolak versi yang sama/sudah pernah dipakai.**
- `minSdk`: **24** (Play Automatic Protection menolak 23 pada percobaan ini).
- `targetSdk`: **36** (Play Console menolak API 35 pada Oktober 2026).
- `compileSdk`: **36**.
- Android Gradle Plugin: **8.11.1**; Gradle: **8.13**; JDK: **17**.
- UI: Android WebView + HTML/CSS/JavaScript; **bukan** migrasi Kotlin atau Native Scroll Reader.
- Ads: **2 banner native tetap** (di atas header dan di bawah menu bawah) + **1 interstitial** yang hanya bisa muncul pada transisi yang sesuai, dengan jeda global sekurang-kurangnya 180 detik. Semua banner di dalam isi artikel, beranda, daftar isi, favorit, Kultum dan Tausiah dihapus.
- Iklan harus memakai **test ad unit** pada tahap QA. Saat produksi, cek consent dan kebijakan Google Mobile Ads.
- Billing Library: `com.android.billingclient:billing:9.1.0`; produk non-consumable: `remove_ads_lifetime`. **Belum terbukti sukses transaksi**. Perlu produk aktif, jalur pengujian Play, dan pengujian pembelian/pemulihan; backend verification disarankan untuk rilis produksi.
- Distribusi: GitHub Actions menghasilkan APK debug dan AAB signed untuk Play Internal Testing. **Keystore upload tidak boleh masuk repository**; simpan backup rahasia dan GitHub Secrets.

## Bug/regresi dan solusi

| Temuan | Pencegahan / Perbaikan |
|---|---|
| WebView+native inline banner tertinggal saat scrolling | Jangan aktifkan lagi overlay native yang mengikuti posisi slot konten. Dua banner tetap saja. |
| Native Scroll Reader lag dan menghilangkan fungsi teleprompter | Jangan gunakan eksperimen multi-WebView/native reader. Pertahankan WebView stabil. |
| Auto-hide menu bawah tiba-tiba hilang | Pertahankan transisi CSS translateY/opacity; audit aturan `transition:none`, `transform:none`, dan prioritas `!important`. |
| Kesalahan Java: `refreshPurchases()` dipanggil tanpa argumen | Sesuaikan dengan signature `refreshPurchases(boolean)`; build compile Java harus hijau. |
| Tombol Billing tanpa umpan balik | Tampilkan hasil operasi yang bermakna; bedakan *produk belum aktif*, *tidak ada pembelian*, dan *koneksi gagal*. |
| Belum membuat aplikasi di Play Console, pembelian belum bisa diverifikasi | Buat aplikasi dengan package sama, daftar internal testers, rilis internal signed AAB, serta produk sekali beli. |
| AAB ditolak karena min SDK 23 | Kunci baseline `minSdk 24` pada seluruh build. |
| AAB ditolak karena target API 35 | Kunci baseline `targetSdk 36` dan `compileSdk 36` pada seluruh build. |
| AAB rilis kosong | Download artifact AAB dari GitHub **Actions**, bukan ZIP source yang diunggah; unggah file `.aab` ke rilis. |
| Play mengeluarkan peringatan Advertising ID | Audit deklarasi iklan dan `com.google.android.gms.permission.AD_ID` bila relevan dengan SDK yang dipakai. |
| Play memperingatkan deobfuscation file | Tidak menghalangi build jika obfuscation/R8 dimatikan; audit lagi sebelum produksi. |

## Fitur/keputusan produk yang harus dipertahankan

- 127 Hikmah, teks Arab, terjemahan Indonesia/Inggris, terjemahan per kata dan per frasa, syarah, Kultum/Tausiah, teleprompter, pencarian, Favorit, catatan, bookmark, dan statistik belajar.
- Tema Ivory Manuscript dan header ringkas: ikon auto-hide serta tema monokrom; bendera bahasa tetap berwarna.
- Beranda tanpa header pemandangan/masjid/tanggal, quote di kartu sederhana, serta daftar fitur dua kolom.
- Label "Terjemah Indonesia", bukan "Terjemah Utuh".
- Ukuran font awal sistem dan konten 14; jangan timpakan pengaturan tersimpan pengguna.
- Hak Ads-Free sah harus menghilangkan banner atas/bawah, interstitial, ruang kosong, dan kartu promosi; haknya harus bisa dipulihkan.

## Gate sebelum setiap ZIP / rilis

1. Pastikan memulai dari ZIP/source **terbaru yang lulus uji**, bukan versi lama.
2. Bandingkan seluruh butir baseline di atas dengan `app/build.gradle`, workflow, manifest, dan CSS.
3. Pastikan `applicationId` tidak berubah dan upload key tetap konsisten.
4. Periksa sintaks HTML/JS dan kompilasi Java dengan GitHub Actions; ZIP valid bukan berarti APK/AAB pasti lolos.
5. Jalankan QA fisik untuk scrolling, auto-hide, navigasi, teleprompter, dua banner, interstitial, bookmark dan semua label.
6. Untuk Billing, test menggunakan Play Internal Testing/akun tester dan produk aktif; jangan menyatakan transaksi sukses berdasarkan build hijau saja.
7. Untuk upload AAB berikutnya, pastikan `versionCode` sesuai ketentuan Play Console dan ditingkatkan setelah kode itu sudah dipakai untuk rilis.
8. Jika ada error, tambah entri diagnosis dan solusi di dokumen ini.

## Status saat dicatat

- Step 13 API 36 sedang dibangun/diuji di GitHub menurut pengguna; belum ada konfirmasi sukses Google Play Console.
- Step 12 AAB dengan min SDK 24 diterima sebagai paket, tetapi ditolak saat pemeriksaan karena target API 35.
- Produk Billing dan pengujian transaksi **belum terselesaikan**.
