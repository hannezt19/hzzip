# STATUS - FileReaderApp

> Update terakhir: revisi Beranda (kartu kategori + Direktori + Favorit) selesai, sedang menunggu build fitur tombol favorit di semua viewer.
> File ini isinya keputusan desain penting + catatan teknis biar sesi berikutnya (akun mana pun) gak mengulang diskusi/kesalahan yang sama.
> Kalau file ini diupdate: tambahkan keputusan/catatan baru di bagian yang sesuai, jangan hapus yang lama kecuali sudah tidak relevan.

## Keputusan Desain Penting
- **Arsitektur**: Jetpack Compose (native Kotlin) + minSdk 28, bukan Capacitor/hybrid - demi performa di device RAM kecil (target: Motorola Moto G45)
- **xlsx**: parser & writer dibikin sendiri (baca/tulis ZIP+XML manual), BUKAN pakai Apache POI - karena POI terlalu berat untuk device RAM kecil, dan berisiko masalah build di Android (javax.xml.stream, method count)
- **Favorit**: status favorit disimpan TERPISAH dari database file utama (pakai SharedPreferences via FavoritesStore.kt), karena database file (Room) selalu di-scan-ulang & ditulis-ulang tiap Beranda dibuka - kalau favorit disimpan di tabel yang sama, akan ke-reset terus
- **Kategori Beranda**: PDF, Gambar, Excel, Teks/Kode (gabungan JSON/HTML/JS/TXT/CSS/dll), Direktori (info penyimpanan, bukan file), Favorit (file yang ditandai user) - keputusan gabung "Teks/Kode" jadi satu kategori (bukan dipecah per ekstensi) karena jumlahnya tidak beraturan dan fungsinya mirip
- **Penandaan favorit**: dilakukan dari DALAM viewer (tombol bintang), bukan dari list Beranda

## Catatan Teknis / Jebakan yang Sudah Ditemukan
- **Ikon Compose Material**: HANYA pakai ikon yang ada di paket dasar (`material-icons-core`). JANGAN pakai `Icons.Default.Save`, `Icons.Outlined.StarBorder`, atau ikon lain yang tidak familiar - ini butuh dependency tambahan `material-icons-extended` yang belum ada di project & bikin gagal build. Kalau butuh ikon "belum favorit", pakai `Icons.Filled.Star` yang sama tapi beda warna/tint, bukan icon berbeda.
- **TopAppBar / Scaffold**: API ini experimental di Material3, wajib tambahkan `@OptIn(ExperimentalMaterial3Api::class)` di atas fungsi Composable yang memakainya, atau build gagal dengan error "experimental API".
- **Parameter wajib baru**: kalau menambah parameter wajib (non-default) ke sebuah Composable (misalnya `onExit: () -> Unit`), WAJIB cek & update semua tempat yang memanggil Composable itu juga (biasanya di MainActivity.kt) - kalau lupa, build gagal dengan error "No value passed for parameter".
- **Proses patch kode**: semua edit kode dilakukan lewat script `python3` (bukan `sed`/manual edit) dengan pola cari `old` string persis, ganti ke `new`, lalu print jumlah berhasil (misal "2/2") - ini WAJIB dicek dulu sebelum commit, karena kalau hasilnya "0/1" berarti teks yang dicari tidak ketemu persis (biasanya beda whitespace/indentasi), dan file BELUM berubah.
- **Environment kerja**: tidak ada Android Studio/laptop, semua dikerjakan dari HP via Termux. Build APK selalu lewat GitHub Actions (push ke GitHub -> otomatis build -> unduh APK dari halaman Actions -> instal manual di HP). Setiap selesai patch, WAJIB commit+push lalu cek hasil build di GitHub Actions sebelum lanjut ke langkah berikutnya.
- **File besar**: kalau bikin file baru yang panjang (100+ baris) lewat heredoc (`cat > file << 'EOF'`), selalu cek `wc -l` setelahnya dan `tail -5` untuk pastikan file tidak terpotong sebelum lanjut.

## Bug Diketahui (belum diperbaiki)
- Kartu "Gambar" di Beranda menunjukkan 0 file padahal ada foto di HP - kemungkinan FileScanner belum mendeteksi ekstensi gambar dengan benar
- Zoom PDF: konten bisa melebihi batas frame saat diperbesar (beda dari Google PDF Viewer yang tetap terkurung rapi)
