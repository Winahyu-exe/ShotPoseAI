# Revisi UI dan integrasi data — 3 September 2026

Status: perubahan sumber selesai untuk fungsi di bawah; belum ada build Android berhasil atau uji perangkat pada lingkungan pengerjaan ini. Kesamaan visual 1:1 belum tercapai/terverifikasi.

## Perubahan yang disertakan

- Home: progress Dribble Challenge dihitung dari jumlah siswa unik dengan sesi Dribbling COMPLETED berskor >=85, dibanding jumlah siswa terdaftar. Percobaan berulang tidak menambah jumlah siswa. Nilai diperbarui saat kembali ke home. Foto profil guru ditampilkan dari database.
- Foto: tombol kamera terlihat pada foto siswa, profil guru, dan pengaturan akun. Foto baru disalin ke folder internal aplikasi, diperkecil sampai 1024 px, dikoreksi orientasinya, disimpan JPEG, dan path-nya disimpan di kolom profilePhotoPath yang sudah ada. Pemilihan foto tidak mengunggah foto ke internet.
- Avatar: satu loader digunakan di daftar siswa, pemilih siswa, detail siswa, anggota kelas, form kelas, hasil analisis, laporan, leaderboard, achievement, home, dan profil/pengaturan guru. Path lama tetap dibaca bila sumbernya masih tersedia.
- Pemilihan gerakan: area scroll sekitar 1970 dp untuk sepuluh gerakan. Tombol native 91×78 dp, label, target skor, dan kartu progres. Posisi awal diarahkan ke gerakan pertama yang belum selesai. Semua gerakan masih dapat dipilih guru; tidak ditambahkan prasyarat yang membatasi penilaian.
- Centang: hanya muncul jika skor terbaik siswa pada gerakan tersebut >=85/100. Base level siswa bukan sumber centang. Progres = gerakan mencapai achievement / 10. Skor dan gerakan pada kartu bukan contoh statis.
- YouTube: sepuluh gerakan memiliki video referensi. WebView memuat IFrame Player dengan app identity Referer, kontrol putar manual, status kegagalan, tombol coba lagi dan buka YouTube. Pemutaran dihentikan saat halaman ditinggalkan. Video memerlukan internet; data dan analisis aplikasi tetap lokal. Ketersediaan/izin embed tetap ditentukan pemilik video; pemutaran aktual belum diuji di perangkat.
- Leaderboard: podium tiga siswa terbaik dengan urutan visual 2–1–3, foto masing-masing, skor, dan kartu peringkat 4 ke bawah. Data kosong tidak diisi nama/skor contoh. Jika skor sama, urutan mengikuti nama lalu ID siswa. Menekan peserta membuka laporan hasil terbaiknya.
- Form kelas: kartu olahraga berwarna dengan hanya Sepak bola aktif; Basket, Voli, Bulu tangkis, dan Atletik nonaktif. Pilihan kategori menggunakan chip, tersedia pencarian, preview enam siswa dan dialog seluruh siswa. Keanggotaan serta data kelas disimpan dalam satu transaksi.
- Hasil: tombol di layar analisis diubah menjadi “Simpan & Lihat Hasil”. Penyimpanan sesi, analisis, dan unlock achievement menggunakan transaksi Room. Halaman hasil juga dapat dibuka dari detail laporan.
- Achievement: penghargaan baru hanya dipresentasikan ketika INSERT OR IGNORE benar-benar menambahkan achievement siswa. Nilai excellent kedua untuk gerakan yang sama tidak memicu penghargaan baru. Koleksi achievement dapat dibuka dari detail siswa atau halaman hasil. Label menunjukkan nama badge gerakan sesungguhnya.

## Alur untuk mencoba

1. Tambah/edit siswa, ketuk foto atau tombol kamera, pilih foto, lalu Simpan.
2. Buka profil guru, ketuk foto/kamera; pemilihan foto di halaman profil langsung disimpan.
3. Pilih siswa → Pilih Gerakan → Instruksi. Uji video dengan internet hidup dan mati; instruksi teks tetap tersedia.
4. Jalankan analisis, lalu “Simpan & Lihat Hasil”. Bila siswa pertama kali mencapai 85 pada gerakan itu, layar achievement tampil. Tombol Lanjutkan kembali ke hasil analisis.
5. Dari hasil, buka achievement atau lanjut latihan. Peta harus menunjukkan centang baru. Buka home: progres tantangan berubah hanya untuk hasil Dribbling.
6. Laporan → detail laporan → Lihat Hasil Analisis. Laporan → Papan Peringkat untuk podium.
7. Tambah kelas → pilih kategori dan anggota. Olahraga non-Sepak bola tidak dapat dipilih.

## Bagian visual yang belum 1:1

Figma file `lZXESTUe0rdJsNI0ozEUoX`, halaman `1:3`, berhasil memberikan konteks home dan peta gerakan, serta metadata layar lainnya. Akses berikutnya ditolak karena kuota MCP paket Starter. Permintaan unduhan aset Figma juga gagal; tidak ada gambar layar Figma ditempel sebagai pengganti UI.

- Peta memakai perbaikan grass/path yang sudah ada, belum memakai artwork rumput dan batu asli Figma.
- Kartu olahraga serta medal/trofi masih memakai simbol/emoji dan resource bawaan/lokal, belum ikon ekspor Figma. Bentuk/glyph dapat berbeda menurut perangkat.
- Podium meniru struktur dan perbandingan tinggi dari metadata, namun ilustrasi latar, warna detil, dan ikon podium belum divalidasi dengan screenshot terbaru.
- Font project tetap sans-serif; font Plus Jakarta Sans belum ditambahkan.
- Profil, form siswa, hasil, achievement, instruksi dan form kelas belum dibandingkan piksel dengan render perangkat.

Untuk menyelesaikan bagian ini diperlukan ekspor layar PNG dan aset SVG/PNG asli, atau akses Figma yang kembali dapat digunakan. Node relevan: home `129:9870`, gerakan `100:6214`, kelas `70:1307`, leaderboard `117:5802`, siswa `112:3960`, profil `121:9928`, instruksi `104:3066`, hasil `114:4517`, achievement `117:5368`.

## Validasi dan batasnya

`python tools/verify_source.py`: PASS; 478 pemeriksaan sumber/resource/query, 74 XML, 31 query DAO. Pemeriksaan mencakup ID unik, referensi ViewBinding/resource, SQL berdasarkan deklarasi entity, data kosong, skor 84/85, percobaan berulang, pemisahan gerakan/siswa, sesi belum selesai, dan siswa yang sudah tidak ada. Ini bukan kompilasi Kotlin/Java atau uji aplikasi.

`bash gradlew :app:assembleDebug --no-daemon`: gagal sebelum kompilasi karena unduhan Gradle 9.3.1: `java.net.SocketException: Network is unreachable`. Lingkungan tidak memiliki Android SDK/emulator. APK tidak disertakan dan keberhasilan build tidak diklaim.

Schema Room tetap versi 1; perubahan hanya query dan tipe hasil query. Data aplikasi lama tidak dihapus oleh perubahan ini. Buka folder ShotPoseAI di Android Studio, gunakan Gradle JVM 21 sesuai gradle/gradle-daemon-jvm.properties yang sudah ada, lalu Gradle Sync dan build. Target bytecode Java tetap 17.
