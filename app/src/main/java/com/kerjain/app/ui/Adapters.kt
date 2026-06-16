package com.kerjain.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.kerjain.app.R
import com.kerjain.app.model.*
import java.text.SimpleDateFormat
import java.util.*

class ProgramAdapter(
    private var programs: List<Program>,
    private val isMitraView: Boolean = false,
    private val layoutResId: Int = R.layout.item_program,
    private var programStats: Map<String, Pair<Int, Int>> = emptyMap(), // ProgramId -> (ParticipantCount, AvgCompletion)
    private val onItemClick: (Program) -> Unit
) : RecyclerView.Adapter<ProgramAdapter.ViewHolder>() {

    fun updateData(newPrograms: List<Program>, newStats: Map<String, Pair<Int, Int>> = emptyMap()) {
        programs = newPrograms
        programStats = newStats
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView? = view.findViewById(R.id.tv_item_title)
        val tvPartner: TextView? = view.findViewById(R.id.tv_item_partner)
        val tvCategory: TextView? = view.findViewById(R.id.tv_item_category)
        val tvDate: TextView? = view.findViewById(R.id.tv_item_date)
        val tvQuota: TextView? = view.findViewById(R.id.tv_item_quota)
        val tvStatus: TextView? = view.findViewById(R.id.tv_item_status)
        
        // For Task Home / Featured Layout
        val ivIcon: ImageView? = view.findViewById(R.id.iv_program_icon)
        val tvDuration: TextView? = view.findViewById(R.id.tv_item_duration)
        val cardIcon: com.google.android.material.card.MaterialCardView? = view.findViewById(R.id.card_icon_container)
        val tvPartnerInitial: TextView? = view.findViewById(R.id.tv_item_partner_initial)

        // For Mitra Home specific fields (item_program_mitra_home)
        val tvSubtitle: TextView? = view.findViewById(R.id.tv_item_subtitle)
        val progressBar: com.google.android.material.progressindicator.LinearProgressIndicator? = view.findViewById(R.id.progress_bar)
        val tvProgressPercent: TextView? = view.findViewById(R.id.tv_progress_percent)
        
        // For standard program list (item_program)
        val cardLogo: com.google.android.material.card.MaterialCardView? = view.findViewById(R.id.card_logo)
        val ivLogoIcon: ImageView? = view.findViewById(R.id.card_logo_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = programs[position]
        holder.tvTitle?.text = program.judul
        
        // Dynamic partner name and optional subtitle for item_task_home
        if (layoutResId == R.layout.item_task_home) {
            holder.tvPartner?.text = "${program.mitraNama} • Deadline hari ini"
        } else {
            holder.tvPartner?.text = program.mitraNama
        }

        holder.tvCategory?.text = program.bidang.uppercase()
        
        if (holder.tvDate != null) {
            val dateLabel = formatStartDate(program.tanggalMulai)
            holder.tvDate.text = if (dateLabel.isNotEmpty()) "Mulai $dateLabel" else ""
            holder.tvDate.visibility = if (dateLabel.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        holder.tvQuota?.text = "Kuota: ${program.kuota ?: "Tidak Terbatas"}"
        
        holder.tvPartnerInitial?.text = program.mitraNama.take(2).uppercase()

        val colors = listOf("#F59E0B", "#10B981", "#4F46E5", "#F43F5E", "#06B6D4")
        val colorString = colors[position % colors.size]
        val colorInt = Color.parseColor(colorString)

        // Apply colors to standard card logo if it exists
        holder.cardLogo?.setCardBackgroundColor(colorInt)
        holder.cardLogo?.strokeColor = colorInt
        holder.cardLogo?.strokeWidth = 1
        holder.ivLogoIcon?.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        
        // Stats for Mitra Home
        val stats = programStats[program.id] ?: (0 to 0)
        holder.tvSubtitle?.text = "${stats.first} peserta • ${program.tanggalSelesai}"
        holder.progressBar?.progress = stats.second
        holder.tvProgressPercent?.text = "${stats.second}% rata-rata selesai"
        
        // Featured Layout specific fields
        if (holder.tvDuration != null) {
            val durationText = getProgramDuration(program)
            holder.tvDuration.text = durationText
            holder.tvDuration.visibility = if (durationText.isEmpty()) View.GONE else View.VISIBLE
        }

        if (holder.cardIcon != null) {
            holder.cardIcon.setCardBackgroundColor(colorInt)
            
            // Sync stroke color of the icon container with its background color
            holder.cardIcon.strokeColor = colorInt
            holder.cardIcon.strokeWidth = 1
            
            // Set the main card border to a light grey color as before
            val parentCard = holder.itemView as? com.google.android.material.card.MaterialCardView
            parentCard?.strokeColor = Color.parseColor("#E2E8F0")
        }

        if (isMitraView && holder.tvStatus != null) {
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = program.status.uppercase()
            if (program.status == "published") {
                holder.tvStatus.setTextColor(Color.parseColor("#10AC84"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_primary)
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D1F2EB"))
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#FF9F43"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_primary)
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF8E7"))
            }
        } else {
            // For Task Home, if status is "Proses"
            if (holder.tvStatus != null && layoutResId == R.layout.item_task_home) {
                holder.tvStatus.text = "Proses"
                holder.tvStatus.visibility = View.VISIBLE
            } else {
                holder.tvStatus?.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener { onItemClick(program) }
    }

    private fun getProgramDuration(program: Program): String {
        if (program.tanggalMulai.isEmpty() || program.tanggalSelesai.isEmpty()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = sdf.parse(program.tanggalMulai)
            val end = sdf.parse(program.tanggalSelesai)

            if (start == null || end == null) return ""

            val diff = end.time - start.time
            if (diff < 0) return ""

            val days = (diff / (1000 * 60 * 60 * 24)).toInt()
            val weeks = days / 7
            val months = (days / 30.42).toInt() // More accurate month calculation

            val durationLabel = when {
                months >= 1 -> "$months Bulan"
                weeks >= 1 -> "$weeks Minggu"
                else -> "$days Hari"
            }

            // Sertakan bulan dan tahun dari tanggal mulai
            val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val startMonthYear = monthYearFormat.format(start)

            "$startMonthYear • $durationLabel"
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatStartDate(dateStr: String): String {
        if (dateStr.isEmpty()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return ""
            val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    override fun getItemCount() = programs.size
}

class TaskAdapter(
    private var tasks: List<Task>,
    private var submissionStatuses: Map<String, String> = emptyMap(), // var to allow updates
    private val onItemClick: ((Task, String) -> Unit)? = null,
    private val hideStatus: Boolean = false
) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    fun updateData(newTasks: List<Task>, newStatuses: Map<String, String> = emptyMap()) {
        tasks = newTasks
        submissionStatuses = newStatuses
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tv_task_number)
        val tvTitle: TextView = view.findViewById(R.id.tv_task_item_title)
        val tvDeadline: TextView = view.findViewById(R.id.tv_task_item_deadline)
        val tvStatus: TextView = view.findViewById(R.id.tv_task_item_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvNumber.text = (position + 1).toString()
        holder.tvTitle.text = task.judul
        holder.tvDeadline.text = "Tenggat: ${task.deadline}"

        // Check if locked: locked if hideStatus is false and any previous task is not approved (disetujui)
        var isLocked = false
        if (!hideStatus) {
            for (i in 0 until position) {
                val prevTaskId = tasks[i].id
                val prevStatus = submissionStatuses[prevTaskId] ?: ""
                if (prevStatus.lowercase() != "disetujui") {
                    isLocked = true
                    break
                }
            }
        }

        if (isLocked) {
            holder.tvTitle.setTextColor(Color.parseColor("#B2BEC3"))
            holder.tvDeadline.setTextColor(Color.parseColor("#B2BEC3"))
        } else {
            holder.tvTitle.setTextColor(Color.parseColor("#2D3436"))
            holder.tvDeadline.setTextColor(Color.parseColor("#636E72"))
        }

        if (hideStatus) {
            holder.tvStatus.visibility = View.GONE
        } else {
            holder.tvStatus.visibility = View.VISIBLE
            if (isLocked) {
                holder.tvStatus.text = "TERKUNCI"
                holder.tvStatus.setTextColor(Color.parseColor("#B2BEC3"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
            } else {
                val status = submissionStatuses[task.id] ?: "Belum Dikirim"
                holder.tvStatus.text = if (status.lowercase() == "menunggu") "MENUNGGU REVIEW" else status.uppercase()

                when (status.lowercase()) {
                    "disetujui" -> {
                        holder.tvStatus.setTextColor(Color.parseColor("#10AC84"))
                        holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D1F2EB"))
                    }
                    "menunggu" -> {
                        holder.tvStatus.setTextColor(Color.parseColor("#0984E3"))
                        holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EBF5FB"))
                    }
                    "revisi" -> {
                        holder.tvStatus.setTextColor(Color.parseColor("#EE5253"))
                        holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE4D6"))
                    }
                    else -> {
                        holder.tvStatus.setTextColor(Color.parseColor("#636E72"))
                        holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
                    }
                }
            }
        }

        holder.itemView.setOnClickListener {
            if (isLocked) {
                android.widget.Toast.makeText(
                    holder.itemView.context,
                    "Tugas ini terkunci. Selesaikan tugas sebelumnya terlebih dahulu!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                val status = submissionStatuses[task.id] ?: "Belum Dikirim"
                onItemClick?.invoke(task, status)
            }
        }
    }

    override fun getItemCount() = tasks.size
}

class SubmissionAdapter(
    private var submissions: List<Submission>,
    private val onItemClick: (Submission) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.ViewHolder>() {

    fun updateData(newSubmissions: List<Submission>) {
        submissions = newSubmissions
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tv_sub_student_name)
        val tvProgramTitle: TextView = view.findViewById(R.id.tv_sub_program_title)
        val tvTaskTitle: TextView = view.findViewById(R.id.tv_sub_task_title)
        val tvNotes: TextView = view.findViewById(R.id.tv_sub_notes)
        val tvStatus: TextView = view.findViewById(R.id.tv_sub_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submission, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = submissions[position]
        holder.tvStudentName.text = sub.pelajarNama
        holder.tvProgramTitle.text = sub.taskJudul // Note: we can use task title or program info
        holder.tvTaskTitle.text = "Tugas: ${sub.taskJudul}"
        holder.tvNotes.text = "Catatan: ${sub.catatan}"
        holder.tvStatus.text = sub.status.uppercase()

        when (sub.status.lowercase()) {
            "disetujui" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#10AC84"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D1F2EB"))
            }
            "revisi" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#EE5253"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE4D6"))
            }
            else -> {
                holder.tvStatus.setTextColor(Color.parseColor("#0984E3"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EBF5FB"))
            }
        }

        holder.itemView.setOnClickListener { onItemClick(sub) }
    }

    override fun getItemCount() = submissions.size
}

class CertificateAdapter(
    private var certificates: List<Certificate>,
    private val onItemClick: ((Certificate) -> Unit)? = null
) : RecyclerView.Adapter<CertificateAdapter.ViewHolder>() {

    fun updateData(newCertificates: List<Certificate>) {
        certificates = newCertificates
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProgram: TextView = view.findViewById(R.id.tv_cert_program)
        val tvNumber: TextView = view.findViewById(R.id.tv_cert_number)
        val tvDateIssuer: TextView = view.findViewById(R.id.tv_cert_date_issuer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_certificate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cert = certificates[position]
        holder.tvProgram.text = cert.programJudul
        holder.tvNumber.text = "No: ${cert.nomorSertifikat}"
        
        val dateString = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(cert.issuedAt))
        holder.tvDateIssuer.text = "Diterbitkan oleh ${cert.mitraNama} pada $dateString"

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(cert)
        }
    }

    override fun getItemCount() = certificates.size
}

class CertificateTaskAdapter(
    private var tasks: List<Task>
) : RecyclerView.Adapter<CertificateTaskAdapter.ViewHolder>() {

    fun updateData(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_cert_task_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_cert_task_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_certificate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvTitle.text = task.judul
        holder.tvSubtitle.text = "Tugas ${position + 1} - Terselesaikan"
    }

    override fun getItemCount() = tasks.size
}

class TalentAdapter(
    private var talents: List<User>,
    private val hidePoints: Boolean = false,
    private var averageScores: Map<String, Int>? = null, // StudentId -> AverageScore
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<TalentAdapter.ViewHolder>() {

    fun updateData(newTalents: List<User>, newScores: Map<String, Int>? = null) {
        talents = newTalents
        averageScores = newScores
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatarText: TextView = view.findViewById(R.id.tv_talent_avatar_text)
        val ivAvatarPhoto: ImageView = view.findViewById(R.id.iv_talent_avatar_photo)
        val tvName: TextView = view.findViewById(R.id.tv_talent_name)
        val tvSchool: TextView = view.findViewById(R.id.tv_talent_school)
        val tvPoints: TextView = view.findViewById(R.id.tv_talent_points)
        val tvBio: TextView = view.findViewById(R.id.tv_talent_bio)
        
        // Average score layout elements
        val layoutAverage: View? = view.findViewById(R.id.layout_talent_average)
        val tvAverageValue: TextView? = view.findViewById(R.id.tv_talent_average_value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_talent, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val talent = talents[position]
        holder.tvAvatarText.text = talent.namaLengkap.take(1).uppercase()
        ImageUtils.loadProfileImage(talent.fotoProfil, holder.tvAvatarText, holder.ivAvatarPhoto)
        
        holder.tvName.text = talent.namaLengkap
        holder.tvSchool.text = "${talent.universitas ?: "Pelajar"} • ${talent.jurusan ?: ""}"
        holder.tvBio.text = talent.bio ?: "Tidak ada biografi."

        val score = averageScores?.get(talent.id)
        if (score != null && holder.layoutAverage != null) {
            holder.tvPoints.visibility = View.GONE
            holder.layoutAverage.visibility = View.VISIBLE
            holder.tvAverageValue?.text = score.toString()
            
            // Adjust name width to avoid overlap
            val params = holder.tvName.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.marginEnd = 80 // Provide space for average score
            holder.tvName.layoutParams = params
        } else if (hidePoints) {
            holder.tvPoints.visibility = View.GONE
            holder.layoutAverage?.visibility = View.GONE
        } else {
            holder.tvPoints.visibility = View.VISIBLE
            holder.layoutAverage?.visibility = View.GONE
            holder.tvPoints.text = "${talent.totalPoin} Poin"
        }

        holder.itemView.setOnClickListener { onItemClick(talent) }
    }

    override fun getItemCount() = talents.size
}

class StudentProgressAdapter(
    private var data: List<Pair<User, Int>>,
    private val hideProgress: Boolean = false,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<StudentProgressAdapter.ViewHolder>() {

    fun updateData(newData: List<Pair<User, Int>>) {
        data = newData
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tv_student_initial)
        val ivPhoto: ImageView = view.findViewById(R.id.iv_student_photo)
        val tvName: TextView = view.findViewById(R.id.tv_student_name)
        val tvInfo: TextView = view.findViewById(R.id.tv_student_info)
        val layoutProgress: View = view.findViewById(R.id.layout_progress)
        val tvPercent: TextView = view.findViewById(R.id.tv_progress_percent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_progress, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (user, progress) = data[position]
        holder.tvInitial.text = user.namaLengkap.take(1).uppercase()
        ImageUtils.loadProfileImage(user.fotoProfil, holder.tvInitial, holder.ivPhoto)

        holder.tvName.text = user.namaLengkap
        holder.tvInfo.text = "${user.jurusan ?: "Pelajar"} • ${user.universitas ?: ""}"
        
        if (hideProgress) {
            holder.layoutProgress.visibility = View.GONE
        } else {
            holder.layoutProgress.visibility = View.VISIBLE
            holder.tvPercent.text = "$progress%"
        }
        
        holder.itemView.setOnClickListener { onItemClick(user) }
    }

    override fun getItemCount() = data.size
}

class PendingTaskAdapter(
    private var submissions: List<Submission>,
    private val onItemClick: (Submission) -> Unit
) : RecyclerView.Adapter<PendingTaskAdapter.ViewHolder>() {

    fun updateData(newSubmissions: List<Submission>) {
        submissions = newSubmissions
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_task_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_task_subtitle)
        val tvInitial: TextView = view.findViewById(R.id.tv_pending_initial)
        val ivPhoto: ImageView = view.findViewById(R.id.iv_pending_photo)
        val btnGrade: View = view.findViewById(R.id.btn_grade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_pending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = submissions[position]
        holder.tvTitle.text = sub.taskJudul
        
        // Use a placeholder or actual submission time if available
        holder.tvSubtitle.text = "${sub.pelajarNama} • Dikumpul baru saja"

        // Load student photo from mock/real data if possible, for now we use repository lookup
        holder.tvInitial.text = sub.pelajarNama.take(1).uppercase()
        // Note: PendingTaskAdapter might need User objects to load photos effectively
        // If Submission doesn't have photo, we'd need to fetch user, but for now we'll check if a photo path is in Submission
        // Assuming your Submission model doesn't have photo yet, but we'll try to handle it if you add it.
        
        holder.itemView.setOnClickListener { onItemClick(sub) }
        holder.btnGrade.setOnClickListener { onItemClick(sub) }
    }

    override fun getItemCount() = submissions.size
}

class EnrolledProgramAdapter(
    private var data: List<Triple<Enrollment, Program, Int>>,
    private val hideProgress: Boolean = false,
    private val onItemClick: (Program) -> Unit
) : RecyclerView.Adapter<EnrolledProgramAdapter.ViewHolder>() {

    fun updateData(newData: List<Triple<Enrollment, Program, Int>>) {
        data = newData
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_item_title)
        val tvPartner: TextView = view.findViewById(R.id.tv_item_partner)
        val tvStatus: TextView = view.findViewById(R.id.tv_item_status)
        val progressBar: com.google.android.material.progressindicator.LinearProgressIndicator = view.findViewById(R.id.progress_bar)
        val tvPercent: TextView = view.findViewById(R.id.tv_progress_percent)
        val btnContinue: View = view.findViewById(R.id.btn_continue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_enrolled_program, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (enrollment, program, progress) = data[position]
        holder.tvTitle.text = program.judul
        holder.tvPartner.text = "${program.mitraNama} • ${program.bidang}"
        holder.tvStatus.text = enrollment.status.uppercase()
        
        holder.progressBar.progress = progress
        holder.tvPercent.text = "$progress% selesai"

        if (hideProgress) {
            holder.tvPartner.visibility = View.GONE
            holder.tvStatus.visibility = View.GONE
            holder.progressBar.visibility = View.GONE
            holder.tvPercent.visibility = View.GONE
            holder.btnContinue.visibility = View.GONE
        } else {
            holder.tvPartner.visibility = View.VISIBLE
            holder.tvStatus.visibility = View.VISIBLE
            holder.progressBar.visibility = View.VISIBLE
            holder.tvPercent.visibility = View.VISIBLE
            holder.btnContinue.visibility = View.VISIBLE
        }
        
        val clickListener = View.OnClickListener { onItemClick(program) }
        holder.itemView.setOnClickListener(clickListener)
        holder.btnContinue.setOnClickListener(clickListener)
    }

    override fun getItemCount() = data.size
}

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vUnreadDot: View = view.findViewById(R.id.v_notif_unread_dot)
        val tvTitle: TextView = view.findViewById(R.id.tv_notif_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_notif_message)
        val tvTime: TextView = view.findViewById(R.id.tv_notif_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifications[position]
        holder.vUnreadDot.visibility = if (notif.read) View.GONE else View.VISIBLE
        
        holder.tvTitle.text = when (notif.type) {
            "new_enrollment" -> "Pendaftaran Program Baru"
            "program_completed" -> "Pelajar Menyelesaikan Program"
            "new_submission" -> "Tugas Menunggu Review"
            "submission_revision" -> "Pelajar Mengirim Revisi"
            "submission_reviewed" -> "Hasil Review Tugas"
            "program_created" -> "Program Baru Diterbitkan"
            "welcome" -> "Selamat Datang"
            else -> "Pemberitahuan"
        }
        
        holder.tvMessage.text = notif.message
        
        val dateString = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(notif.createdAt))
        holder.tvTime.text = dateString

        holder.itemView.setOnClickListener { onItemClick(notif) }
    }

    override fun getItemCount() = notifications.size
}

object ProgressUtils {
    fun calculateWeightedProgress(totalTasks: Int, approvedCount: Int): Int {
        if (totalTasks <= 0) return 0
        if (approvedCount >= totalTasks) return 100
        
        return when (totalTasks) {
            3 -> when (approvedCount) {
                1 -> 30
                2 -> 60
                else -> 0
            }
            2 -> when (approvedCount) {
                1 -> 40
                else -> 0
            }
            else -> (approvedCount * 100) / totalTasks
        }
    }
}

