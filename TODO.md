# TODO - FileReaderApp

> Dikelola oleh hz11 (koordinator). Update terakhir: 8 Sept 2026 - cross-check ke kondisi main terbaru via Termux (git log/git show), bukan cuma laporan yang sudah basi.
> File ini isinya tugas AKTIF tiap akun + keputusan yang masih menunggu user. Rencana besar ada di ROADMAP.md.
> PENTING: semua tugas dikerjakan LANGSUNG di branch main (bukan branch terpisah per akun lagi) - lihat CONVENTIONS.md.

## Pembagian Kerja Aktif

### hz19 - Kategori Gambar
- [PROSES] Investigasi bug label bulan hilang mode Terbaru galeri Gambar. 2 dugaan TERBUKTI SALAH: (1) lastModified vs EXIF, (2) contentType/daur-ulang slot Compose - JANGAN ulangi, perlu sudut analisis baru
- Sudah SETUJU pola integrasi FileActionSheet dari hz25 (onLongClick+combinedClickable di ImageThumbnail) - tidak ada tindakan lanjutan dari hz19, tinggal tunggu patch dari hz25

### hz21 - PDF (SettingsPanel + zoom mode Scroll)
- [PROSES] Fix zoom PDF mode Scroll - 5 tahap (zoom seragam, fix tarik pinch, satukan pan, fix numpuk 2 halaman, satukan gesture detector) - tahap 5 baru push ke branch hz21, MENUNGGU build+tes di HP
- [PROSES] Verifikasi visual final SettingsPanel (Warna Latar/Kontras/Mode Baca/TTS/ID) - kode sudah dicek benar, tinggal konfirmasi visual di HP

### hz25 - Video / Toggle Terbaru-Folder / FileActionSheet
- [SELESAI] Tugas 2: sistem FileActionSheet (clipboard salin/potong/hapus/ganti nama, properti file) - 5/5 langkah tuntas
- [BELUM] Eksekusi integrasi FileActionSheet ke `ImageThumbnail` (Gambar) - hz19 SUDAH SETUJU pola yang diusulkan (parameter `onLongClick`, `combinedClickable`), tinggal hz25 kirim & jalankan patch-nya

### ydiv2 - PDF TTS / Audio Player
- [PROSES] Checkpoint A - Tahap C Lirik: PENDEKATAN BERUBAH LAGI - bukan `.lrc` privat (rencana sebelumnya), BALIK ke tag ID3 USLT tertanam di file, deteksi otomatis pola `[mm:ss.ss]` di dalam teksnya (ala Musicolet). `Id3UsltReader.kt` digeneralisasi + tambah `readTitle()` (frame TIT2, judul asli lagu). `LyricsStore.kt` dirombak total.
- [BELUM] Checkpoint B: UI PlaylistPage (thumbnail cover album + menu titik-tiga: Info lagu/Hapus/Tambah/Bagikan), pakai `readTitle()` sebagai judul utama
- [BELUM] Tugas kecil: hapus tombol ☰ dari `PlayerControlBar` (redundant, navigasi sudah lewat swipe 3 halaman)

## Menunggu Keputusan User
- (kosong saat ini)

## Belum Ada yang Pegang
- Fitur Analisis (Semua Partisi/File Besar/Berkas Terbaru/Folder Kosong/File Redundan/File Duplikat/Keranjang Sampah) - baru tampilan kosong tanpa fungsi
- Penomoran versi app - khusus dipegang hz11/user sendiri

## Update (hz11)
- [SELESAI] Bug build-breaking clickable di AudioPlayerScreen.kt - fix oleh ydiv2+yhs13, build hijau.
