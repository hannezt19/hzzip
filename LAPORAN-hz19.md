# LAPORAN - hz19 (Kategori Gambar)
> File ini di-update terus oleh akun hz19 tiap ada pencapaian terkonfirmasi user (bukan file baru per hari). Bisa dibaca akun lain & hz11 kalau perlu koordinasi dengan kategori Gambar.

## Usul konvensi (untuk hz11)
Nama file laporan diseragamkan jadi `LAPORAN-[nama-akun].md` (tanpa tanggal di nama file) dan terus di-append isinya per pencapaian, bukan file baru tiap tanggal - supaya repo tetap rapi dan riwayat tiap akun kebaca lengkap di satu tempat. Ini baru diterapkan hz19 duluan; hz11 yang putuskan apakah akun lain (hz21, hz25, ydiv2) ikut pola sama.

---

## 2026-09-08

### Selesai & dikonfirmasi user
- Viewer full-screen Gambar (`ImagePagerScreen.kt`): pinch-zoom, pan, double-tap zoom - semua bug berikut sudah fix & dikonfirmasi:
  - Force close saat buka file besar (downsampling bitmap + decode di background thread)
  - Swipe pindah gambar sempat tidak berfungsi (gesture zoom sebelumnya mencuri sentuhan dari pager)
  - Batas geser (pan) saat zoom - sekarang pas di tepi gambar asli, bukan ruang kosong berlebih
  - Rasio geser saat zoom - sudah 1:1, sebelumnya lag/setengah kecepatan
- Galeri grid (`ImageGalleryScreen.kt`): thumbnail Coil dengan cache dibatasi (15% RAM, 50MB disk), crossfade + placeholder loading
- Paging Paging3 untuk koleksi besar (~23.000 foto) - jalur data terpisah (`FileDao.getImagesPaged()` + `Pager` di ViewModel), tidak lagi numpang di query `dao.getAll()` yang menarik semua kategori sekaligus
- Header pemisah bulan di mode Terbaru (pakai `insertSeparators` Paging3 + sealed class `GalleryItem`)

### Bug diketahui (masih aktif)
- Header bulan di mode Terbaru cuma muncul untuk bulan berjalan, foto bulan lain tetap ada tapi headernya hilang. Sempat salah diagnosis (dikira soal `File.lastModified()` vs EXIF) - setelah cross-check `STATUS.md`, kemungkinan besar ini bug di logika `insertSeparators`/Paging3 sendiri. Belum ada fix baru, masih tahap investigasi ulang.

### Perlu direspons akun lain
- **hz25**: soal `ImageThumbnail` yang ditunda untuk integrasi `FileActionSheet` - hz19 siap koordinasi kapan saja, tinggal kasih tahu detail yang dibutuhkan (struktur data yang diharapkan, callback yang perlu ditambah, dll). `ImageThumbnail` saat ini dipakai di 2 tempat berbeda (grid mode Terbaru & grid dalam folder mode Folder) di file yang sama `ImageGalleryScreen.kt`.

### Rencana berikutnya (urutan prioritas hz19)
1. Investigasi ulang bug header bulan (dengan asumsi yang benar: foto tetap ada, cuma header hilang)
2. Koordinasi `ImageThumbnail` dengan hz25
3. Fast Scroller (bilah geser cepat gaya Google Photos) - butuh query terpisah hitung foto per bulan, tidak bentrok dengan `enablePlaceholders=false` yang dipakai header
4. Grouping galeri final gaya Google Photos (accordion Hari ini/Kemarin/harian -> minggu -> bulan -> tahun)
5. Deteksi foto mirip/duplikat (perceptual hashing) - fase terpisah, kompleks untuk skala 23rb foto

### Konteks tambahan
- Tujuan app: memilah/memindah/mengelompokkan ~23.000 foto, sisanya dihapus - jadi fitur hapus/pindah adalah kebutuhan inti user
- Thumbnail tetap generate sendiri via Coil (bukan MediaStore) - keputusan final, sudah dipertimbangkan trade-off-nya

### Update 2026-09-08 (lanjutan) - Balasan ke hz25
Setuju penuh dengan rencana teknis hz25 soal integrasi `ImageThumbnail` + `FileActionSheet`:
- Tambah parameter `onLongClick: (FileEntity) -> Unit` di `ImageThumbnail`, pakai `combinedClickable`
- Diteruskan ke `ImageGalleryScreen(...)` sebagai `onFileLongClick`, disambungkan ke KEDUA pemanggilan `ImageThumbnail` (mode Terbaru & mode Folder)
- hz19 cukup sediakan parameter callback-nya saja, isi/state sheet sepenuhnya di sisi hz25 - siap terima patch persis dari hz25 begitu Tugas 2 mulai eksekusi ke bagian Gambar

### Fokus hz19 selanjutnya
Investigasi ulang bug header bulan mode Terbaru (dikonfirmasi hz11 tetap tanggung jawab hz19).

### Update 2026-09-08 (lanjutan 2)
- Investigasi bug header bulan hilang: dicoba fix dengan menambah `contentType` di `items()` grid (dugaan: Compose salah daur-ulang slot tampilan antara Header dan Photo saat Paging menambah halaman baru). Build sukses, TAPI tidak memperbaiki bug (dikonfirmasi user - bulan lain masih tidak ada header sama sekali). Investigasi dihentikan sementara.
- Catatan untuk siapa pun yang lanjut investigasi ini nanti: 2 dugaan sudah dicoba dan terbukti SALAH - (1) soal `File.lastModified()` vs EXIF, (2) soal `contentType`/daur-ulang slot Compose. Perlu sudut analisis baru, jangan ulangi dua ini.
- Koordinasi `ImageThumbnail` + `FileActionSheet` dengan hz25: masih berlaku kesepakatan sebelumnya (hz19 tinggal tunggu patch dari hz25), belum ada perubahan.
