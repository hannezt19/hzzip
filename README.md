# FileReaderApp

Aplikasi Android pengganti beberapa app reader/editor file: baca & edit PDF, gambar, video, audio, xlsx, dan file teks/kode (JSON/HTML/JS/CSS/TXT/dll) dalam satu app.
Dikerjakan sepenuhnya dari HP lewat Termux (tanpa laptop/Android Studio). Build APK otomatis lewat GitHub Actions.

## Dokumentasi Proyek

Sebelum melanjutkan pengerjaan (dari sesi/akun manapun), baca dulu 4 file ini secara berurutan:

1. CONVENTIONS.md - aturan kerja tetap: peta file, alur kerja, environment, kewajiban laporan
2. STATUS.md - keputusan desain penting + catatan teknis/jebakan yang sudah ditemukan
3. ROADMAP.md - semua fitur & prioritas jangka panjang
4. TODO.md - tugas paling mendesak (maks 5 poin)

File-file ini (kecuali CONVENTIONS.md) dikelola oleh koordinator (hz11) dan jadi acuan utama tiap akun - kalau ada progress/keputusan baru, laporkan ke koordinator lewat file `LAPORAN-[akun]-[tanggal].md` (lihat CONVENTIONS.md), bukan edit langsung ke 4 file ini.

## Target Device

- Perangkat: Motorola Moto G45 (RAM kecil - semua keputusan teknis prioritaskan ringan)
- minSdk: 34 (Android 14)
- targetSdk: 35
- compileSdk: 35

## Arsitektur

- Jetpack Compose (native Kotlin) - dipilih daripada Capacitor/hybrid demi performa di device RAM kecil
- Build dan signing APK release otomatis lewat GitHub Actions (keystore signing permanen, update tanpa perlu uninstall)

## Fitur yang Sudah Selesai

- **Beranda**: scan otomatis file (incremental - hanya file berubah/baru/hilang yang disentuh, bukan hapus-insert-total), kartu kategori bulat berwarna (PDF/Gambar/Excel/Video/Audio/Teks-Kode/Favorit), kartu Direktori (info penyimpanan) & Analisis (belum berfungsi), search (hanya di Beranda)
- **Navigasi app**: drawer/hamburger (lebar 3/4 layar, buka hanya lewat tombol pojok kiri atas, gesture-buka dimatikan) berisi Beranda/Terakhir/Direktori/Favorit/Pengaturan (Pengaturan berupa accordion di dalam drawer, bukan layar terpisah)
- **Di dalam kategori**: tampilan full-screen tanpa TopAppBar/judul/tombol back - back sepenuhnya pakai tombol/gesture sistem Android. Toggle "Terbaru"/"Folder" tersedia di semua kategori KECUALI Audio (untuk kategori list seperti PDF/Excel/Teks-Kode/Favorit: mode Folder = section header per folder; untuk Video/Gambar: mode Folder = drill-down 2 tingkat kartu folder -> isi folder)
- **PDF Viewer**: swipe antar halaman ala buku, pinch-zoom (bug melebihi frame sudah fix), pan, dark theme, page grid navigasi cepat, mode baca 3-tingkat, OCR (ML Kit v2, untuk PDF hasil scan), Text-to-Speech (translate ke Bahasa Indonesia, kontrol dari notifikasi/lock screen)
- **Image Viewer & Galeri**: pinch-zoom, pan, double-tap zoom; grid galeri dikelompokkan per bulan (Paging3, untuk koleksi besar ~23rb foto) dengan toggle Terbaru/Folder
- **Video Player**: ExoPlayer/Media3 - kontrol custom minimal, gesture lengkap (tap toggle kontrol, swipe ganti video, tahan-geser speed 2x, double-tap lompat 10 detik), fullscreen ikut rotasi sistem; grid galeri Video dengan toggle Terbaru/Folder (thumbnail frame asli video)
- **Audio Player**: pemutaran background (MediaSessionService+ExoPlayer, kontrol notifikasi/lock screen), UI neumorphism dengan cover album otomatis dari metadata file
- **Excel (xlsx) Viewer**: lihat/edit sel, simpan (parser dan writer buatan sendiri, bukan Apache POI)
- **Text/Code Editor**: buka dan edit file teks/kode, konfirmasi simpan sebelum keluar (pengembangan lanjutan dihentikan, versi dasar tetap dipakai)
- **Sistem Favorit**: tombol bintang di semua viewer, tersimpan terpisah dari database scan

## Fitur dalam Rencana

Lihat ROADMAP.md untuk daftar lengkap dan status detail tiap fitur.

## Cara Kerja Pengembangan

Semua dikerjakan dari HP via Termux: edit file pakai heredoc untuk file baru atau besar, atau python3 untuk patch kecil, lalu git add, git commit, git push. Setelah push, cek hasil build di tab Actions repo GitHub, baru unduh dan instal APK manual di HP setelah build sukses.

Detail lengkap aturan kerja ada di CONVENTIONS.md.
