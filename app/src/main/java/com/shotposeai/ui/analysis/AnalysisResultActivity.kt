package com.shotposeai.ui.analysis

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.analysis.MovementCatalog
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.databinding.ActivityAnalysisResultBinding
import com.shotposeai.ui.common.AvatarStore
import com.shotposeai.ui.home.MainActivity
import com.shotposeai.ui.home.StudentPickerActivity
import com.shotposeai.ui.home.MovementSelectionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalysisResultActivity : AppCompatActivity() {
    private lateinit var b: ActivityAnalysisResultBinding
    private var newBadgeShown = false
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); b = ActivityAnalysisResultBinding.inflate(layoutInflater); setContentView(b.root)
        newBadgeShown = s?.getBoolean("new_badge_shown") ?: false
        b.btnBack.setOnClickListener { finish() }
        b.btnDone.setOnClickListener { startActivity(Intent(this, MainActivity::class.java).apply { flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP });finish() }
        val id=intent.getLongExtra(EXTRA_SESSION_ID,0)
        lifecycleScope.launch {
            val snapshot=withContext(Dispatchers.IO) {
                val dao=DatabaseProvider.get(this@AnalysisResultActivity).appDao()
                val result=dao.getReport(id)
                result to result?.let { dao.getStudent(it.studentId) }
            }
            val result=snapshot.first ?: run { Toast.makeText(this@AnalysisResultActivity,"Hasil tidak ditemukan",Toast.LENGTH_SHORT).show();finish();return@launch }
            AvatarStore.bind(b.ivPhoto,snapshot.second?.profilePhotoPath,lifecycleScope)
            b.tvStudent.text=result.studentName;b.tvMovement.text=result.movementName
            b.tvScore.text=(result.score/10.0).toString();b.tvLevel.text=result.level.uppercase()
            b.tvBadge.text="${result.level} Level!"
            b.tvCorrect.text="Poin Benar\n${result.correctPoints}"
            b.tvImprove.text="Poin Perbaikan\n${result.improvementPoints}"
            b.tvNote.text=result.coachNote
            b.btnAchievements.setOnClickListener { startActivity(Intent(this@AnalysisResultActivity,AchievementActivity::class.java).putExtra(AchievementActivity.EXTRA_STUDENT_ID,result.studentId)) }
            b.btnPractice.setOnClickListener {
                startActivity(Intent(this@AnalysisResultActivity,MovementSelectionActivity::class.java).apply {
                    putExtra(StudentPickerActivity.EXTRA_STUDENT_ID,result.studentId);putExtra(StudentPickerActivity.EXTRA_STUDENT_NAME,result.studentName)
                    flags=Intent.FLAG_ACTIVITY_CLEAR_TOP
                });finish()
            }
            val newBadge=intent.getLongExtra(EXTRA_NEW_ACHIEVEMENT_ID,0)
            if(newBadge>0&&!newBadgeShown){
                newBadgeShown=true
                startActivity(Intent(this@AnalysisResultActivity,AchievementActivity::class.java).apply {
                    putExtra(AchievementActivity.EXTRA_STUDENT_ID,result.studentId);putExtra(AchievementActivity.EXTRA_ACHIEVEMENT_ID,newBadge)
                })
            }
        }
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putBoolean("new_badge_shown",newBadgeShown);super.onSaveInstanceState(outState)}
    companion object{const val EXTRA_SESSION_ID="session_id";const val EXTRA_STUDENT_NAME="student_name";const val EXTRA_NEW_ACHIEVEMENT_ID="new_achievement_id"}
}
