package com.kerjain.app.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.kerjain.app.model.*
import java.util.UUID
import android.net.Uri

class KerjainRepository private constructor(context: Context) {

    private val sharedPrefs = context.getSharedPreferences("kerjain_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    
    // Fallback Mock Database (in-memory)
    private var mockUsers = mutableListOf<User>()
    private var mockPrograms = mutableListOf<Program>()
    private var mockTasks = mutableListOf<Task>()
    private var mockEnrollments = mutableListOf<Enrollment>()
    private var mockSubmissions = mutableListOf<Submission>()
    private var mockCertificates = mutableListOf<Certificate>()
    private var mockNotifications = mutableListOf<Notification>()
    private var mockMessages = mutableListOf<Message>()

    var currentUser: User? = null
        private set

    init {
        setupMockData()
    }

    companion object {
        private const val TAG = "KerjainRepository"
        @Volatile
        private var instance: KerjainRepository? = null

        fun getInstance(context: Context): KerjainRepository {
            return instance ?: synchronized(this) {
                instance ?: KerjainRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun setupMockData() {
        // Dummy Mitra
        val mitra = User(
            id = "mitra_goto",
            namaLengkap = "Dewi Lestari",
            email = "mitra@kerjain.com",
            password = "password123",
            role = "mitra",
            namaUsaha = "GoTo Group",
            bidangUsaha = "Teknologi & Transportasi",
            kota = "Jakarta",
            kontakBisnis = "hrd@goto.com",
            emailDomain = "goto.com",
            logoPerusahaan = "goto_logo",
            website = "www.gotocompany.com"
        )
        
        // Dummy Mentor
        val mentor = User(
            id = "mentor_andi",
            namaLengkap = "Andi Wijaya",
            email = "mentor@kerjain.com",
            password = "password123",
            role = "mentor",
            profesi = "Senior Mobile Engineer",
            perusahaan = "GoTo Group",
            tahunPengalaman = 5,
            bio = "Spesialis Android Jetpack dan Clean Architecture.",
            mitraId = "mitra_goto"
        )

        // Dummy Pelajar
        val pelajar = User(
            id = "pelajar_budi",
            namaLengkap = "Budi Santoso",
            email = "pelajar@kerjain.com",
            password = "password123",
            role = "pelajar",
            universitas = "Universitas Indonesia",
            jurusan = "Teknik Informatika",
            angkatan = 2023,
            bio = "Mahasiswa semester 6 yang antusias dengan mobile app development.",
            totalPoin = 250
        )

        mockUsers.addAll(listOf(mitra, mentor, pelajar))

        // Dummy Programs
        val prog1 = Program(
            id = "prog_android",
            mitraId = "mitra_goto",
            mitraNama = "GoTo Group",
            judul = "Android Developer Internship",
            deskripsi = "Program magang intensif untuk membangun aplikasi mobile berskala besar menggunakan Kotlin. Peserta akan mempelajari arsitektur MVVM, Firebase integration, dan best practices testing.",
            bidang = "Android",
            status = "published",
            kuota = 20,
            tanggalMulai = "2026-07-01",
            tanggalSelesai = "2026-09-30"
        )
        
        val prog2 = Program(
            id = "prog_uiux",
            mitraId = "mitra_goto",
            mitraNama = "GoTo Group",
            judul = "UI/UX Designer Apprentice",
            deskripsi = "Belajar mendesain antarmuka pengguna yang menarik dan intuitif. Fokus pada pembuatan wireframe, prototyping di Figma, user research, dan usability testing.",
            bidang = "UI/UX",
            status = "published",
            kuota = 15,
            tanggalMulai = "2026-07-10",
            tanggalSelesai = "2026-10-10"
        )

        val prog3 = Program(
            id = "prog_backend",
            mitraId = "mitra_goto",
            mitraNama = "GoTo Group",
            judul = "Backend Engineer Java",
            deskripsi = "Kembangkan RESTful API yang aman dan scalable menggunakan Spring Boot dan PostgreSQL. Program mencakup caching, messaging queue, dan containerization dengan Docker.",
            bidang = "Backend",
            status = "draft",
            kuota = 10,
            tanggalMulai = "2026-08-01",
            tanggalSelesai = "2026-11-01"
        )

        mockPrograms.addAll(listOf(prog1, prog2, prog3))

        // Dummy Tasks for Android
        val t1 = Task(
            id = "task_and_1",
            programId = "prog_android",
            judul = "Setup Project & Git",
            deskripsi = "Inisialisasi project Android baru menggunakan Android Studio, konfigurasikan Kotlin DSL, tambahkan Git, dan unggah ke repositori GitHub privat. Kumpulkan tautan repositori Anda.",
            urutan = 1,
            deadline = "2026-07-07 23:59"
        )
        val t2 = Task(
            id = "task_and_2",
            programId = "prog_android",
            judul = "Slicing UI Login & Register",
            deskripsi = "Implementasikan antarmuka pendaftaran dan masuk menggunakan XML/Compose dengan desain responsif. Pastikan validasi form (email & password) bekerja dengan baik.",
            urutan = 2,
            deadline = "2026-07-14 23:59"
        )

        mockTasks.addAll(listOf(t1, t2))

        // Dummy Enrollment (Agar Mentor ada Pelajar Aktif)
        val e1 = Enrollment(
            id = "enroll_budi_android",
            pelajarId = "pelajar_budi",
            pelajarNama = "Budi Santoso",
            programId = "prog_android",
            programJudul = "Android Developer Internship",
            status = "aktif",
            enrolledAt = System.currentTimeMillis()
        )
        mockEnrollments.add(e1)

        // Dummy Submission (Agar Mentor ada Tugas untuk dinilai)
        val s1 = Submission(
            id = "sub_budi_1",
            enrollmentId = "enroll_budi_android",
            taskId = "task_and_1",
            taskJudul = "Setup Project & Git",
            pelajarId = "pelajar_budi",
            pelajarNama = "Budi Santoso",
            fileUrl = "https://github.com/budi/kerjain-task1",
            catatan = "Sudah saya push kodenya pak. Mohon direview.",
            status = "menunggu"
        )
        mockSubmissions.add(s1)

        // Initial Notification
        mockNotifications.add(Notification(
            id = "notif_welcome",
            userId = "pelajar_budi",
            type = "welcome",
            message = "Selamat datang di Kerjain! Mulailah mencari program magang impian Anda.",
            read = false,
            createdAt = System.currentTimeMillis()
        ))
    }

    fun seedFirestoreIfNeeded(callback: (Boolean) -> Unit) {
        firestore.collection("users").limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val batch = firestore.batch()

                    mockUsers.forEach { user ->
                        val docRef = firestore.collection("users").document(user.id)
                        batch.set(docRef, user)
                    }

                    mockPrograms.forEach { program ->
                        val docRef = firestore.collection("programs").document(program.id)
                        batch.set(docRef, program)
                    }

                    mockTasks.forEach { task ->
                        val docRef = firestore.collection("tasks").document(task.id)
                        batch.set(docRef, task)
                    }

                    mockEnrollments.forEach { enrollment ->
                        val docRef = firestore.collection("enrollments").document(enrollment.id)
                        batch.set(docRef, enrollment)
                    }

                    mockSubmissions.forEach { submission ->
                        val docRef = firestore.collection("submissions").document(submission.id)
                        batch.set(docRef, submission)
                    }

                    mockNotifications.forEach { notification ->
                        val docRef = firestore.collection("notifications").document(notification.id)
                        batch.set(docRef, notification)
                    }

                    batch.commit().addOnSuccessListener {
                        callback(true)
                    }.addOnFailureListener {
                        callback(false)
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // ==========================================
    // AUTH & USER APIs
    // ==========================================

    fun loadSession(callback: (User?) -> Unit) {
        val savedEmail = sharedPrefs.getString("saved_email", null)
        if (savedEmail != null) {
            firestore.collection("users")
                .whereEqualTo("email", savedEmail)
                .get()
                .addOnSuccessListener { snapshot ->
                    val user = if (!snapshot.isEmpty) {
                        snapshot.toObjects(User::class.java).firstOrNull()
                    } else {
                        mockUsers.find { it.email == savedEmail }
                    }
                    currentUser = user
                    callback(user)
                }
                .addOnFailureListener {
                    val mockUser = mockUsers.find { it.email == savedEmail }
                    currentUser = mockUser
                    callback(mockUser)
                }
        } else {
            callback(null)
        }
    }

    fun login(email: String, pass: String, callback: (Boolean, User?, String?) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get(Source.DEFAULT)
            .addOnSuccessListener { snapshot ->
                val user = if (!snapshot.isEmpty) {
                    snapshot.toObjects(User::class.java).find { it.password == pass }
                } else {
                    mockUsers.find { it.email == email && it.password == pass }
                }

                if (user != null) {
                    currentUser = user
                    sharedPrefs.edit().putString("saved_email", email).apply()
                    callback(true, user, null)
                } else {
                    callback(false, null, "Email atau kata sandi salah")
                }
            }
            .addOnFailureListener {
                // Offline/Network error - try local cache
                val user = mockUsers.find { it.email == email && it.password == pass }
                if (user != null) {
                    currentUser = user
                    sharedPrefs.edit().putString("saved_email", email).apply()
                    callback(true, user, null)
                } else {
                    callback(false, null, "Terjadi kesalahan jaringan.")
                }
            }
    }

    fun logout() {
        currentUser = null
        sharedPrefs.edit().remove("saved_email").apply()
    }

    fun register(user: User, callback: (Boolean, String?) -> Unit) {
        val newId = UUID.randomUUID().toString()
        user.id = newId

        // Save to Firestore
        firestore.collection("users").document(newId).set(user)
            .addOnSuccessListener {
                mockUsers.add(user) // Update local mock cache too
                currentUser = user
                sharedPrefs.edit().putString("saved_email", user.email).apply()
                sendNotification(
                    userId = newId,
                    type = "welcome",
                    message = "Selamat datang di Kerjain! Silakan lengkapi profil Anda."
                )
                callback(true, null)
            }
            .addOnFailureListener {
                // If it fails, save it locally in mock data
                mockUsers.add(user)
                currentUser = user
                sharedPrefs.edit().putString("saved_email", user.email).apply()
                sendNotification(
                    userId = newId,
                    type = "welcome",
                    message = "Selamat datang di Kerjain secara lokal!"
                )
                Log.d(TAG, "Registration fallback to Local Mock: ${user.namaLengkap}")
                callback(true, "Terdaftar lokal (offline/no firestore permission)")
            }
    }

    // ==========================================
    // PROGRAM & TASK APIs
    // ==========================================

    fun getPrograms(callback: (List<Program>) -> Unit) {
        firestore.collection("programs")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Program::class.java).toMutableList()
                    // Sync with local mock data (for newly created programs in this session)
                    for (mp in mockPrograms) {
                        if (list.none { it.id == mp.id }) list.add(mp)
                    }
                    
                    // Enrich with latest Mitra names from 'users' collection to ensure consistency
                    enrichProgramsWithLatestMitraNames(list, callback)
                } else {
                    enrichProgramsWithLatestMitraNames(mockPrograms, callback)
                }
            }
    }

    private fun enrichProgramsWithLatestMitraNames(programs: List<Program>, callback: (List<Program>) -> Unit) {
        if (programs.isEmpty()) {
            callback(programs)
            return
        }

        firestore.collection("users")
            .whereEqualTo("role", "mitra")
            .get()
            .addOnSuccessListener { userSnapshot ->
                val mitraNames = userSnapshot.documents.associate { 
                    it.id to (it.getString("namaUsaha") ?: it.getString("namaLengkap") ?: "")
                }
                
                // Also check mockUsers for local changes
                val mockMitraNames = mockUsers.filter { it.role == "mitra" }.associate {
                    it.id to (it.namaUsaha ?: it.namaLengkap)
                }

                programs.forEach { p ->
                    // Priority: Firestore users > Mock users > existing field
                    val latestName = mitraNames[p.mitraId] ?: mockMitraNames[p.mitraId]
                    if (!latestName.isNullOrEmpty()) {
                        p.mitraNama = latestName
                    }
                }
                callback(programs)
            }
            .addOnFailureListener {
                // Fallback to mockUsers enrichment even if Firestore users fetch fails
                val mockMitraNames = mockUsers.filter { it.role == "mitra" }.associate {
                    it.id to (it.namaUsaha ?: it.namaLengkap)
                }
                programs.forEach { p ->
                    mockMitraNames[p.mitraId]?.let { if (it.isNotEmpty()) p.mitraNama = it }
                }
                callback(programs)
            }
    }

    fun getProgramsByMitra(mitraId: String, callback: (List<Program>) -> Unit) {
        firestore.collection("programs")
            .whereEqualTo("mitraId", mitraId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Program::class.java).toMutableList()
                    for (mp in mockPrograms.filter { it.mitraId == mitraId }) {
                        if (list.none { it.id == mp.id }) list.add(mp)
                    }
                    enrichProgramsWithLatestMitraNames(list, callback)
                } else {
                    enrichProgramsWithLatestMitraNames(mockPrograms.filter { it.mitraId == mitraId }, callback)
                }
            }
    }

    fun createProgram(program: Program, tasks: List<Task>, callback: (Boolean) -> Unit) {
        val id = if (program.id.isEmpty()) "prog_${UUID.randomUUID()}" else program.id
        program.id = id

        val batch = firestore.batch()
        batch.set(firestore.collection("programs").document(id), program)

        tasks.forEach { task ->
            if (task.id.isEmpty()) task.id = "task_${UUID.randomUUID()}"
            task.programId = id
            batch.set(firestore.collection("tasks").document(task.id), task)
        }

        batch.commit()
            .addOnSuccessListener {
                if (mockPrograms.none { it.id == program.id }) {
                    mockPrograms.add(program)
                } else {
                    val idx = mockPrograms.indexOfFirst { it.id == program.id }
                    if (idx != -1) mockPrograms[idx] = program
                }

                tasks.forEach { t ->
                    if (mockTasks.none { it.id == t.id }) mockTasks.add(t)
                }
                callback(true)
            }
            .addOnFailureListener {
                if (mockPrograms.none { it.id == program.id }) {
                    mockPrograms.add(program)
                }
                callback(true) // Optimistic offline
            }
    }

    fun deleteProgram(id: String, callback: (Boolean) -> Unit) {
        firestore.collection("programs").document(id).delete()
            .addOnSuccessListener {
                mockPrograms.removeAll { it.id == id }
                callback(true)
            }
            .addOnFailureListener {
                mockPrograms.removeAll { it.id == id }
                callback(true)
            }
    }

    fun updateProgram(program: Program, callback: (Boolean) -> Unit) {
        firestore.collection("programs").document(program.id).set(program)
            .addOnSuccessListener {
                val idx = mockPrograms.indexOfFirst { it.id == program.id }
                if (idx != -1) mockPrograms[idx] = program
                callback(true)
            }
            .addOnFailureListener {
                val idx = mockPrograms.indexOfFirst { it.id == program.id }
                if (idx != -1) mockPrograms[idx] = program
                callback(true)
            }
    }

    fun getTasks(programId: String, callback: (List<Task>) -> Unit) {
        firestore.collection("tasks")
            .whereEqualTo("programId", programId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Task::class.java).toMutableList()
                    val localTasks = mockTasks.filter { it.programId == programId }
                    for (lt in localTasks) {
                        val idx = list.indexOfFirst { it.id == lt.id }
                        if (idx != -1) {
                            list[idx] = lt
                        } else {
                            list.add(lt)
                        }
                    }
                    callback(list.sortedBy { it.urutan })
                } else {
                    callback(mockTasks.filter { it.programId == programId }.sortedBy { it.urutan })
                }
            }
    }

    fun addTask(task: Task, callback: (Boolean) -> Unit) {
        val id = "task_${UUID.randomUUID()}"
        task.id = id
        firestore.collection("tasks").document(id).set(task)
            .addOnSuccessListener {
                mockTasks.add(task)
                callback(true)
            }
            .addOnFailureListener {
                mockTasks.add(task)
                callback(true)
            }
    }

    // ==========================================
    // ENROLLMENT APIs
    // ==========================================

    fun enrollInProgram(pelajarId: String, pelajarNama: String, program: Program, callback: (Boolean) -> Unit) {
        val enrollId = "enroll_${pelajarId}_${program.id}"
        val enrollment = Enrollment(
            id = enrollId,
            pelajarId = pelajarId,
            pelajarNama = pelajarNama,
            programId = program.id,
            programJudul = program.judul,
            status = "aktif",
            enrolledAt = System.currentTimeMillis()
        )

        firestore.collection("enrollments").document(enrollId).set(enrollment)
            .addOnSuccessListener {
                mockEnrollments.add(enrollment)
                
                // Send system notification for new enrollment to Student
                sendNotification(
                    userId = pelajarId,
                    type = "program_enrolled",
                    message = "Anda berhasil mendaftar pada program '${program.judul}'."
                )

                // Send system notification for new enrollment to Mitra
                sendNotification(
                    userId = program.mitraId,
                    type = "new_enrollment",
                    message = "$pelajarNama mendaftar pada program '${program.judul}'."
                )

                // Notify Mentors of this Mitra
                notifyMentors(program.mitraId, "new_enrollment", 
                    "$pelajarNama mendaftar pada program '${program.judul}'.")

                callback(true)
            }
            .addOnFailureListener {
                mockEnrollments.add(enrollment)
                callback(true)
            }
    }

    fun getEnrollments(userId: String, role: String, callback: (List<Enrollment>) -> Unit) {
        val queryField = if (role == "pelajar") "pelajarId" else "programId" // simplified for mentor/mitra
        
        if (role == "pelajar") {
            firestore.collection("enrollments")
                .whereEqualTo("pelajarId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        val list = snapshot.toObjects(Enrollment::class.java).toMutableList()
                        for (me in mockEnrollments.filter { it.pelajarId == userId }) {
                            if (list.none { it.id == me.id }) list.add(me)
                        }
                        callback(list)
                    } else {
                        callback(mockEnrollments.filter { it.pelajarId == userId })
                    }
                }
        } else {
            // For Mentor/Mitra: get programs first, then enrollments for those programs
            getProgramsByOwner(userId, role) { programs ->
                val programIds = programs.map { it.id }
                if (programIds.isEmpty()) {
                    callback(emptyList())
                    return@getProgramsByOwner
                }

                firestore.collection("enrollments")
                    .whereIn("programId", programIds)
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null) {
                            val list = snapshot.toObjects(Enrollment::class.java).toMutableList()
                            for (me in mockEnrollments.filter { it.programId in programIds }) {
                                if (list.none { it.id == me.id }) list.add(me)
                            }
                            callback(list)
                        } else {
                            callback(mockEnrollments.filter { it.programId in programIds })
                        }
                    }
            }
        }
    }

    private fun getProgramsByOwner(userId: String, role: String, callback: (List<Program>) -> Unit) {
        if (role == "mitra") {
            getProgramsByMitra(userId, callback)
        } else {
            // Mentor belongs to a Mitra, so get programs of that Mitra
            val mentor = currentUser?.takeIf { it.id == userId } ?: mockUsers.find { it.id == userId }
            
            if (mentor?.mitraId != null) {
                getProgramsByMitra(mentor.mitraId!!, callback)
            } else {
                // Try from Firestore directly
                firestore.collection("users").document(userId).get().addOnSuccessListener { doc ->
                    val u = doc.toObject(User::class.java)
                    if (u?.mitraId != null) {
                        getProgramsByMitra(u.mitraId!!, callback)
                    } else {
                        // Special fallback: if mentor has a company email domain, match with mitra
                        val domain = u?.email?.substringAfter("@") ?: ""
                        if (domain.isNotEmpty() && domain != "gmail.com" && domain != "gmail.com") {
                            firestore.collection("users")
                                .whereEqualTo("role", "mitra")
                                .whereEqualTo("emailDomain", domain)
                                .get()
                                .addOnSuccessListener { mSnap ->
                                    val mitra = mSnap.toObjects(User::class.java).firstOrNull()
                                    if (mitra != null) {
                                        getProgramsByMitra(mitra.id, callback)
                                    } else {
                                        callback(emptyList())
                                    }
                                }
                                .addOnFailureListener { callback(emptyList()) }
                        } else {
                            callback(emptyList())
                        }
                    }
                }.addOnFailureListener { callback(emptyList()) }
            }
        }
    }

    // ==========================================
    // SUBMISSION & REVIEW APIs
    // ==========================================

    fun submitTask(submission: Submission, callback: (Boolean) -> Unit) {
        val id = "sub_${UUID.randomUUID()}"
        submission.id = id
        submission.status = "menunggu"
        
        firestore.collection("submissions").document(id).set(submission)
            .addOnSuccessListener {
                mockSubmissions.add(submission)
                
                // Notify Mentors about new submission
                val enrollment = mockEnrollments.find { it.id == submission.enrollmentId }
                if (enrollment != null) {
                    val program = mockPrograms.find { it.id == enrollment.programId }
                    if (program != null) {
                        notifyMentors(program.mitraId, "new_submission", 
                            "${submission.pelajarNama} mengumpulkan tugas '${submission.taskJudul}'.")
                    } else {
                        firestore.collection("programs").document(enrollment.programId).get().addOnSuccessListener { pDoc ->
                            pDoc.getString("mitraId")?.let { mId ->
                                notifyMentors(mId, "new_submission", "${submission.pelajarNama} mengumpulkan tugas '${submission.taskJudul}'.")
                            }
                        }
                    }
                } else {
                    firestore.collection("enrollments").document(submission.enrollmentId).get().addOnSuccessListener { eDoc ->
                        eDoc.getString("programId")?.let { pId ->
                            firestore.collection("programs").document(pId).get().addOnSuccessListener { pDoc ->
                                pDoc.getString("mitraId")?.let { mId ->
                                    notifyMentors(mId, "new_submission", "${submission.pelajarNama} mengumpulkan tugas '${submission.taskJudul}'.")
                                }
                            }
                        }
                    }
                }
                
                callback(true)
            }
            .addOnFailureListener {
                mockSubmissions.add(submission)
                callback(true)
            }
    }

    fun getSubmissions(enrollmentId: String, callback: (List<Submission>) -> Unit) {
        firestore.collection("submissions")
            .whereEqualTo("enrollmentId", enrollmentId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Submission::class.java).toMutableList()
                    val localSubmissions = mockSubmissions.filter { it.enrollmentId == enrollmentId }
                    for (localSub in localSubmissions) {
                        val idx = list.indexOfFirst { it.id == localSub.id }
                        if (idx != -1) {
                            list[idx] = localSub
                        } else {
                            list.add(localSub)
                        }
                    }
                    callback(list)
                } else {
                    callback(mockSubmissions.filter { it.enrollmentId == enrollmentId })
                }
            }
    }

    fun getAllSubmissionsForMentorOrMitra(userId: String, role: String, callback: (List<Submission>) -> Unit) {
        firestore.collection("submissions")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Submission::class.java).toMutableList()
                    for (localSub in mockSubmissions) {
                        val idx = list.indexOfFirst { it.id == localSub.id }
                        if (idx != -1) {
                            list[idx] = localSub
                        } else {
                            list.add(localSub)
                        }
                    }
                    filterSubmissions(userId, role, list, callback)
                } else {
                    filterSubmissions(userId, role, mockSubmissions, callback)
                }
            }
    }

    private fun filterSubmissions(userId: String, role: String, allSubs: List<Submission>, callback: (List<Submission>) -> Unit) {
        getProgramsByOwner(userId, role) { programs ->
            val programIds = programs.map { it.id }
            if (programIds.isEmpty()) {
                callback(emptyList())
                return@getProgramsByOwner
            }

            // Get all enrollments for these programs to match submission's enrollmentId
            firestore.collection("enrollments").whereIn("programId", programIds).get().addOnSuccessListener { snap ->
                val enrollIds = snap.toObjects(Enrollment::class.java).map { it.id }.toMutableSet()
                // Add from mock
                enrollIds.addAll(mockEnrollments.filter { it.programId in programIds }.map { it.id })
                
                callback(allSubs.filter { it.enrollmentId in enrollIds })
            }.addOnFailureListener {
                val enrollIds = mockEnrollments.filter { it.programId in programIds }.map { it.id }
                callback(allSubs.filter { it.enrollmentId in enrollIds })
            }
        }
    }

    fun reviewSubmission(submissionId: String, status: String, feedback: String, nilai: Int, mentorId: String, callback: (Boolean) -> Unit) {
        // Query submission first
        val docRef = firestore.collection("submissions").document(submissionId)
        docRef.get().addOnSuccessListener { doc ->
            val sub = doc.toObject(Submission::class.java) ?: mockSubmissions.find { it.id == submissionId }
            if (sub != null) {
                sub.status = status
                sub.feedback = feedback
                sub.nilai = nilai
                sub.reviewedBy = mentorId
                sub.reviewedAt = System.currentTimeMillis()

                val statusText = if (status == "disetujui") "telah disetujui dengan nilai $nilai" else "memerlukan revisi"

                docRef.set(sub).addOnSuccessListener {
                    // Update mock cache
                    mockSubmissions.removeAll { it.id == submissionId }
                    mockSubmissions.add(sub)

                    // Send notification to student
                    sendNotification(
                        userId = sub.pelajarId,
                        type = "submission_reviewed",
                        message = "Tugas '${sub.taskJudul}' Anda $statusText."
                    )

                    // Send notification to mentor
                    sendNotification(
                        userId = mentorId,
                        type = "submission_reviewed",
                        message = "Penilaian tugas '${sub.taskJudul}' untuk ${sub.pelajarNama} berhasil dikirim."
                    )

                    // If status is disetujui, update points (gamification) based on the assigned grade/score
                    if (status == "disetujui") {
                        addPointsToPelajar(sub.pelajarId, nilai, "Menyelesaikan task: ${sub.taskJudul}")
                        
                        // Check if all tasks in program are finished to issue certificate!
                        checkAndIssueCertificate(sub.enrollmentId, sub.pelajarId, sub.pelajarNama)
                    }

                    callback(true)
                }.addOnFailureListener {
                    // Offline fallback
                    mockSubmissions.removeAll { it.id == submissionId }
                    mockSubmissions.add(sub)

                    // Send notification to student
                    sendNotification(
                        userId = sub.pelajarId,
                        type = "submission_reviewed",
                        message = "Tugas '${sub.taskJudul}' Anda $statusText."
                    )

                    if (status == "disetujui") {
                        addPointsToPelajar(sub.pelajarId, nilai, "Menyelesaikan task: ${sub.taskJudul}")
                        checkAndIssueCertificate(sub.enrollmentId, sub.pelajarId, sub.pelajarNama)
                    }
                    callback(true)
                }
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun addPointsToPelajar(pelajarId: String, points: Int, description: String) {
        val userRef = firestore.collection("users").document(pelajarId)
        userRef.get().addOnSuccessListener { doc ->
            val user = doc.toObject(User::class.java) ?: mockUsers.find { it.id == pelajarId }
            if (user != null) {
                user.totalPoin += points
                userRef.set(user)
                val mockIdx = mockUsers.indexOfFirst { it.id == pelajarId }
                if (mockIdx != -1) mockUsers[mockIdx] = user
            }
        }
    }

    fun checkAndIssueCertificate(enrollmentId: String, pelajarId: String, pelajarNama: String) {
        // Query enrollment
        firestore.collection("enrollments").document(enrollmentId).get().addOnCompleteListener { t ->
            val enrollment = if (t.isSuccessful) {
                t.result?.toObject(Enrollment::class.java) ?: mockEnrollments.find { it.id == enrollmentId }
            } else {
                mockEnrollments.find { it.id == enrollmentId }
            }
            
            if (enrollment != null) {
                // Find all tasks of program
                getTasks(enrollment.programId) { tasks ->
                    getSubmissions(enrollmentId) { subs ->
                        val approvedCount = subs.filter { it.status.lowercase() == "disetujui" }.map { it.taskId }.toSet().size
                        if (approvedCount >= tasks.size && tasks.isNotEmpty()) {
                            
                            // Check if certificate already exists to prevent duplicates
                            firestore.collection("certificates")
                                .whereEqualTo("enrollmentId", enrollmentId)
                                .get()
                                .addOnSuccessListener { certDocs ->
                                    val existsInFirestore = !certDocs.isEmpty
                                    val existsInMock = mockCertificates.any { it.enrollmentId == enrollmentId }
                                    
                                    if (!existsInFirestore && !existsInMock) {
                                        // Mark enrollment as selesai if it wasn't already
                                        if (enrollment.status.lowercase() != "selesai") {
                                            enrollment.status = "selesai"
                                            enrollment.selesaiAt = System.currentTimeMillis()
                                            firestore.collection("enrollments").document(enrollmentId).set(enrollment)
                                            val idx = mockEnrollments.indexOfFirst { it.id == enrollmentId }
                                            if (idx != -1) mockEnrollments[idx] = enrollment
                                        }

                                        // Issue Certificate
                                        getPrograms { progs ->
                                            val prog = progs.find { it.id == enrollment.programId }
                                            val mitraNama = prog?.mitraNama ?: "GoTo Group"
                                            val certId = "cert_${UUID.randomUUID()}"
                                            val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
                                            val certNo = "KJ-" + (1..8)
                                                .map { _ -> kotlin.random.Random.nextInt(0, charPool.size) }
                                                .map(charPool::get)
                                                .joinToString("")
                                            
                                            val certificate = Certificate(
                                                id = certId,
                                                enrollmentId = enrollmentId,
                                                pelajarId = pelajarId,
                                                nomorSertifikat = certNo,
                                                issuedAt = System.currentTimeMillis(),
                                                programJudul = enrollment.programJudul,
                                                pelajarNama = pelajarNama,
                                                mitraNama = mitraNama
                                            )
                                            
                                            firestore.collection("certificates").document(certId).set(certificate)
                                            mockCertificates.add(certificate)

                                            // Add notification for Student
                                            sendNotification(
                                                userId = pelajarId,
                                                type = "certificate_issued",
                                                message = "Selamat! Sertifikat kelulusan program '${enrollment.programJudul}' Anda telah diterbitkan."
                                            )

                                            // Notify Mentors that a student has completed the program
                                            notifyMentors(prog?.mitraId ?: "", "program_completed", 
                                                "$pelajarNama telah menyelesaikan program '${enrollment.programJudul}'.")
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // NOTIFICATION & CERTIFICATE APIs
    // ==========================================
    
    fun sendNotification(userId: String, type: String, message: String) {
        val notifId = UUID.randomUUID().toString()
        val notification = Notification(
            id = notifId,
            userId = userId,
            type = type,
            message = message,
            read = false,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("notifications").document(notifId).set(notification)
        mockNotifications.add(notification)
    }

    private fun notifyMentors(mitraId: String, type: String, message: String) {
        val mentorIds = mutableSetOf<String>()
        
        // 1. Notify Mentors from Mock Data (Instant)
        mockUsers.filter { it.role == "mentor" && it.mitraId == mitraId }.forEach { mentor ->
            if (mentorIds.add(mentor.id)) {
                sendNotification(mentor.id, type, message)
            }
        }
        
        // 2. Notify Mentors from Firestore
        firestore.collection("users")
            .whereEqualTo("role", "mentor")
            .whereEqualTo("mitraId", mitraId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.toObjects(User::class.java).forEach { mentor ->
                    if (mentorIds.add(mentor.id)) {
                        sendNotification(mentor.id, type, message)
                    }
                }
            }
    }

    fun getNotifications(userId: String, callback: (List<Notification>) -> Unit) {
        firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val fetched = snapshot.toObjects(Notification::class.java).toMutableList()
                    val localNotifs = mockNotifications.filter { it.userId == userId }
                    for (ln in localNotifs) {
                        val idx = fetched.indexOfFirst { it.id == ln.id }
                        if (idx != -1) {
                            fetched[idx] = ln
                        } else {
                            fetched.add(ln)
                        }
                    }
                    callback(fetched.sortedByDescending { it.createdAt })
                } else {
                    callback(mockNotifications.filter { it.userId == userId }.sortedByDescending { it.createdAt })
                }
            }
    }

    fun markNotificationAsRead(notifId: String, callback: (Boolean) -> Unit) {
        firestore.collection("notifications").document(notifId).update("read", true)
            .addOnSuccessListener {
                val idx = mockNotifications.indexOfFirst { it.id == notifId }
                if (idx != -1) mockNotifications[idx].read = true
                callback(true)
            }
            .addOnFailureListener {
                val idx = mockNotifications.indexOfFirst { it.id == notifId }
                if (idx != -1) mockNotifications[idx].read = true
                callback(true)
            }
    }

    fun getCertificates(pelajarId: String, callback: (List<Certificate>) -> Unit) {
        firestore.collection("users").document(pelajarId).get()
            .addOnSuccessListener { uDoc ->
                val user = uDoc.toObject(User::class.java) ?: mockUsers.find { it.id == pelajarId }
                val targetName = user?.namaLengkap ?: "Budi Santoso"
                
                firestore.collection("certificates")
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null) {
                            val list = snapshot.toObjects(Certificate::class.java).toMutableList()
                            for (mc in mockCertificates) {
                                val idx = list.indexOfFirst { it.id == mc.id }
                                if (idx != -1) {
                                    list[idx] = mc
                                } else {
                                    list.add(mc)
                                }
                            }
                            // Filter by pelajarId for accuracy, with fallback to name for older records
                            callback(list.filter { 
                                it.pelajarId == pelajarId || (it.pelajarId.isEmpty() && it.pelajarNama.lowercase() == targetName.lowercase())
                            })
                        } else {
                            callback(mockCertificates.filter { 
                                it.pelajarId == pelajarId || (it.pelajarId.isEmpty() && it.pelajarNama.lowercase() == targetName.lowercase())
                            })
                        }
                    }
            }
            .addOnFailureListener {
                val user = mockUsers.find { it.id == pelajarId }
                val targetName = user?.namaLengkap ?: "Budi Santoso"
                callback(mockCertificates.filter { 
                    it.pelajarId == pelajarId || (it.pelajarId.isEmpty() && it.pelajarNama.lowercase() == targetName.lowercase())
                })
            }
    }

    // ==========================================
    // CHAT / MESSAGING APIs
    // ==========================================

    fun getMessages(uid1: String, uid2: String, callback: (List<Message>) -> Unit) {
        // Simple chat query: all messages where sender/receiver is uid1 or uid2
        firestore.collection("messages")
            .addSnapshotListener { snapshot, error ->
                val list = if (snapshot != null) {
                    snapshot.toObjects(Message::class.java).toMutableList().apply {
                        addAll(mockMessages)
                    }
                } else {
                    mockMessages.toMutableList()
                }
                
                val filtered = list.filter {
                    (it.senderId == uid1 && it.receiverId == uid2) || (it.senderId == uid2 && it.receiverId == uid1)
                }.sortedBy { it.createdAt }
                
                callback(filtered)
            }
    }

    fun sendMessage(msg: Message, callback: (Boolean) -> Unit) {
        val id = UUID.randomUUID().toString()
        msg.id = id
        firestore.collection("messages").document(id).set(msg)
            .addOnSuccessListener {
                mockMessages.add(msg)
                callback(true)
            }
            .addOnFailureListener {
                mockMessages.add(msg)
                callback(true)
            }
    }

    fun getTalentPool(callback: (List<User>) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("role", "pelajar")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(User::class.java).toMutableList()
                    for (mu in mockUsers.filter { it.role == "pelajar" }) {
                        if (list.none { it.id == mu.id }) list.add(mu)
                    }
                    callback(list)
                } else {
                    callback(mockUsers.filter { it.role == "pelajar" })
                }
            }
    }

    fun updateUserProfile(user: User, callback: (Boolean) -> Unit) {
        firestore.collection("users").document(user.id).set(user)
            .addOnSuccessListener {
                val idx = mockUsers.indexOfFirst { it.id == user.id }
                if (idx != -1) mockUsers[idx] = user
                if (currentUser?.id == user.id) currentUser = user

                // If Mitra changes their company name, sync it with their programs
                if (user.role == "mitra") {
                    syncMitraNameToPrograms(user.id, user.namaUsaha ?: user.namaLengkap)
                }
                
                callback(true)
            }
            .addOnFailureListener {
                val idx = mockUsers.indexOfFirst { it.id == user.id }
                if (idx != -1) mockUsers[idx] = user
                if (currentUser?.id == user.id) currentUser = user
                callback(true)
            }
    }

    private fun syncMitraNameToPrograms(mitraId: String, newName: String) {
        firestore.collection("programs")
            .whereEqualTo("mitraId", mitraId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "mitraNama", newName)
                }
                batch.commit()
                
                // Update local mock data as well
                mockPrograms.forEach { 
                    if (it.mitraId == mitraId) it.mitraNama = newName
                }
            }
    }

    fun uploadProfilePhoto(userId: String, uri: Uri, callback: (Boolean, String?) -> Unit) {
        // Simplified: in real app, use Firebase Storage
        // For now, we return the local path as the "URL"
        callback(true, uri.toString())
    }

    fun changePassword(old: String, new: String, callback: (Boolean, String) -> Unit) {
        val user = currentUser ?: run {
            callback(false, "User session not found")
            return
        }

        if (user.password != old) {
            callback(false, "Kata sandi lama salah")
            return
        }

        user.password = new
        firestore.collection("users").document(user.id).set(user)
            .addOnSuccessListener {
                val idx = mockUsers.indexOfFirst { it.id == user.id }
                if (idx != -1) mockUsers[idx] = user
                currentUser = user
                callback(true, "Kata sandi berhasil diubah")
            }
            .addOnFailureListener {
                val idx = mockUsers.indexOfFirst { it.id == user.id }
                if (idx != -1) mockUsers[idx] = user
                currentUser = user
                callback(true, "Kata sandi diubah (Offline)")
            }
    }
}
