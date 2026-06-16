package com.kerjain.app.ui.mentor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kerjain.app.MainActivity
import com.kerjain.app.R
import com.kerjain.app.model.*
import com.kerjain.app.repository.KerjainRepository
import com.kerjain.app.ui.ProgressUtils
import com.kerjain.app.ui.*
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.kerjain.app.ui.ImageUtils

// ==========================================
// 1. MENTOR HOME FRAGMENT
// ==========================================
class MentorHomeFragment : Fragment() {

    private lateinit var rvSubmissions: RecyclerView
    private lateinit var rvStudents: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCountStudents: TextView
    private lateinit var tvCountReviewed: TextView
    
    private lateinit var submissionAdapter: PendingTaskAdapter
    private lateinit var studentAdapter: StudentProgressAdapter
    private lateinit var repo: KerjainRepository
    
    private var allSubmissions = listOf<Submission>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mentor_home, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        // Header
        view.findViewById<TextView>(R.id.tv_greeting).text = getGreeting() + ", 👋"
        view.findViewById<TextView>(R.id.tv_mentor_name).text = user.namaLengkap + "!"
        val tvInitialHome = view.findViewById<TextView>(R.id.tv_profile_initial_home)
        val ivPhotoHome = view.findViewById<ImageView>(R.id.iv_profile_photo_home)
        tvInitialHome.text = user.namaLengkap.take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, tvInitialHome, ivPhotoHome)

        tvCountStudents = view.findViewById(R.id.tv_count_students)
        tvCountReviewed = view.findViewById(R.id.tv_count_reviewed)
        tvEmpty = view.findViewById(R.id.tv_empty_submissions)

        // Adapters
        rvStudents = view.findViewById(R.id.rv_mentor_students)
        rvStudents.layoutManager = LinearLayoutManager(context)
        studentAdapter = StudentProgressAdapter(emptyList()) { student ->
            showStudentDetailDialog(requireContext(), student, repo)
        }
        rvStudents.adapter = studentAdapter

        rvSubmissions = view.findViewById(R.id.rv_mentor_submissions)
        rvSubmissions.layoutManager = LinearLayoutManager(context)
        submissionAdapter = PendingTaskAdapter(emptyList()) { submission ->
            val fragment = MentorReviewFragment().apply {
                arguments = Bundle().apply { putSerializable("submission", submission) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvSubmissions.adapter = submissionAdapter

        view.findViewById<View>(R.id.btn_reviewed_tasks).setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(MentorTaskHistoryFragment(), true)
        }

        view.findViewById<View>(R.id.btn_review_now).setOnClickListener {
            val fragment = MentorTaskHistoryFragment().apply {
                arguments = Bundle().apply { putInt("start_tab", 0) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }

        view.findViewById<View>(R.id.tv_see_all_students)?.setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(MentorStudentListFragment(), true)
        }

        val btnNotif = view.findViewById<View>(R.id.btn_notif)
        btnNotif.setOnClickListener {
            val fragment = com.kerjain.app.ui.pelajar.PelajarNotificationsFragment()
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        
        val vNotifBadge = view.findViewById<View>(R.id.v_notif_badge)
        repo.getNotifications(user.id) { list ->
            val hasUnread = list.any { !it.read }
            vNotifBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
        }

        loadData()
        return view
    }

    private fun loadData() {
        if (!isAdded) return
        val user = repo.currentUser ?: return
        
        // Use real-time snapshot listener for submissions
        repo.getAllSubmissionsForMentorOrMitra(user.id, "mentor") { list ->
            if (!isAdded) return@getAllSubmissionsForMentorOrMitra
            
            allSubmissions = list
            val pending = list.filter { it.status.lowercase() == "menunggu" }
            val reviewed = list.filter { it.status.lowercase() == "disetujui" || it.status.lowercase() == "revisi" }
                .sortedByDescending { it.reviewedAt ?: 0L }
            
            // "Tugas Belum Dinilai" card shows count of pending tasks
            tvCountReviewed.text = pending.size.toString()
            view?.findViewById<TextView>(R.id.tv_featured_title)?.text = "Ada ${pending.size} Tugas Baru\nPerlu Dinilai!"
            
            // "History Tugas Baru Dinilai" section shows recent reviewed tasks
            if (reviewed.isEmpty()) {
                view?.findViewById<View>(R.id.tv_empty_submissions_layout)?.visibility = View.VISIBLE
                rvSubmissions.visibility = View.GONE
            } else {
                view?.findViewById<View>(R.id.tv_empty_submissions_layout)?.visibility = View.GONE
                rvSubmissions.visibility = View.VISIBLE
                submissionAdapter.updateData(reviewed.take(5)) // Show only top 5 recent
            }
        }
        
        // Load Active Students and their progress independently
        loadStudentsProgress(user)
    }

    private fun loadStudentsProgress(mentor: User) {
        if (!isAdded) return
        
        repo.getEnrollments(mentor.id, "mentor") { enrollments ->
            if (!isAdded) return@getEnrollments
            
            val uniqueStudentsCount = enrollments.distinctBy { it.pelajarId }.size
            tvCountStudents.text = uniqueStudentsCount.toString()
            
            if (enrollments.isEmpty()) {
                studentAdapter.updateData(emptyList())
                return@getEnrollments
            }

            val uniquePelajarIds = enrollments.map { it.pelajarId }.toSet()

            repo.getTalentPool { allStudents ->
                if (!isAdded) return@getTalentPool
                val studentsInMyPrograms = allStudents.filter { it.id in uniquePelajarIds }

                if (studentsInMyPrograms.isEmpty()) {
                    studentAdapter.updateData(emptyList())
                    return@getTalentPool
                }

                val studentProgressList = mutableListOf<Pair<User, Int>>()
                var processedCount = 0

                for (student in studentsInMyPrograms) {
                    val enrollment = enrollments.find { it.pelajarId == student.id }
                    if (enrollment != null) {
                        repo.getTasks(enrollment.programId) { tasks ->
                            repo.getSubmissions(enrollment.id) { subs ->
                                if (!isAdded) return@getSubmissions
                                val approved = subs.count { it.status.lowercase() == "disetujui" }
                                val percent = ProgressUtils.calculateWeightedProgress(tasks.size, approved)
                                
                                synchronized(studentProgressList) {
                                    studentProgressList.add(student to percent)
                                }
                                
                                processedCount++
                                if (processedCount == studentsInMyPrograms.size) {
                                    studentAdapter.updateData(studentProgressList.sortedByDescending { it.second }.take(5))
                                }
                            }
                        }
                    } else {
                        processedCount++
                        if (processedCount == studentsInMyPrograms.size) {
                            studentAdapter.updateData(studentProgressList.sortedByDescending { it.second }.take(5))
                        }
                    }
                }
            }
        }
    }

    private fun getGreeting(): String {
        return "Hallo"
    }
}

// ==========================================
// 1.1 MENTOR STUDENT LIST FRAGMENT
// ==========================================
class MentorStudentListFragment : Fragment() {

    private lateinit var rvStudents: RecyclerView
    private lateinit var studentAdapter: StudentProgressAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mentor_student_list, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        rvStudents = view.findViewById(R.id.rv_mentor_students_full)
        rvStudents.layoutManager = LinearLayoutManager(context)
        studentAdapter = StudentProgressAdapter(emptyList(), hideProgress = true) { student ->
            showStudentDetailDialog(requireContext(), student, repo)
        }
        rvStudents.adapter = studentAdapter

        loadData(user)
        return view
    }

    private fun loadData(mentor: User) {
        if (!isAdded) return
        
        repo.getEnrollments(mentor.id, "mentor") { enrollments ->
            if (!isAdded) return@getEnrollments
            
            val uniquePelajarIds = enrollments.map { it.pelajarId }.toSet()
            if (uniquePelajarIds.isEmpty()) {
                studentAdapter.updateData(emptyList())
                return@getEnrollments
            }

            repo.getTalentPool { allStudents ->
                if (!isAdded) return@getTalentPool
                val studentProgressList = mutableListOf<Pair<User, Int>>()
                val studentsInMyPrograms = allStudents.filter { it.id in uniquePelajarIds }

                if (studentsInMyPrograms.isEmpty()) {
                    studentAdapter.updateData(emptyList())
                    return@getTalentPool
                }

                var loadedCount = 0
                for (student in studentsInMyPrograms) {
                    val enrollment = enrollments.find { it.pelajarId == student.id }
                    if (enrollment != null) {
                        repo.getTasks(enrollment.programId) { tasks ->
                            repo.getSubmissions(enrollment.id) { subs ->
                                if (!isAdded) return@getSubmissions
                                val approved = subs.count { it.status.lowercase() == "disetujui" }
                                val percent = ProgressUtils.calculateWeightedProgress(tasks.size, approved)
                                
                                studentProgressList.removeAll { it.first.id == student.id }
                                studentProgressList.add(student to percent)
                                
                                loadedCount++
                                if (loadedCount == studentsInMyPrograms.size) {
                                    studentAdapter.updateData(studentProgressList.sortedByDescending { it.second })
                                }
                            }
                        }
                    } else {
                        loadedCount++
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MENTOR TASK HISTORY FRAGMENT
// ==========================================
class MentorTaskHistoryFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout
    private lateinit var adapter: SubmissionAdapter
    private lateinit var repo: KerjainRepository
    private var allSubmissions = listOf<Submission>()
    private var selectedTab = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mentor_task_history, container, false)
        (activity as? MainActivity)?.showToolbar(false)

        repo = KerjainRepository.getInstance(requireContext())
        
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        tabLayout = view.findViewById(R.id.tab_layout_history)
        rvHistory = view.findViewById(R.id.rv_task_history)
        rvHistory.layoutManager = LinearLayoutManager(context)
        adapter = SubmissionAdapter(emptyList()) { submission ->
            val fragment = MentorReviewFragment().apply {
                arguments = Bundle().apply { putSerializable("submission", submission) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvHistory.adapter = adapter

        // Set starting tab from arguments if available
        val startTab = arguments?.getInt("start_tab", 1) ?: 1
        tabLayout.getTabAt(startTab)?.select()
        selectedTab = startTab

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                selectedTab = tab?.position ?: 0
                filterList()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        loadData()
        return view
    }

    private fun loadData() {
        val user = repo.currentUser ?: return
        repo.getAllSubmissionsForMentorOrMitra(user.id, "mentor") { list ->
            allSubmissions = list
            filterList()
        }
    }

    private fun filterList() {
        val filtered = if (selectedTab == 0) {
            allSubmissions.filter { it.status.lowercase() == "menunggu" }
        } else {
            allSubmissions.filter { it.status.lowercase() == "disetujui" || it.status.lowercase() == "revisi" }
        }
        adapter.updateData(filtered)
    }
}


// ==========================================
// 2. MENTOR REVIEW DETAIL FRAGMENT
// ==========================================
class MentorReviewFragment : Fragment() {

    private lateinit var submission: Submission
    private lateinit var etFeedback: EditText
    private lateinit var repo: KerjainRepository
    private var selectedScore: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mentor_review, container, false)
        (activity as? MainActivity)?.showToolbar(false)
        (activity as? MainActivity)?.updateToolbarTitle("Beri Penilaian")

        submission = arguments?.getSerializable("submission") as? Submission ?: return view
        repo = KerjainRepository.getInstance(requireContext())

        // Header Subtitle
        view.findViewById<TextView>(R.id.tv_header_subtitle).text = "${submission.pelajarNama} • ${submission.taskJudul}"
        
        // Student Info
        view.findViewById<TextView>(R.id.tv_student_initial).text = submission.pelajarNama.take(1).uppercase()
        view.findViewById<TextView>(R.id.tv_review_student_name).text = submission.pelajarNama
        view.findViewById<TextView>(R.id.tv_review_student_info).text = "Peserta Magang KerjaIn"
        
        view.findViewById<TextView>(R.id.tv_review_student_notes).text = submission.catatan.ifEmpty { "(Tidak ada catatan)" }
        view.findViewById<TextView>(R.id.tv_file_name).text = submission.fileUrl.split("/").last().ifEmpty { "Lampiran_Tugas.link" }

        val tvScoreDisplay = view.findViewById<TextView>(R.id.tv_score_display)
        etFeedback = view.findViewById(R.id.et_review_feedback)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        view.findViewById<View>(R.id.btn_open_url).setOnClickListener {
            val url = submission.fileUrl
            if (url.startsWith("http://") || url.startsWith("https://")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                Toast.makeText(context, "Format URL salah: $url", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Chips
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_scores)
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId != null) {
                val chip = view.findViewById<com.google.android.material.chip.Chip>(checkedId)
                selectedScore = chip.text.toString().toIntOrNull() ?: 0
                tvScoreDisplay.text = selectedScore.toString()
                tvScoreDisplay.setTextColor(android.graphics.Color.parseColor("#4F46E5"))
            }
        }

        val btnApprove = view.findViewById<View>(R.id.btn_approve)
        val btnReject = view.findViewById<View>(R.id.btn_reject)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_submit_review)
        
        var decision: String? = null
        
        btnApprove.setOnClickListener {
            decision = "disetujui"
            (btnApprove as com.google.android.material.card.MaterialCardView).strokeColor = android.graphics.Color.parseColor("#10B981")
            (btnReject as com.google.android.material.card.MaterialCardView).strokeColor = android.graphics.Color.parseColor("#FEE2E2")
        }
        
        btnReject.setOnClickListener {
            decision = "revisi"
            (btnReject as com.google.android.material.card.MaterialCardView).strokeColor = android.graphics.Color.parseColor("#EF4444")
            (btnApprove as com.google.android.material.card.MaterialCardView).strokeColor = android.graphics.Color.parseColor("#DCFCE7")
        }

        if (submission.status.lowercase() != "menunggu") {
            selectedScore = submission.nilai ?: 0
            tvScoreDisplay.text = selectedScore.toString()
            etFeedback.setText(submission.feedback ?: "")
            etFeedback.isEnabled = false
            chipGroup.isEnabled = false
            btnApprove.isEnabled = false
            btnReject.isEnabled = false
            btnSubmit.visibility = View.GONE
            
            if (submission.status.lowercase() == "disetujui") {
                (btnApprove as com.google.android.material.card.MaterialCardView).strokeColor = android.graphics.Color.parseColor("#10B981")
            } else {
                (btnReject as com.google.android.material.card.MaterialCardView).strokeColor = android.graphics.Color.parseColor("#EF4444")
            }
        } else {
            btnSubmit.setOnClickListener {
                if (decision == null) {
                    Toast.makeText(context, "Pilih keputusan terlebih dahulu", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                submitReview(decision!!, btnSubmit)
            }
        }

        return view
    }

    private fun submitReview(status: String, btnSubmit: MaterialButton) {
        val feedback = etFeedback.text.toString().trim()

        if (selectedScore == 0) {
            Toast.makeText(context, "Harap pilih nilai", Toast.LENGTH_SHORT).show()
            return
        }

        if (feedback.isEmpty()) {
            Toast.makeText(context, "Harap masukkan umpan balik/catatan", Toast.LENGTH_SHORT).show()
            return
        }

        val mentor = repo.currentUser ?: return
        btnSubmit.isEnabled = false

        repo.reviewSubmission(submission.id, status, feedback, selectedScore, mentor.id) { success ->
            if (success) {
                Toast.makeText(context, "Review berhasil dikirim!", Toast.LENGTH_SHORT).show()
                activity?.onBackPressedDispatcher?.onBackPressed()
            } else {
                btnSubmit.isEnabled = true
                Toast.makeText(context, "Gagal mengirimkan review", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class MentorProfileFragment : Fragment() {

    private lateinit var repo: KerjainRepository
    
    private lateinit var tvProfileInitial: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var cardProfileAvatar: View
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileTitle: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileExperience: TextView
    private lateinit var tvProfileBio: TextView

    private var tempPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dataUri = result.data?.data
            if (dataUri != null) {
                saveProfilePhoto(dataUri)
            }
        }
    }

    private val captureImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempPhotoUri?.let { saveProfilePhoto(it) }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfilePhoto(uri: Uri) {
        val user = repo.currentUser ?: return
        val savedPath = ImageUtils.saveProfilePhoto(requireContext(), uri, user.id)
        if (savedPath != null) {
            user.fotoProfil = savedPath
            ImageUtils.loadProfileImage(savedPath, tvProfileInitial, ivProfilePhoto)
            
            repo.uploadProfilePhoto(user.id, Uri.parse(savedPath)) { success, remoteUrl ->
                if (success && remoteUrl != null) {
                    user.fotoProfil = remoteUrl
                }
                repo.updateUserProfile(user) { updateSuccess ->
                    if (updateSuccess) {
                        Toast.makeText(context, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Gagal memproses foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteProfilePhoto() {
        val user = repo.currentUser ?: return
        user.fotoProfil = null
        ImageUtils.loadProfileImage(null, tvProfileInitial, ivProfilePhoto)
        repo.updateUserProfile(user) { success ->
            if (success) {
                Toast.makeText(context, "Foto profil dihapus", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPhotoPickerDialog() {
        val options = arrayOf<CharSequence>("Kamera", "Galeri", "Hapus Foto")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Ubah Foto Profil")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Kamera" -> {
                    checkCameraPermissionAndLaunch()
                }
                options[item] == "Galeri" -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImageLauncher.launch(intent)
                }
                options[item] == "Hapus Foto" -> {
                    deleteProfilePhoto()
                }
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val uri = ImageUtils.getTempCameraUri(requireContext())
        tempPhotoUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        captureImageLauncher.launch(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mentor_profile, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.updateToolbarTitle("")

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        tvProfileInitial = view.findViewById(R.id.tv_mentor_profile_initial)
        ivProfilePhoto = view.findViewById(R.id.iv_mentor_profile_photo)
        cardProfileAvatar = view.findViewById(R.id.card_mentor_profile_avatar)
        tvProfileName = view.findViewById(R.id.tv_mentor_profile_name)
        tvProfileTitle = view.findViewById(R.id.tv_mentor_profile_title)
        tvProfileEmail = view.findViewById(R.id.tv_mentor_profile_email)
        tvProfileExperience = view.findViewById(R.id.tv_mentor_experience)
        tvProfileBio = view.findViewById(R.id.tv_mentor_bio)

        // Populate UI
        tvProfileInitial.text = user.namaLengkap.take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, tvProfileInitial, ivProfilePhoto)
        tvProfileName.text = user.namaLengkap
        tvProfileTitle.text = "${user.profesi} • ${user.perusahaan}"
        tvProfileEmail.text = user.email
        tvProfileExperience.text = "Pengalaman: ${user.tahunPengalaman} Tahun"
        tvProfileBio.text = user.bio ?: "Tidak ada biografi."

        cardProfileAvatar.setOnClickListener {
            showPhotoPickerDialog()
        }

        val btnEditProfile = view.findViewById<Button>(R.id.btn_edit_profile)
        btnEditProfile.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile_mentor, null)
            val etName = dialogView.findViewById<EditText>(R.id.et_edit_mentor_name)
            val etEmail = dialogView.findViewById<EditText>(R.id.et_edit_mentor_email)
            val etProfession = dialogView.findViewById<EditText>(R.id.et_edit_mentor_profession)
            val etCompany = dialogView.findViewById<EditText>(R.id.et_edit_mentor_company)
            val etExperience = dialogView.findViewById<EditText>(R.id.et_edit_mentor_experience)
            val etBio = dialogView.findViewById<EditText>(R.id.et_edit_mentor_bio)

            etName.setText(user.namaLengkap)
            etEmail?.setText(user.email)
            etEmail?.isEnabled = false
            
            etProfession.setText(user.profesi ?: "")
            etCompany.setText(user.perusahaan ?: "")
            etExperience.setText(user.tahunPengalaman.toString())
            etBio.setText(user.bio ?: "")

            android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Simpan") { dialog, _ ->
                    val newName = etName.text.toString().trim()
                    val newProfession = etProfession.text.toString().trim()
                    val newCompany = etCompany.text.toString().trim()
                    val newExpText = etExperience.text.toString().trim()
                    val newBio = etBio.text.toString().trim()

                    if (newName.isEmpty()) {
                        Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    user.namaLengkap = newName
                    user.profesi = newProfession
                    user.perusahaan = newCompany
                    user.tahunPengalaman = newExpText.toIntOrNull() ?: user.tahunPengalaman
                    user.bio = newBio

                    // Optimistic UI updates
                    tvProfileInitial.text = user.namaLengkap.take(1).uppercase()
                    tvProfileName.text = user.namaLengkap
                    tvProfileTitle.text = "${user.profesi} • ${user.perusahaan}"
                    tvProfileEmail.text = user.email
                    tvProfileExperience.text = "Pengalaman: ${user.tahunPengalaman} Tahun"
                    tvProfileBio.text = user.bio ?: "Tidak ada biografi."

                    repo.updateUserProfile(user) { success ->
                        if (success) {
                            Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal memperbarui profil di server", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        view.findViewById<Button>(R.id.btn_change_password).setOnClickListener {
            showChangePasswordDialog()
        }

        view.findViewById<Button>(R.id.btn_mentor_logout).setOnClickListener {
            repo.logout()
            (activity as? MainActivity)?.onUserLoggedOut()
        }

        return view
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<EditText>(R.id.et_old_password)
        val etNew = dialogView.findViewById<EditText>(R.id.et_new_password)
        val etConfirm = dialogView.findViewById<EditText>(R.id.et_confirm_password)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save_password)

        val builder = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
        
        val dialog = builder.create()
        
        btnSave.setOnClickListener {
            val oldPass = etOld.text.toString().trim()
            val newPass = etNew.text.toString().trim()
            val confirmPass = etConfirm.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 8) {
                Toast.makeText(context, "Kata sandi baru minimal 8 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(context, "Konfirmasi kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repo.changePassword(oldPass, newPass) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (success) {
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
}

// ==========================================
// SHARED TALENT DETAIL DIALOG HELPER
// ==========================================
fun showStudentDetailDialog(context: android.content.Context, student: User, repo: KerjainRepository) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_talent_detail, null)
    val tvInitial = dialogView.findViewById<TextView>(R.id.tv_talent_detail_initial)
    val ivPhoto = dialogView.findViewById<ImageView>(R.id.iv_talent_detail_photo)
    tvInitial.text = student.namaLengkap.take(1).uppercase()
    ImageUtils.loadProfileImage(student.fotoProfil, tvInitial, ivPhoto)
    dialogView.findViewById<TextView>(R.id.tv_talent_detail_name).text = student.namaLengkap
    dialogView.findViewById<TextView>(R.id.tv_talent_detail_school).text = "${student.universitas ?: "Pelajar"} • ${student.jurusan ?: ""}"
    dialogView.findViewById<TextView>(R.id.tv_talent_detail_year).text = "Angkatan ${student.angkatan ?: "-"}"
    dialogView.findViewById<TextView>(R.id.tv_talent_detail_points).text = "${student.totalPoin} Poin"
    dialogView.findViewById<TextView>(R.id.tv_talent_detail_bio).text = student.bio ?: "Tidak ada biografi."

    val rvCerts = dialogView.findViewById<RecyclerView>(R.id.rv_talent_detail_certificates)
    val tvEmptyCerts = dialogView.findViewById<TextView>(R.id.tv_talent_detail_empty_certificates)
    rvCerts.layoutManager = LinearLayoutManager(context)
    val certAdapter = com.kerjain.app.ui.CertificateAdapter(emptyList())
    rvCerts.adapter = certAdapter

    repo.getCertificates(student.id) { certs ->
        val filteredCerts = certs.filter { it.pelajarNama == student.namaLengkap }
        if (filteredCerts.isEmpty()) {
            tvEmptyCerts.visibility = View.VISIBLE
            rvCerts.visibility = View.GONE
        } else {
            tvEmptyCerts.visibility = View.GONE
            rvCerts.visibility = View.VISIBLE
            certAdapter.updateData(filteredCerts)
        }
    }

    val rvPrograms = dialogView.findViewById<RecyclerView>(R.id.rv_talent_enrolled_programs)
    val tvEmptyPrograms = dialogView.findViewById<TextView>(R.id.tv_talent_detail_empty_programs)
    rvPrograms.layoutManager = LinearLayoutManager(context)
    val programAdapter = com.kerjain.app.ui.EnrolledProgramAdapter(emptyList(), hideProgress = true) { /* ignore click */ }
    rvPrograms.adapter = programAdapter

    repo.getPrograms { allPrograms ->
        val mentor = repo.currentUser ?: return@getPrograms
        repo.getEnrollments(mentor.id, "mentor") { mentorEnrollments ->
            val relevantEnrollments = mentorEnrollments.filter { it.pelajarId == student.id }

            if (relevantEnrollments.isEmpty()) {
                tvEmptyPrograms.visibility = View.VISIBLE
                rvPrograms.visibility = View.GONE
            } else {
                tvEmptyPrograms.visibility = View.GONE
                rvPrograms.visibility = View.VISIBLE
                val displayData = mutableListOf<Triple<Enrollment, Program, Int>>()
                var processed = 0
                for (enroll in relevantEnrollments) {
                    val prog = allPrograms.find { it.id == enroll.programId }
                    if (prog != null) {
                        repo.getTasks(prog.id) { tasks ->
                            repo.getSubmissions(enroll.id) { subs ->
                                val approved = subs.count { it.status.lowercase() == "disetujui" }
                                val progress = com.kerjain.app.ui.ProgressUtils.calculateWeightedProgress(tasks.size, approved)
                                displayData.add(Triple(enroll, prog, progress))
                                processed++
                                if (processed == relevantEnrollments.size) {
                                    programAdapter.updateData(displayData)
                                }
                            }
                        }
                    } else {
                        processed++
                        if (processed == relevantEnrollments.size) {
                            programAdapter.updateData(displayData)
                        }
                    }
                }
            }
        }
    }

    // Monitoring logic for Mentor
    if (repo.currentUser?.role == "mentor") {
        val layoutMonitoring = dialogView.findViewById<View>(R.id.layout_mentor_monitoring)
        val rvMonitoring = dialogView.findViewById<RecyclerView>(R.id.rv_mentor_monitoring_tasks)
        if (layoutMonitoring != null && rvMonitoring != null) {
            rvMonitoring.layoutManager = LinearLayoutManager(context)
            val monitoringAdapter = SubmissionAdapter(emptyList()) { /* no-op */ }
            rvMonitoring.adapter = monitoringAdapter

            repo.getAllSubmissionsForMentorOrMitra(repo.currentUser!!.id, "mentor") { allSubs ->
                val studentSubs = allSubs.filter { it.pelajarId == student.id }
                if (studentSubs.isEmpty()) {
                    layoutMonitoring.visibility = View.GONE
                } else {
                    layoutMonitoring.visibility = View.VISIBLE
                    monitoringAdapter.updateData(studentSubs.sortedByDescending { it.reviewedAt ?: 0L })
                }
            }
        }
    }

    val alertDialog = android.app.AlertDialog.Builder(context)
        .setView(dialogView)
        .create()

    dialogView.findViewById<Button>(R.id.btn_talent_detail_close).setOnClickListener {
        alertDialog.dismiss()
    }

    alertDialog.show()
}
