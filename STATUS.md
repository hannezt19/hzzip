# STATUS - FileReaderApp

> Update terakhir: keputusan target device diubah jadi minSdk 34 (Moto G45 saja); rancangan besar Mode Baca + Translate + TTS sudah disepakati detailnya (masih tahap diskusi, belum mulai coding).
> File ini isinya keputusan desain penting + catatan teknis biar sesi berikutnya (akun mana pun) gak mengulang diskusi/kesalahan yang sama.
> Kalau file ini diupdate: tambahkan keputusan/catatan baru di bagian yang sesuai, jangan hapus yang lama kecuali sudah tidak relevan.

## Keputusan Desain Penting
- **Arsitektur**: Jetpack Compose (native Kotlin), bukan Capacitor/hybrid - demi performa di device RAM kecil
- **Target device & minSdk**: diubah jadi HANYA Motorola Moto G45 (Android 14+), minSdk dinaikkan dari 28 ke 34, targetSdk 35 - tidak perlu lagi dukung device lama (Android 9/10), supaya bisa pakai API/fitur Android yang lebih baru tanpa version-check tambahan
- **xlsx**: parser & writer dibikin sendiri (baca/tulis ZIP+XML manual), BUKAN pakai Apache POI - karena POI terlalu berat untuk device RAM kecil, dan berisiko masalah build di Android (javax.xml.stream, method count)
- **Favorit**: status favorit disimpan TERPISAH dari database file utama (pakai SharedPreferences via FavoritesStore.kt), karena database file (Room) selalu di-scan-ulang & ditulis-ulang tiap Beranda dibuka - kalau favorit disimpan di tabel yang sama, akan ke-reset terus
- **Kategori Beranda**: PDF, Gambar, Excel, Teks/Kode (gabungan JSON/HTML/JS/TXT/CSS/dll), Direktori (info penyimpanan, bukan file), Favorit (file yang ditandai user) - keputusan gabung "Teks/Kode" jadi satu kategori (bukan dipecah per ekstensi) karena jumlahnya tidak beraturan dan fungsinya mirip
- **Penandaan favorit**: dilakukan dari DALAM viewer (tombol bintang), bukan dari list Beranda
- **OCR (implementasi lama, sudah beres)**: cache disimpan PER-HALAMAN (bukan satu file gabungan), diproses progresif dengan buffer 10 halaman di depan posisi baca. Tombol OCR ini AKAN DIGANTIKAN oleh saklar "Mode Baca" di rencana baru (lihat bagian di bawah)

## Rencana Desain: Mode Baca (reflow) + Translate + TTS
Status: masih tahap DISKUSI, belum mulai coding (sengaja didiskusikan tuntas dulu karena sempat ada pergeseran pemahaman soal istilah teknis - lihat catatan komunikasi di bawah).

**Mode Baca (menggantikan rencana "panel OCR" sebelumnya):**
- Tap layar HANYA membuka modal/panel pengaturan (bukan langsung ubah tampilan)
- Di dalam modal ada saklar "Mode Baca": nyala = tampilan berubah jadi reflow (gambar+teks ditata ulang rapi, dark background), mati = balik ke PDF normal
- User TIDAK perlu tahu istilah "OCR" - sistem otomatis pilih sumber teks (teks asli PDF, atau OCR sebagai cadangan kalau halamannya hasil scan/gambar) di belakang layar
- Tiap gambar dalam Mode Baca punya tombol expand kecil di pojok (ikon panah menyudut keluar, ala tombol fullscreen Google Street View) - tap untuk fullscreen dengan pinch-zoom/pan (pakai ulang logic ZoomablePdfPage), tombol berubah jadi ikon compress untuk kembali
- Isi modal lengkap: saklar "Mode Baca", kontrol Kontras, pilihan Warna Latar, kontrol Ukuran Teks (-/+, permanen), pilihan navigasi Scroll/Swipe
- Indikator halaman ("N/total") tetap selalu terlihat, posisinya dipindah ke tempat tombol OCR yang sekarang
- Pilihan navigasi Scroll vs Swipe mengatur cara pindah halaman 1 ke berikutnya, berlaku untuk PDF normal MAUPUN Mode Baca (bukan cuma khusus Mode Baca)

**Translate (saklar tambahan di modal, aktif kalau Mode Baca nyala):**
- Pakai ML Kit Translate on-device (offline, sekeluarga dengan ML Kit OCR yang sudah dipakai) - BUKAN API online
- Bahasa tujuan tetap Bahasa Indonesia (bukan pilihan bebas)
- Teks asli DIGANTI langsung jadi teks terjemahan (bukan ditampilkan berdampingan)

**TTS (bagian dari Pengaturan, bukan modal utama):**
- Pakai TextToSpeech bawaan Android (offline, gratis)
- Membacakan teks yang SEDANG ditampilkan (kalau Terjemahkan nyala, baca versi Indonesia; kalau mati, baca teks asli)
- Kontrol Putar/Jeda + kecepatan bicara ada di Pengaturan
- Highlight kata/kalimat saat dibaca: TIDAK diperlukan (diputuskan biar ringan)
- Auto-scroll: layar ikut gulir mengikuti bacaan, dilacak PER PARAGRAF (bukan per kata, biar ringan) - begitu 1 paragraf selesai dibacakan, layar gulir sedikit; posisi paragraf yang dibaca diletakkan di bagian ATAS layar yang kelihatan
- Perilaku lanjut halaman terikat ke Scroll/Swipe: mode Scroll bisa baca terus tanpa putus; mode Swipe, TTS baca 1 halaman lalu berhenti - TAPI kalau user geser manual ke halaman berikutnya sementara TTS masih aktif, otomatis lanjut baca (tidak perlu tap Play lagi)

## Catatan Teknis / Jebakan yang Sudah Ditemukan
- **Ikon Compose Material**: HANYA pakai ikon yang ada di paket dasar (`material-icons-core`). JANGAN pakai `Icons.Default.Save`, `Icons.Outlined.StarBorder`, atau ikon lain yang tidak familiar - ini butuh dependency tambahan `material-icons-extended` yang belum ada di project & bikin gagal build. Kalau butuh ikon "belum favorit", pakai `Icons.Filled.Star` yang sama tapi beda warna/tint, bukan icon berbeda.
- **TopAppBar / Scaffold**: API ini experimental di Material3, wajib tambahkan `@OptIn(ExperimentalMaterial3Api::class)` di atas fungsi Composable yang memakainya, atau build gagal dengan error "experimental API".
- **Parameter wajib baru**: kalau menambah parameter wajib (non-default) ke sebuah Composable (misalnya `onExit: () -> Unit`), WAJIB cek & update semua tempat yang memanggil Composable itu juga (biasanya di MainActivity.kt) - kalau lupa, build gagal dengan error "No value passed for parameter".
- **Pan/gesture setelah zoom**: kalau logic pan/geser membaca posisi/ukuran bitmap dari closure yang dibuat sekali di awal (misal saat composable pertama render), nilainya bisa "basi" begitu state berubah (misal setelah zoom). Solusi: baca state terbaru langsung (misal dari `remember`/State yang di-observe), bukan lewat variabel closure lama.
- **Proses patch kode**: untuk perubahan kecil, pakai script `python3` dengan pola cari `old` string persis, ganti ke `new`, lalu print jumlah berhasil (cek dulu sebelum commit, kalau "0/1" berarti teks tidak ketemu persis, file belum berubah). Untuk perubahan besar/file baru, pakai heredoc `cat > file << 'EOF' ... EOF` (WAJIB seluruh isi termasuk baris `cat` dan `EOF` disalin sekaligus sebagai satu blok - kalau ditempel sebagian/terpisah, bash akan coba menjalankan isi teks sebagai perintah dan error "syntax error near unexpected token").
- **Environment kerja**: tidak ada Android Studio/laptop, semua dikerjakan dari HP via Termux. Build APK selalu lewat GitHub Actions (push ke GitHub -> otomatis build -> unduh APK dari halaman Actions -> instal manual di HP). Setiap selesai patch, WAJIB commit+push lalu cek hasil build di GitHub Actions sebelum lanjut ke langkah berikutnya.
- **File besar**: kalau bikin file baru yang panjang (100+ baris) lewat heredoc, selalu cek `wc -l` setelahnya dan `cat -n` untuk pastikan file tidak terpotong/rusak sebelum lanjut.
- **Komunikasi desain**: user (pemilik proyek) punya kosakata teknis terbatas dalam Bahasa Indonesia - WAJIB jelaskan istilah asing/teknis dengan format "istilah (arti dalam kurung)" saat mendiskusikan desain, dan diskusikan dulu sampai tuntas sebelum mulai coding untuk fitur yang kompleks/ambigu (lihat kasus OCR yang sempat salah paham arah lalu direvisi total jadi rencana "Mode Baca").

## Bug Diketahui (belum diperbaiki)
- (kosong saat ini)
