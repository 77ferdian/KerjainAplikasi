# KerjainAplikasi 🚀

**KerjainAplikasi** adalah platform Android yang dirancang untuk menghubungkan Pelajar, Mentor, dan Mitra dalam satu ekosistem pembelajaran dan rekrutmen yang terintegrasi. Aplikasi ini memfasilitasi manajemen tugas, pemantauan progres pelajar, serta pencarian bakat bagi mitra industri.

## 🌟 Fitur Utama

Aplikasi ini memiliki tiga peran utama dengan fitur yang disesuaikan:

### 1. Pelajar (Student)
*   **Home Dashboard**: Melihat ringkasan aktivitas dan tugas terbaru.
*   **Explore**: Menjelajahi program dan peluang yang tersedia dari Mitra.
*   **Progress Tracking**: Memantau perkembangan pembelajaran dan status penyelesaian tugas.
*   **Profile**: Mengelola data diri dan portofolio.

### 2. Mentor
*   **Review Dashboard**: Mengelola antrean tugas yang dikirimkan oleh pelajar untuk dinilai.
*   **Student Monitoring**: Melihat progres belajar dari pelajar yang dibimbing.
*   **Feedback System**: Memberikan penilaian dan masukan langsung pada tugas pelajar.

### 3. Mitra (Partner)
*   **Program Management**: Membuat dan mengelola program kerja atau magang.
*   **Talent Pool**: Menemukan kandidat pelajar terbaik berdasarkan performa dan keahlian mereka.
*   **Analytics**: Memantau perkembangan program yang sedang berjalan.

## 🛠️ Teknologi yang Digunakan

*   **Bahasa Pemrograman**: Kotlin
*   **Arsitektur**: MVVM (Model-View-ViewModel) / Repository Pattern
*   **UI Framework**: Material Design Components, Jetpack Compose (jika ada), XML Layouts
*   **Database & Backend**: 
    *   Firebase Authentication (Keamanan Akun)
    *   Cloud Firestore (Database NoSQL Real-time)
    *   Firebase Storage (Penyimpanan File)
*   **Libraries**:
    *   Jetpack Navigation
    *   RecyclerView & ConstraintLayout
    *   Kotlin Coroutines & Flow

## 🚀 Cara Menjalankan Project

1.  **Clone Repository**
    ```bash
    git clone https://github.com/77ferdian/KerjainAplikasi.git
    ```
2.  **Buka di Android Studio**
    Pastikan menggunakan versi Android Studio terbaru (Ladybug atau lebih baru).
3.  **Setup Firebase**
    *   Buat project baru di [Firebase Console](https://console.firebase.google.com/).
    *   Tambahkan aplikasi Android dengan package name `com.kerjain.app` (atau sesuaikan dengan `build.gradle`).
    *   Unduh file `google-services.json` dan letakkan di folder `app/`.
    *   Aktifkan Authentication (Email/Password), Firestore Database, dan Storage.
4.  **Build & Run**
    Tunggu proses Gradle Sync selesai, lalu jalankan aplikasi di Emulator atau Perangkat Fisik.

## 🤝 Kontribusi

Kontribusi selalu terbuka! Silakan lakukan fork pada repository ini dan ajukan pull request jika ingin menambahkan fitur atau memperbaiki bug.

---
Dibuat dengan ❤️ untuk kemajuan pendidikan dan kolaborasi industri.
