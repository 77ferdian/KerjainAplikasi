package com.kerjain.app.ui.pelajar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.kerjain.app.MainActivity
import com.kerjain.app.R
import com.kerjain.app.model.*
import com.kerjain.app.repository.KerjainRepository
import com.kerjain.app.ui.CertificateAdapter
import com.kerjain.app.ui.ProgramAdapter
import com.kerjain.app.ui.TaskAdapter
import com.kerjain.app.ui.NotificationAdapter
import com.kerjain.app.ui.EnrolledProgramAdapter
import com.kerjain.app.ui.ProgressUtils
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.kerjain.app.ui.ImageUtils
import com.kerjain.app.ui.CertificateTaskAdapter
import android.graphics.Color

// ==========================================
// 1. PELAJAR HOME FRAGMENT
// ==========================================
class PelajarHomeFragment : Fragment() {

    private lateinit var rvActive: RecyclerView
    private lateinit var rvRecommended: RecyclerView
    private lateinit var tvEmptyActive: TextView
    private lateinit var activeAdapter: ProgramAdapter
    private lateinit var recommendedAdapter: ProgramAdapter
    private lateinit var repo: KerjainRepository
    
    private lateinit var tvCountActive: TextView
    private lateinit var tvCountCerts: TextView
    private lateinit var tvCompletionPercent: TextView
    private lateinit var tvFeaturedTitle: TextView
    private var activeProgram: Program? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_home, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)
        (activity as? MainActivity)?.updateToolbarTitle("Beranda KerjaIn")

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        view.findViewById<TextView>(R.id.tv_greeting).text = getGreeting() + ", 👋"
        view.findViewById<TextView>(R.id.tv_student_name).text = user.namaLengkap.substringBefore(" ") + "!"
        val tvInitialHome = view.findViewById<TextView>(R.id.tv_profile_initial_home)
        val ivPhotoHome = view.findViewById<ImageView>(R.id.iv_profile_photo_home)
        tvInitialHome.text = user.namaLengkap.take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, tvInitialHome, ivPhotoHome)
        
        tvCountActive = view.findViewById(R.id.tv_count_active)
        tvCountCerts = view.findViewById(R.id.tv_count_certs)
        tvCompletionPercent = view.findViewById(R.id.tv_completion_percent)
        tvFeaturedTitle = view.findViewById(R.id.tv_featured_title)
        
        tvEmptyActive = view.findViewById(R.id.tv_empty_active)

        view.findViewById<View>(R.id.card_active_programs).setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(PelajarEnrolledProgramsFragment(), true)
        }

        view.findViewById<View>(R.id.card_certificates).setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(PelajarCertificatesFragment(), true)
        }

        view.findViewById<View>(R.id.btn_continue).setOnClickListener {
            val program = activeProgram
            if (program != null) {
                val fragment = PelajarProgressFragment().apply {
                    arguments = Bundle().apply { putSerializable("selected_program", program) }
                }
                (activity as? MainActivity)?.navigateFragment(fragment, true)
            } else {
                Toast.makeText(context, "Mulai daftar program dulu yuk!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnNotif = view.findViewById<View>(R.id.btn_notif)
        btnNotif.setOnClickListener {
            val fragment = PelajarNotificationsFragment()
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        val vNotifBadge = view.findViewById<View>(R.id.v_notif_badge)
        repo.getNotifications(user.id) { list ->
            val hasUnread = list.any { !it.read }
            vNotifBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
        }

        rvActive = view.findViewById(R.id.rv_active_programs)
        rvRecommended = view.findViewById(R.id.rv_recommended_programs)

        rvActive.layoutManager = LinearLayoutManager(context)
        rvRecommended.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        activeAdapter = ProgramAdapter(emptyList(), layoutResId = R.layout.item_task_home) { program ->
            val fragment = PelajarProgressFragment().apply {
                arguments = Bundle().apply { putSerializable("selected_program", program) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvActive.adapter = activeAdapter

        recommendedAdapter = ProgramAdapter(emptyList(), layoutResId = R.layout.item_program_featured) { program ->
            val fragment = PelajarExploreDetailFragment().apply {
                arguments = Bundle().apply { putSerializable("program", program) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvRecommended.adapter = recommendedAdapter

        loadData()
        return view
    }

    private fun loadData() {
        val user = repo.currentUser ?: return
        
        // Use repo snapshots for real-time calculation
        repo.getEnrollments(user.id, "pelajar") { enrollments ->
            if (!isAdded) return@getEnrollments
            
            repo.getPrograms { allPrograms ->
                if (!isAdded) return@getPrograms
                
                val activeEnrollments = enrollments.filter { it.status == "aktif" }
                val completedProgramIds = enrollments.filter { it.status == "selesai" }.map { it.programId }
                
                val activePrograms = allPrograms.filter { p -> activeEnrollments.any { it.programId == p.id } }
                val recommendedPrograms = allPrograms.filter { 
                    it.status == "published" && 
                    it.id !in activePrograms.map { it.id } && 
                    it.id !in completedProgramIds 
                }

                if (activePrograms.isNotEmpty()) {
                    activeProgram = activePrograms.first()
                    tvFeaturedTitle.text = "Lanjutkan Simulasi\n${activeProgram?.judul}!"
                    
                    tvEmptyActive.visibility = View.GONE
                    rvActive.visibility = View.VISIBLE
                    activeAdapter.updateData(activePrograms)
                } else {
                    activeProgram = null
                    tvFeaturedTitle.text = "Mulai Karirmu\nSekarang!"
                    tvEmptyActive.visibility = View.VISIBLE
                    rvActive.visibility = View.GONE
                }

                recommendedAdapter.updateData(recommendedPrograms)
                
                // Update metrics
                tvCountActive.text = (activeEnrollments.size + completedProgramIds.size).toString()
                repo.getCertificates(user.id) { certs ->
                    tvCountCerts.text = certs.size.toString()
                }
                
                // Calculate average completion across all enrollments
                if (enrollments.isNotEmpty()) {
                    var totalPercent = 0
                    var loaded = 0
                    for (e in enrollments) {
                        repo.getTasks(e.programId) { tasks ->
                            repo.getSubmissions(e.id) { subs ->
                                if (!isAdded) return@getSubmissions
                                val approved = subs.count { it.status.lowercase() == "disetujui" }
                                val p = ProgressUtils.calculateWeightedProgress(tasks.size, approved)
                                totalPercent += p
                                loaded++
                                if (loaded == enrollments.size) {
                                    val finalAvg = totalPercent / enrollments.size
                                    tvCompletionPercent.text = "$finalAvg%"
                                }
                            }
                        }
                    }
                } else {
                    tvCompletionPercent.text = "0%"
                }
            }
        }
    }

    private fun getGreeting(): String {
        return "Hallo"
    }
}

// ==========================================
// 2. PELAJAR EXPLORE FRAGMENT
// ==========================================
class PelajarExploreFragment : Fragment() {

    private lateinit var rvPrograms: RecyclerView
    private lateinit var adapter: ProgramAdapter
    private lateinit var repo: KerjainRepository
    private var allProgramsList = listOf<Program>()
    private var currentFilter = "All"
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_explore, container, false)
        (activity as? MainActivity)?.updateToolbarTitle("Jelajahi Program")

        repo = KerjainRepository.getInstance(requireContext())
        rvPrograms = view.findViewById(R.id.rv_explore_programs)
        rvPrograms.layoutManager = LinearLayoutManager(context)

        adapter = ProgramAdapter(emptyList()) { program ->
            val fragment = PelajarExploreDetailFragment().apply {
                arguments = Bundle().apply { putSerializable("program", program) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvPrograms.adapter = adapter

        // Setup Chips Filter
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_categories)
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_android -> "Android"
                R.id.chip_uiux -> "UI/UX"
                R.id.chip_backend -> "Backend"
                else -> "All"
            }
            filterList()
        }

        // Setup Search
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterList()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadData()
        return view
    }

    private fun loadData() {
        val user = repo.currentUser ?: return
        repo.getPrograms { programs ->
            repo.getEnrollments(user.id, "pelajar") { enrollments ->
                val enrolledIds = enrollments.map { it.programId }
                // Only show programs that the student hasn't registered for yet
                allProgramsList = programs.filter { it.status == "published" && it.id !in enrolledIds }
                filterList()
            }
        }
    }

    private fun filterList() {
        var filtered = allProgramsList
        if (currentFilter != "All") {
            filtered = filtered.filter { it.bidang.lowercase() == currentFilter.lowercase() }
        }
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { it.judul.lowercase().contains(searchQuery.lowercase()) || it.mitraNama.lowercase().contains(searchQuery.lowercase()) }
        }
        adapter.updateData(filtered)
    }
}

// ==========================================
// 3. PELAJAR EXPLORE DETAIL FRAGMENT
// ==========================================
class PelajarExploreDetailFragment : Fragment() {

    private lateinit var program: Program
    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: TaskAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_explore_detail, container, false)
        (activity as? MainActivity)?.updateToolbarTitle("Detail Program")

        program = arguments?.getSerializable("program") as? Program ?: return view
        repo = KerjainRepository.getInstance(requireContext())

        view.findViewById<TextView>(R.id.tv_detail_title).text = program.judul
        view.findViewById<TextView>(R.id.tv_detail_partner).text = program.mitraNama
        view.findViewById<TextView>(R.id.tv_detail_category).text = program.bidang.uppercase()
        view.findViewById<TextView>(R.id.tv_detail_duration).text = "Mulai ${program.tanggalMulai} s/d ${program.tanggalSelesai}"
        view.findViewById<TextView>(R.id.tv_detail_description).text = program.deskripsi

        rvTasks = view.findViewById(R.id.rv_detail_tasks)
        rvTasks.layoutManager = LinearLayoutManager(context)
        adapter = TaskAdapter(emptyList(), hideStatus = true) // Simple display, no status needed
        rvTasks.adapter = adapter

        val btnEnroll = view.findViewById<MaterialButton>(R.id.btn_enroll)
        btnEnroll.setOnClickListener {
            val user = repo.currentUser ?: return@setOnClickListener
            btnEnroll.isEnabled = false
            repo.enrollInProgram(user.id, user.namaLengkap, program) { success ->
                if (success) {
                    Toast.makeText(context, "Berhasil Mendaftar Program!", Toast.LENGTH_SHORT).show()
                    // Navigate to Progress tab
                    val progressFrag = PelajarProgressFragment().apply {
                        arguments = Bundle().apply { putSerializable("selected_program", program) }
                    }
                    (activity as? MainActivity)?.navigateFragment(progressFrag, false)
                } else {
                    btnEnroll.isEnabled = true
                    Toast.makeText(context, "Gagal mendaftar program", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadTasks()
        return view
    }

    private fun loadTasks() {
        repo.getTasks(program.id) { tasks ->
            adapter.updateData(tasks)
        }
    }
}

// ==========================================
// 4. PELAJAR PROGRESS FRAGMENT
// ==========================================
class PelajarProgressFragment : Fragment() {

    private lateinit var spinner: Spinner
    private lateinit var rvTasks: RecyclerView
    private lateinit var layoutNoProgress: LinearLayout
    private lateinit var adapter: TaskAdapter
    private lateinit var repo: KerjainRepository
    
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvTaskSummary: TextView
    private lateinit var progressBar: com.google.android.material.progressindicator.CircularProgressIndicator
    
    private var activeEnrollments = listOf<Enrollment>()
    private var currentEnrollment: Enrollment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_progress, container, false)
        (activity as? MainActivity)?.updateToolbarTitle("Progres Belajar")

        repo = KerjainRepository.getInstance(requireContext())
        spinner = view.findViewById(R.id.spinner_active_programs)
        rvTasks = view.findViewById(R.id.rv_progress_tasks)
        layoutNoProgress = view.findViewById(R.id.layout_no_progress)
        
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent_large)
        tvTaskSummary = view.findViewById(R.id.tv_task_count_summary)
        progressBar = view.findViewById(R.id.circular_progress)

        rvTasks.layoutManager = LinearLayoutManager(context)
        adapter = TaskAdapter(emptyList(), emptyMap(), onItemClick = { task, status ->
            val enroll = currentEnrollment
            if (enroll != null) {
                val fragment = PelajarTaskSubmitFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("task", task)
                        putString("enrollment_id", enroll.id)
                        putString("task_status", status)
                    }
                }
                (activity as? MainActivity)?.navigateFragment(fragment, true)
            }
        })
        rvTasks.adapter = adapter

        loadEnrollments()
        return view
    }

    private fun loadEnrollments() {
        val user = repo.currentUser ?: return
        repo.getEnrollments(user.id, "pelajar") { enrollments ->
            activeEnrollments = enrollments.filter { it.status == "aktif" || it.status == "selesai" }

            if (activeEnrollments.isEmpty()) {
                layoutNoProgress.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.card_spinner)?.visibility = View.GONE
                view?.findViewById<View>(R.id.card_progress_summary)?.visibility = View.GONE
                view?.findViewById<View>(R.id.tv_tasks_header)?.visibility = View.GONE
                rvTasks.visibility = View.GONE
            } else {
                layoutNoProgress.visibility = View.GONE
                view?.findViewById<View>(R.id.card_spinner)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.card_progress_summary)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.tv_tasks_header)?.visibility = View.VISIBLE
                rvTasks.visibility = View.VISIBLE

                val programTitles = activeEnrollments.map { it.programJudul }
                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, programTitles)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = spinnerAdapter

                // If pre-selected program from bundle or saved in currentEnrollment
                val preSelected = arguments?.getSerializable("selected_program") as? Program
                var selectedIdx = 0
                if (preSelected != null) {
                    val idx = activeEnrollments.indexOfFirst { it.programId == preSelected.id }
                    if (idx != -1) selectedIdx = idx
                } else if (currentEnrollment != null) {
                    val idx = activeEnrollments.indexOfFirst { it.id == currentEnrollment?.id }
                    if (idx != -1) selectedIdx = idx
                }
                spinner.setSelection(selectedIdx)

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        currentEnrollment = activeEnrollments[pos]
                        loadTasks(activeEnrollments[pos])
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun loadTasks(enrollment: Enrollment) {
        repo.getTasks(enrollment.programId) { tasks ->
            repo.getSubmissions(enrollment.id) { submissions ->
                val statuses = submissions.associate { it.taskId to it.status }
                adapter.updateData(tasks, statuses)
                
                // Calculate progress: count only "disetujui" for true completion
                val approvedCount = submissions.count { it.status.lowercase() == "disetujui" }
                val totalCount = tasks.size
                val percent = ProgressUtils.calculateWeightedProgress(totalCount, approvedCount)
                
                tvProgressPercent.text = "$percent%"
                tvTaskSummary.text = "$approvedCount dari $totalCount tugas selesai"
                progressBar.progress = percent

                val btnCert = view?.findViewById<MaterialButton>(R.id.btn_view_certificate)
                if (percent == 100) {
                    btnCert?.visibility = View.VISIBLE
                    
                    // Trigger a re-check in case certificate was missed
                    repo.checkAndIssueCertificate(enrollment.id, repo.currentUser?.id ?: "", repo.currentUser?.namaLengkap ?: "")
                    
                    btnCert?.setOnClickListener {
                        repo.getCertificates(repo.currentUser?.id ?: "") { certs ->
                            val myCert = certs.find { it.enrollmentId == enrollment.id }
                            if (myCert != null) {
                                val fragment = PelajarCertificateDetailFragment().apply {
                                    arguments = Bundle().apply { putSerializable("certificate", myCert) }
                                }
                                (activity as? MainActivity)?.navigateFragment(fragment, true)
                            } else {
                                Toast.makeText(context, "Sertifikat sedang diproses...", Toast.LENGTH_SHORT).show()
                                // Re-trigger just in case
                                repo.checkAndIssueCertificate(enrollment.id, repo.currentUser?.id ?: "", repo.currentUser?.namaLengkap ?: "")
                            }
                        }
                    }
                } else {
                    btnCert?.visibility = View.GONE
                }
            }
        }
    }
}

// ==========================================
// 8. PELAJAR ENROLLED PROGRAMS FRAGMENT
// ==========================================
class PelajarEnrolledProgramsFragment : Fragment() {

    private lateinit var rvPrograms: RecyclerView
    private lateinit var adapter: EnrolledProgramAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_enrolled_programs, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.updateToolbarTitle("Program Saya")

        repo = KerjainRepository.getInstance(requireContext())
        rvPrograms = view.findViewById(R.id.rv_enrolled_programs)
        rvPrograms.layoutManager = LinearLayoutManager(context)

        adapter = EnrolledProgramAdapter(emptyList()) { program ->
            val fragment = PelajarProgressFragment().apply {
                arguments = Bundle().apply { putSerializable("selected_program", program) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvPrograms.adapter = adapter

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        loadData()
        return view
    }

    private fun loadData() {
        val user = repo.currentUser ?: return
        repo.getEnrollments(user.id, "pelajar") { enrollments ->
            val activeEnrollments = enrollments.filter { it.status == "aktif" || it.status == "selesai" }
            val programDataList = mutableListOf<Triple<Enrollment, Program, Int>>()
            
            if (activeEnrollments.isEmpty()) {
                adapter.updateData(emptyList())
                return@getEnrollments
            }

            var loadedCount = 0
            for (enroll in activeEnrollments) {
                repo.getPrograms { allPrograms ->
                    val program = allPrograms.find { it.id == enroll.programId }
                    if (program != null) {
                        repo.getTasks(program.id) { tasks ->
                            repo.getSubmissions(enroll.id) { subs ->
                                val approved = subs.count { it.status.lowercase() == "disetujui" }
                                val progress = ProgressUtils.calculateWeightedProgress(tasks.size, approved)
                                programDataList.add(Triple(enroll, program, progress))
                                
                                loadedCount++
                                if (loadedCount == activeEnrollments.size) {
                                    adapter.updateData(programDataList)
                                }
                            }
                        }
                    } else {
                        loadedCount++
                        if (loadedCount == activeEnrollments.size) {
                            adapter.updateData(programDataList)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. PELAJAR TASK SUBMIT FRAGMENT
// ==========================================
class PelajarTaskSubmitFragment : Fragment() {

    private lateinit var task: Task
    private lateinit var enrollmentId: String
    private lateinit var status: String
    private lateinit var etUrl: EditText
    private lateinit var etNotes: EditText
    private lateinit var cardFeedback: View
    private lateinit var tvFeedbackHeader: TextView
    private lateinit var tvFeedbackText: TextView
    private lateinit var tvFeedbackScore: TextView
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_task_submit, container, false)
        (activity as? MainActivity)?.updateToolbarTitle("Kumpulkan Tugas")

        task = arguments?.getSerializable("task") as? Task ?: return view
        enrollmentId = arguments?.getString("enrollment_id") ?: return view
        status = arguments?.getString("task_status") ?: "Belum Dikirim"
        repo = KerjainRepository.getInstance(requireContext())

        view.findViewById<TextView>(R.id.tv_task_title).text = task.judul
        view.findViewById<TextView>(R.id.tv_task_deadline).text = "Batas Waktu: ${task.deadline}"
        view.findViewById<TextView>(R.id.tv_task_description).text = task.deskripsi

        etUrl = view.findViewById(R.id.et_submission_url)
        etNotes = view.findViewById(R.id.et_submission_notes)

        cardFeedback = view.findViewById(R.id.card_feedback)
        tvFeedbackHeader = view.findViewById(R.id.tv_feedback_header)
        tvFeedbackText = view.findViewById(R.id.tv_feedback_text)
        tvFeedbackScore = view.findViewById(R.id.tv_feedback_score)

        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_submit_task)

        if (status.lowercase() == "disetujui") {
            btnSubmit.text = "Tugas Disetujui"
            btnSubmit.isEnabled = false
            etUrl.isEnabled = false
            etNotes.isEnabled = false
        } else if (status.lowercase() == "menunggu" || status.lowercase() == "menunggu review") {
            btnSubmit.text = "Menunggu Penilaian"
            btnSubmit.isEnabled = false
            etUrl.isEnabled = false
            etNotes.isEnabled = false
        } else if (status.lowercase() == "revisi") {
            btnSubmit.text = "Kirim Revisi"
            btnSubmit.isEnabled = true
            etUrl.isEnabled = true
            etNotes.isEnabled = true
        } else {
            btnSubmit.text = "Kumpulkan Tugas"
            btnSubmit.isEnabled = true
            etUrl.isEnabled = true
            etNotes.isEnabled = true
        }

        btnSubmit.setOnClickListener {
            val url = etUrl.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(context, "Tautan tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = repo.currentUser ?: return@setOnClickListener
            btnSubmit.isEnabled = false

            val sub = Submission(
                enrollmentId = enrollmentId,
                taskId = task.id,
                taskJudul = task.judul,
                pelajarId = user.id,
                pelajarNama = user.namaLengkap,
                fileUrl = url,
                catatan = notes,
                status = "menunggu"
            )

            repo.submitTask(sub) { success ->
                if (success) {
                    Toast.makeText(context, "Tugas berhasil dikumpulkan!", Toast.LENGTH_SHORT).show()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                } else {
                    btnSubmit.isEnabled = true
                    Toast.makeText(context, "Gagal mengumpulkan tugas", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadFeedback()
        return view
    }

    private fun loadFeedback() {
        repo.getSubmissions(enrollmentId) { list ->
            val sub = list.find { it.taskId == task.id }
            if (sub != null) {
                etUrl.setText(sub.fileUrl)
                etNotes.setText(sub.catatan)

                val btnSubmit = view?.findViewById<MaterialButton>(R.id.btn_submit_task)
                if (btnSubmit != null) {
                    val currentStatus = sub.status.lowercase()
                    if (currentStatus == "disetujui") {
                        btnSubmit.text = "Tugas Disetujui"
                        btnSubmit.isEnabled = false
                        etUrl.isEnabled = false
                        etNotes.isEnabled = false
                    } else if (currentStatus == "menunggu" || currentStatus == "menunggu review") {
                        btnSubmit.text = "Menunggu Penilaian"
                        btnSubmit.isEnabled = false
                        etUrl.isEnabled = false
                        etNotes.isEnabled = false
                    } else if (currentStatus == "revisi") {
                        btnSubmit.text = "Kirim Revisi"
                        btnSubmit.isEnabled = true
                        etUrl.isEnabled = true
                        etNotes.isEnabled = true
                    } else {
                        btnSubmit.text = "Kumpulkan Tugas"
                        btnSubmit.isEnabled = true
                        etUrl.isEnabled = true
                        etNotes.isEnabled = true
                    }
                }

                if (sub.status.lowercase() == "disetujui" || sub.status.lowercase() == "revisi") {
                    cardFeedback.visibility = View.VISIBLE
                    tvFeedbackHeader.text = if (sub.status == "disetujui") "Tugas Disetujui" else "Permintaan Revisi"
                    tvFeedbackHeader.setTextColor(if (sub.status == "disetujui") Color.parseColor("#10AC84") else Color.parseColor("#EE5253"))
                    tvFeedbackText.text = sub.feedback ?: "Tidak ada catatan feedback."
                    tvFeedbackScore.text = "Nilai: ${sub.nilai ?: "-"}"
                } else {
                    cardFeedback.visibility = View.GONE
                }
            }
        }
    }
}

// Helper color representation
class Color {
    companion object {
        fun parseColor(colorString: String): Int {
            return android.graphics.Color.parseColor(colorString)
        }
    }
}

// ==========================================
// 6. PELAJAR PROFILE FRAGMENT
// ==========================================
class PelajarProfileFragment : Fragment() {

    private lateinit var rvCertificates: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: CertificateAdapter
    private lateinit var repo: KerjainRepository
    
    private lateinit var tvProfileInitial: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var cardProfileAvatar: View
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileSchool: TextView
    private lateinit var tvProfileEmail: TextView
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
        val view = inflater.inflate(R.layout.fragment_pelajar_profile, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)
        (activity as? MainActivity)?.updateToolbarTitle("")

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        tvProfileInitial = view.findViewById(R.id.tv_profile_initial)
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo)
        cardProfileAvatar = view.findViewById(R.id.card_profile_avatar)
        tvProfileName = view.findViewById(R.id.tv_profile_name)
        tvProfileSchool = view.findViewById(R.id.tv_profile_school)
        tvProfileEmail = view.findViewById(R.id.tv_profile_email)
        tvProfileBio = view.findViewById(R.id.tv_profile_bio)

        // Populate initial UI
        tvProfileInitial.text = user.namaLengkap.take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, tvProfileInitial, ivProfilePhoto)
        tvProfileName.text = user.namaLengkap
        tvProfileSchool.text = "${user.universitas} • ${user.jurusan} ${user.angkatan}"
        tvProfileEmail.text = user.email
        tvProfileBio.text = user.bio

        cardProfileAvatar.setOnClickListener {
            showPhotoPickerDialog()
        }

        rvCertificates = view.findViewById(R.id.rv_certificates)
        tvEmpty = view.findViewById(R.id.tv_empty_certificates)
        rvCertificates.layoutManager = LinearLayoutManager(context)
        adapter = CertificateAdapter(emptyList()) { cert ->
            val fragment = PelajarCertificateDetailFragment().apply {
                arguments = Bundle().apply { putSerializable("certificate", cert) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvCertificates.adapter = adapter

        val btnEditProfile = view.findViewById<Button>(R.id.btn_edit_profile)
        btnEditProfile.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile_pelajar, null)
            val etName = dialogView.findViewById<EditText>(R.id.et_edit_name)
            val etEmail = dialogView.findViewById<EditText>(R.id.et_edit_email)
            val etUniv = dialogView.findViewById<EditText>(R.id.et_edit_univ)
            val etMajor = dialogView.findViewById<EditText>(R.id.et_edit_major)
            val etYear = dialogView.findViewById<EditText>(R.id.et_edit_year)
            val etBio = dialogView.findViewById<EditText>(R.id.et_edit_bio)

            etName.setText(user.namaLengkap)
            etEmail?.setText(user.email)
            etEmail?.isEnabled = false // Disable email edit

            etUniv.setText(user.universitas ?: "")
            etMajor.setText(user.jurusan ?: "")
            etYear.setText(user.angkatan?.toString() ?: "")
            etBio.setText(user.bio ?: "")

            android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Simpan") { dialog, _ ->
                    val newName = etName.text.toString().trim()
                    val newUniv = etUniv.text.toString().trim()
                    val newMajor = etMajor.text.toString().trim()
                    val newYearText = etYear.text.toString().trim()
                    val newBio = etBio.text.toString().trim()

                    if (newName.isEmpty()) {
                        Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    user.namaLengkap = newName
                    user.universitas = newUniv
                    user.jurusan = newMajor
                    user.angkatan = newYearText.toIntOrNull() ?: user.angkatan
                    user.bio = newBio

                    // Optimistic instant UI update
                    tvProfileInitial.text = user.namaLengkap.take(1).uppercase()
                    tvProfileName.text = user.namaLengkap
                    tvProfileSchool.text = "${user.universitas} • ${user.jurusan} ${user.angkatan}"
                    tvProfileBio.text = user.bio

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

        view.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            repo.logout()
            (activity as? MainActivity)?.onUserLoggedOut()
        }

        loadCertificates()
        return view
    }

    private fun loadCertificates() {
        val user = repo.currentUser ?: return
        repo.getCertificates(user.id) { list ->
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvCertificates.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvCertificates.visibility = View.VISIBLE
                adapter.updateData(list)
            }
        }
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
// 7. PELAJAR NOTIFICATIONS FRAGMENT
// ==========================================
class PelajarNotificationsFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: NotificationAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_notifications, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.updateToolbarTitle("Pusat Notifikasi")

        repo = KerjainRepository.getInstance(requireContext())
        rvNotifications = view.findViewById(R.id.rv_pelajar_notifications)
        tvEmpty = view.findViewById(R.id.tv_empty_notifications)

        rvNotifications.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(emptyList()) { notif ->
            // Mark as read in repository
            repo.markNotificationAsRead(notif.id) { success ->
                if (success && isAdded) {
                    loadNotifications() // Reload list to remove unread dot
                }
            }

            // Show a dialog detailing the notification
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(when (notif.type) {
                    "new_enrollment" -> "Pendaftaran Program Baru"
                    "program_completed" -> "Pelajar Menyelesaikan Program"
                    "new_submission" -> "Tugas Menunggu Review"
                    "submission_revision" -> "Pelajar Mengirim Revisi"
                    "submission_reviewed" -> "Hasil Review Tugas"
                    "program_created" -> "Program Baru Diterbitkan"
                    "welcome" -> "Selamat Datang"
                    else -> "Pemberitahuan"
                })
                .setMessage(notif.message)
                .setPositiveButton("Tutup", null)
            
            val dialog = builder.create()
            dialog.show()

            // Adjust position to avoid notch/status bar
            val window = dialog.window
            if (window != null) {
                val params = window.attributes
                params.gravity = android.view.Gravity.CENTER
                params.y = 50 // Move down slightly from center if needed, or just rely on CENTER
                window.attributes = params
            }
        }
        rvNotifications.adapter = adapter

        loadNotifications()
        return view
    }

    private fun loadNotifications() {
        val user = repo.currentUser ?: return
        repo.getNotifications(user.id) { list ->
            if (!isAdded) return@getNotifications
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvNotifications.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvNotifications.visibility = View.VISIBLE
                adapter.updateData(list)
            }
        }
    }
}

// ==========================================
// 8. PELAJAR CERTIFICATE DETAIL FRAGMENT
// ==========================================
class PelajarCertificateDetailFragment : Fragment() {
    private lateinit var certificate: Certificate
    private lateinit var repo: KerjainRepository
    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: CertificateTaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_certificate_detail, container, false)
        (activity as? MainActivity)?.showBottomNav(false)
        (activity as? MainActivity)?.showToolbar(false)

        certificate = arguments?.getSerializable("certificate") as? Certificate ?: return view
        repo = KerjainRepository.getInstance(requireContext())

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        view.findViewById<TextView>(R.id.tv_detail_cert_mitras).text = "KERJAIN x ${certificate.mitraNama.uppercase()}"
        view.findViewById<TextView>(R.id.tv_detail_cert_program).text = certificate.programJudul
        
        val dateString = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date(certificate.issuedAt))
        view.findViewById<TextView>(R.id.tv_detail_cert_date).text = dateString
        view.findViewById<TextView>(R.id.tv_detail_cert_id).text = certificate.nomorSertifikat

        rvTasks = view.findViewById(R.id.rv_cert_tasks)
        rvTasks.layoutManager = LinearLayoutManager(context)
        adapter = CertificateTaskAdapter(emptyList())
        rvTasks.adapter = adapter

        view.findViewById<View>(R.id.btn_share_linkedin).setOnClickListener {
            shareToLinkedIn()
        }

        loadData()
        return view
    }

    private fun loadData() {
        if (!isAdded) return
        
        repo.getSubmissions(certificate.enrollmentId) { submissions ->
            if (!isAdded) return@getSubmissions
            
            val approvedSubs = submissions.filter { it.status.lowercase() == "disetujui" }
            
            // 1. Calculate Average Score from approved tasks
            val finalScore = if (approvedSubs.isNotEmpty()) {
                approvedSubs.mapNotNull { it.nilai }.average().toInt()
            } else {
                0
            }
            view?.findViewById<TextView>(R.id.tv_detail_cert_score)?.text = finalScore.toString()

            repo.getEnrollments(repo.currentUser?.id ?: "", "pelajar") { enrollments ->
                if (!isAdded) return@getEnrollments
                
                val enrollment = enrollments.find { it.id == certificate.enrollmentId }
                if (enrollment != null) {
                    repo.getTasks(enrollment.programId) { tasks ->
                        if (!isAdded) return@getTasks
                        adapter.updateData(tasks)
                        
                        repo.getPrograms { programs ->
                            if (!isAdded) return@getPrograms
                            val program = programs.find { it.id == enrollment.programId }
                            if (program != null) {
                                try {
                                    val dbFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    val displayFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                                    
                                    val start = dbFormat.parse(program.tanggalMulai)
                                    val end = dbFormat.parse(program.tanggalSelesai)
                                    
                                    if (start != null && end != null) {
                                        // 2. Show Date Range (Start - End)
                                        val dateRange = "${displayFormat.format(start)} - ${displayFormat.format(end)}"
                                        view?.findViewById<TextView>(R.id.tv_detail_cert_date)?.text = dateRange
                                        
                                        // 3. Calculate Duration in Days/Weeks
                                        val diffMillis = end.time - start.time
                                        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                                        
                                        val durationText = when {
                                            diffDays >= 30 -> "${diffDays / 7} Minggu"
                                            else -> "$diffDays Hari"
                                        }
                                        view?.findViewById<TextView>(R.id.tv_detail_cert_duration)?.text = durationText
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shareToLinkedIn() {
        val certName = Uri.encode(certificate.programJudul)
        val orgName = Uri.encode(certificate.mitraNama)
        val certId = Uri.encode(certificate.nomorSertifikat)
        
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = certificate.issuedAt }
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        
        val url = "https://www.linkedin.com/profile/add?startTask=CERTIFICATION_NAME&name=$certName&organizationName=$orgName&issueYear=$year&issueMonth=$month&certId=$certId"
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}

// ==========================================
// 9. PELAJAR ALL CERTIFICATES FRAGMENT
// ==========================================
class PelajarCertificatesFragment : Fragment() {
    private lateinit var rvCertificates: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var adapter: CertificateAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelajar_certificates, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)

        repo = KerjainRepository.getInstance(requireContext())
        rvCertificates = view.findViewById(R.id.rv_all_certificates)
        layoutEmpty = view.findViewById(R.id.layout_empty_certs)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        rvCertificates.layoutManager = LinearLayoutManager(context)
        adapter = CertificateAdapter(emptyList()) { cert ->
            val fragment = PelajarCertificateDetailFragment().apply {
                arguments = Bundle().apply { putSerializable("certificate", cert) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvCertificates.adapter = adapter

        loadData()
        return view
    }

    private fun loadData() {
        val user = repo.currentUser ?: return
        repo.getCertificates(user.id) { list ->
            if (!isAdded) return@getCertificates
            if (list.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvCertificates.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvCertificates.visibility = View.VISIBLE
                adapter.updateData(list)
            }
        }
    }
}
