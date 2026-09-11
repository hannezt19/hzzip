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

## 2026-09-11

### Update status bug lama
Bug header bulan hilang: DIKONFIRMASI ULANG MASIH AKTIF. Cross-check langsung ke kode `HomeViewModel.kt` (baik branch lama hz19 maupun main) - masih pakai `monthLabelOf` (per-bulan), bukan `dayLabelOf` per-hari. Catatan progress sebelumnya yang menyebut ini "sudah difix" ternyata keliru, fix itu tidak pernah ter-commit. 2 dugaan lama tetap terbukti salah: (1) File.lastModified() vs EXIF, (2) contentType/daur-ulang slot Compose - jangan diulang.

### Keputusan baru: desain accordion galeri final (mode Terbaru)
Struktur berlapis Tahun -> Bulan -> Tanggal, menggantikan rencana fix per-hari sebelumnya karena sekaligus menghilangkan akar bug (bulan lama tidak lagi lewat insertSeparators/Paging3):
- Bulan berjalan: semua tanggal tampil langsung (format "11 Sept"), jumlah foto di sebelah kanan tiap baris
- Bulan lalu (tahun sama): dilipat jadi 1 baris "NamaBulan — jumlah", tap untuk buka daftar tanggal
- Tahun lalu: dilipat jadi 1 baris "Tahun — jumlah", tap buka daftar bulan, tap bulan buka daftar tanggal
- Mode Folder tidak berubah

### Progress: query hitung jumlah foto (langkah 1 rencana accordion)
Ditambahkan ke `FileDao.kt`: `countPhotosPerDayInMonth`, `countPhotosPerMonthInYear`, `countPhotosPerYear` (+ data class `DayCount`/`MonthCount`/`YearCount`). Pakai `strftime` pada kolom `lastModified` (epoch ms) + modifier `'localtime'`, filter `extension IN ('jpg','jpeg','png','webp','gif')`.
Status: [PROSES] - patch ditempel ke branch main, MENUNGGU build & tes di HP dikonfirmasi (build sebelumnya sempat sukses tapi itu di branch hz19 lama yang sudah ditinggalkan, jadi perlu dites ulang di main).

### Rencana kerja & file terkait (aktif)
1. [PROSES] Query hitung foto per grup (FileDao.kt, di main) - tinggal tes build
2. [BELUM] Susun bentuk data 3 jenis baris tampilan (tanggal biasa / ringkasan bulan / ringkasan tahun)
3. [BELUM] Sambungkan ke ViewModel - bulan berjalan tetap via Paging3, bulan/tahun lalu pakai jalur data ringan terpisah + state buka/tutup accordion di ViewModel
4. [BELUM] UI + logika tap buka/tutup
5. [BELUM] Tes dengan data asli ~23rb foto
6. [BELUM] Sticky header & Fast Scroller (menyusul setelah accordion final stabil)

### Catatan workflow
Sejak 11 Sept, semua kerja pindah ke branch main (branch per-akun dihapus, resmi di CONVENTIONS.md). Update ini adalah update pertama ke LAPORAN-hz19.md di branch main - update sebelumnya (yang sempat ditulis 2x) ada di branch `hz19` lama yang sekarang ditinggalkan.

## 2026-09-11 (lanjutan) - Checkpoint 2a+2b

### Progress: backend accordion (query + state ViewModel)
- FileDao.kt: tambah `getImagesPagedForMonth(yearMonth)` (paging dibatasi 1 bulan saja) dan `getImagesForDate(date)` (list foto 1 tanggal spesifik, non-paged, dipakai saat tanggal di accordion lama di-tap - alurnya sama seperti buka folder di mode Folder)
- HomeViewModel.kt: 
  - `imagesPaged` sekarang dibatasi ke bulan berjalan saja (`getImagesPagedForMonth(currentYearMonth)`), header diganti dari `monthLabelOf` ke `dayLabelOf` (format "11 Sep", tanpa "Hari ini"/"Kemarin") - INI SEKALIGUS jadi fix bug header hilang, karena bulan lama sudah tidak lewat Paging3/insertSeparators sama sekali
  - Tambah state accordion: `pastMonthsInCurrentYear`, `pastYears`, `expandedMonthKey`+`daysForExpandedMonth`, `expandedYear`+`monthsForExpandedYear`, `selectedDatePhotos`
  - Tambah fungsi: `loadImageAccordionSummaries()`, `toggleAccordionMonth()`, `toggleAccordionYear()`, `selectAccordionDate()`, `clearSelectedAccordionDate()`
Status: [PROSES] - patch ditempel, MENUNGGU build & tes di HP. Alur tap: tahun -> tap -> daftar bulan -> tap -> daftar tanggal -> tap -> grid foto (grid akhir tidak accordion lagi, sesuai kesepakatan).

### Rencana kerja & file terkait (aktif)
1. [SELESAI] Query hitung foto per grup (FileDao.kt, di main)
2. [PROSES] Backend accordion (FileDao.kt query tambahan + HomeViewModel.kt state) - tinggal tes build
3. [BELUM] UI ImageGalleryScreen.kt: render baris ringkasan bulan/tahun (dengan jumlah foto di kanan) di bawah grid bulan berjalan, sambungkan tap ke fungsi toggle/select ViewModel, tampilkan grid saat selectedDatePhotos terisi (pola sama seperti selectedFolder)
4. [BELUM] Tes dengan data asli ~23rb foto
5. [BELUM] Sticky header & Fast Scroller (menyusul setelah accordion final stabil)
