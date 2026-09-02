# STATUS - FileReaderApp

> Update terakhir: Mode Baca dasar sudah jalan (modal saklar, reflow gambar+teks). Beberapa jebakan baru ditemukan & diperbaiki: sortByPosition PDFBox terbukti salah untuk 2-kolom (di-revert), edge-to-edge targetSdk 35 nutupin top bar (fix statusBarsPadding), gambar dobel dengan teks (fix pakai extractMainImage, bukan render seluruh halaman).
> File ini isinya keputusan desain penting + catatan teknis biar sesi berikutnya (akun mana pun) gak mengulang diskusi/kesalahan yang sama.
> Kalau file ini diupdate: tambahkan keputusan/catatan baru di bagian yang sesuai, jangan hapus yang lama kecuali sudah tidak relevan.

## Keputusan Desain Penting
- **Arsitektur**: Jetpack Compose (native Kotlin), bukan Capacitor/hybrid - demi performa di device RAM kecil
- **Target device & minSdk**: HANYA Motorola Moto G45 (Android 14+), minSdk 34, targetSdk 35, compileSdk 35
- **xlsx**: parser & writer dibikin sendiri (baca/tulis ZIP+XML manual), BUKAN pakai Apache POI - terlalu berat untuk device RAM kecil
- **Favorit**: status favorit disimpan TERPISAH dari database file utama (SharedPreferences via FavoritesStore.kt) - kalau disatukan, akan ke-reset tiap scan ulang
- **Kategori Beranda**: PDF, Gambar, Excel, Teks/Kode (gabungan JSON/HTML/JS/TXT/CSS/dll), Direktori, Favorit
- **Penandaan favorit**: dilakukan dari DALAM viewer (tombol bintang), bukan dari list Beranda
- **Ekstraksi teks/gambar PDF**: pakai PdfBox-Android 2.0.27.0 (bukan Pdfium-Android) - kemampuan ekstraksi teks & gambar tertanamnya lebih matang, Pdfium fokus render bukan ekstraksi
- **OCR (implementasi lama, per-halaman + prefetch)**: SUDAH DIGANTIKAN oleh saklar "Mode Baca" - tombol OCR terpisah sudah dihapus dari UI, tapi OcrStore.kt masih ada dan akan dipakai lagi sebagai fallback (lihat TODO.md)

## Progres Implementasi Mode Baca
Status: fondasi dasar SUDAH JALAN dan dites di device nyata. Detail lengkap desain (Translate, TTS, kontrol tampilan) masih di bagian "Rencana Desain" di bawah - belum semua diimplementasi, lihat TODO.md untuk urutan pengerjaan berikutnya.

Yang sudah jadi:
- Tap layar membuka modal berisi saklar "Mode Baca" (menggantikan sepenuhnya panel OCR lama)
- PdfTextExtractor.kt: `extractPageText()` (teks per halaman via PDFTextStripper, default order TANPA sortByPosition) dan `extractMainImage()` (ambil gambar tertanam terbesar per halaman via PDResources/PDImageXObject)
- ReflowPage di PdfViewerScreen.kt: menampilkan gambar utama (bersih, bukan screenshot seluruh halaman) lalu teks di bawahnya, background gelap, bisa discroll
- Tombol "Uji Teks" (SEMENTARA, untuk diagnostik) - akan dihapus setelah kontrol dasar Mode Baca lengkap

Yang belum (lihat TODO.md untuk urutan): fallback OCR untuk halaman teks kosong, kontrol Ukuran Teks/Warna Latar/Kontras/Scroll-Swipe di modal, tombol expand/fullscreen per gambar dengan galeri gambar tersembunyi (kalau 1 halaman punya >1 gambar tertanam), Translate, TTS.

## Rencana Desain: Mode Baca (reflow) + Translate + TTS (detail lengkap)
**Mode Baca:**
- Saklar "Mode Baca" di modal: nyala = reflow, mati = PDF normal. User TIDAK perlu tahu istilah "OCR"
- Tiap gambar dalam Mode Baca akan punya tombol expand kecil di pojok (ikon panah menyudut keluar ala fullscreen Google Street View) - tap untuk fullscreen dengan pinch-zoom/pan; kalau halaman punya >1 gambar tertanam, sebelum di-zoom user bisa geser/slide lihat gambar lain di halaman itu dulu
- Isi modal lengkap (target akhir): saklar "Mode Baca", kontrol Kontras, pilihan Warna Latar, kontrol Ukuran Teks (-/+, permanen), pilihan navigasi Scroll/Swipe
- Indikator halaman ("N/total") tetap selalu terlihat
- Scroll vs Swipe mengatur cara pindah halaman, berlaku untuk PDF normal MAUPUN Mode Baca

**Translate (saklar tambahan di modal, aktif kalau Mode Baca nyala):**
- ML Kit Translate on-device (offline), bahasa tujuan tetap Bahasa Indonesia, teks asli DIGANTI langsung (bukan berdampingan)

**TTS (bagian dari Pengaturan):**
- TextToSpeech bawaan Android (offline), baca teks yang sedang ditampilkan
- Kontrol Putar/Jeda + kecepatan bicara di Pengaturan, highlight kata TIDAK diperlukan
- Auto-scroll per PARAGRAF (bukan per kata), posisi paragraf yang dibaca di bagian ATAS layar
- Mode Scroll: baca terus tanpa putus. Mode Swipe: baca 1 halaman lalu berhenti, TAPI kalau user geser manual ke halaman berikutnya sementara TTS masih aktif, otomatis lanjut baca

## Catatan Teknis / Jebakan yang Sudah Ditemukan
- **Ikon Compose Material**: HANYA pakai ikon di paket dasar (`material-icons-core`). JANGAN pakai `Icons.Default.Save` dll yang butuh `material-icons-extended` - bikin gagal build.
- **TopAppBar / Scaffold**: experimental di Material3, wajib `@OptIn(ExperimentalMaterial3Api::class)`.
- **Parameter wajib baru**: kalau menambah parameter wajib ke Composable, WAJIB cek & update semua tempat yang memanggilnya.
- **Pan/gesture setelah zoom**: baca state terbaru langsung dari `remember`/State, jangan lewat closure basi.
- **Proses patch kode**: perubahan kecil pakai python3 (cari old string persis, cek jumlah match sebelum percaya hasilnya). Perubahan besar/file baru pakai heredoc `cat > file << 'EOF' ... EOF` - WAJIB disalin sekaligus sebagai satu blok, jangan terpisah/dipotong.
- **Environment kerja**: semua dari HP via Termux, tanpa Android Studio. Build APK lewat GitHub Actions. WAJIB commit+push lalu cek hasil build sebelum lanjut.
- **File besar**: setelah heredoc, selalu `wc -l` dan `cat -n` untuk pastikan file tidak terpotong/rusak.
- **Komunikasi desain**: user punya kosakata teknis terbatas dalam Bahasa Indonesia - WAJIB jelaskan istilah asing dengan format "istilah (arti dalam kurung)", dan diskusikan tuntas dulu sebelum coding fitur kompleks/ambigu.
- **PDFTextStripper.sortByPosition**: JANGAN diaktifkan tanpa bukti kuat dari pengujian nyata. Sempat dicoba untuk membantu urutan baca dokumen 2-kolom, TERNYATA MALAH MENGACAK urutan baris antar kolom yang sejajar tinggi. Default order (mengikuti urutan penulisan asli di file PDF) justru sudah benar untuk kasus 2-kolom yang diuji. Sudah di-revert, pakai default order.
- **Edge-to-edge di targetSdk 35 (Android 15)**: sistem otomatis memaksa app digambar sampai ke belakang status bar, bikin top bar/tombol ketutup dan tidak bisa ditekan. Solusi: tambahkan `.statusBarsPadding()` di root Composable tiap layar (bukan cuma PdfViewerScreen - CEK LAYAR LAIN juga kalau muncul masalah serupa).
- **Gambar dobel dengan teks di Mode Baca**: kalau ambil gambar dengan cara render/screenshot SELURUH halaman PDF (`renderSinglePage`), teks yang ada di halaman itu ikut kebawa masuk ke dalam gambar - jadi teksnya keliatan dobel (sekali di dalam gambar, sekali lagi di teks hasil ekstraksi di bawahnya). Solusi: ekstrak gambar yang BENAR-BENAR TERTANAM (embedded) di dalam PDF pakai `PDResources`/`PDImageXObject` (fungsi `extractMainImage` di PdfTextExtractor.kt), bukan screenshot seluruh halaman.
- **Package PdfBox-Android**: nama package Java-nya `com.tom_roush.pdfbox.*` (pakai underscore), BUKAN `com.tom-roush.pdfbox.*` - beda dari koordinat Gradle-nya (`com.tom-roush:pdfbox-android`) yang pakai strip.
- **Panel/modal dengan konten yang bisa discroll**: gestur tap-to-dismiss (tap di luar buat nutup) sering konflik dengan gestur scroll di dalamnya. Solusi: sediakan tombol "Tutup" eksplisit, jangan cuma andalkan tap-to-dismiss untuk panel berisi teks panjang yang bisa discroll.

## Bug Diketahui (belum diperbaiki)
- (kosong saat ini)
