package com.shotposeai.ui.management

import com.shotposeai.ui.common.AvatarStore
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.R
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.StudentEntity
import com.shotposeai.databinding.ActivityStudentFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentFormBinding
    private val id by lazy { intent.getLongExtra(EXTRA_ID, 0) }
    private var editing: StudentEntity? = null
    private var photoUri = ""
    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            binding.btnSave.isEnabled = false
            binding.ivPhoto.isEnabled = false
            binding.btnChangePhoto.isEnabled = false
            try {
                photoUri = AvatarStore.import(this@StudentFormActivity, uri)
                AvatarStore.bind(binding.ivPhoto, photoUri, lifecycleScope)
            } catch (e: Exception) {
                Toast.makeText(this@StudentFormActivity, "Foto tidak dapat dibaca. Pilih foto lain.", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.ivPhoto.isEnabled = true
                binding.btnChangePhoto.isEnabled = true
            }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityStudentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(binding.rbBeginner,binding.rbGood,binding.rbExcellent).forEach { tile ->
            tile.text=android.text.SpannableString(tile.text).apply {
                setSpan(android.text.style.AbsoluteSizeSpan(34,true),0,tile.text.indexOf('\n'),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        binding.btnBack.setOnClickListener { finish() }
        photoUri = state?.getString("selected_photo").orEmpty()
        AvatarStore.bind(binding.ivPhoto, photoUri, lifecycleScope)
        binding.btnChangePhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.ivPhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        if (id > 0) { binding.btnSave.isEnabled = false; binding.ivPhoto.isEnabled = false; binding.btnChangePhoto.isEnabled = false }
        if (id > 0) lifecycleScope.launch {
            editing = withContext(Dispatchers.IO) { DatabaseProvider.get(this@StudentFormActivity).appDao().getStudent(id) }
            if (editing == null) { Toast.makeText(this@StudentFormActivity,"Siswa tidak ditemukan",Toast.LENGTH_SHORT).show();finish();return@launch }
            editing?.let { student ->
                if (state == null) binding.etName.setText(student.name)
                if (state == null) binding.etNumber.setText(student.attendanceNumber)
                if (state == null) binding.rgLevel.check(if (student.baseLevel == "Good") R.id.rb_good else if (student.baseLevel == "Excellent") R.id.rb_excellent else R.id.rb_beginner)
                if (state == null) photoUri = student.profilePhotoPath.orEmpty()
                AvatarStore.bind(binding.ivPhoto, photoUri, lifecycleScope)
                binding.btnSave.text = "Simpan Perubahan"
                binding.btnSave.isEnabled = true
                binding.ivPhoto.isEnabled = true
                binding.btnChangePhoto.isEnabled = true
            }
        }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selected_photo", photoUri)
        super.onSaveInstanceState(outState)
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val number = binding.etNumber.text.toString().trim()
        if (name.isBlank() || number.isBlank()) {
            Toast.makeText(this, "Lengkapi identitas siswa", Toast.LENGTH_SHORT).show(); return
        }
        val level = when (binding.rgLevel.checkedRadioButtonId) {
            R.id.rb_good -> "Good"; R.id.rb_excellent -> "Excellent"; else -> "Beginner"
        }
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val dao = DatabaseProvider.get(this@StudentFormActivity).appDao()
                    val old = editing
                    if (old == null) dao.insertStudent(StudentEntity(name, number, level, photoUri, System.currentTimeMillis()))
                    else { old.name=name; old.attendanceNumber=number; old.baseLevel=level; old.profilePhotoPath=photoUri; dao.updateStudent(old) }
                }.isSuccess
            }
            binding.btnSave.isEnabled = true
            if (success) finish() else Toast.makeText(this@StudentFormActivity, "Nomor absen sudah digunakan", Toast.LENGTH_SHORT).show()
        }
    }

    companion object { const val EXTRA_ID = "edit_student_id" }
}
