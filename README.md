# KerjainAplikasi

KerjainAplikasi adalah platform Android yang menghubungkan mahasiswa, mentor, dan mitra industri dalam satu ekosistem pembelajaran, penilaian, dan rekrutmen berbasis kinerja. Aplikasi ini dirancang untuk mendukung proses belajar, evaluasi tugas, serta pengelolaan program magang dan pelatihan secara lebih terstruktur dan transparan.

## Gambaran Umum

Aplikasi ini dibuat untuk menjembatani kebutuhan tiga pihak utama:

- Mahasiswa dapat mengeksplorasi program, mengirim tugas, memantau progres belajar, dan memperoleh sertifikat digital.
- Mentor dapat menilai tugas, memantau perkembangan peserta, dan memberikan umpan balik yang konstruktif.
- Mitra industri dapat mengelola program, menyeleksi kandidat, dan memantau efektivitas program yang dijalankan.

## Fitur Utama

### Mahasiswa
- Eksplorasi program magang dan pelatihan
- Pelacakan progres belajar dan pencapaian tugas
- Pengiriman tugas dan portofolio
- Penerimaan sertifikat digital setelah menyelesaikan program

### Mentor
- Dashboard penilaian tugas
- Monitoring perkembangan peserta
- Sistem pemberian feedback yang terstruktur

### Mitra
- Manajemen program dan pendaftaran
- Akses ke talent pool berbasis performa
- Analisis efektivitas program secara lebih objektif

## Tech Stack

- Android
- Kotlin
- Jetpack Components
- Material Design 3
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- MVVM Architecture
- Repository Pattern
- Gradle

## Arsitektur dan Teknologi

Aplikasi ini dibangun dengan pendekatan modern untuk memastikan performa, keamanan, dan kemudahan pengembangan:

- Bahasa pemrograman: Kotlin
- Arsitektur: MVVM + Repository Pattern
- Database: Cloud Firestore
- Autentikasi: Firebase Authentication
- Penyimpanan file: Firebase Storage
- UI: Material Design 3
- Komponen pendukung: Lifecycle, Coroutines, Flow, Navigation

## Struktur Proyek

```text
KerjainAplikasi/
├── app/
│   ├── src/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle.properties
├── .gitignore
└── README.md
```

## Persyaratan

- Android Studio terbaru
- JDK 11 atau lebih tinggi
- Emulator atau perangkat Android fisik
- Akun Firebase aktif

## Cara Menjalankan

1. Clone repository ini:

```bash
git clone https://github.com/77ferdian/KerjainAplikasi.git
```

2. Masuk ke direktori proyek:

```bash
cd KerjainAplikasi
```

3. Tambahkan file konfigurasi Firebase:
   - Salin file `google-services.json` ke folder `app/`
   - Pastikan Firebase Authentication, Firestore, dan Storage sudah aktif di Firebase Console

4. Buka proyek di Android Studio.

5. Sinkronkan Gradle dan jalankan aplikasi di emulator atau perangkat Android.

## Kontribusi

Kontribusi sangat terbuka untuk pengembangan lebih lanjut, termasuk perbaikan bug, peningkatan UI/UX, optimasi performa, dan pengembangan fitur baru. Silakan fork repository ini lalu buat pull request dengan deskripsi yang jelas.

## Tim Pengembang

| Nama | NPM | Peran |
| --- | --- | --- |
| Agil Adiwidya Nurfadillah | 24082010199 | Developer |
| Asti Sofiana Anastasia | 24082010207 | Developer |
| Ferdian | 24082010213 | Developer |

## Status Proyek

Proyek ini dikembangkan sebagai bagian dari upaya penguatan kolaborasi antara dunia pendidikan dan industri dalam menciptakan ekosistem pembelajaran yang lebih relevan dan aplikatif.
