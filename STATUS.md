# STATUS - FileReaderApp

> Update terakhir: Mode Baca (reflow) SELESAI total - semua kontrol (Kontras, Warna Latar, Ukuran Teks, Navigasi Scroll/Swipe, expand gambar) jalan, Panel Pengaturan sudah dirombak sesuai desain user (tanpa teks judul). Build sukses & terinstal. Berikutnya: Translate & TTS.
> File ini isinya keputusan desain penting + catatan teknis biar sesi berikutnya (akun mana pun) gak mengulang diskusi/kesalahan yang sama.
> Kalau file ini diupdate: tambahkan keputusan/catatan baru di bagian yang sesuai, jangan hapus yang lama kecuali sudah tidak relevan.

## Keputusan Desain Penting
- **Arsitektur**: Jetpack Compose (native Kotlin), bukan Capacitor/hybrid - demi performa di device RAM kecil (target: HANYA Motorola Moto G45)
- **SDK**: minSdk 34, targetSdk/compileSdk 35
- **xlsx**: parser & writer dibikin sendiri (baca/tulis ZIP+XML manual), BUKAN pakai Apache POI
- **Favorit**: status favorit disimpan TERPISAH dari database file utama (SharedPreferences via FavoritesStore.kt)
- **Kategori Beranda**: PDF, Gambar, Excel, Teks/Kode, Direktori, Favorit
- **Ekstraksi teks PDF**: pakai PdfBox-Android 2.0.27.0 (BUKAN Pdfium-Android)
- **PENTING - sortByPosition PDFTextStripper**: JANGAN diaktifkan - terbukti mengacak urutan baris di PDF 2 kolom. Default order sudah benar.
- **Edge-to-edge Android 15**: pakai `statusBarsPadding()` di Column utama layar viewer

## Klarifikasi Penting: "OCR" vs "Mode Baca"
Fitur OCR pertama (tombol manual → bottom sheet teks terpisah) BUKAN yang dimaksud user sejak awal. Yang diminta: tampilan **reflow** (gambar+teks PDF ditata ulang jadi satu alur baca rapi, dark background). Sistem sekarang otomatis pakai teks asli PDF kalau ada, fallback ke OCR kalau halaman hasil scan - user tidak perlu tahu istilah "OCR", cukup saklar "Mode Baca".

## Mode Baca (reflow) - Status Implementasi (SELESAI)
- **ReflowPage**: render gambar utama halaman (`PdfTextExtractor.extractMainImage`) + teks (`extractPageText`, fallback `OcrStore` untuk PDF hasil scan) - gambar & teks terpisah (bukan satu bitmap gabungan)
- **ReaderSettingsStore.kt**: penyimpanan permanen (SharedPreferences) untuk textSizeSp, contrast, warnaLatar (enum BacaWarnaLatar: GELAP/SEPIA/TERANG/ABU), navMode (enum NavigasiMode: SWIPE/SCROLL)
- **Kontras Gambar**: diterapkan ke gambar via `ColorFilter.colorMatrix` (ColorMatrix custom berdasarkan nilai contrast)
- **Warna Latar**: 4 pilihan, mengubah warna background & warna teks sekaligus (pasangan bg+teks per enum)
- **Navigasi Scroll/Swipe**: pengaturan GLOBAL (bukan cuma Mode Baca) - `HorizontalPager` untuk Swipe, `LazyColumn` untuk Scroll; indikator halaman ikut menyesuaikan (currentPage vs firstVisibleItemIndex)
- **Tombol expand gambar**: ikon kecil (⤢, U+2922) di pojok kanan atas gambar dalam ReflowPage → buka overlay fullscreen pakai `ZoomableImageBox` (pinch-zoom/pan yang sama dengan PDF normal) → tombol ✕ (U+2715) untuk tutup
- **Refactor**: `ZoomablePdfPage` dipecah jadi `ZoomableImageBox` (composable reusable, terima Bitmap langsung) - dipakai baik untuk PDF normal maupun overlay fullscreen di Mode Baca

## Panel Pengaturan (SettingsPanel) - Desain Final (SELESAI, sesuai sketsa user)
- **Tanpa teks judul sama sekali** - semua label dihapus (Mode Baca, Ukuran Teks, Kontras Gambar, Warna Latar, Navigasi Halaman tidak ada tulisannya lagi)
- **Baris 1**: kontrol Ukuran Teks (-/+/angka sp) sejajar kiri + saklar Mode Baca di kanan, satu Row yang sama
- **Baris 2**: Slider Kontras Gambar selebar layar
- **Baris 3 - Navigasi Halaman**: 2 kotak (56dp, rounded 12dp) berisi simbol panah teks - "↔" (U+2194) untuk Swipe, "↕" (U+2195) untuk Scroll. Kotak aktif dapat background `primaryContainer`, tidak aktif `surfaceVariant`
- **Baris 4 - Warna Latar**: 4 kotak kecil (44dp, rounded 8dp) berisi swatch warna asli (`Color(warna.bg)`). Border 1dp abu-abu transparan kalau tidak terpilih, border 3dp warna `primary` (jadi cincin) kalau terpilih
- **Alasan pakai simbol teks/unicode, bukan Material Icon**: proyek ini HANYA boleh pakai `material-icons-core` (bukan `material-icons-extended`, itu bikin gagal build - lihat Catatan Teknis). Simbol panah unicode aman & konsisten dengan gaya app (tombol -/+ dan ✕ juga sudah pakai teks, bukan Icon)

## Rencana Translate (belum dikoding)
- Saklar tambahan di Panel Pengaturan, aktif kalau Mode Baca nyala
- ML Kit Translate on-device (offline, bukan API online)
- Bahasa tujuan tetap Bahasa Indonesia (bukan pilihan bebas)
- Teks asli DIGANTI langsung jadi teks terjemahan (bukan ditampilkan berdampingan)

## Rencana TTS (belum dikoding)
- Pakai TextToSpeech bawaan Android (offline, gratis)
- Baca teks yang sedang ditampilkan: versi terjemahan kalau Translate nyala, teks asli kalau mati
- Kontrol Play/Jeda + kecepatan bicara di bagian Pengaturan (bukan di panel utama)
- Auto-lanjut halaman terikat mode navigasi: Scroll = baca lanjut terus tanpa putus; Swipe = TTS cuma baca halaman yang dibuka, tapi kalau user geser manual saat TTS aktif, otomatis lanjut baca halaman baru itu
- TIDAK perlu highlight kata/kalimat saat dibacakan
- Auto-scroll dilacak per PARAGRAF, posisi paragraf aktif diletakkan di bagian atas layar yang kelihatan

## Catatan Teknis / Jebakan yang Sudah Ditemukan
- **Ikon Compose Material**: HANYA pakai ikon dari `material-icons-core`. JANGAN pakai `Icons.Default.Save`, `Icons.Outlined.StarBorder`, dll - butuh dependency `material-icons-extended` yang tidak ada di project & bikin gagal build. Solusi yang dipakai: simbol unicode/teks biasa (-, +, ✕, ⤢, ↔, ↕) sebagai pengganti ikon
- **TopAppBar / Scaffold**: API experimental di Material3, wajib `@OptIn(ExperimentalMaterial3Api::class)`
- **Parameter wajib baru**: kalau menambah parameter wajib (non-default) ke Composable, WAJIB cek & update semua tempat yang memanggilnya
- **Proses patch kode**: semua edit lewat script `python3` heredoc (cari string persis `old`, ganti `new`), WAJIB cek jumlah "X/Y patch berhasil" sebelum commit. Kalau ada yang gagal, minta lihat isi file asli persis (sed -n) dulu sebelum retry - JANGAN menebak indentasi
- **WAJIB commit SEMUA file yang diubah**: pernah kejadian build gagal ("Unresolved reference") karena satu file (ReaderSettingsStore.kt) ketinggalan tidak ter-commit sementara file lain yang bergantung padanya (PdfViewerScreen.kt) sudah ter-push duluan. Selalu cek `git status` sebelum commit untuk pastikan tidak ada file "Changes not staged" yang tersisa
- **Environment kerja**: tidak ada Android Studio/laptop, semua dari HP via Termux. Build APK selalu lewat GitHub Actions. GitHub CLI (`gh`) sudah terpasang & login (akun hannezt19) - dipakai untuk cek log build (`gh run list`, `gh run view <id> --log-failed`) tanpa perlu scroll manual di browser HP
- **git pager**: sudah di-set `git config --global core.pager cat` supaya `git log`/`git status` tidak error "unable to execute pager"
- **File besar**: kalau bikin file baru panjang (100+ baris) lewat heredoc, selalu cek `wc -l` dan `tail -5` untuk pastikan file tidak terpotong

## Bug Diketahui
- (tidak ada bug terbuka saat ini - semua yang dilaporkan sudah diperbaiki)
