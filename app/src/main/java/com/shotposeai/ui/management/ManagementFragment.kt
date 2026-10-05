package com.shotposeai.ui.management

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.shotposeai.ui.common.StudentCard
import com.shotposeai.R
import com.shotposeai.data.ClassRoomEntity
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.ReportRow
import com.shotposeai.data.StudentEntity
import com.shotposeai.databinding.FragmentManagementBinding
import com.shotposeai.ui.report.ReportDetailActivity
import com.shotposeai.ui.report.LeaderboardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManagementFragment : Fragment() {
    private var _binding: FragmentManagementBinding? = null
    private val binding get() = _binding!!
    private val mode get() = requireArguments().getString(ARG_MODE) ?: MODE_STUDENTS
    private var classes = emptyList<ClassRoomEntity>()
    private var students = emptyList<StudentEntity>()
    private var reports = emptyList<ReportRow>()
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _binding=FragmentManagementBinding.inflate(i,c,false);return binding.root }
    override fun onViewCreated(view: View, state: Bundle?) {
        val labels=when(mode){MODE_CLASSES->Triple("Kelola Kelas","Kelola Kelas Anda","Pantau dan evaluasi progress latihan pemain");MODE_REPORTS->Triple("Laporan","Ringkasan Laporan","Evaluasi performa dan rekomendasi latihan");else->Triple("Kelola Siswa","Kelola Siswa","Pantau dan evaluasi progress latihan pemain")}
        binding.tvTitle.text=labels.first;binding.tvHeading.text=labels.second;binding.tvSubtitle.text=labels.third
        binding.btnAdd.visibility=View.VISIBLE
        binding.btnAdd.text=if(mode==MODE_CLASSES)"+  Tambah Kelas" else if(mode==MODE_REPORTS)"🏆  Papan Peringkat" else "+  Tambah Siswa"
        binding.etSearch.hint=if(mode==MODE_CLASSES)"Cari kelas" else if(mode==MODE_REPORTS)"Cari laporan siswa" else "Cari siswa"
        binding.btnAdd.setOnClickListener{startActivity(Intent(requireContext(),if(mode==MODE_CLASSES)ClassFormActivity::class.java else if(mode==MODE_REPORTS)LeaderboardActivity::class.java else StudentFormActivity::class.java))}
        binding.etSearch.doAfterTextChanged{render(it?.toString().orEmpty())}
    }
    override fun onResume(){super.onResume();load()}
    private fun load()=viewLifecycleOwner.lifecycleScope.launch{withContext(Dispatchers.IO){val d=DatabaseProvider.get(requireContext()).appDao();when(mode){MODE_CLASSES->classes=d.classes;MODE_REPORTS->{reports=d.reports;students=d.students};else->students=d.students}};render(binding.etSearch.text.toString())}
    private fun render(query:String){binding.listContent.removeAllViews();val q=query.trim().lowercase();when(mode){MODE_CLASSES->classes.filter{q.isBlank()||it.name.lowercase().contains(q)}.forEach(::addClass);MODE_REPORTS->{if(q.isBlank())addReportSummary();reports.filter{q.isBlank()||it.studentName.lowercase().contains(q)||it.movementName.lowercase().contains(q)}.forEach(::addReport)};else->students.filter{q.isBlank()||it.name.lowercase().contains(q)}.forEach(::addStudent)};if(binding.listContent.childCount==0)addEmpty()}
    private fun card():Pair<MaterialCardView,LinearLayout>{val c=MaterialCardView(requireContext()).apply{radius=18f;strokeWidth=0;setCardBackgroundColor(ContextCompat.getColor(requireContext(),R.color.white));layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=14}};val l=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16)};c.addView(l);binding.listContent.addView(c);return c to l}
    private fun text(p:LinearLayout,v:String,size:Float=15f,bold:Boolean=false,color:Int=R.color.text_primary){p.addView(TextView(requireContext()).apply{text=v;textSize=size;setTextColor(ContextCompat.getColor(requireContext(),color));if(bold)setTypeface(typeface,Typeface.BOLD)})}
    private fun addClass(x:ClassRoomEntity){val(c,l)=card();text(l,x.name,19f,true);text(l,"Pelatih: Coach • ${x.category}",12f,false,R.color.text_secondary);text(l,"Kelola anggota, latihan, dan laporan kelas",12f,false,R.color.info_blue);c.setOnClickListener{startActivity(Intent(requireContext(),ClassDetailActivity::class.java).putExtra(ClassDetailActivity.EXTRA_ID,x.id))}}
    private fun addStudent(x: StudentEntity) {
        binding.listContent.addView(StudentCard.create(requireContext(), x, viewLifecycleOwner.lifecycleScope) {
            startActivity(Intent(requireContext(), StudentDetailActivity::class.java).putExtra(StudentDetailActivity.EXTRA_ID, x.id))
        })
    }

    private fun addReport(x: ReportRow) {
        val student = students.firstOrNull { it.id == x.studentId } ?: return
        binding.listContent.addView(StudentCard.create(requireContext(), student, viewLifecycleOwner.lifecycleScope, "${x.movementName} • Skor ${x.score/10.0} • ${x.level}") {
            startActivity(Intent(requireContext(), ReportDetailActivity::class.java).putExtra(ReportDetailActivity.EXTRA_ID, x.sessionId))
        })
    }

    private fun addReportSummary(){val(_,l)=card();val avg=if(reports.isEmpty())0.0 else reports.map{it.score}.average()/10.0;val passed=if(reports.isEmpty())0 else reports.count{it.score>=75}*100/reports.size;text(l,"Ringkasan Kelas",19f,true);text(l,"Average Score  ${"%.1f".format(avg)}     •     Total Siswa  ${reports.map{it.studentId}.distinct().size}",14f,false,R.color.info_blue_dark);text(l,"Tingkat Lulus  $passed%     •     ${reports.size} penilaian",13f,false,R.color.text_secondary)}
    private fun addEmpty(){val(_,l)=card();l.gravity=Gravity.CENTER;text(l,"Belum ada data",17f,true);text(l,"Tambahkan data pertama untuk mulai menggunakan fitur ini.",12f,false,R.color.text_secondary)}
    override fun onDestroyView(){super.onDestroyView();_binding=null}
    companion object{private const val ARG_MODE="mode";const val MODE_CLASSES="classes";const val MODE_STUDENTS="students";const val MODE_REPORTS="reports";fun newInstance(mode:String)=ManagementFragment().apply{arguments=Bundle().apply{putString(ARG_MODE,mode)}}}
}
