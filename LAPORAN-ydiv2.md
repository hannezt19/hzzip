# Laporan Kerja - ydiv2

**Update terakhir:** 8 September 2026

**Area yang dipegang:** PDF Text-to-Speech (kontrol notifikasi/lock screen), Audio Player (implementasi + redesain UI + playlist + lirik)

---

## ⛔ Perlu Perhatian / Blocker

Tidak ada saat ini.

---

## Rencana Kerja & Fase Saat Ini

**PDF - Text-to-Speech**
`Tombol close bar TTS` [SELESAI] → `Kontrol notifikasi/lock screen` [SELESAI]

**Audio Player**
`Implementasi dasar` [SELESAI] → `Tahap A - Redesain UI neumorphism` [SELESAI, terkonfirmasi] → `Tahap B - Playlist custom (tabel Room + tambah/hapus lagu)` [SELESAI, terkonfirmasi] → `Tahap C1 - Rombak jadi 3 halaman (Playlist|Pemutar|Lirik) via swipe` [SELESAI, terkonfirmasi] → `Tahap C2 - Penyimpanan lirik privat + parser .lrc` [BELUM MULAI] → `Tahap C3 - Highlight lirik sinkron` [BELUM MULAI] → `Tahap C4 - Editor lirik mode Sederhana` [BELUM MULAI] → `Tahap C5 - Editor lirik mode Disinkronkan (tap-waktu sambil dengar)` [BELUM MULAI]

**Tugas kecil tertunda:** hapus tombol ☰ dari bar kontrol (jadi berlebihan sejak navigasi utama pindah ke swipe kiri/kanan antar halaman) - sederhanakan jadi 3 tombol (prev-play-next) di tengah tanpa trik tata letak tambahan.

---

## Log Pencapaian

### 8 September 2026
- **Audio Player - Tahap B (Playlist custom) SELESAI & terkonfirmasi.** Tabel Room baru: `PlaylistEntity` (path unik + urutan) + `PlaylistDao`. Migration `AppDatabase` v1→v2 (aman, tidak menghapus data user yang sudah ada). Awalnya diimplementasi sebagai bottom sheet dari bawah (sesuai `ROADMAP.md`), tapi user uji-coba langsung dan menilai kurang nyaman diakses dari bawah.
  - Commit: `[ydiv2] Tahap B1: tabel Room Playlist baru...`, `[ydiv2] Tahap B2: panel Playlist bottom sheet 2 tab...`, `[ydiv2] fix: tambah import clickable yang kelewat...`
- **Audio Player - Tahap C1 (navigasi 3 halaman via swipe) SELESAI & terkonfirmasi.** Setelah diskusi ulang, bottom sheet Playlist diganti total jadi **HorizontalPager 3 halaman**: **Playlist ← Pemutar → Lirik**. Masuk dari Beranda/file tetap langsung ke halaman Pemutar (tengah, `initialPage = 1`) - TIDAK berubah dari kesepakatan awal. Geser kiri = Playlist, geser kanan = Lirik (placeholder, isi sesungguhnya nyusul Tahap C2/C3). Bar kontrol (seekbar + tombol) dijadikan komponen reusable (`PlayerControlBar`, `SeekBarSection`) dan tampil konsisten di ketiga halaman - permintaan user supaya kontrol tetap bisa diakses dari halaman mana pun, termasuk saat baca lirik.
  - **Keputusan yang menyimpang dari `ROADMAP.md`:** navigasi Playlist BUKAN lagi bottom sheet gaya Spotify, tapi HorizontalPager (alasan: bottom sheet kurang nyaman diakses menurut user + konsisten dengan pola HorizontalPager yang sudah dipakai di PDF/galeri Gambar + menghindari konflik gestur dengan drawer navigasi utama app yang sudah sengaja mematikan gestur dari tepi, per catatan `STATUS.md`).
  - Commit: `[ydiv2] Tahap C1: rombak AudioPlayerScreen jadi HorizontalPager 3 halaman...`
- **Rencana Lirik (Tahap C, disepakati lewat diskusi panjang bareng user):**
  - Sumber data: file `.lrc` (format `[mm:ss.ms] teks`) untuk lirik BERSINKRON dengan highlight berjalan.
  - **Disimpan di penyimpanan privat khusus app** (bukan folder publik) - alasan: tidak ikut ke-scan `FileScanner`/Beranda, tidak berisiko terhapus app pembersih, dikunci pakai path lagu (bukan nama file) supaya tidak ketuker walau ada nama lagu kembar di folder beda.
  - **Mekanisme bersih-otomatis**: menumpang di `FileDao.syncAll()` yang sudah ada - begitu sebuah lagu terdeteksi hilang dari HP, data lirik terkait ikut dihapus di siklus scan yang sama (tidak perlu proses/jadwal terpisah).
  - **Fitur bikin lirik sendiri** (terinspirasi Musicolet, user kirim referensi screenshot): editor 2 mode - "Sederhana" (ketik polos tanpa waktu) dan "Disinkronkan" (tap tombol jam per baris sambil lagu diputar via mini transport play/mundur/maju, waktu tercatat otomatis) - hasil akhir digenerate jadi `.lrc` yang sama formatnya, disimpan ke folder privat.
  - Rincian sub-tahap (C2-C5) lihat tabel fase di atas.

---

## Rencana Selanjutnya

1. Hapus tombol ☰ dari `PlayerControlBar` (redundant, navigasi sudah lewat swipe) - tugas kecil, dikerjakan duluan sebelum lanjut C2.
2. **Tahap C2**: penyimpanan lirik privat (kunci dari path lagu) + hook pembersihan otomatis di `syncAll()` + parser `.lrc` + tampilan dasar halaman Lirik (list baris statis, atau "Lirik tidak tersedia").
3. **Tahap C3**: highlight baris berjalan + auto-scroll sinkron posisi lagu.
4. **Tahap C4**: editor lirik mode Sederhana.
5. **Tahap C5**: editor lirik mode Disinkronkan (tap-waktu sambil dengar, mini transport player).

## Update terakhir: Checkpoint A - Tahap C Lirik & Judul Asli
- [PROSES] Id3UsltReader.kt digeneralisasi + tambah readTitle() (frame TIT2)
- [PROSES] LyricsStore.kt dirombak total - TIDAK PAKAI file .lrc privat lagi, sumber lirik murni dari tag USLT tertanam, deteksi otomatis pola [mm:ss.ss] di dalam teksnya (ala Musicolet)
- [PROSES] Info: item URGENT build 'clickable' di TODO.md (8 Sept) SUDAH BASI - dicek ydiv2, import ada, gh run list nunjukkin 5 build terakhir main semua sukses. Mohon dihapus dari TODO.md.
- [BELUM] Checkpoint B: UI PlaylistPage (thumbnail cover album + menu ... 4 opsi: Info lagu/Hapus/Tambah/Bagikan) + pakai readTitle() sebagai judul utama

## Update terakhir: Checkpoint B - UI Playlist (thumbnail + judul asli + menu opsi)
- [PROSES] PlaylistPage: tambah thumbnail kotak (reuse loadAlbumArt/albumArtCache), judul pakai resolveTitle() (tag TIT2, fallback cleanTitle), menu "..." 3 opsi (Info lagu/Hapus-Tambah Playlist/Bagikan pakai FileProvider yang sudah dikonfigurasi)
- [BELUM] Testing manual di HP (thumbnail muncul, judul asli kebaca, share jalan tanpa crash)

## Fix bug: judul & lagu tidak sinkron di tab Playlist
- [SELESAI] playFromCustomPlaylist(): startIndex dulu dihitung dari customPlaylistEntries MENTAH, sementara mediaItems dibuang (mapNotNull) kalau file sudah tidak ketemu di fileByPath - bikin index geser & judul/lagu tidak sinkron begitu ada 1 entry "yatim" (path tercatat di playlist tapi file sudah dipindah/hilang). Fix: filter dulu baru hitung ulang index dari list yang sudah valid.
