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
- **Zoom PDF mode Scroll**: SUDAH DIPERBAIKI TOTAL (11 Sept, [yhs13]) - lihat "Keputusan Desain Tambahan (11 Sept)" di bawah untuk detail arsitektur baru. Verified di HP.

## Keputusan Desain Tambahan (8 Sept)

- **Sistem koordinasi proyek**: hz11 sebagai koordinator/router murni, 4 akun eksekutor kerja paralel-bergantian. Alur: user eksekusi langsung dengan eksekutor -> user informasikan hasil ke hz11 -> hz11 update file acuan. Detail lengkap di CONVENTIONS.md & TODO.md.
- **Kartu Favorit di grid Beranda**: DIKONFIRMASI FINAL SUDAH DIHAPUS (dobel dengan menu Favorit di drawer) - tugas dilimpahkan ke hz25
- **Navigasi Audio Player**: 3 halaman (Playlist|Pemutar|Lirik) via HorizontalPager swipe kiri-kanan, BUKAN bottom sheet dari bawah seperti rencana awal - alasan: bottom sheet kurang nyaman diakses (uji-coba langsung user), konsisten dengan pola HorizontalPager yang sudah dipakai PDF/galeri Gambar, dan hindari konflik gestur dengan drawer app (gestur tepi sudah dimatikan)
- **Lirik Audio**: sumber dari tag ID3 USLT tertanam (bukan file .lrc terpisah) - deteksi otomatis pola timestamp dalam teks lirik ala Musicolet

## Update (hz11)
- Bug build-breaking `Unresolved reference: clickable` di AudioPlayerScreen.kt SUDAH FIX oleh ydiv2 (dibantu akun baru yhs13). Build sudah hijau.
- Akun **yhs13** kini membantu ydiv2 untuk bagian Audio.


## Keputusan Desain Tambahan (11 Sept, [yhs13])

- **Zoom PDF mode Scroll pakai Telephoto**: setelah 8 percobaan gesture custom shared-state (commit d776407 s/d ec2aa53, semua oleh hz21) gagal stabil (bug tumpuk/scroll macet berulang), pendekatan diganti total: pakai library **Telephoto** (`me.saket.telephoto:zoomable:0.11.2`, dependency sudah ada di `build.gradle.kts`). `LazyColumn` mode Scroll dibungkus `Modifier.zoomable(rememberZoomableState(), onClick = {...})` - zoom & tap jadi satu di level yang membungkus SEMUA halaman, bukan per-halaman lagi.
- **JANGAN kembalikan ke gesture custom manual untuk zoom mode Scroll** - sudah terbukti gagal 8x. Kalau ada bug baru di zoom mode Scroll, perbaiki di sekitar `Modifier.zoomable()`/Telephoto API, JANGAN bikin ulang `awaitEachGesture`/`detectTransformGestures` custom dari nol.
- **Mode Swipe (HorizontalPager) TIDAK ikut diubah** - tetap pakai gesture custom lama (`ZoomableImageBox` dengan `ownGestures = true`, default), karena di mode Swipe cuma 1 halaman terlihat sekaligus jadi tidak butuh zoom menyatu, dan sudah stabil dari awal.
- **Parameter `ownGestures: Boolean`** ditambahkan ke `ZoomableImageBox`/`ZoomablePdfPage`/`ScrollPdfPage` - `true` (default) = pakai gesture custom lama (dipakai mode Swipe), `false` = tanpa gesture sendiri karena zoom+tap sudah ditangani Telephoto di level `LazyColumn` (dipakai mode Scroll).
- **Known issue kecil (belum diperbaiki, prioritas rendah)**: sedikit "menyendat" saat zoom di beberapa file berukuran kecil - dugaan awal: render Bitmap halaman baru (`PdfRenderSessionCache.getOrCreate`/`renderPage`) kebetulan terjadi bersamaan dengan animasi zoom. User anggap sudah cukup baik, belum perlu dioptimasi sekarang.
