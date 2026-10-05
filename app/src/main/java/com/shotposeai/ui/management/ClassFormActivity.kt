package com.shotposeai.ui.management

import android.os.Bundle
import android.view.View
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import java.util.concurrent.Callable
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.shotposeai.data.*
import com.shotposeai.databinding.ActivityClassFormBinding
import com.shotposeai.ui.common.AvatarStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassFormActivity : AppCompatActivity() {
    private lateinit var b: ActivityClassFormBinding
    private val id by lazy { intent.getLongExtra(EXTRA_ID, 0) }
    private var selectedCategory = "Passing"
    private var restored = false
    private var editing: ClassRoomEntity? = null
    private var students = emptyList<StudentEntity>()
    private val checkedIds = linkedSetOf<Long>()
    private val categories = listOf("Passing", "Dribbling", "Shooting", "Kebugaran", "Campuran")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); b = ActivityClassFormBinding.inflate(layoutInflater); setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }
        restored = s != null
        checkedIds.addAll(s?.getLongArray("checked_students")?.toList().orEmpty())
        selectedCategory=s?.getString("category")?:"Passing"
        categories.forEach { category ->
            b.categoryChips.addView(Chip(this).apply {
                this.id=View.generateViewId(); text=category; isCheckable=true;isChecked=category==selectedCategory
                setOnCheckedChangeListener { _, checked -> if(checked) selectedCategory=category }
            })
        }
        listOf(b.rbBall,b.rbBasket,b.rbVolley,b.rbBadminton,b.rbRun).forEach { tile ->
            val text=SpannableString(tile.text)
            val lineEnd=tile.text.indexOf('\n').coerceAtLeast(0)
            text.setSpan(AbsoluteSizeSpan(28,true),0,lineEnd,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tile.text=text
        }
        b.rbBall.setTextColor(android.graphics.Color.WHITE)
        b.btnSave.isEnabled=false
        b.btnAllStudents.setOnClickListener { showAllStudents() }
        b.etSearch.doAfterTextChanged { renderStudents(it?.toString().orEmpty()) }
        b.btnSave.setOnClickListener { save() }
        load()
    }
    private fun load() = lifecycleScope.launch {
        val data = withContext(Dispatchers.IO) {
            val dao = DatabaseProvider.get(this@ClassFormActivity).appDao()
            Triple(if (id > 0) dao.getClassById(id) else null, dao.students, if (id > 0) dao.getClassStudents(id).map { it.id } else emptyList())
        }
        editing = data.first; students = data.second; if(!restored) checkedIds.addAll(data.third)
        editing?.let {
            b.tvTitle.text = "Edit Kelas"; if(!restored) { b.etName.setText(it.name);selectedCategory=it.category }
            for(i in 0 until b.categoryChips.childCount){ val chip=b.categoryChips.getChildAt(i) as Chip;chip.isChecked=chip.text.toString()==selectedCategory }
            b.btnSave.text = "Simpan Perubahan"
        }
        b.btnSave.isEnabled=true
        renderStudents(b.etSearch.text.toString())
    }
    private fun renderStudents(query: String) {
        b.studentList.removeAllViews(); val match = students.filter { query.isBlank() || it.name.contains(query, true) }
        match.take(6).forEach { student ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(8, 8, 8, 8) }
            val avatar = android.widget.ImageView(this); row.addView(avatar, LinearLayout.LayoutParams(dp(42), dp(42))); AvatarStore.bind(avatar, student.profilePhotoPath, lifecycleScope)
            row.addView(TextView(this).apply { text = "${student.name}\nNo. ${student.attendanceNumber}"; textSize = 13f }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart=dp(12) })
            row.addView(CheckBox(this).apply { isChecked = student.id in checkedIds; contentDescription = "Pilih ${student.name}"; setOnCheckedChangeListener { _, yes -> if (yes) checkedIds += student.id else checkedIds -= student.id } })
            b.studentList.addView(row)
        }
        if (match.isEmpty()) b.studentList.addView(TextView(this).apply { text = "Siswa tidak ditemukan"; setPadding(8, 16, 8, 16) })
    }
    private fun save() {
        val name=b.etName.text.toString().trim(); if(name.isBlank()){Toast.makeText(this,"Nama kelas wajib diisi",Toast.LENGTH_SHORT).show();return}
        b.btnSave.isEnabled=false
        val category=selectedCategory
        val members=checkedIds.toList()
        lifecycleScope.launch {
            val session=SessionStore(this@ClassFormActivity).current()
            val result=withContext(Dispatchers.IO){ runCatching {
                val database=DatabaseProvider.get(this@ClassFormActivity)
                database.runInTransaction(Callable {
                val dao=database.appDao(); val old=editing
                val classId=if(old==null) dao.insertClass(ClassRoomEntity(name,category,"BALL",session?.userId?:0,System.currentTimeMillis())) else {old.name=name;old.category=category;old.iconType="BALL";dao.updateClass(old);old.id}
                dao.clearClassStudents(classId);members.forEach{dao.addStudentToClass(ClassStudentEntity(classId,it))}
                })
            }}
            b.btnSave.isEnabled=true;if(result.isSuccess)finish() else Toast.makeText(this@ClassFormActivity,"Kelas belum tersimpan",Toast.LENGTH_LONG).show()
        }
    }
    private fun showAllStudents() {
        if(students.isEmpty()){Toast.makeText(this,"Tambahkan siswa melalui menu Siswa",Toast.LENGTH_SHORT).show();return}
        val selected=BooleanArray(students.size){students[it].id in checkedIds}
        AlertDialog.Builder(this).setTitle("Pilih Siswa").setMultiChoiceItems(students.map{it.name}.toTypedArray(),selected){_,i,yes->selected[i]=yes}
            .setNegativeButton("Batal",null).setPositiveButton("Terapkan"){_,_->
                checkedIds.clear();students.forEachIndexed{i,student->if(selected[i])checkedIds+=student.id};renderStudents(b.etSearch.text.toString())
            }.show()
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putLongArray("checked_students",checkedIds.toLongArray());outState.putString("category",selectedCategory);super.onSaveInstanceState(outState)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    companion object{const val EXTRA_ID="edit_class_id"}
}
