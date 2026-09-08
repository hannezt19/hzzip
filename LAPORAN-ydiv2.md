# Laporan Kerja - ydiv2

**Update terakhir:** 8 September 2026

**Area yang dipegang:** PDF Text-to-Speech (kontrol notifikasi/lock screen), Audio Player (implementasi + redesain UI)

---

## ⛔ Perlu Perhatian / Blocker

Tidak ada saat ini.

---

## Rencana Kerja & Fase Saat Ini

**PDF - Text-to-Speech**
`Tombol close bar TTS` [SELESAI] → `Kontrol notifikasi/lock screen` [SELESAI]

**Audio Player**
`Implementasi dasar (playback, background, notifikasi)` [SELESAI] → `Tahap A - Redesain UI neumorphism` [SELESAI, menunggu konfirmasi tes terakhir] → `Tahap B - Playlist custom` [BELUM MULAI, sudah cek AppDatabase/FileDao] → `Tahap C - Lirik dari tag ID3` [BELUM MULAI]

---

## Log Pencapaian

### 8 September 2026
- **Audio Player - Tahap A (redesain UI) selesai, sebagian belum dikonfirmasi.** Redesain total `AudioPlayerScreen.kt` jadi gaya neumorphism lewat banyak iterasi bareng user. Sudah dikonfirmasi: fungsi dasar (autoplay, kontrol notifikasi/lock screen, background play). Belum dikonfirmasi: patch terakhir (ukuran tombol/judul dikecilkan, posisi bar kontrol digeser turun).
  - Keputusan yang menyimpang dari rencana awal:
    - Latar **terang** (`#EEEEF2`), bukan gelap solid seperti rencana awal (neumorphism lebih kelihatan di terang).
    - Nama artis (tag ID3) **dihapus** dari tampilan (banyak file rekaman pribadi, bukan musik).
    - Fitur favorit (bintang) **dihapus total** dari layar ini.
  - Commit: beberapa commit `[ydiv2]` prefix "Redesain AudioPlayerScreen...", "Tahap A redesain...", "Tahap A revisi..." (cek `git log` untuk daftar lengkap)
- **Audio Player - Tahap B, persiapan.** Sudah cek `AppDatabase.kt` (v1, cuma entity `FileEntity`) & `FileDao.kt` - masih bersih, aman ditambah tabel Playlist tanpa bentrok kerjaan hz19. Belum sempat menulis `PlaylistEntity`/DAO baru.
- **PDF - Tombol close bar TTS.** Tombol ✕ ditambahkan di bar TTS, matikan TTS cepat tanpa lewat panel pengaturan.
  - Commit: `ydiv2 fix: tombol close terpisah di bar TTS PdfViewerScreen`
- **PDF - Kontrol TTS di notifikasi & lock screen.** File baru: `TtsPlaybackService.kt` (Foreground Service + MediaSessionCompat), `TtsPlaybackBridge.kt` (jembatan ke state TTS).
  - Commit: `ydiv2 fix: kontrol TTS di notifikasi & lock screen (foreground service + MediaSessionCompat)`, `ydiv2 fix: pindahkan minta izin notifikasi ke MainActivity...`

---

## Rencana Selanjutnya

**Tahap B - Playlist Audio** (sesuai `TODO.md`/`ROADMAP.md`): tabel Room baru (path+urutan lagu), panel bottom sheet dari bawah gaya Spotify (bukan drawer samping), 2 tab "Playlist" (kosong awal) + "Semua Audio" (tombol tambah ke Playlist).

**Tahap C - Lirik** (setelah Tahap B): baca tag ID3 `USLT` (parsing manual), akses via gestur geser di layar pemutar. Ekspektasi: banyak file (rekaman pribadi) kemungkinan tidak punya lirik.

<details>
<summary>Detail teknis Tahap A (opsional dibaca)</summary>

- Efek neumorphism manual (bukan library luar): `Modifier.shadow()` + border gradasi, fungsi `softRaised()`.
- Cover album dari `MediaMetadataRetriever.embeddedPicture`, cache `Map<String, Bitmap?>` di memori.
- Judul dibersihkan otomatis (buang ekstensi, underscore jadi spasi).
- Bar kontrol pil: ☰ nempel kiri (placeholder, belum berfungsi - scope Tahap B), grup Prev-Play-Next benar-benar di tengah.
- Sempat bug bar kontrol ketutup navbar sistem - fix pakai `navigationBarsPadding()`.
- Ikon ☰ pakai `Icons.Default.Menu` (Material), bukan teks Unicode.
- Sempat bug notifikasi TTS PDF tidak muncul - root cause: `rememberLauncherForActivityResult` didaftarkan terlalu telat (di composable, bukan di Activity.onCreate). Fix: izin dipindah ke `MainActivity.onCreate()`.

</details>
