# STATUS - FileReaderApp

> Update terakhir (8 Sept, oleh hz11/koordinator): ditulis ulang total, konsolidasi keputusan desain & temuan teknis dari beberapa minggu terakhir. Kalau file ini diupdate: tambahkan di bagian yang sesuai, jangan hapus yang lama kecuali sudah terbukti tidak relevan lagi.
> File ini isinya keputusan desain penting + catatan teknis/jebakan, BUKAN alur kerja umum (itu di CONVENTIONS.md) dan BUKAN daftar tugas (itu di TODO.md/ROADMAP.md).

## Keputusan Desain Penting

- **Arsitektur**: Jetpack Compose (native Kotlin) + minSdk 34 - demi performa di device RAM kecil (Motorola Moto G45)
- **xlsx**: parser & writer dibikin sendiri (ZIP+XML manual), BUKAN Apache POI - terlalu berat untuk device RAM kecil & berisiko masalah build Android
- **Favorit**: status favorit disimpan TERPISAH dari database file utama (SharedPreferences via FavoritesStore.kt) - karena Room di-scan-ulang, kalau digabung akan ke-reset
- **Kategori Beranda**: PDF, Gambar, Excel, Video, Audio, Teks/Kode (gabungan JSON/HTML/JS/TXT/CSS/dll - sengaja tidak dipecah per ekstensi), Direktori, Favorit
- **Scan Beranda incremental**: `FileDao.syncAll()` diff berdasarkan path (insert/update yang berubah, delete yang hilang, DIAMKAN yang sama persis) - menggantikan `replaceAll()` (hapus-total-insert-ulang) yang tadinya bikin SEMUA observer Flow Room ter-notifikasi "data berubah" tiap scan, walau isinya sama - efeknya nge-reset kedip UI di Beranda
- **Navigasi app**: drawer/hamburger 3/4 layar (bukan bottom nav bar) - dibuka HANYA lewat tombol ikon (gesture swipe-from-edge dimatikan via `gesturesEnabled = drawerState.isOpen`, supaya tidak bentrok gestur lain seperti swipe player). Pengaturan jadi accordion expand-in-place di drawer, bukan layar/tab terpisah
- **Di dalam kategori**: TIDAK ada TopAppBar/judul/tombol back - full-screen, back sepenuhnya pakai sistem Android. Ikon buka-drawer diberi reserved space (bukan overlay bebas) supaya tidak menumpuk konten
- **State navigasi yang harus "diingat"** (misal folder yang lagi dibuka di galeri Video/Gambar) WAJIB disimpan di ViewModel, BUKAN `remember` lokal composable - composable grid/list bisa dilepas total (disposed) saat pindah ke viewer file, dan `remember` lokal ikut ter-reset. Pola: StateFlow di HomeViewModel + fungsi setter
- **Thumbnail video**: pakai `coil-video` (VideoFrameDecoder) untuk ambil frame asli - versi HARUS sama dengan `coil-compose` yang sudah ada (2.6.0)
- **Toggle Terbaru/Folder**: berlaku semua kategori kecuali Audio (konsepnya beda, playlist-based)
- **OCR PDF**: ML Kit Text Recognition v2 (on-device), diproses sekali di background, deteksi otomatis kebutuhan OCR (cek text layer dulu), hasil di mode terpisah "Lihat sebagai teks"

## Catatan Teknis / Jebakan yang Sudah Ditemukan

- **Ikon Compose Material**: dependency `material-icons-extended` SUDAH ditambahkan ke project (untuk kebutuhan CategoryCircleCard & drawer) - jadi ikon di luar `material-icons-core` sekarang BOLEH dipakai, tidak lagi terlarang seperti catatan lama. Tetap perhatikan ukuran APK kalau pakai banyak ikon jarang dipakai.
- **API eksperimental Material3**: `TopAppBar`, `Scaffold` dengan sebagian parameter, `ModalNavigationDrawer`, `ModalDrawerSheet` - semua butuh `@OptIn(ExperimentalMaterial3Api::class)` di atas fungsi Composable pemakainya, atau build gagal
- **Parameter wajib baru** pada Composable (misal `onExit: () -> Unit`) WAJIB dicek ulang semua tempat pemanggilnya (biasanya MainActivity.kt) - lupa update = build gagal "No value passed for parameter"
- **Properti Kotlin `private`**: fungsi/nilai `private` di satu file (misal `FileRow`/`formatSize` di HomeScreen.kt) TIDAK bisa dipinjam file lain - kalau butuh dipakai bareng, buat versi non-private terpisah atau pindah ke file util bersama
- **File besar via heredoc**: kalau bikin file baru panjang (100+ baris), selalu cek `wc -l` dan `tail -5` setelahnya - pastikan tidak terpotong
- **Proses patch**: python3 heredoc dengan pola cari `old_str` persis -> ganti `new_str` -> print status berhasil/gagal - WAJIB dicek sebelum commit (detail alur di CONVENTIONS.md)

## Bug Diketahui

- **PDF page-turn**: sudah jauh lebih nyaman, tapi masih ada ruang perbaikan/polish kecil (belum dianggap tuntas 100%)
- **Zoom PDF melebihi frame**: SUDAH FIX
- **Label bulan hilang di mode Terbaru galeri Gambar**: header pemisah bulan cuma tampil untuk bulan berjalan, bulan lain fotonya tetap ada tapi headernya hilang - kepemilikan dipegang hz19, dugaan terbaru mengarah ke logika insertSeparators/Paging3, masih investigasi
- **hamburger Beranda tidak sejajar 1 baris dengan search bar**: SUDAH DIPUTUSKAN user - kondisi sekarang (reserved space baris terpisah) sudah cukup, TIDAK perlu diubah

## Keputusan Desain Tambahan (8 Sept)

- **Sistem koordinasi proyek**: hz11 sebagai koordinator/router murni, 4 akun eksekutor kerja paralel-bergantian. Alur: user eksekusi langsung dengan eksekutor -> user informasikan hasil ke hz11 -> hz11 update file acuan. Detail lengkap di CONVENTIONS.md & TODO.md.
- **Kartu Favorit di grid Beranda**: DIKONFIRMASI FINAL akan dihapus (dobel dengan menu Favorit di drawer) - tugas dilimpahkan ke hz25
