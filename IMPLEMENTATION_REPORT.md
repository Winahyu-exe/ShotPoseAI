> Laporan lama dari ZIP awal. Untuk status terbaru gunakan [UI_REVISION.md](docs/UI_REVISION.md); klaim validasi lama bukan hasil pengujian revisi ini.

# Implementation Report

## Revisi kompatibilitas Motorola Moto G45 / Android 15 (2026-09-04)

- Layar pilih gerakan memakai jalur native responsif dengan node 72dp, label stabil, tinggi dinamis, dan progress achievement dari Room.
- Detail kelas dan detail siswa memiliki layout terpisah sesuai referensi desain, termasuk statistik, pencarian anggota, achievement, progress, dan riwayat latihan.
- Hapus kelas/siswa memakai dialog konfirmasi kustom; penghapusan relasi dan riwayat tetap dilakukan di thread IO.
- Pemilih video memakai OpenDocument dan mempertahankan izin baca URI.
- Preview VideoView memasang listener sebelum URI, memvalidasi akses file, dan menampilkan error codec/URI secara eksplisit.
- VideoPoseAnalyzer mengambil frame terdekat setiap 250ms, bukan sync frame berulang, lalu mensyaratkan minimal tiga pose valid dan gerakan antar-frame.
- Skor video memakai agregasi median/persentil dan coverage frame; tidak lagi mengambil satu frame tertinggi atau menghasilkan nilai 100 otomatis.

## Implemented

- Authentication guru: splash/session, register, login, logout, forgot password, OTP lokal, reset password.
- Room entities: user, student, classroom, cross-reference class/student, training session, analysis result, achievement, dan student achievement.
- Home guru sesuai hierarchy Figma: progress, jumlah kelas/siswa, CTA kamera, CTA import video, navigation bar.
- Kelola/tambah/edit/hapus/detail siswa, termasuk foto lokal yang persisten.
- Kelola/tambah/edit/hapus/detail kelas dan pemilihan anggota.
- Pilih siswa sebelum penilaian.
- Pilih gerakan dalam visual gamified path yang digambar native menggunakan Canvas.
- Instruksi gerakan, preview, checklist, camera tips, kamera dan upload video.
- CameraX front/back, countdown, batas 30 detik, MediaPipe, pose overlay, multi-person rejection.
- Scoring rule-based per kategori gerakan.
- Processing dialog, hasil analisis, badge/achievement.
- Laporan, ringkasan, pencarian/filter teks, leaderboard, detail laporan.
- Export PDF, CSV, XLS dan Android Sharesheet.
- Profile guru, foto lokal, pengaturan nama/NIP, bantuan, dan logout.

## Native UI rule

Tidak ada screenshot layar Figma yang digunakan sebagai background atau layout. Semua layar dibentuk dari XML, Material components, drawable XML, dan custom Canvas view. Asset bola, launcher, dan model yang tersedia pada project dipakai sebagai resource terpisah. Ilustrasi Figma yang tidak tersedia sebagai binary di ZIP direkonstruksi secara native.

## QA performed in workspace

- Seluruh XML resource dan AndroidManifest berhasil diparse.
- Seluruh referensi resource yang dianalisis tersedia.
- Referensi ViewBinding diperiksa terhadap ID layout: tidak ada ID yang hilang.
- Activity pada manifest diperiksa terhadap class sumber: lengkap.
- Pemeriksaan keseimbangan delimiter/braces sumber Kotlin: lolos.
- Model MediaPipe terdeteksi di folder assets.

Gradle build penuh perlu dijalankan saat project dibuka di Android Studio karena runtime pengerjaan tidak memiliki Gradle 9.3.1 dan akses unduh distribusi Gradle.
