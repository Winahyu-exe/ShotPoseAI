package com.shotposeai.ui.report

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.ReportRow
import com.shotposeai.data.StudentEntity
import com.shotposeai.databinding.ActivityLeaderboardBinding
import com.shotposeai.ui.common.AvatarStore
import com.shotposeai.ui.common.StudentCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var b: ActivityLeaderboardBinding
    private data class Entry(val student: StudentEntity, val result: ReportRow)
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); b=ActivityLeaderboardBinding.inflate(layoutInflater); setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }
    }
    override fun onResume() { super.onResume(); load() }
    private fun load() = lifecycleScope.launch {
        val rows=withContext(Dispatchers.IO){
            val dao=DatabaseProvider.get(this@LeaderboardActivity).appDao(); val students=dao.students.associateBy { it.id }
            dao.reports.groupBy { it.studentId }.mapNotNull { (id, reports) ->
                val student=students[id] ?: return@mapNotNull null
                reports.maxWithOrNull(compareBy<ReportRow>{it.score}.thenBy{it.date}.thenBy{it.sessionId})?.let { Entry(student,it) }
            }.sortedWith(compareByDescending<Entry>{it.result.score}.thenBy{it.student.name.lowercase()}.thenBy{it.student.id})
        }
        b.podium.removeAllViews(); b.listContent.removeAllViews()
        if(rows.isEmpty()) {
            b.podium.visibility=View.GONE
            b.tvRankingNote.text="Belum ada hasil latihan. Simpan penilaian siswa untuk membuat peringkat."
            return@launch
        }
        b.podium.visibility=View.VISIBLE
        listOf(1,0,2).forEach { place -> addPodium(rows.getOrNull(place), place+1) }
        b.tvRankingNote.text="${rows.size} siswa dinilai • Peringkat berdasarkan skor terbaik"
        rows.drop(3).forEachIndexed { i,entry ->
            b.listContent.addView(StudentCard.create(this@LeaderboardActivity,entry.student,lifecycleScope,"${entry.result.level} • Skor ${entry.result.score/10.0}",i+4){open(entry)})
        }
        if(rows.size<=3)b.listContent.addView(label("Semua siswa yang telah dinilai sudah ditampilkan.",12f,Color.GRAY))
    }
    private fun addPodium(entry:Entry?,rank:Int) {
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL }
        b.podium.addView(column,LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(3);marginEnd=dp(3) })
        if(entry==null)return
        column.addView(label(entry.student.name,13f,0xff191e28.toInt()).apply { minHeight=dp(36);setTypeface(typeface,Typeface.BOLD) })
        val photo=ImageView(this).apply { contentDescription="Foto ${entry.student.name}" }
        column.addView(photo,LinearLayout.LayoutParams(dp(70),dp(70)).apply { topMargin=dp(8);bottomMargin=dp(8) })
        AvatarStore.bind(photo,entry.student.profilePhotoPath,lifecycleScope)
        column.addView(label("${entry.result.score/10.0}/10",12f,0xff575f6d.toInt()).apply { minHeight=dp(24) },LinearLayout.LayoutParams(-1,-2))
        val colors=when(rank){1->intArrayOf(0xffffce55.toInt(),0xffed950c.toInt());2->intArrayOf(0xffe2e7ee.toInt(),0xff9daabb.toInt());else->intArrayOf(0xffeeb993.toInt(),0xffb5744f.toInt())}
        val pedestal=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            minimumHeight=dp(when(rank){1->190;2->140;else->110})
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,colors).apply { cornerRadii=floatArrayOf(dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat(),dp(12).toFloat(),0f,0f,0f,0f) }
        }
        if(rank==1)pedestal.addView(label("🏆",36f,Color.WHITE))
        pedestal.addView(label("#$rank",29f,Color.WHITE).apply{setTypeface(typeface,Typeface.BOLD)})
        column.addView(pedestal,LinearLayout.LayoutParams(-1,-2))
        column.setOnClickListener{open(entry)}
        column.contentDescription="Peringkat $rank, ${entry.student.name}, skor ${entry.result.score/10.0}. Lihat laporan"
    }
    private fun open(entry:Entry){startActivity(Intent(this,ReportDetailActivity::class.java).putExtra(ReportDetailActivity.EXTRA_ID,entry.result.sessionId))}
    private fun label(value:String,size:Float,color:Int)=TextView(this).apply{text=value;textSize=size;gravity=Gravity.CENTER;setTextColor(color)}
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
}
