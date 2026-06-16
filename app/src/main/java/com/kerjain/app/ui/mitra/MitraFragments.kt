package com.kerjain.app.ui.mitra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kerjain.app.MainActivity
import com.kerjain.app.R
import com.kerjain.app.model.*
import com.kerjain.app.repository.KerjainRepository
import com.kerjain.app.ui.ProgramAdapter
import com.kerjain.app.ui.TalentAdapter
import com.kerjain.app.ui.TaskAdapter
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.kerjain.app.ui.ImageUtils
import android.app.DatePickerDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

// ==========================================
// 1. MITRA HOME FRAGMENT
// ==========================================
class MitraHomeFragment : Fragment() {

    private lateinit var rvPrograms: RecyclerView
    private lateinit var rvTalents: RecyclerView
    private lateinit var tvProgramCount: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var adapter: ProgramAdapter
    private lateinit var talentAdapter: TalentAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_home, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)
        (activity as? MainActivity)?.updateToolbarTitle("Dashboard Mitra")

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        view.findViewById<TextView>(R.id.tv_mitra_header_label).text = getGreeting() + " 🏢"
        view.findViewById<TextView>(R.id.tv_mitra_company).text = user.namaUsaha ?: "Perusahaan Anda"

        val tvInitialHome = view.findViewById<TextView>(R.id.tv_profile_initial_home)
        val ivPhotoHome = view.findViewById<ImageView>(R.id.iv_profile_photo_home)
        tvInitialHome.text = (user.namaUsaha ?: user.namaLengkap).take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, tvInitialHome, ivPhotoHome)

        tvProgramCount = view.findViewById(R.id.tv_mitra_program_count)
        tvStudentCount = view.findViewById(R.id.tv_mitra_student_count)

        rvPrograms = view.findViewById(R.id.rv_mitra_programs)
        rvPrograms.layoutManager = LinearLayoutManager(context)
        adapter = ProgramAdapter(emptyList(), isMitraView = true, layoutResId = R.layout.item_program_mitra_home) { program ->
            val fragment = MitraProgramDetailFragment().apply {
                arguments = Bundle().apply { putSerializable("program", program) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvPrograms.adapter = adapter
        
        view.findViewById<View>(R.id.btn_see_applicants).setOnClickListener {
            val fragment = MitraStudentListFragment()
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }

        rvTalents = view.findViewById(R.id.rv_mitra_talents)
        rvTalents.layoutManager = LinearLayoutManager(context)
        talentAdapter = TalentAdapter(emptyList(), onItemClick = { student ->
            showStudentDetailDialog(requireContext(), student, repo)
        })
        rvTalents.adapter = talentAdapter


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

        loadMetricsAndPrograms()
        return view
    }

    private fun loadMetricsAndPrograms() {
        val user = repo.currentUser ?: return
        
        view?.findViewById<TextView>(R.id.tv_mitra_header_label)?.text = getGreeting() + " 🏢"
        
        view?.findViewById<View>(R.id.card_total_students)?.setOnClickListener {
            val fragment = MitraStudentListFragment()
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        
        repo.getPrograms { list ->
            if (!isAdded) return@getPrograms
            val myPrograms = list.filter { it.mitraId == user.id && it.status == "published" }
            val myProgramIds = myPrograms.map { it.id }.toSet()
            tvProgramCount.text = myPrograms.size.toString()

            repo.getEnrollments(user.id, "mitra") { enrollments ->
                if (!isAdded) return@getEnrollments
                val filteredEnrollments = enrollments.filter { it.programId in myProgramIds }
                
                // Total unique participants count
                val uniqueStudentCount = filteredEnrollments.distinctBy { it.pelajarId }.size
                tvStudentCount.text = uniqueStudentCount.toString()
                
                // Real-time calculation for "New Applicants" (Pendaftar Baru)
                val now = System.currentTimeMillis()
                val threeDaysMillis = 3 * 24 * 60 * 60 * 1000L
                val recentEnrollments = filteredEnrollments.filter { (now - it.enrolledAt) < threeDaysMillis }
                
                view?.findViewById<TextView>(R.id.tv_count_new_applicants)?.text = recentEnrollments.size.toString()
                view?.findViewById<TextView>(R.id.tv_featured_title)?.text = "${recentEnrollments.size} Pelamar Baru\nMinggu Ini!"

                // Map for statistics: ProgramId -> (ParticipantCount, AverageCompletion)
                val programStats = mutableMapOf<String, Pair<Int, Int>>()
                var processedCount = 0

                if (myPrograms.isEmpty()) {
                    adapter.updateData(emptyList(), emptyMap())
                    talentAdapter.updateData(emptyList())
                    return@getEnrollments
                }

                for (prog in myPrograms) {
                    val programEnrollments = filteredEnrollments.filter { it.programId == prog.id }
                    val participantCount = programEnrollments.size
                    
                    if (participantCount == 0) {
                        programStats[prog.id] = 0 to 0
                        processedCount++
                        if (processedCount == myPrograms.size) {
                            adapter.updateData(myPrograms, programStats)
                        }
                    } else {
                        repo.getTasks(prog.id) { tasks ->
                            if (!isAdded) return@getTasks
                            var completedParticipants = 0
                            var loadedSubs = 0
                            
                            for (enroll in programEnrollments) {
                                repo.getSubmissions(enroll.id) { subs ->
                                    if (!isAdded) return@getSubmissions
                                    val approvedCount = subs.count { it.status.lowercase() == "disetujui" }
                                    if (approvedCount >= tasks.size && tasks.isNotEmpty()) {
                                        completedParticipants++
                                    }
                                    
                                    loadedSubs++
                                    if (loadedSubs == programEnrollments.size) {
                                        val avgComp = (completedParticipants * 100) / participantCount
                                        programStats[prog.id] = participantCount to avgComp
                                        
                                        processedCount++
                                        if (processedCount == myPrograms.size) {
                                            adapter.updateData(myPrograms, programStats)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fetch student profiles for the Latest Applicants section
                if (filteredEnrollments.isNotEmpty()) {
                    val latestEnrollments = filteredEnrollments.sortedByDescending { it.enrolledAt }.take(10)
                    val latestStudentIds = latestEnrollments.map { it.pelajarId }.toSet()

                    repo.getTalentPool { allStudents ->
                        if (!isAdded) return@getTalentPool
                        // Filter students who are in the latest enrollments
                        val latestStudents = allStudents.filter { it.id in latestStudentIds }
                        // Sort them to match the enrollment order
                        val sortedStudents = latestStudents.sortedBy { s -> 
                            latestEnrollments.find { it.pelajarId == s.id }?.enrolledAt ?: 0L 
                        }.reversed()
                        
                        talentAdapter.updateData(sortedStudents)
                    }
                } else {
                    talentAdapter.updateData(emptyList())
                }
            }
        }
    }

    private fun getGreeting(): String {
        return "Hallo"
    }
}

// ==========================================
// 2. MITRA MANAGE PROGRAMS FRAGMENT
// ==========================================
class MitraManageFragment : Fragment() {

    private lateinit var rvPrograms: RecyclerView
    private lateinit var adapter: ProgramAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_manage, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.updateToolbarTitle("Kelola Program")

        repo = KerjainRepository.getInstance(requireContext())
        rvPrograms = view.findViewById(R.id.rv_mitra_manage_programs)
        rvPrograms.layoutManager = LinearLayoutManager(context)
        
        adapter = ProgramAdapter(emptyList(), isMitraView = true) { program ->
            val fragment = MitraProgramDetailFragment().apply {
                arguments = Bundle().apply { putSerializable("program", program) }
            }
            (activity as? MainActivity)?.navigateFragment(fragment, true)
        }
        rvPrograms.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_program)
        fab.setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(MitraCreateProgramFragment(), true)
        }

        view.findViewById<View>(R.id.btn_view_drafts).setOnClickListener {
            (activity as? MainActivity)?.navigateFragment(MitraDraftProgramsFragment(), true)
        }

        loadPrograms()
        return view
    }

    private fun loadPrograms() {
        val user = repo.currentUser ?: return
        repo.getPrograms { list ->
            val myPrograms = list.filter { it.mitraId == user.id && it.status == "published" }
            adapter.updateData(myPrograms)
        }
    }
}

// ==========================================
// 3. MITRA CREATE PROGRAM FRAGMENT
// ==========================================
class MitraCreateProgramFragment : Fragment() {

    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var spinnerBidang: AutoCompleteTextView
    private lateinit var etBidangCustom: EditText
    private lateinit var layoutBidangCustom: View
    private lateinit var etKuota: EditText
    private lateinit var etStart: EditText
    private lateinit var etEnd: EditText
    
    // Task builder fields
    private lateinit var etTaskTitle: EditText
    private lateinit var etTaskDesc: EditText
    private lateinit var tvTaskCountHeader: TextView
    private lateinit var tvTaskListSummary: TextView

    private val tempTasks = mutableListOf<Task>()
    private lateinit var repo: KerjainRepository
    private var existingProgram: Program? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_create_program, container, false)
        (activity as? MainActivity)?.showToolbar(false)
        (activity as? MainActivity)?.showBottomNav(false)

        repo = KerjainRepository.getInstance(requireContext())
        existingProgram = arguments?.getSerializable("program") as? Program

        val tvHeader = view.findViewById<TextView>(R.id.tv_create_prog_header)
        if (existingProgram != null) {
            tvHeader?.text = "Edit Program"
        } else {
            tvHeader?.text = "Buat Program Baru"
        }

        etTitle = view.findViewById(R.id.et_prog_title)
        etDesc = view.findViewById(R.id.et_prog_desc)
        spinnerBidang = view.findViewById(R.id.spinner_prog_bidang)
        etBidangCustom = view.findViewById(R.id.et_prog_bidang_custom)
        layoutBidangCustom = view.findViewById(R.id.layout_bidang_lainnya)
        etKuota = view.findViewById(R.id.et_prog_kuota)
        etStart = view.findViewById(R.id.et_prog_start)
        etEnd = view.findViewById(R.id.et_prog_end)

        setupBidangSpinner()
        setupDatePickers()

        etTaskTitle = view.findViewById(R.id.et_task_builder_title)
        etTaskDesc = view.findViewById(R.id.et_task_builder_desc)
        tvTaskCountHeader = view.findViewById(R.id.tv_added_tasks_header)
        tvTaskListSummary = view.findViewById(R.id.tv_added_tasks_list)

        if (existingProgram != null) {
            populateData(existingProgram!!)
        }

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        
        view.findViewById<View>(R.id.btn_cancel)?.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        val btnAddTask = view.findViewById<Button>(R.id.btn_add_task_to_list)
        btnAddTask.setOnClickListener {
            addTask()
        }

        val btnSaveDraft = view.findViewById<Button>(R.id.btn_save_draft)
        btnSaveDraft.setOnClickListener {
            publishProgram(isDraft = true)
        }

        val btnPublish = view.findViewById<Button>(R.id.btn_publish_program)
        btnPublish.setOnClickListener {
            publishProgram(isDraft = false)
        }

        return view
    }

    private fun populateData(program: Program) {
        etTitle.setText(program.judul)
        etDesc.setText(program.deskripsi)
        etKuota.setText(program.kuota?.toString() ?: "")
        etStart.setText(program.tanggalMulai)
        etEnd.setText(program.tanggalSelesai)
        
        val standardBidang = arrayOf("UI/UX", "Pemrograman", "Akuntansi", "Design", "Data Analyst")
        if (program.bidang in standardBidang) {
            spinnerBidang.setText(program.bidang, false)
        } else {
            spinnerBidang.setText("Lainnya", false)
            layoutBidangCustom.visibility = View.VISIBLE
            etBidangCustom.setText(program.bidang)
        }

        repo.getTasks(program.id) { tasks ->
            tempTasks.clear()
            tempTasks.addAll(tasks)
            updateTaskUI()
        }
    }

    private fun setupBidangSpinner() {
        val items = arrayOf("UI/UX", "Pemrograman", "Akuntansi", "Design", "Data Analyst", "Lainnya")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        spinnerBidang.setAdapter(adapter)

        spinnerBidang.setOnItemClickListener { _, _, position, _ ->
            if (items[position] == "Lainnya") {
                layoutBidangCustom.visibility = View.VISIBLE
            } else {
                layoutBidangCustom.visibility = View.GONE
            }
        }
    }

    private fun setupDatePickers() {
        etStart.setOnClickListener {
            showDatePicker(etStart)
        }
        etEnd.setOnClickListener {
            showDatePicker(etEnd)
        }
        
        // Prevent manual typing
        etStart.isFocusable = false
        etStart.isClickable = true
        etEnd.isFocusable = false
        etEnd.isClickable = true
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate.time))
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun addTask() {
        val title = etTaskTitle.text.toString().trim()
        val desc = etTaskDesc.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(context, "Judul tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val order = tempTasks.size + 1
        val newTask = Task(
            judul = title,
            deskripsi = desc,
            urutan = order,
            deadline = "2026-08-${(10..28).random()} 23:59"
        )
        tempTasks.add(newTask)

        // Clear task builder inputs
        etTaskTitle.setText("")
        etTaskDesc.setText("")

        updateTaskUI()
    }

    private fun updateTaskUI() {
        tvTaskCountHeader.text = "Daftar Tugas yang Ditambahkan (${tempTasks.size})"
        val sb = StringBuilder()
        for ((i, task) in tempTasks.withIndex()) {
            sb.append("${i + 1}. ${task.judul}\n")
        }
        tvTaskListSummary.text = if (tempTasks.isEmpty()) "Belum ada tugas ditambahkan." else sb.toString()
    }

    private fun publishProgram(isDraft: Boolean = false) {
        val title = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()
        
        val selectedBidang = spinnerBidang.text.toString()
        val bidang = if (selectedBidang == "Lainnya") {
            etBidangCustom.text.toString().trim()
        } else {
            selectedBidang
        }

        val kuotaText = etKuota.text.toString().trim()
        val start = etStart.text.toString().trim()
        val end = etEnd.text.toString().trim()

        if (!isDraft) {
            if (title.isEmpty() || desc.isEmpty() || bidang.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi semua data program", Toast.LENGTH_SHORT).show()
                return
            }

            if (tempTasks.size < 3) {
                Toast.makeText(context, "Program minimal harus memiliki 3 tugas sebelum dapat dipublikasikan.", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (title.isEmpty()) {
                Toast.makeText(context, "Berikan judul untuk menyimpan draft", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val kuota = kuotaText.toIntOrNull() ?: 10
        val user = repo.currentUser ?: return

        val programToSave = existingProgram ?: Program()
        programToSave.apply {
            mitraId = user.id
            mitraNama = user.namaUsaha ?: user.namaLengkap
            judul = title
            deskripsi = desc
            this.bidang = bidang
            status = if (isDraft) "draft" else "published"
            this.kuota = kuota
            tanggalMulai = start
            tanggalSelesai = end
        }

        repo.createProgram(programToSave, tempTasks) { success ->
            if (success) {
                val msg = if (isDraft) "Draft Berhasil Disimpan!" else "Program Berhasil Diterbitkan!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                activity?.onBackPressedDispatcher?.onBackPressed()
            } else {
                Toast.makeText(context, "Gagal menyimpan program", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ==========================================
// 8. MITRA DRAFT PROGRAMS FRAGMENT
// ==========================================
class MitraDraftProgramsFragment : Fragment() {

    private lateinit var rvPrograms: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var adapter: ProgramAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_drafts, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)

        repo = KerjainRepository.getInstance(requireContext())
        rvPrograms = view.findViewById(R.id.rv_mitra_draft_programs)
        layoutEmpty = view.findViewById(R.id.layout_empty_drafts)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        rvPrograms.layoutManager = LinearLayoutManager(context)
        adapter = ProgramAdapter(emptyList(), isMitraView = true) { program ->
            // Show options for draft
            val options = arrayOf<CharSequence>("Lihat / Edit", "Hapus Permanen")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(program.judul)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val fragment = MitraCreateProgramFragment().apply {
                                arguments = Bundle().apply { putSerializable("program", program) }
                            }
                            (activity as? MainActivity)?.navigateFragment(fragment, true)
                        }
                        1 -> {
                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Hapus Draft?")
                                .setMessage("Tindakan ini tidak dapat dibatalkan.")
                                .setPositiveButton("Hapus") { _, _ ->
                                    repo.deleteProgram(program.id) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Draft dihapus", Toast.LENGTH_SHORT).show()
                                            loadPrograms()
                                        }
                                    }
                                }
                                .setNegativeButton("Batal", null)
                                .show()
                        }
                    }
                }.show()
        }
        rvPrograms.adapter = adapter

        loadPrograms()
        return view
    }

    private fun loadPrograms() {
        val user = repo.currentUser ?: return
        repo.getPrograms { list ->
            if (!isAdded) return@getPrograms
            val myDrafts = list.filter { it.mitraId == user.id && it.status == "draft" }
            if (myDrafts.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvPrograms.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvPrograms.visibility = View.VISIBLE
                adapter.updateData(myDrafts)
            }
        }
    }
}

// ==========================================
// 4. MITRA TALENT POOL FRAGMENT
// ==========================================
class MitraTalentFragment : Fragment() {

    private lateinit var rvTalents: RecyclerView
    private lateinit var spinnerProgram: AutoCompleteTextView
    private lateinit var adapter: TalentAdapter
    private lateinit var repo: KerjainRepository
    
    private var myPrograms = listOf<Program>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_talent, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.updateToolbarTitle("Talenta Pelajar")

        repo = KerjainRepository.getInstance(requireContext())
        rvTalents = view.findViewById(R.id.rv_mitra_talents)
        rvTalents.layoutManager = LinearLayoutManager(context)
        spinnerProgram = view.findViewById(R.id.spinner_filter_program)

        adapter = TalentAdapter(emptyList(), onItemClick = { student ->
            showStudentDetailDialog(requireContext(), student, repo)
        })
        rvTalents.adapter = adapter

        setupProgramSpinner()
        return view
    }

    private fun setupProgramSpinner() {
        val user = repo.currentUser ?: return
        repo.getPrograms { programs ->
            myPrograms = programs.filter { it.mitraId == user.id }
            val titles = myPrograms.map { it.judul }
            val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, titles)
            spinnerProgram.setAdapter(spinnerAdapter)

            if (myPrograms.isNotEmpty()) {
                spinnerProgram.setText(myPrograms[0].judul, false)
                loadTalentsForProgram(myPrograms[0])
            }

            spinnerProgram.setOnItemClickListener { _, _, position, _ ->
                loadTalentsForProgram(myPrograms[position])
            }
        }
    }

    private fun loadTalentsForProgram(program: Program) {
        val mitra = repo.currentUser ?: return
        repo.getEnrollments(mitra.id, "mitra") { enrollments ->
            val programEnrollments = enrollments.filter { it.programId == program.id }
            val studentIds = programEnrollments.map { it.pelajarId }.toSet()
            
            repo.getTalentPool { allTalents ->
                val participants = allTalents.filter { it.id in studentIds }
                val studentScores = mutableMapOf<String, Int>()
                var processed = 0

                if (participants.isEmpty()) {
                    adapter.updateData(emptyList(), emptyMap())
                    return@getTalentPool
                }

                for (student in participants) {
                    val enroll = programEnrollments.find { it.pelajarId == student.id }
                    if (enroll != null) {
                        repo.getSubmissions(enroll.id) { subs ->
                            val approved = subs.filter { it.status.lowercase() == "disetujui" }
                            if (approved.isNotEmpty()) {
                                val avg = approved.map { it.nilai ?: 0 }.average().toInt()
                                studentScores[student.id] = avg
                            } else {
                                studentScores[student.id] = 0
                            }
                            
                            processed++
                            if (processed == participants.size) {
                                adapter.updateData(participants, studentScores)
                            }
                        }
                    } else {
                        processed++
                        if (processed == participants.size) {
                            adapter.updateData(participants, studentScores)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. MITRA PROFILE FRAGMENT
// ==========================================
class MitraProfileFragment : Fragment() {

    private lateinit var repo: KerjainRepository
    
    private lateinit var tvProfileInitial: TextView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var cardProfileAvatar: View
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileCompany: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileLocation: TextView
    private lateinit var tvProfileWebsite: TextView
    private lateinit var tvProfileContact: TextView
    private lateinit var tvProfileDomain: TextView

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
        val view = inflater.inflate(R.layout.fragment_mitra_profile, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.updateToolbarTitle("")

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        tvProfileInitial = view.findViewById(R.id.tv_mitra_profile_initial)
        ivProfilePhoto = view.findViewById(R.id.iv_mitra_profile_photo)
        cardProfileAvatar = view.findViewById(R.id.card_mitra_profile_avatar)
        tvProfileName = view.findViewById(R.id.tv_mitra_profile_name)
        tvProfileCompany = view.findViewById(R.id.tv_mitra_profile_company)
        tvProfileEmail = view.findViewById(R.id.tv_mitra_profile_email)
        tvProfileLocation = view.findViewById(R.id.tv_mitra_location)
        tvProfileWebsite = view.findViewById(R.id.tv_mitra_website)
        tvProfileContact = view.findViewById(R.id.tv_mitra_contact)
        tvProfileDomain = view.findViewById(R.id.tv_mitra_domain)

        // Populate initial UI
        tvProfileInitial.text = (user.namaUsaha ?: user.namaLengkap).take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, tvProfileInitial, ivProfilePhoto)
        tvProfileName.text = user.namaLengkap
        tvProfileCompany.text = "${user.namaUsaha} • ${user.bidangUsaha}"
        tvProfileEmail.text = user.email
        tvProfileLocation.text = "Kota: ${user.kota}"
        tvProfileWebsite.text = user.website ?: "Tidak ada website."
        tvProfileContact.text = user.kontakBisnis ?: user.email
        tvProfileDomain.text = user.emailDomain ?: "tidak_diatur.com"

        cardProfileAvatar.setOnClickListener {
            showPhotoPickerDialog()
        }

        val btnEditProfile = view.findViewById<Button>(R.id.btn_edit_profile)
        btnEditProfile.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile_mitra, null)
            val etName = dialogView.findViewById<EditText>(R.id.et_edit_mitra_name)
            val etEmail = dialogView.findViewById<EditText>(R.id.et_edit_mitra_email_account)
            val etCompany = dialogView.findViewById<EditText>(R.id.et_edit_mitra_company)
            val etField = dialogView.findViewById<EditText>(R.id.et_edit_mitra_field)
            val etCity = dialogView.findViewById<EditText>(R.id.et_edit_mitra_city)
            val etWebsite = dialogView.findViewById<EditText>(R.id.et_edit_mitra_website)
            val etContact = dialogView.findViewById<EditText>(R.id.et_edit_mitra_contact)

            etName.setText(user.namaLengkap)
            etEmail?.setText(user.email)
            etEmail?.isEnabled = false
            
            etCompany.setText(user.namaUsaha ?: "")
            etField.setText(user.bidangUsaha ?: "")
            etCity.setText(user.kota ?: "")
            etWebsite.setText(user.website ?: "")
            etContact.setText(user.kontakBisnis ?: "")

            android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Simpan") { dialog, _ ->
                    val newName = etName.text.toString().trim()
                    val newCompany = etCompany.text.toString().trim()
                    val newField = etField.text.toString().trim()
                    val newCity = etCity.text.toString().trim()
                    val newWebsite = etWebsite.text.toString().trim()
                    val newContact = etContact.text.toString().trim()

                    if (newName.isEmpty() || newCompany.isEmpty()) {
                        Toast.makeText(context, "Nama dan Nama Perusahaan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    user.namaLengkap = newName
                    user.namaUsaha = newCompany
                    user.bidangUsaha = newField
                    user.kota = newCity
                    user.website = newWebsite
                    user.kontakBisnis = newContact

                    // Optimistic UI updates
                    tvProfileInitial.text = (user.namaUsaha ?: user.namaLengkap).take(1).uppercase()
                    tvProfileName.text = user.namaLengkap
                    tvProfileCompany.text = "${user.namaUsaha} • ${user.bidangUsaha}"
                    tvProfileEmail.text = user.email
                    tvProfileLocation.text = "Kota: ${user.kota}"
                    tvProfileWebsite.text = user.website ?: "Tidak ada website."
                    tvProfileContact.text = user.kontakBisnis ?: user.email

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

        view.findViewById<Button>(R.id.btn_mitra_logout).setOnClickListener {
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
// 6. MITRA PROGRAM DETAIL FRAGMENT
// ==========================================
class MitraProgramDetailFragment : Fragment() {
    private lateinit var program: Program
    private lateinit var rvTasks: RecyclerView
    private lateinit var rvParticipants: RecyclerView
    private lateinit var tvEmptyParticipants: TextView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var participantAdapter: TalentAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_program_detail, container, false)
        (activity as? MainActivity)?.updateToolbarTitle("Kelola Program")

        program = arguments?.getSerializable("program") as? Program ?: return view
        repo = KerjainRepository.getInstance(requireContext())

        view.findViewById<TextView>(R.id.tv_mitra_detail_title).text = program.judul
        view.findViewById<TextView>(R.id.tv_mitra_detail_category).text = program.bidang.uppercase()
        view.findViewById<TextView>(R.id.tv_mitra_detail_quota).text = "Kuota: ${program.kuota ?: "Tidak Terbatas"}"
        view.findViewById<TextView>(R.id.tv_mitra_detail_duration).text = "Mulai ${program.tanggalMulai} s/d ${program.tanggalSelesai}"
        view.findViewById<TextView>(R.id.tv_mitra_detail_description).text = program.deskripsi

        rvTasks = view.findViewById(R.id.rv_mitra_program_detail_tasks)
        rvParticipants = view.findViewById(R.id.rv_mitra_program_detail_participants)
        tvEmptyParticipants = view.findViewById(R.id.tv_mitra_program_detail_empty_participants)

        rvTasks.layoutManager = LinearLayoutManager(context)
        taskAdapter = TaskAdapter(emptyList(), hideStatus = true)
        rvTasks.adapter = taskAdapter

        rvParticipants.layoutManager = LinearLayoutManager(context)
        participantAdapter = TalentAdapter(emptyList(), onItemClick = { student ->
            showStudentDetailDialog(requireContext(), student, repo)
        })
        rvParticipants.adapter = participantAdapter

        loadTasks()
        loadParticipants()

        val btnPublish = view.findViewById<Button>(R.id.btn_publish_draft)
        if (program.status == "draft") {
            btnPublish.visibility = View.VISIBLE
            btnPublish.setOnClickListener {
                repo.getTasks(program.id) { tasks ->
                    if (tasks.size < 3) {
                        Toast.makeText(context, "Draft harus memiliki minimal 3 tugas sebelum rilis", Toast.LENGTH_SHORT).show()
                    } else {
                        program.status = "published"
                        repo.createProgram(program, emptyList()) { success -> // Passing empty tasks because they already exist in firestore
                            if (success) {
                                Toast.makeText(context, "Program Berhasil Dirilis!", Toast.LENGTH_SHORT).show()
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            }
                        }
                    }
                }
            }
        }

        return view
    }

    private fun loadTasks() {
        repo.getTasks(program.id) { list ->
            taskAdapter.updateData(list)
        }
    }

    private fun loadParticipants() {
        val user = repo.currentUser ?: return
        repo.getEnrollments(user.id, "mitra") { enrollments ->
            val programEnrollments = enrollments.filter { it.programId == program.id }
            val studentIds = programEnrollments.map { it.pelajarId }.toSet()
            if (studentIds.isEmpty()) {
                tvEmptyParticipants.visibility = View.VISIBLE
                rvParticipants.visibility = View.GONE
            } else {
                tvEmptyParticipants.visibility = View.GONE
                rvParticipants.visibility = View.VISIBLE
                repo.getTalentPool { allTalents ->
                    if (!isAdded) return@getTalentPool
                    val participants = allTalents.filter { it.id in studentIds }
                    participantAdapter.updateData(participants)
                }
            }
        }
    }
}

// ==========================================
// 7. MITRA STUDENT LIST FRAGMENT
// ==========================================
class MitraStudentListFragment : Fragment() {
    private lateinit var rvStudents: RecyclerView
    private lateinit var adapter: TalentAdapter
    private lateinit var repo: KerjainRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mitra_student_list, container, false)
        (activity as? MainActivity)?.showBottomNav(true)
        (activity as? MainActivity)?.showToolbar(false)
        (activity as? MainActivity)?.updateToolbarTitle("Daftar Pelajar Mitra")

        repo = KerjainRepository.getInstance(requireContext())
        val user = repo.currentUser ?: return view

        rvStudents = view.findViewById(R.id.rv_mitra_students_full)
        rvStudents.layoutManager = LinearLayoutManager(context)
        adapter = TalentAdapter(emptyList(), hidePoints = true, onItemClick = { student ->
            showStudentDetailDialog(requireContext(), student, repo)
        })
        rvStudents.adapter = adapter

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadData(user)
        return view
    }

    private fun loadData(user: User) {
        repo.getEnrollments(user.id, "mitra") { enrollments ->
            if (!isAdded) return@getEnrollments
            val uniqueStudentIds = enrollments.map { it.pelajarId }.toSet()
            if (uniqueStudentIds.isEmpty()) {
                adapter.updateData(emptyList())
            } else {
                repo.getTalentPool { allTalents ->
                    if (!isAdded) return@getTalentPool
                    val participants = allTalents.filter { it.id in uniqueStudentIds }
                    adapter.updateData(participants)
                }
            }
        }
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
    val programAdapter = com.kerjain.app.ui.EnrolledProgramAdapter(emptyList(), hideProgress = true) { /* ignore click in detail */ }
    rvPrograms.adapter = programAdapter

    repo.getPrograms { allPrograms ->
        val mitra = repo.currentUser ?: return@getPrograms
        repo.getEnrollments(mitra.id, "mitra") { mitraEnrollments ->
            // Filter to only this student's enrollments within this Mitra's context
            val studentEnrollments = mitraEnrollments.filter { it.pelajarId == student.id }

            if (studentEnrollments.isEmpty()) {
                tvEmptyPrograms.visibility = View.VISIBLE
                rvPrograms.visibility = View.GONE
            } else {
                tvEmptyPrograms.visibility = View.GONE
                rvPrograms.visibility = View.VISIBLE
                
                val displayData = mutableListOf<Triple<Enrollment, Program, Int>>()
                var processed = 0
                for (enroll in studentEnrollments) {
                    val prog = allPrograms.find { it.id == enroll.programId }
                    if (prog != null) {
                        repo.getTasks(prog.id) { tasks ->
                            repo.getSubmissions(enroll.id) { subs ->
                                val approved = subs.count { it.status.lowercase() == "disetujui" }
                                val progress = com.kerjain.app.ui.ProgressUtils.calculateWeightedProgress(tasks.size, approved)
                                displayData.add(Triple(enroll, prog, progress))
                                processed++
                                if (processed == studentEnrollments.size) {
                                    programAdapter.updateData(displayData)
                                }
                            }
                        }
                    } else {
                        processed++
                        if (processed == studentEnrollments.size) {
                            programAdapter.updateData(displayData)
                        }
                    }
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
