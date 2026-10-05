package com.shotposeai.ui.report

import android.content.Intent
import android.os.Bundle
import com.shotposeai.ui.common.AvatarStore
import com.shotposeai.ui.analysis.AnalysisResultActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.ReportRow
import com.shotposeai.databinding.ActivityReportDetailBinding
import com.shotposeai.report.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDetailActivity:AppCompatActivity(){private lateinit var b:ActivityReportDetailBinding;private var row:ReportRow?=null;override fun onCreate(s:Bundle?){super.onCreate(s);b=ActivityReportDetailBinding.inflate(layoutInflater);setContentView(b.root);b.btnBack.setOnClickListener{finish()};val id=intent.getLongExtra(EXTRA_ID,0);lifecycleScope.launch{row=withContext(Dispatchers.IO){DatabaseProvider.get(this@ReportDetailActivity).appDao().getReport(id)};val r=row?:return@launch;val student=withContext(Dispatchers.IO){DatabaseProvider.get(this@ReportDetailActivity).appDao().getStudent(r.studentId)};AvatarStore.bind(b.ivPhoto,student?.profilePhotoPath,lifecycleScope);b.btnAnalysis.setOnClickListener{startActivity(Intent(this@ReportDetailActivity,AnalysisResultActivity::class.java).putExtra(AnalysisResultActivity.EXTRA_SESSION_ID,r.sessionId))};b.tvStudent.text=r.studentName;b.tvMeta.text="${r.movementName} • ${SimpleDateFormat("dd MMM yyyy",Locale("id","ID")).format(Date(r.date))}";b.tvScore.text="${r.score/10.0}\n${r.level}";b.tvDetails.text="Poin benar\n${r.correctPoints}\n\nPoin perbaikan\n${r.improvementPoints}\n\nCatatan Coach\n${r.coachNote}"};b.btnPdf.setOnClickListener{export("application/pdf"){ReportExporter.pdf(this,it)}};b.btnCsv.setOnClickListener{export("text/csv"){ReportExporter.csv(this,it)}};b.btnXls.setOnClickListener{export("application/vnd.ms-excel"){ReportExporter.xls(this,it)}}};private fun export(type:String,create:(ReportRow)->File){val r=row?:return;lifecycleScope.launch{val f=withContext(Dispatchers.IO){create(r)};val uri=FileProvider.getUriForFile(this@ReportDetailActivity,"${packageName}.fileprovider",f);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{this.type=type;putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Bagikan laporan"))}};companion object{const val EXTRA_ID="report_id"}}
