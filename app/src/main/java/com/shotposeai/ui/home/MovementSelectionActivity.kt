package com.shotposeai.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shotposeai.analysis.AchievementRules
import com.shotposeai.data.DatabaseProvider
import androidx.appcompat.app.AppCompatActivity
import com.shotposeai.databinding.ActivityMovementSelectionBinding
import com.shotposeai.analysis.MovementCatalog
import com.shotposeai.ui.analysis.AnalysisActivity

class MovementSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovementSelectionBinding
    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: AnalysisActivity.MODE_CAMERA }
    private val studentId by lazy { intent.getLongExtra(StudentPickerActivity.EXTRA_STUDENT_ID, 0L) }
    private val studentName by lazy { intent.getStringExtra(StudentPickerActivity.EXTRA_STUDENT_NAME) ?: "Siswa" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovementSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        hasPositioned = savedInstanceState != null
    }

    private var hasPositioned = false
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                val dao = DatabaseProvider.get(this@MovementSelectionActivity).appDao()
                dao.getStudent(studentId) to dao.getMovementBest(studentId).associate { it.movementName to it.bestScore }
            }
            if (snapshot.first == null) { finish(); return@launch }
            val scores = snapshot.second
            val completed = MovementCatalog.all.count { AchievementRules.qualifies(scores[it.name] ?: 0) }
            val currentIndex = MovementCatalog.all.indexOfFirst { !AchievementRules.qualifies(scores[it.name] ?: 0) }
            val current = MovementCatalog.all.getOrNull(currentIndex)
            binding.tvStudent.text = snapshot.first!!.name
            binding.tvCurrent.text = current?.let { "${it.name} • Terbaik ${(scores[it.name] ?: 0) / 10.0}/10" } ?: "Seluruh achievement gerakan tercapai!"
            binding.tvProgress.text = "$completed/${MovementCatalog.all.size} achievement • Target tiap gerakan 8,5/10"
            binding.progressMovements.progress = AchievementRules.percentage(completed, MovementCatalog.all.size)
            binding.movementPath.submit(MovementCatalog.all, scores) { selectMovement(it.name) }
            if (!hasPositioned) {
                binding.pathScroll.post { binding.pathScroll.scrollTo(0, binding.movementPath.positionOf(currentIndex.coerceAtLeast(0))) }
                hasPositioned = true
            }
        }
    }

    private fun selectMovement(movement: String) {
        startActivity(Intent(this, MovementInstructionActivity::class.java).apply {
            putExtra(MovementInstructionActivity.EXTRA_MODE, mode)
            putExtra(MovementInstructionActivity.EXTRA_MOVEMENT, movement)
            putExtra(StudentPickerActivity.EXTRA_STUDENT_ID, studentId)
            putExtra(StudentPickerActivity.EXTRA_STUDENT_NAME, studentName)
        })
    }

    companion object { const val EXTRA_MODE = "extra_mode" }
}
