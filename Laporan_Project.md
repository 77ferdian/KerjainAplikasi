# Laporan Pengembangan Aplikasi: KerjainAplikasi

Laporan ini merinci aspek teknis pengembangan aplikasi KerjainAplikasi, mencakup struktur proyek, implementasi antarmuka, fungsionalitas, dan manajemen basis data.

## 3.3 Struktur Project

Aplikasi KerjainAplikasi dikembangkan menggunakan bahasa pemrograman **Kotlin** dengan mengikuti pola arsitektur **MVVM (Model-View-ViewModel)** yang dikombinasikan dengan **Repository Pattern**. Struktur paket (`package`) disusun secara logis untuk memisahkan tanggung jawab antar komponen:

### 1. Model Data (`com.kerjain.app.model`)
Berisi entitas data utama yang digunakan dalam aplikasi. Semua model menggunakan `data class` Kotlin yang mendukung serialisasi untuk komunikasi antar komponen:
*   `User`: Menyimpan informasi profil untuk Pelajar, Mentor, dan Mitra.
*   `Program`: Representasi program magang atau pelatihan.
*   `Task`: Detail tugas dalam suatu program.
*   `Enrollment`: Data pendaftaran pelajar ke dalam program.
*   `Submission`: Data pengumpulan tugas oleh pelajar.
*   `Certificate`: Informasi sertifikat digital yang diterbitkan.
*   `Notification` & `Message`: Untuk sistem notifikasi dan komunikasi.

### 2. Repository (`com.kerjain.app.repository`)
Komponen **`KerjainRepository`** bertindak sebagai perantara tunggal (*Single Source of Truth*) antara UI dan sumber data. Repository menangani logika pemilihan data dari Firebase Firestore atau data lokal (*Mock Data*) saat perangkat dalam kondisi offline.

### 3. User Interface (`com.kerjain.app.ui`)
UI disusun secara modular berdasarkan peran pengguna:
*   **`ui.auth`**: Fragmen untuk Login dan Registrasi.
*   **`ui.pelajar`**: Fitur khusus pelajar (Explore program, Progress belajar).
*   **`ui.mentor`**: Fitur penilaian tugas dan monitoring pelajar.
*   **`ui.mitra`**: Fitur manajemen program dan pencarian bakat (*Talent Pool*).
*   **`Adapters`**: Kumpulan `RecyclerView.Adapter` untuk menyajikan daftar data secara dinamis.

---

## 3.4 Implementasi Antarmuka

Antarmuka pengguna KerjainAplikasi diimplementasikan menggunakan **XML Layouts** dengan standar **Material Design 3**.

### Komponen Utama:
1.  **Activity & Fragment**: `MainActivity` bertindak sebagai *host* yang mengelola navigasi antar fragmen menggunakan `FragmentContainerView`.
2.  **Navigation**: Implementasi `BottomNavigationView` yang berubah secara dinamis berdasarkan *role* pengguna (Pelajar, Mentor, atau Mitra).
3.  **Layout Manager**: Penggunaan `ConstraintLayout` untuk desain yang responsif dan `LinearLayout` (Nested) untuk form input yang kompleks.
4.  **Visual Elements**:
    *   `MaterialCardView`: Digunakan untuk memberikan efek elevasi dan radius pada kartu program dan profil.
    *   `TextInputLayout`: Memberikan validasi input yang elegan dengan gaya *OutlinedBox*.
    *   `RecyclerView`: Menampilkan daftar data secara dinamis (program, tugas, dsb) dengan performa tinggi.

---

## 3.5 Implementasi Fungsionalitas

### 3.5.1 CRUD Data

Implementasi CRUD (Create, Read, Update, Delete) dilakukan secara terpusat melalui `KerjainRepository` untuk memastikan konsistensi data.

1.  **Create (C)**:
    *   **User Registration**: Menyimpan kredensial dan profil awal ke Firestore.
    *   **Program Creation**: Mitra dapat membuat program baru beserta daftar tugas terkait dalam satu operasi batch.
    *   **Task Submission**: Pelajar mengirimkan hasil pengerjaan tugas melalui fungsi `submitTask()`.

2.  **Read (R)**:
    *   **Real-time Fetching**: Menggunakan `addSnapshotListener` untuk pembaruan data instan (program, notifikasi, status tugas).
    *   **Role-based Filtering**: Data difilter agar relevan dengan peran pengguna yang aktif.

3.  **Update (U)**:
    *   **Review Process**: Mentor memberikan nilai dan feedback yang memicu pembaruan status pengerjaan dan poin pelajar.
    *   **Profile Management**: Pengguna dapat memperbarui informasi profil mereka.

4.  **Delete (D)**:
    *   **Program Deletion**: Mitra memiliki otoritas untuk menghapus program dari database.

### 3.5.2 Database

Aplikasi menggunakan **Google Firebase Firestore** sebagai basis data utama. Firestore dipilih karena kemampuannya dalam sinkronisasi data real-time dan skalabilitasnya.

#### Struktur Koleksi Firestore:
*   **`users`**: Dokumen profil pengguna.
*   **`programs`**: Detail program magang/pelatihan.
*   **`tasks`**: Daftar tugas yang terkait dengan program.
*   **`enrollments`**: Data pendaftaran pelajar.
*   **`submissions`**: Pengumpulan tugas dan status penilaian.
*   **`notifications`**: Log notifikasi sistem untuk tiap user.

#### Mekanisme Fallback:
Repositori dilengkapi dengan **In-Memory Mock Database**. Jika Firestore tidak tersedia, aplikasi akan memuat data awal (*seeded data*) agar fungsionalitas UI tetap dapat diuji dalam lingkungan pengembangan.

### 3.5.3 Session/Login State

Manajemen sesi pengguna dalam aplikasi diimplementasikan untuk memastikan pengalaman pengguna yang mulus tanpa perlu login berulang kali (*persistent login*).

*   **Penyimpanan Kredensial**: Aplikasi menggunakan **`SharedPreferences`** (`kerjain_prefs`) untuk menyimpan identitas pengguna yang berhasil login secara lokal (menggunakan field `saved_email`).
*   **Persistent Session**: Pada saat `MainActivity` dimulai, aplikasi memanggil fungsi `loadSession()` pada `KerjainRepository`. Fungsi ini akan memeriksa apakah ada email yang tersimpan, jika ada, aplikasi akan otomatis memvalidasi ke Firestore/Mock data dan memulihkan objek `currentUser`.
*   **State Management**: `KerjainRepository` menyimpan objek `currentUser` di memori selama aplikasi berjalan. Seluruh komponen UI merujuk ke objek ini untuk menentukan akses fitur dan data yang relevan.
*   **Logout Mechanism**: Fungsi `logout()` akan menghapus data email dari `SharedPreferences` dan mengosongkan objek `currentUser`, sehingga pengguna akan diarahkan kembali ke halaman login.

### 3.5.4 API/Firebase

Aplikasi KerjainAplikasi mengandalkan ekosistem **Google Firebase** sebagai pengganti API backend tradisional, yang memungkinkan sinkronisasi data secara *real-time* dan manajemen infrastruktur yang efisien.

*   **Firebase Cloud Firestore**: Digunakan sebagai database NoSQL utama. Implementasi mencakup penggunaan:
    *   **Queries**: Pengambilan data terfilter (misal: `whereEqualTo("role", "pelajar")`).
    *   **Listeners**: Penggunaan `addSnapshotListener` yang memungkinkan aplikasi menerima pembaruan data secara otomatis tanpa *request* ulang (misal: notifikasi baru atau perubahan status tugas).
    *   **Batch Writes**: Digunakan untuk menyimpan beberapa dokumen sekaligus (seperti Program dan daftar Tasks-nya) untuk menjamin integritas data (atomisitas).
*   **Firebase Authentication** (Conceptual Integration): Meskipun aplikasi saat ini menggunakan manajemen profil kustom di Firestore untuk mendukung mode *Mock Data*, infrastruktur telah disiapkan untuk beralih sepenuhnya ke Firebase Auth guna meningkatkan keamanan enkripsi kata sandi.
*   **Firebase Storage**: Digunakan untuk mengelola penyimpanan file biner seperti foto profil pengguna dan file tugas yang diunggah oleh pelajar.
*   **Offline Persistence**: Firebase Firestore dikonfigurasi untuk menyimpan *cache* data lokal secara otomatis, sehingga pengguna tetap dapat melihat data yang sebelumnya telah dimuat meskipun koneksi internet terputus.
