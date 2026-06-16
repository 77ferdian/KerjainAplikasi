package com.kerjain.app.model

import java.io.Serializable

data class User(
    var id: String = "",
    var namaLengkap: String = "",
    var email: String = "",
    var password: String = "",
    var role: String = "", // "pelajar", "mentor", "mitra"
    var fotoProfil: String? = null,
    
    // Pelajar fields
    var universitas: String? = null,
    var jurusan: String? = null,
    var angkatan: Int? = null,
    var bio: String? = null,
    var totalPoin: Int = 0,
    
    // Mitra fields
    var namaUsaha: String? = null,
    var bidangUsaha: String? = null,
    var kota: String? = null,
    var kontakBisnis: String? = null,
    var emailDomain: String? = null,
    var logoPerusahaan: String? = null,
    var website: String? = null,
    
    // Mentor fields
    var profesi: String? = null,
    var perusahaan: String? = null,
    var tahunPengalaman: Int = 0,
    var mitraId: String? = null
) : Serializable

data class Program(
    var id: String = "",
    var mitraId: String = "",
    var mitraNama: String = "",
    var judul: String = "",
    var deskripsi: String = "",
    var bidang: String = "", // UI/UX, Backend, Android, dll
    var status: String = "draft", // "draft", "published", "closed"
    var kuota: Int? = null,
    var tanggalMulai: String = "",
    var tanggalSelesai: String = ""
) : Serializable

data class Task(
    var id: String = "",
    var programId: String = "",
    var judul: String = "",
    var deskripsi: String = "",
    var urutan: Int = 1,
    var deadline: String = ""
) : Serializable

data class Enrollment(
    var id: String = "",
    var pelajarId: String = "",
    var pelajarNama: String = "",
    var programId: String = "",
    var programJudul: String = "",
    var status: String = "aktif", // "aktif", "selesai", "dibatalkan", "ditolak"
    var enrolledAt: Long = 0,
    var selesaiAt: Long? = null
) : Serializable

data class Submission(
    var id: String = "",
    var enrollmentId: String = "",
    var taskId: String = "",
    var taskJudul: String = "",
    var pelajarId: String = "",
    var pelajarNama: String = "",
    var fileUrl: String = "",
    var catatan: String = "",
    var status: String = "menunggu", // "menunggu", "disetujui", "revisi"
    var feedback: String? = null,
    var nilai: Int? = null,
    var reviewedBy: String? = null, // ID Mentor
    var reviewedAt: Long? = null
) : Serializable

data class Certificate(
    var id: String = "",
    var enrollmentId: String = "",
    var pelajarId: String = "",
    var nomorSertifikat: String = "",
    var issuedAt: Long = 0,
    var programJudul: String = "",
    var pelajarNama: String = "",
    var mitraNama: String = ""
) : Serializable

data class Notification(
    var id: String = "",
    var userId: String = "",
    var type: String = "", // "submission_approved", "new_feedback", etc
    var message: String = "",
    var read: Boolean = false,
    var createdAt: Long = 0
) : Serializable

data class Message(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var senderNama: String = "",
    var isi: String = "",
    var createdAt: Long = 0
) : Serializable
