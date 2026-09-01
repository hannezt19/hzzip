# TODO - FileReaderApp

> Update terakhir: favorit, bug "Gambar 0 file", dark theme, dan OCR PDF semua sudah beres. Prioritas berikutnya: fix bug zoom PDF, lalu mulai fitur thumbnail nyata di Beranda (Gambar & PDF, pakai Coil).
> File ini isinya HANYA tugas yang paling dekat/mendesak (maksimal 5 poin). Rencana besar/jangka panjang ada di ROADMAP.md, bukan di sini.
> Kalau file ini diupdate: hapus poin yang sudah selesai, tulis status terbaru di baris "Update terakhir" di atas.

## Sedang Dikerjakan / Berikutnya
1. [ ] Fix bug zoom PDF (konten melebihi frame saat diperbesar) di PdfViewerScreen.kt
2. [ ] Tambah dependency Coil ke build.gradle - commit+push, pastikan build GitHub Actions sukses DULU sebelum nulis kode lain yang pakai Coil
3. [ ] Thumbnail Gambar di Beranda (pakai Coil, load gambar asli diperkecil otomatis)
4. [ ] Thumbnail PDF di Beranda (render halaman pertama pakai PdfRenderer, simpan hasil ke cache)
5. [ ] Placeholder + loading background untuk thumbnail (list tampil dulu, thumbnail nyusul, biar Beranda gak lag)

## Menunggu Keputusan User
- (kosong)
