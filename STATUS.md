# STATUS - FileReaderApp

> Update terakhir: fondasi ekstraksi teks PDF untuk Mode Baca selesai. Klarifikasi penting: fitur "OCR" pertama BUKAN yang dimaksud user - yang dimaksud adalah "Mode Baca" (reflow). Desain final Mode Baca sudah disepakati lengkap, coding UI sungguhan belum dimulai.
> File ini isinya keputusan desain penting + catatan teknis biar sesi berikutnya (akun mana pun) gak mengulang diskusi/kesalahan yang sama.
> Kalau file ini diupdate: tambahkan keputusan/catatan baru di bagian yang sesuai, jangan hapus yang lama kecuali sudah tidak relevan.

## Keputusan Desain Penting
- **Arsitektur**: Jetpack Compose (native Kotlin), bukan Capacitor/hybrid - demi performa di device RAM kecil (target: HANYA Motorola Moto G45)
- **SDK**: minSdk 34, targetSdk/compileSdk 35 (dinaikkan dari minSdk 28 untuk dukung PdfBox-Android & fitur Mode Baca; device lama Android 9/10 tidak lagi jadi target)
- **xlsx**: parser & writer dibikin sendiri (baca/tulis ZIP+XML manual), BUKAN pakai Apache POI - karena POI terlalu berat & berisiko masalah build di Android
- **Favorit**: status favorit disimpan TERPISAH dari database file utama (SharedPreferences via FavoritesStore.kt), karena Room DB selalu di-scan-ulang & ditulis-ulang tiap Beranda dibuka
- **Kategori Beranda**: PDF, Gambar, Excel, Teks/Kode (gabungan JSON/HTML/JS/TXT/CSS/dll), Direktori (info penyimpanan, bukan file), Favorit
- **Penandaan favorit**: dilakukan dari DALAM viewer (tombol bintang), bukan dari list Beranda
- **Ekstraksi teks PDF**: pakai PdfBox-Android 2.0.27.0 (BUKAN Pdfium-Android) karena kemampuan ekstraksi teks lebih matang/terstruktur
- **PENTING - sortByPosition PDFTextStripper**: JANGAN diaktifkan. Sudah diuji ke PDF nyata (soal ujian 2 kolom) dan TERBUKTI MENGACAK urutan baris antar kolom. Default order PDFTextStripper sudah benar (ikut urutan penulisan asli di file PDF). Jangan aktifkan lagi kecuali ada bukti kuat baru dari pengujian nyata.
- **Edge-to-edge Android 15**: setelah targetSdk naik ke 35, top bar ketutup status bar - fix pakai `statusBarsPadding()` di Column utama layar viewer

## Klarifikasi Penting: "OCR" vs "Mode Baca"
Fitur OCR pertama (tombol manual -> bottom sheet teks terpisah per halaman) BUKAN yang dimaksud user sejak awal - istilah "OCR" sendiri user tidak paham/tidak pakai. Yang diinginkan sejak awal: tampilan **reflow** (gambar+teks PDF ditata ulang jadi satu alur baca rapi, dark background), sesuai referensi custom yang dibuat sendiri oleh user (bukan dari aplikasi tertentu) - bukan panel teks kecil terpisah dari gambar.

### Desain Final "Mode Baca" (disepakati, belum dikoding)
- Tap layar HANYA membuka modal/panel (tidak langsung ubah tampilan)
- Di dalam modal ada saklar **"Mode Baca"**: nyala = tampilan jadi reflow (gambar+teks ditata rapi), mati = balik ke PDF normal. User tidak perlu tahu istilah "OCR" - itu jadi mesin di belakang layar, sistem otomatis deteksi teks asli PDF vs OCR cadangan per halaman
- Tiap gambar dalam Mode Baca punya tombol expand kecil di pojok (ikon panah menyudut keluar ala fullscreen) - tap masuk fullscreen dengan pinch-zoom/pan (pakai ulang `ZoomablePdfPage` yang sudah ada), tombol berubah jadi ikon compress untuk kembali ke alur baca
- Kalau 1 halaman punya >1 gambar tertanam, saat expand aktif user bisa geser lihat gambar lain di halaman itu (bukan cuma gambar utama)
- Pilihan navigasi **Scroll** atau **Swipe** (di dalam modal) = pengaturan GLOBAL, berlaku untuk PDF normal MAUPUN Mode Baca - bukan cuma khusus Mode Baca
- Indikator halaman ("N/total") tetap selalu terlihat, posisi di pojok kanan atas (posisi tombol OCR lama)
- Semua mode (OCR lama, Mode Baca) boleh aktif bersamaan, tidak eksklusif

### Isi Modal/Panel Final
Saklar "Mode Baca" (on/off), kontrol Kontras, pilihan Warna Latar, kontrol Ukuran Teks (-/+, simpan permanen di Pengaturan), pilihan navigasi Scroll/Swipe

### Rencana Translate (belum dikoding)
- Saklar tambahan di panel yang sama, aktif kalau Mode Baca nyala
- ML Kit Translate on-device (offline, bukan API online)
- Bahasa tujuan tetap Bahasa Indonesia (bukan pilihan bebas)
- Teks asli DIGANTI langsung jadi teks terjemahan (bukan ditampilkan berdampingan)

### Rencana TTS (belum dikoding)
- Pakai TextToSpeech bawaan Android (offline, gratis)
- Baca teks yang sedang ditampilkan: versi terjemahan kalau Translate nyala, teks asli kalau mati
- Kontrol Play/Jeda + kecepatan bicara di bagian Pengaturan (bukan di panel utama)
- Auto-lanjut halaman terikat mode navigasi: Scroll = baca lanjut terus tanpa putus; Swipe = TTS cuma baca halaman yang dibuka, TAPI kalau user geser manual saat TTS aktif, otomatis lanjut baca halaman baru itu
- TIDAK perlu highlight kata/kalimat saat dibacakan (diputuskan tidak perlu, biar ringan)
- Auto-scroll dilacak per PARAGRAF (bukan per kata), posisi paragraf aktif diletakkan di bagian atas layar yang kelihatan

## Catatan Teknis / Jebakan yang Sudah Ditemukan
- **Ikon Compose Material**: HANYA pakai ikon dari `material-icons-core`. JANGAN pakai `Icons.Default.Save`, `Icons.Outlined.StarBorder`, dll - butuh dependency `material-icons-extended` yang tidak ada di project & bikin gagal build. Kalau butuh ikon "belum favorit", pakai `Icons.Filled.Star` sama tapi beda warna/tint.
- **TopAppBar / Scaffold**: API experimental di Material3, wajib `@OptIn(ExperimentalMaterial3Api::class)` di atas Composable yang memakainya.
- **Parameter wajib baru**: kalau menambah parameter wajib (non-default) ke Composable, WAJIB cek & update semua tempat yang memanggilnya (biasanya di MainActivity.kt).
- **Proses patch kode**: semua edit lewat script `python3` (bukan `sed`/manual edit) dengan pola cari `old` string persis, ganti ke `new`, print jumlah berhasil (misal "2/2") - WAJIB dicek sebelum commit, kalau hasilnya "0/1" berarti teks yang dicari tidak ketemu persis, file BELUM berubah.
- **Environment kerja**: tidak ada Android Studio/laptop, semua dari HP via Termux. Build APK selalu lewat GitHub Actions. Setiap selesai patch, WAJIB commit+push lalu cek hasil build di GitHub Actions sebelum lanjut.
- **File besar**: kalau bikin file baru panjang (100+ baris) lewat heredoc, selalu cek `wc -l` dan `tail -5` untuk pastikan file tidak terpotong.

## Bug Diketahui
- ~~Kartu "Gambar" 0 file~~ - SUDAH DIPERBAIKI ✅ (sekarang deteksi 22218 file)
- ~~Zoom PDF melebihi frame~~ - SUDAH DIPERBAIKI ✅
- ~~Pan tidak berfungsi setelah zoom~~ - SUDAH DIPERBAIKI ✅ (fix: baca state bitmap langsung, bukan closure basi)
