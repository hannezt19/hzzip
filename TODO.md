# TODO - FileReaderApp

> Update terakhir: dokumentasi (README/STATUS/ROADMAP) sudah sinkron dengan semua keputusan Mode Baca/Translate/TTS. Mulai kerjakan Mode Baca dari fondasi: ekstraksi teks PDF pakai PdfBox-Android.
> File ini isinya HANYA tugas yang paling dekat/mendesak (maksimal 5 poin). Rencana besar/jangka panjang ada di ROADMAP.md, bukan di sini.
> Kalau file ini diupdate: hapus poin yang sudah selesai, tulis status terbaru di baris "Update terakhir" di atas.

## Sedang Dikerjakan / Berikutnya
1. [ ] Tambahkan dependency PdfBox-Android ke build.gradle, pastikan build tetap sukses (uji integrasi paling dasar dulu, belum dipakai di kode)
2. [ ] Bikin fungsi terpisah untuk ekstraksi teks per halaman PDF pakai PdfBox-Android (fungsi utilitas saja, belum nyambung ke tampilan)
3. [ ] Uji fungsi ekstraksi di beberapa PDF berbeda (dokumen teks biasa vs hasil scan) - pastikan urutan baca & pemisahan paragraf masuk akal
4. [ ] Rancang UI dasar Mode Baca: modal dengan saklar "Mode Baca" + tampilan reflow sederhana (gambar+teks ditata rapi), belum termasuk Kontras/Warna Latar/Ukuran Teks/Scroll-Swipe
5. [ ] Sambungkan logic: kalau ekstraksi teks kosong/gagal (halaman ternyata hasil scan), otomatis pakai OCR yang sudah ada sebagai cadangan

## Menunggu Keputusan User
- (kosongkan bagian ini kalau tidak ada yang perlu ditanyakan ke user)
