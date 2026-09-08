path = "LAPORAN-hz21.md"
content = open(path, encoding="utf-8").read()

tambahan = """
## Update - Konfirmasi Perbaikan (screenshot dari user)
- Perbaikan padding/tata letak Beranda & layar kategori (dikerjakan hz11/hz25 setelah laporan di atas) SUDAH DIKONFIRMASI user via screenshot langsung di HP:
  1. Hamburger di Beranda sudah tidak nabrak search bar lagi (reserved space, bukan overlay bebas)
  2. Baris pertama list kategori (PDF/Excel/dll) sudah tidak ketiban hamburger
  3. Toggle Terbaru/Folder sudah tidak ketutup navigation bar sistem
- Ketiga poin blocker/temuan yang tercatat di laporan sesi sebelumnya (bagian "Blocker / Pertanyaan Terbuka" terkait Beranda & kategori) dinyatakan SELESAI oleh user, siap dilaporkan ke hz11 untuk sinkronisasi TODO.md/STATUS.md
- Catatan: SettingsPanel (Warna Latar/Kontras/Mode Baca/TTS/ID) di PdfViewerScreen.kt MASIH belum dikonfirmasi terpisah - bukan bagian dari konfirmasi screenshot sesi ini
"""

if "## Update - Konfirmasi Perbaikan" in content:
    print("SKIP: bagian update sudah pernah ditambahkan sebelumnya, tidak ditambah lagi (cek manual kalau perlu revisi).")
else:
    with open(path, "a", encoding="utf-8") as f:
        f.write(tambahan)
    print(f"BERHASIL: laporan diupdate, tambahan {len(tambahan)} karakter")
