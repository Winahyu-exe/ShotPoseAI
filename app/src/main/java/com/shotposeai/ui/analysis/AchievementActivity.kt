package com.shotposeai.ui.analysis

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.analysis.AchievementRules
import com.shotposeai.analysis.MovementCatalog
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.databinding.ActivityAchievementBinding
import com.shotposeai.ui.common.AvatarStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AchievementActivity : AppCompatActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(s:Bundle?){
        super.onCreate(s);val b=ActivityAchievementBinding.inflate(layoutInflater);setContentView(b.root)
        b.btnContinue.setOnClickListener{finish()}
        val studentId=intent.getLongExtra(EXTRA_STUDENT_ID,0)
        val badgeId=intent.getLongExtra(EXTRA_ACHIEVEMENT_ID,0)
        lifecycleScope.launch{
            val snapshot=withContext(Dispatchers.IO){
                val dao=DatabaseProvider.get(this@AchievementActivity).appDao()
                Triple(dao.getStudent(studentId),dao.getUnlocked(studentId),dao.getMovementBest(studentId).associate{it.movementName to it.bestScore})
            }
            val student=snapshot.first ?: run{finish();return@launch}
            AvatarStore.bind(b.ivPhoto,student.profilePhotoPath,lifecycleScope)
            val awarded=snapshot.second.firstOrNull{it.id==badgeId}
            if(awarded!=null){
                b.tvHeading.text="Achievement Didapatkan!";b.tvTitle.text=awarded.name
                b.tvMessage.text="Selamat, ${student.name}!\n${awarded.description}"
            }else{
                b.tvHeading.text="Achievement Siswa";b.tvTitle.text=student.name
                val count=MovementCatalog.all.count{AchievementRules.qualifies(snapshot.third[it.name]?:0)}
                b.tvMessage.text="$count/${MovementCatalog.all.size} achievement tercapai\nCapai skor 8,5/10 pada setiap gerakan."
            }
            b.achievementList.removeAllViews()
            MovementCatalog.all.forEach { movement ->
                val score=snapshot.third[movement.name]?:0
                val completed=AchievementRules.qualifies(score)
                b.achievementList.addView(TextView(this@AchievementActivity).apply {
                    text="${if(completed) "✓" else "○"}  ${movement.badgeName}\n${movement.name} • Skor terbaik ${score/10.0}/10"
                    textSize=14f;setPadding(16,16,16,16)
                    setTextColor(if(completed) 0xff15803d.toInt() else 0xff575f6d.toInt())
                    setBackgroundResource(if(completed)com.shotposeai.R.drawable.bg_success_panel else com.shotposeai.R.drawable.bg_card)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                })
            }
            b.achievementList.visibility=if(awarded==null)View.VISIBLE else View.GONE
        }
    }
    companion object{const val EXTRA_TITLE="title";const val EXTRA_STUDENT_ID="student_id";const val EXTRA_ACHIEVEMENT_ID="achievement_id"}
}
