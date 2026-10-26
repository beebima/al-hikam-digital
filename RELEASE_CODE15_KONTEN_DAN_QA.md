# CODE 15 v.1.1.15 — perubahan terkontrol

- Kultum dan Tausiyah Bahasa Indonesia diganti **hanya isi naskahnya** dari 127 DOCX yang diterima. Judul/subjudul editorial, penulis Gus Bee Al-Imarah, S.S., IG @gusbee.alimarah dan tampilan dipertahankan.
- 3 menit refresh Billing foreground. Saat upgrade, pekerjaan WorkManager Billing lama `alhikam-background-billing-3h` dibatalkan. Tidak ada lagi Worker Billing berkala. Dependensi WorkManager tetap diperlukan untuk membatalkan job lama.
- Menu atas/bawah Beranda memakai mekanisme auto-hide global 3 detik yang juga dipakai tab lain, tanpa mengubah durasi iklan/interstitial.
- Label panel diagnostik pembelian pada Settings dilokalkan saat bahasa Inggris, termasuk sebagian besar teks diagnostik Android melalui pemetaan tampilan; nilai API mentah tidak diubah.
- Versi dinaikkan ke Code 15 v.1.1.15.

**Catatan QA**: Arsip DOCX menghasilkan 127 pasang naskah (254 naskah) dan tidak mengubah judul/byline. Isi kultum/tausiyah Inggris dari source lama tidak diganti karena DOCX berbahasa Indonesia. Pemeriksaan ini statis; build Android dan pengujian perangkat/GitHub Actions masih diperlukan.
