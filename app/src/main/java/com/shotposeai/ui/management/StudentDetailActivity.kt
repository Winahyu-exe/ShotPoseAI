package com.shotposeai.ui.management

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shotposeai.R
import com.shotposeai.analysis.AchievementRules
import com.shotposeai.analysis.MovementCatalog
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.MovementBest
import com.shotposeai.data.ReportRow
import com.shotposeai.data.StudentEntity
import com.shotposeai.databinding.ActivityStudentDetailBinding
import com.shotposeai.ui.analysis.AchievementActivity
import com.shotposeai.ui.common.AvatarStore
import com.shotposeai.ui.common.ConfirmActionDialog
import com.shotposeai.ui.home.StudentPickerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StudentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDetailBinding

    private val studentId: Long by lazy {
        intent.getLongExtra(EXTRA_ID, 0L)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStudentDetail()
    }

    private fun loadStudentDetail() {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                val dao = DatabaseProvider
                    .get(this@StudentDetailActivity)
                    .appDao()

                StudentSnapshot(
                    student = dao.getStudent(studentId),
                    bestScores = dao.getMovementBest(studentId),
                    badges = dao.getUnlocked(studentId)
                        .map { achievement -> achievement.name },
                    reports = dao.getStudentReports(studentId)
                )
            }

            val student = data.student

            if (student == null) {
                finish()
                return@launch
            }

            showStudentInformation(student)
            showAchievements(data.badges)
            showMovementProgress(data.bestScores)
            renderHistory(data.reports)
            configureButtons(student)
        }
    }

    private fun showStudentInformation(student: StudentEntity) {
        binding.tvName.text = student.name

        binding.tvMeta.text =
            "${student.baseLevel} • Nomor Absen ${student.attendanceNumber}"

        AvatarStore.bind(
            binding.ivPhoto,
            student.profilePhotoPath,
            lifecycleScope
        )
    }

    private fun showAchievements(badges: List<String>) {
        binding.achievementList.removeAllViews()

        if (badges.isEmpty()) {
            binding.achievementList.addView(
                TextView(this@StudentDetailActivity).apply {
                    text = "Belum ada achievement\nCapai skor 8,5 untuk membuka badge"
                    gravity = Gravity.CENTER
                    textSize = 12f
                    setTextColor(
                        ContextCompat.getColor(
                            this@StudentDetailActivity,
                            R.color.text_secondary
                        )
                    )
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        } else {
            badges.take(3).forEach { badgeName ->
                binding.achievementList.addView(
                    LinearLayout(this@StudentDetailActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        background = ContextCompat.getDrawable(
                            this@StudentDetailActivity,
                            R.drawable.bg_achievement_chip
                        )
                        addView(TextView(this@StudentDetailActivity).apply {
                            text = "🏅"
                            textSize = 32f
                            gravity = Gravity.CENTER
                            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ))
                        addView(TextView(this@StudentDetailActivity).apply {
                            text = badgeName
                            gravity = Gravity.CENTER
                            textSize = 11.5f
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(ContextCompat.getColor(
                                this@StudentDetailActivity,
                                R.color.text_primary
                            ))
                        }, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ))
                    },
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(dp(4))
                    }
                )
            }
        }

        binding.achievementList.setOnClickListener { openAchievementActivity() }

        binding.btnAllAchievements.setOnClickListener {
            openAchievementActivity()
        }
    }

    private fun openAchievementActivity() {
        val achievementIntent = Intent(
            this@StudentDetailActivity,
            AchievementActivity::class.java
        ).apply {
            putExtra(
                AchievementActivity.EXTRA_STUDENT_ID,
                studentId
            )
        }

        startActivity(achievementIntent)
    }

    private fun showMovementProgress(
        bestScores: List<MovementBest>
    ) {
        val scoreMap = bestScores.associate { result ->
            result.movementName to result.bestScore
        }

        val nextMovement = MovementCatalog.all.firstOrNull { movement ->
            val score = scoreMap[movement.name] ?: 0
            !AchievementRules.qualifies(score)
        }

        val currentMovement =
            nextMovement ?: MovementCatalog.all.last()

        val currentScore =
            scoreMap[currentMovement.name] ?: 0

        binding.tvCurrentMovement.text =
            currentMovement.name

        binding.tvBestScore.text = String.format(
            Locale.US,
            "SCORE: %.1f",
            currentScore / 10.0
        )

        binding.progressScore.progress =
            currentScore.coerceIn(0, 100)

        binding.tvProgressHint.text =
            if (
                currentScore >=
                AchievementRules.REQUIRED_SCORE
            ) {
                "Achievement gerakan sudah terbuka"
            } else {
                "Capai 8,5 untuk membuka achievement"
            }
    }

    private fun configureButtons(student: StudentEntity) {
        binding.btnEdit.setOnClickListener {
            val editIntent = Intent(
                this@StudentDetailActivity,
                StudentFormActivity::class.java
            ).apply {
                putExtra(
                    StudentFormActivity.EXTRA_ID,
                    studentId
                )
            }

            startActivity(editIntent)
        }

        binding.btnStart.setOnClickListener {
            val trainingIntent = Intent(
                this@StudentDetailActivity,
                StudentPickerActivity::class.java
            ).apply {
                putExtra(
                    StudentPickerActivity.EXTRA_PRESELECTED,
                    studentId
                )
            }

            startActivity(trainingIntent)
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation(student)
        }
    }

    private fun showDeleteConfirmation(
        student: StudentEntity
    ) {
        ConfirmActionDialog.show(
            context = this@StudentDetailActivity,
            message = "Apakah Anda yakin ingin\nmenghapus siswa?"
        ) {
            deleteStudent(student)
        }
    }

    private fun deleteStudent(student: StudentEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = DatabaseProvider
                    .get(this@StudentDetailActivity)
                    .appDao()

                dao.deleteStudentAnalysis(studentId)
                dao.deleteStudentTraining(studentId)
                dao.removeStudentFromClasses(studentId)
                dao.deleteStudentAchievements(studentId)
                dao.deleteStudent(student)
            }

            finish()
        }
    }

    private fun renderHistory(
        reports: List<ReportRow>
    ) {
        binding.historyList.removeAllViews()

        if (reports.isEmpty()) {
            val emptyView = TextView(
                this@StudentDetailActivity
            ).apply {
                text = "Belum ada riwayat latihan."

                setTextColor(
                    ContextCompat.getColor(
                        this@StudentDetailActivity,
                        R.color.text_secondary
                    )
                )

                setPadding(
                    dp(14),
                    dp(18),
                    dp(14),
                    dp(18)
                )
            }

            binding.historyList.addView(emptyView)
            return
        }

        reports.take(5).forEach { report ->
            val historyView = TextView(
                this@StudentDetailActivity
            ).apply {
                text = buildString {
                    append(report.movementName)
                    append("\n")
                    append(report.level.uppercase())
                    append("   Score: ")
                    append(
                        String.format(
                            Locale.US,
                            "%.1f",
                            report.score / 10.0
                        )
                    )
                }

                textSize = 15f

                setTextColor(
                    ContextCompat.getColor(
                        this@StudentDetailActivity,
                        R.color.text_primary
                    )
                )

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                gravity = Gravity.CENTER_VERTICAL

                background = ContextCompat.getDrawable(
                    this@StudentDetailActivity,
                    R.drawable.bg_card
                )

                setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_history,
                    0,
                    0,
                    0
                )

                compoundDrawablePadding = dp(14)

                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )

                minHeight = dp(76)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(9)
                }
            }

            binding.historyList.addView(historyView)
        }
    }

    private data class StudentSnapshot(
        val student: StudentEntity?,
        val bestScores: List<MovementBest>,
        val badges: List<String>,
        val reports: List<ReportRow>
    )

    companion object {
        const val EXTRA_ID = "student_id"
    }
}
