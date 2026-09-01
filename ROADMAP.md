# Roadmap - FileReaderApp

> Update terakhir: setelah menambahkan tombol favorit di semua viewer (PDF/Gambar/Excel/Teks-Kode), sebelum bug "Gambar 0 file" & dark theme dikerjakan.
> Kalau file ini diupdate, tulis di baris "Update terakhir" di atas: apa yang baru selesai, biar sesi berikutnya (akun mana pun) langsung tahu titik pijaknya tanpa scroll riwayat chat.

## Tujuan Proyek
App Android pengganti beberapa app reader/editor: baca & edit banyak jenis file (PDF, xlsx, gambar, JSON/HTML/JS/TXT/CSS), dengan tampilan konsisten di berbagai perangkat & versi Android (target minSdk 28). Device utama: Motorola Moto G45 (RAM kecil, jadi semua keputusan teknis prioritaskan ringan).

## Tahap Selesai
- **Tahap 0**: Keystore signing permanen (APK release, update tanpa uninstall) ✅
- **Tahap 1**: Izin All Files Access, scan otomatis, Room DB, Beranda dasar (search/filter/list), bottom navbar (Beranda/Terakhir/Pengaturan) ✅
- **Tahap 2**: PDF Reader - swipe ala buku, pinch-zoom, back button ke Beranda, indikator halaman (pojok kanan atas) ✅
- **Tahap 3**: Image Viewer (pinch-zoom, pan, double-tap) ✅ | xlsx versi dasar (lihat/edit sel/simpan, formula ditampilkan sebagai teks belum dihitung) ✅ | pptx belum dikerjakan
- **Redesain Beranda** (Material You -> revisi ala "One Read"): kartu kategori warna beda per jenis, ikon kiri, layout rapat, kartu Direktori (info penyimpanan), kartu Favorit ✅
- **Sistem Favorit**: FavoritesStore.kt (SharedPreferences, terpisah dari DB file biar gak kereset scan ulang) + tombol bintang di semua viewer (PDF/Gambar/Excel/Teks-Kode) ✅ (baru saja di-commit, menunggu konfirmasi build sukses)

## Belum Dikerjakan (dari dokumen "Rencana Pengembangan Ebook Reader")
Urutan prioritas usulan dokumen: OCR (fondasi) -> fix bug zoom PDF -> kenyamanan baca dasar -> UI/UX -> fitur lanjutan.

1. Bug: kartu "Gambar" di Beranda menunjukkan 0 file (FileScanner belum deteksi gambar dengan benar) - DILEWATI SEMENTARA
2. Dark theme toggle - DITUNDA
3. OCR untuk PDF hasil scan (fondasi untuk reflow/translate/TTS)
4. Fix bug zoom PDF - konten melebihi frame saat diperbesar
5. Mode E-ink (background putih pucat/sepia, font serif, kontras tinggi, transisi halaman instan)
6. Panel pengaturan baca (kecerahan in-app, mode halaman vertikal/horizontal/ganda, tema warna latar)
7. Mode reflow teks (font besar + auto word-wrap, butuh PDF dibaca sebagai text layer)
8. Pengaturan jarak baris & margin, pilihan jenis font
9. Layar tetap nyala saat baca, kunci orientasi layar
10. Animasi ganti halaman ala membuka lembaran kertas
11. Translate teks + koreksi tata bahasa
12. Text-to-speech
13. Pencarian dalam dokumen (highlight + indikator hasil ala Dropbox)
14. Highlight & catatan pribadi
15. Lanjut baca cepat ke buku/file terakhir dibuka
16. pptx (PowerPoint) viewer
17. Formula aktif di xlsx (Tahap C, ditunda karena jarang dipakai)

## Referensi Desain
- App "One Read": text selection/highlight/copy, konversi file, kategori file, menu file lengkap (cetak, ganti nama, kompresi, gabung/pisah PDF, dll)
- Google PDF Viewer: jadi acuan perilaku zoom yang benar (konten terkurung rapi, beda dari bug yang kita punya sekarang)
