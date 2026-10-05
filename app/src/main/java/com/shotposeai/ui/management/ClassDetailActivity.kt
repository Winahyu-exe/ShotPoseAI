package com.shotposeai.ui.management

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shotposeai.analysis.MovementCatalog
import com.shotposeai.data.ClassRoomEntity
import com.shotposeai.data.ClassStudentEntity
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.StudentEntity
import com.shotposeai.databinding.ActivityClassDetailBinding
import com.shotposeai.ui.common.ConfirmActionDialog
import com.shotposeai.ui.common.StudentCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ClassDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassDetailBinding

    private val classId: Long by lazy {
        intent.getLongExtra(EXTRA_ID, 0L)
    }

    private var members: List<StudentEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddStudents.setOnClickListener {
            showStudents()
        }

        binding.etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    renderMembers(text?.toString().orEmpty())
                }

                override fun afterTextChanged(
                    editable: Editable?
                ) = Unit
            }
        )
    }

    override fun onResume() {
        super.onResume()
        loadClassDetail()
    }

    private fun loadClassDetail() {
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                val dao = DatabaseProvider
                    .get(this@ClassDetailActivity)
                    .appDao()

                ClassSnapshot(
                    classroom = dao.getClassById(classId),
                    members = dao.getClassStudents(classId),
                    average = dao.classAverageScore(classId),
                    completed = dao.classCompletedMovementCount(classId)
                )
            }

            val classroom = snapshot.classroom

            if (classroom == null) {
                finish()
                return@launch
            }

            members = snapshot.members

            binding.tvClassName.text = classroom.name
            binding.tvCategory.text = classroom.category

            binding.tvMemberCount.text =
                "${members.size}/30 Siswa"

            binding.progressMembers.progress =
                members.size.coerceAtMost(30)

            binding.tvAverage.text = String.format(
                Locale.US,
                "%.1f",
                snapshot.average / 10.0
            )

            val totalMovements = MovementCatalog.all.size
            val completedMovements =
                snapshot.completed.coerceAtMost(totalMovements)

            binding.tvLevels.text =
                "$completedMovements/$totalMovements"

            renderMembers(
                binding.etSearch.text
                    ?.toString()
                    .orEmpty()
            )

            binding.btnEdit.setOnClickListener {
                val editIntent = Intent(
                    this@ClassDetailActivity,
                    ClassFormActivity::class.java
                ).apply {
                    putExtra(
                        ClassFormActivity.EXTRA_ID,
                        classId
                    )
                }

                startActivity(editIntent)
            }

            binding.btnDelete.setOnClickListener {
                showDeleteConfirmation(classroom)
            }
        }
    }

    private fun renderMembers(query: String) {
        val keyword = query.trim()

        val shownMembers = members.filter { student ->
            student.name.contains(
                keyword,
                ignoreCase = true
            )
        }

        binding.memberList.removeAllViews()

        shownMembers.forEach { student ->
            val studentCard = StudentCard.create(
                context = this@ClassDetailActivity,
                student = student,
                scope= lifecycleScope
            ) {
                val detailIntent = Intent(
                    this@ClassDetailActivity,
                    StudentDetailActivity::class.java
                ).apply {
                    putExtra(
                        StudentDetailActivity.EXTRA_ID,
                        student.id
                    )
                }

                startActivity(detailIntent)
            }

            binding.memberList.addView(studentCard)
        }

        binding.tvShown.text =
            "${shownMembers.size} Siswa Ditampilkan"
    }

    private fun showDeleteConfirmation(
        classroom: ClassRoomEntity
    ) {
        ConfirmActionDialog.show(
            context = this@ClassDetailActivity,
            message = "Apakah Anda yakin ingin\nmenghapus kelas?"
        ) {
            deleteClass(classroom)
        }
    }

    private fun deleteClass(
        classroom: ClassRoomEntity
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = DatabaseProvider
                    .get(this@ClassDetailActivity)
                    .appDao()

                dao.removeAllStudentsFromClass(classId)
                dao.deleteClass(classroom)
            }

            finish()
        }
    }

    private fun showStudents() {
        lifecycleScope.launch {
            val allStudents = withContext(Dispatchers.IO) {
                DatabaseProvider
                    .get(this@ClassDetailActivity)
                    .appDao()
                    .students
            }

            if (allStudents.isEmpty()) {
                AlertDialog.Builder(
                    this@ClassDetailActivity
                )
                    .setMessage(
                        "Tambahkan siswa terlebih dahulu."
                    )
                    .setPositiveButton("OK", null)
                    .show()

                return@launch
            }

            val memberIds = members
                .map { student -> student.id }
                .toSet()

            val checkedStudents =
                BooleanArray(allStudents.size) { index ->
                    allStudents[index].id in memberIds
                }

            val studentNames = allStudents
                .map { student -> student.name }
                .toTypedArray()

            AlertDialog.Builder(
                this@ClassDetailActivity
            )
                .setTitle("Pilih Siswa")
                .setMultiChoiceItems(
                    studentNames,
                    checkedStudents
                ) { _, index, isChecked ->
                    checkedStudents[index] = isChecked
                }
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan") { _, _ ->
                    saveSelectedStudents(
                        allStudents,
                        checkedStudents
                    )
                }
                .show()
        }
    }

    private fun saveSelectedStudents(
        allStudents: List<StudentEntity>,
        checkedStudents: BooleanArray
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = DatabaseProvider
                    .get(this@ClassDetailActivity)
                    .appDao()

                dao.clearClassStudents(classId)

                allStudents.forEachIndexed { index, student ->
                    if (checkedStudents[index]) {
                        dao.addStudentToClass(
                            ClassStudentEntity(
                                classId,
                                student.id
                            )
                        )
                    }
                }
            }

            loadClassDetail()
        }
    }

    private data class ClassSnapshot(
        val classroom: ClassRoomEntity?,
        val members: List<StudentEntity>,
        val average: Double,
        val completed: Int
    )

    companion object {
        const val EXTRA_ID = "class_id"
    }
}