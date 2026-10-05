package com.shotposeai.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.databinding.ActivityStudentPickerBinding
import com.shotposeai.ui.analysis.AnalysisActivity
import com.shotposeai.ui.common.StudentCard
import com.shotposeai.ui.management.StudentFormActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentPickerActivity : AppCompatActivity() {
    private lateinit var b: ActivityStudentPickerBinding
    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: AnalysisActivity.MODE_CAMERA }
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); b = ActivityStudentPickerBinding.inflate(layoutInflater); setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }
        b.btnAddStudent.setOnClickListener { startActivity(Intent(this, StudentFormActivity::class.java)) }
    }
    override fun onResume() { super.onResume(); load() }
    private fun load() = lifecycleScope.launch {
        val list = withContext(Dispatchers.IO) { DatabaseProvider.get(this@StudentPickerActivity).appDao().students }
        b.listContent.removeAllViews()
        list.forEach { student -> b.listContent.addView(StudentCard.create(this@StudentPickerActivity, student, lifecycleScope) { open(student.id, student.name) }) }
        if (list.isEmpty()) b.listContent.addView(TextView(this@StudentPickerActivity).apply { text = "Belum ada siswa. Tambahkan siswa terlebih dahulu."; setPadding(12, 30, 12, 30) })
        intent.getLongExtra(EXTRA_PRESELECTED, 0).takeIf { it > 0 }?.let { id ->
            intent.removeExtra(EXTRA_PRESELECTED)
            list.firstOrNull { it.id == id }?.let { open(it.id, it.name); finish() }
        }
    }
    private fun open(id: Long, name: String) {
        startActivity(Intent(this, MovementSelectionActivity::class.java).apply {
            putExtra(MovementSelectionActivity.EXTRA_MODE, mode); putExtra(EXTRA_STUDENT_ID, id); putExtra(EXTRA_STUDENT_NAME, name)
        })
    }
    companion object {
        const val EXTRA_MODE = "mode"; const val EXTRA_STUDENT_ID = "student_id"; const val EXTRA_STUDENT_NAME = "student_name"; const val EXTRA_PRESELECTED = "preselected"
    }
}
