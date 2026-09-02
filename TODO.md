# TODO - FileReaderApp

> Update terakhir: Mode Baca dasar sudah jalan - modal saklar "Mode Baca", tampilan reflow (gambar utama tertanam dari PDF + teks di bawahnya), sudah dites di PDF teks asli, PDF scan, dan PDF 2 kolom. Berikutnya: sambungkan fallback OCR untuk halaman yang teksnya kosong.
> File ini isinya HANYA tugas yang paling dekat/mendesak (maksimal 5 poin). Rencana besar/jangka panjang ada di ROADMAP.md, bukan di sini.
> Kalau file ini diupdate: hapus poin yang sudah selesai, tulis status terbaru di baris "Update terakhir" di atas.

## Sedang Dikerjakan / Berikutnya
1. [ ] Sambungkan fallback OCR: kalau PdfTextExtractor.extractPageText kosong/blank di suatu halaman (kemungkinan hasil scan), otomatis pakai OcrStore yang sudah ada sebagai cadangan teks
2. [ ] Tambah kontrol Ukuran Teks (-/+) di modal Mode Baca, disimpan permanen (pakai ulang pola dari OCR lama)
3. [ ] Tambah kontrol Warna Latar (pilihan beberapa warna) dan Kontras di modal Mode Baca
4. [ ] Tambah pilihan navigasi Scroll/Swipe di modal, berlaku untuk PDF normal maupun Mode Baca
5. [ ] Hapus tombol "Uji Teks" sementara setelah semua kontrol dasar di atas selesai dan terbukti stabil

## Menunggu Keputusan User
- (kosongkan bagian ini kalau tidak ada yang perlu ditanyakan ke user)
