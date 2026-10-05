# ShotPose AI

> Revisi UI 3 September 2026: lihat [catatan perubahan dan batas verifikasi](docs/UI_REVISION.md). Build Android belum berhasil diuji dan UI belum dinyatakan identik 1:1 dengan Figma. Preview YouTube memerlukan internet.

ShotPose AI adalah aplikasi Android native offline untuk guru yang mengelola siswa, kelas, latihan sepak bola, analisis pose, achievement, dan laporan. UI mengikuti desain Figma ShotPose AI menggunakan XML + ViewBinding dan komponen Android asli.

## Stack

- Kotlin + Java (Java hanya untuk entity/DAO Room agar stabil dengan AGP built-in Kotlin)
- Android Gradle Plugin 9.1.1, Gradle 9.3.1, JDK 17
- XML Layout + ViewBinding
- MVVM sederhana (`DashboardViewModel` + repository)
- Room 2.8.4
- Preferences DataStore untuk session
- CameraX
- MediaPipe Pose Landmarker (offline)
- PDF, CSV, dan SpreadsheetML/XLS lokal

## Alur utama

1. Guru register/login lokal; password di-hash PBKDF2-HMAC-SHA256.
2. Guru menambah siswa beserta foto dan kelas, mengedit/menghapus data, serta memasukkan siswa ke kelas.
3. Guru memilih siswa dan gerakan.
4. Analisis dilakukan melalui kamera atau video maksimal 30 detik.
5. MediaPipe membatasi penilaian pada satu orang dan menampilkan skeleton overlay.
6. Hasil disimpan ke profil siswa, laporan, leaderboard, dan achievement.
7. Detail laporan dapat dibagikan sebagai PDF, CSV, atau XLS.

## Menjalankan project

1. Extract ZIP dan buka folder project di Android Studio versi yang mendukung AGP 9.1.1.
2. Gunakan Gradle JVM 17 atau yang direkomendasikan Android Studio (target bytecode Java 17).
3. Pastikan Android SDK Platform 35 dan Build Tools yang diminta Gradle tersedia.
4. Jalankan Gradle Sync, lalu `app` pada perangkat fisik untuk pengujian kamera.

Model `app/src/main/assets/pose_landmarker_lite.task` sudah disertakan. Semua data aplikasi tersimpan lokal pada perangkat.

## Implementasi desain dan asset

Hierarchy, warna utama, bentuk kartu, CTA, navigasi, dan seluruh alur layar guru direalisasikan sebagai komponen Android native berdasarkan file Figma. Asset sumber yang tersedia di project—termasuk bola, launcher, dan model MediaPipe—tetap dipakai. Ilustrasi kompleks yang tidak tersedia sebagai file binary di ZIP dibentuk ulang sebagai drawable XML/custom Canvas agar UI tetap responsif dan tidak bergantung pada screenshot.

## OTP lokal

Karena tidak ada backend/email server, alur lupa password menggunakan OTP simulasi `2468`. Kode juga ditampilkan sebagai Toast agar alur dapat diuji sepenuhnya secara offline.

## Batasan akurasi

Scoring merupakan rule-based layer di atas landmark MediaPipe, dengan aturan berbeda untuk throw-in, passing, dribbling, kontrol bola, heading, shooting, free kick, dan penalti. Nilainya perlu dikalibrasi lagi menggunakan dataset video lapangan sebelum dipakai untuk keputusan penilaian formal.
