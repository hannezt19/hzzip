# CONVENTIONS - FileReaderApp

> File ini berisi aturan main tetap proyek ini. Jarang berubah. Kalau ada aturan baru yang disepakati, tambahkan di sini (hz11 yang menulis, tapi usulan boleh dari akun mana pun).

## Struktur Peran & Pembagian Kerja

> Semua akun (koordinator maupun eksekutor) WAJIB mematuhi seluruh aturan di file ini tanpa kecuali.

**Koordinator: hz11**
- TIDAK menulis kode aplikasi. Tugasnya HANYA menjaga 4 file acuan (README.md, STATUS.md, ROADMAP.md, TODO.md) tetap akurat, plus revisi CONVENTIONS.md ini.
- Berperan sebagai ROUTER: user berdiskusi & eksekusi kerja LANGSUNG dengan akun eksekutor sampai satu fase dikonfirmasi selesai, BARU user informasikan hasilnya ke hz11. hz11 tidak memberi instruksi langsung ke eksekutor lewat jalur lain.
- WAJIB verifikasi klaim/progress lewat kondisi asli repo (cek kode & LAPORAN akun via Termux) sebelum update file acuan - bukan hanya mengandalkan riwayat chat/memori sendiri ("pencocokan 2 arah").
- Item yang sudah dikonfirmasi TUNTAS oleh user WAJIB DIHAPUS dari TODO.md/STATUS.md (bukan ditandai 'selesai' lalu dibiarkan) - riwayatnya cukup ada di git log & LAPORAN akun terkait.
- Pembagian kerja (siapa pegang apa) DITENTUKAN & DICATAT oleh hz11 di TODO.md - kalau ada ketidakjelasan/tumpang tindih scope, itu tanggung jawab hz11 untuk meluruskan berdasarkan info dari laporan tiap akun.

**Eksekutor (4 akun, kerja PARALEL tapi BERGANTIAN - tidak di waktu yang sama):**
- **hz19** - kategori Gambar (galeri, viewer, performa koleksi besar ~23rb foto)
- **hz21** - PDF SettingsPanel/Mode Baca (Warna Latar, Kontras, dan pengaturan tampilan baca lainnya)
- **hz25** - Video (player + galeri), toggle Terbaru/Folder lintas kategori, sistem clipboard/file-ops (FileActionSheet)
- **ydiv2** - PDF Text-to-Speech, Audio Player

Setiap akun eksekutor WAJIB baca CONVENTIONS.md, STATUS.md, ROADMAP.md, TODO.md dulu sebelum lanjut kerja. Kalau butuh detail teknis akun lain, baru buka LAPORAN-[akun].md yang relevan.

## Peta File Penting
- `MainActivity.kt` - pusat navigasi (drawer/hamburger via ModalNavigationDrawer, buka file, tentukan viewer sesuai FileType)
- `FileType.kt` - deteksi jenis file (dari nama file & MIME type)
- `ui/BottomNavBar.kt` - isi drawer (`AppDrawerContent`, enum `AppTab` cuma HOME+RECENT - Pengaturan jadi accordion di sini, bukan tab)
- `HomeViewModel.kt` + `ui/HomeScreen.kt` - Beranda (kartu kategori, search, storage info, favorit) + state navigasi kategori/toggle Terbaru-Folder per kategori
- `FavoritesStore.kt` - penyimpanan status favorit (terpisah dari Room DB)
- `data/FileDao.kt` - termasuk `syncAll()` (scan incremental diff-by-path)
- `data/Xlsx*.kt` - parser & writer xlsx buatan sendiri
- `VideoPlayerScreen.kt` / `ui/VideoGalleryScreen.kt` - player Video (ExoPlayer/Media3) & galeri grid Video
- `AudioPlayerScreen.kt` / `AudioPlayerService.kt` - player Audio (MediaSessionService) & UI-nya
- `ui/ImageGalleryScreen.kt` / `ui/ImagePagerScreen.kt` - galeri & viewer Gambar
- `ui/FileListWithModeToggle.kt` - komponen list+toggle Terbaru/Folder dipakai bareng kategori PDF/Excel/Teks-Kode/Favorit
- `ui/PdfViewerScreen.kt` - termasuk SettingsPanel (Mode Baca: Warna Latar/Kontras/dll)
- `ui/*ViewerScreen.kt` lain - satu file per jenis viewer (XlsxViewerScreen, CodeEditorScreen)

## Alur Kerja Standar
1. Cek dulu struktur kode terkait sebelum bikin patch (grep/sed -n/cat -n) - jangan menebak isi file
2. Tulis patch pakai python3 heredoc dengan old/new string, cetak jumlah berhasil
3. Cek hasil patch (harus sesuai jumlah yang diharapkan) sebelum lanjut
4. Kalau bikin file baru, cek wc -l dan tail -5 untuk pastikan tidak terpotong
5. git add -> git commit -m "[nama-akun] ..." -> git push
6. Cek hasil build di GitHub Actions - kalau gagal, baca error log, perbaiki, ulangi dari langkah 2
7. Setelah build sukses & APK diinstal di HP, baru lanjut ke tugas berikutnya
8. Update LAPORAN-[akun].md kalau ada progress atau keputusan baru (lihat aturan wajib di bawah)

## ATURAN WAJIB: Laporan Progress Antar-Akun

1. **Satu file per akun** - LAPORAN-[nama-akun].md di root repo, TANPA tanggal di nama file. Tidak boleh bikin file baru tiap sesi/tanggal - selalu file yang sama, terus di-update.
2. **Wajib commit ke repo** setiap kali satu fase/rencana kerja DIKONFIRMASI SELESAI oleh user (bukan cuma selesai nulis kode, harus sudah dikonfirmasi build sukses & dites) - bukan cuma dilaporkan lewat chat masing-masing.
3. **Skrip patch Python WAJIB dijalankan via heredoc langsung** (python3 << 'EOF' ... EOF) di terminal - TIDAK BOLEH disimpan jadi file .py fisik yang ikut ke-git add/commit. Ini sudah 2x kejadian tidak sengaja (file sampah menuhin repo), jadi berlaku wajib, bukan cuma imbauan.
4. **Checkpoint sekali jalan** - begitu satu langkah kerja (walau baru sebagian dari tugas besar) selesai ditulis dan siap dicoba, PATCH KODE + UPDATE LAPORAN + GIT COMMIT+PUSH harus jadi SATU blok kode sekali tempel, urutannya: (1) Patch/tulis kode, (2) Update LAPORAN-[akun].md (minimal 1 baris progress terbaru), (3) git add (kode yang berubah + file laporan) -> commit -> push. Alasan: kalau token/sesi habis PERSIS setelah blok ini jalan, kondisi repo selalu konsisten.
5. **Fase Kerja pakai 3 status baku**: [SELESAI] / [PROSES] / [BELUM] - supaya kalau sesi terhenti di tengah jalan, siapa pun yang lanjut langsung tahu PERSIS di titik mana harus disambung.

## Format Laporan (rekomendasi struktur, gaya penulisan boleh fleksibel)

Urutan bagian yang disarankan ada di tiap LAPORAN-[akun].md:
1. **Fase Kerja** - ringkasan status tiap tahap kerja, pakai notasi [SELESAI]/[PROSES]/[BELUM]
2. **Kesepakatan Baru dengan User** - keputusan penting yang BELUM sempat diserap hz11 ke file utama. Begitu diserap, pindahkan jadi 1 baris ringkas ke Log Pencapaian.
3. **Rencana Kerja & File Terkait** - HANYA tugas yang SEDANG AKTIF + file yang disentuh. Rencana jangka panjang tetap di ROADMAP.md.
4. **Bug** - HANYA yang masih aktif/belum fix. Begitu fix, pindah ke Log Pencapaian.
5. **Log Pencapaian** - paling bawah, riwayat historis boleh terus bertambah, ringkas 1-2 baris per item.

## ATURAN WAJIB: Semua Akun Kerja Langsung di Branch main

Tidak ada lagi branch terpisah per akun. Semua eksekutor (hz19, hz21, hz25, ydiv2) kerja & push LANGSUNG ke `main`.

Alur kerja WAJIB tiap sesi:
1. `git checkout main && git pull origin main` - WAJIB di awal sesi, sebelum patch apa pun.
2. Kerja & patch seperti biasa.
3. Commit+push ke `main` HANYA setelah 1 fase dikonfirmasi selesai oleh user (build hijau + sudah dites di HP) - bukan tiap potongan kode selesai ditulis. Checkpoint sekali jalan (patch+laporan+commit dalam 1 blok) tetap berlaku.

ATURAN WAJIB kalau sesi harus berhenti SEBELUM fase dikonfirmasi selesai (misal token habis di tengah jalan):
- `git stash` kerjaan yang belum di-commit SEBELUM keluar atau ganti akun. Jangan biarkan file berubah menggantung di working directory - akun lain yang lanjut pakai folder yang sama bisa ketiban kerjaan asing.
- Sesi berikutnya yang melanjutkan: cek `git stash list` dulu, `git stash pop` kalau mau lanjutkan kerjaan yang tertunda.

Proses ke main sekarang dilakukan LANGSUNG oleh eksekutor sendiri - hz11 TIDAK lagi merge manual. hz11 hanya memperbarui dokumentasi (README/STATUS/ROADMAP/TODO.md) setelah user menginformasikan fase selesai.

## Repo & Environment
- Repo: https://github.com/hannezt19/hzzip (nama folder lokal: FileReaderApp)
- Dikerjakan sepenuhnya dari HP via Termux, tanpa Android Studio/laptop
- Build APK release lewat GitHub Actions, sudah pakai keystore signing permanen
- Target device utama: Motorola Moto G45 (RAM kecil)
- minSdk 34, targetSdk 35, compileSdk 35

## Bahasa & Gaya Komunikasi
- Semua nama fitur, teks UI, dan komentar dalam Bahasa Indonesia
- Diskusi konsep/desain dulu sebelum mulai coding, terutama untuk perubahan besar
