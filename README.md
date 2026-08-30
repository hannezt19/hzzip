# File Reader App (v0.1 - versi awal)

App Android (Kotlin + Jetpack Compose, native — bukan Capacitor/WebView) untuk
baca & edit file **PDF, JSON, HTML, JS** langsung dari HP.

## Fitur v0.1
- Muncul di menu **"Buka dengan"** file manager untuk PDF/JSON/HTML/JS
- PDF: dibaca halaman-per-halaman (hemat RAM), zoom, ingat posisi terakhir
- JSON/HTML/JS: editor dengan nomor baris, syntax highlighting, undo/redo, simpan
- Target: Android 9 ke atas (minSdk 28), dites di device RAM 3GB (Redmi C2) & Poco M4

## Cara pakai (build APK lewat GitHub)
1. Push folder ini ke repo GitHub baru (branch `main`)
2. GitHub Actions otomatis jalan (lihat tab **Actions**)
3. Setelah selesai (~3-5 menit), buka run terakhir → bagian **Artifacts** →
   download `file-reader-debug-apk`
4. Extract zip-nya, dapat file `app-debug.apk` → kirim/transfer ke HP → install

## Belum ada di v0.1 (rencana menyusul)
- Edit isi PDF (baru bisa baca)
- xlsx, pptx
- Find & replace di editor teks
- Dukungan file .css, .txt penuh (kerangkanya sudah ada di FileType.TEXT,
  tinggal dites)

## Struktur project
```
app/src/main/java/com/yohanes/filereader/
  MainActivity.kt        -> handle intent "buka dengan", routing
  FileType.kt             -> deteksi jenis file dari nama/mime type
  ui/PdfViewerScreen.kt   -> baca PDF per halaman
  ui/CodeEditorScreen.kt  -> editor teks + nomor baris
  ui/SyntaxHighlight.kt   -> pewarnaan kode sederhana (regex, ringan)
```
