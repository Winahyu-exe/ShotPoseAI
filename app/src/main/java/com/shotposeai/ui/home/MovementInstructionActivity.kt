package com.shotposeai.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.shotposeai.analysis.TechniqueVideos
import com.shotposeai.ui.common.TechniquePreview
import com.shotposeai.databinding.ActivityMovementInstructionBinding
import com.shotposeai.ui.analysis.AnalysisActivity

class MovementInstructionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMovementInstructionBinding
    private lateinit var movement: String
    private var preview: TechniquePreview? = null
    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: AnalysisActivity.MODE_CAMERA }
    private val studentId by lazy { intent.getLongExtra(StudentPickerActivity.EXTRA_STUDENT_ID, 0L) }
    private val studentName by lazy { intent.getStringExtra(StudentPickerActivity.EXTRA_STUDENT_NAME) ?: "Siswa" }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        openAnalysis(AnalysisActivity.MODE_VIDEO, uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovementInstructionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movement = intent.getStringExtra(EXTRA_MOVEMENT) ?: "Gerakan sepak bola"
        binding.tvMovement.text = movement
        binding.tvDescription.text = "Ikuti tahapan gerakan dengan tenang dan pastikan tubuh terlihat jelas."
        binding.tvSteps.text = stepsFor(movement)
        TechniqueVideos.byMovement[movement]?.let { video ->
            preview = TechniquePreview(this,binding.webTechnique,binding.tvPreviewStatus).also { it.load(video) }
            binding.btnRetryPreview.setOnClickListener { preview?.load(video) }
            binding.btnYoutube.setOnClickListener { preview?.open(video.watchUrl) }
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnStart.setOnClickListener { openAnalysis(AnalysisActivity.MODE_CAMERA, null) }
        binding.btnUpload.setOnClickListener { pickVideoLauncher.launch(arrayOf("video/mp4", "video/*")) }
    }

    override fun onPause() { preview?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); preview?.resume() }
    override fun onDestroy() { preview?.destroy(); preview=null; super.onDestroy() }

    private fun stepsFor(name: String): String = when {
        name.contains("Throw", true) -> "1. Berdiri menghadap arah lemparan.\n2. Pegang bola dengan kedua tangan.\n3. Bawa bola melewati atas kepala.\n4. Jaga kedua kaki tetap menyentuh tanah."
        name.contains("Passing", true) -> "1. Posisikan badan menghadap sasaran.\n2. Letakkan kaki tumpu di samping bola.\n3. Ayunkan kaki ke arah sasaran.\n4. Lakukan gerakan lanjutan dengan seimbang."
        name.contains("Dribbling", true) -> "1. Condongkan badan sedikit ke depan.\n2. Sentuh bola secara perlahan dan terkontrol.\n3. Gunakan kedua kaki sesuai arah gerak.\n4. Jaga pandangan dan keseimbangan tubuh."
        name.contains("Heading", true) -> "1. Perhatikan arah datangnya bola.\n2. Gunakan dahi sebagai bidang sentuh.\n3. Gerakkan badan dengan seimbang.\n4. Arahkan bola ke sasaran."
        else -> "1. Siapkan posisi awal dengan seimbang.\n2. Lakukan gerakan sesuai teknik dasar.\n3. Jaga posisi kaki dan badan.\n4. Selesaikan gerakan dengan follow-through."
    }

    private fun openAnalysis(selectedMode: String, uri: String?) {
        startActivity(Intent(this, AnalysisActivity::class.java).apply {
            putExtra(AnalysisActivity.EXTRA_MODE, selectedMode)
            putExtra(AnalysisActivity.EXTRA_MOVEMENT, movement)
            putExtra(StudentPickerActivity.EXTRA_STUDENT_ID, studentId)
            putExtra(StudentPickerActivity.EXTRA_STUDENT_NAME, studentName)
            uri?.let {
                putExtra(AnalysisActivity.EXTRA_VIDEO_URI, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        })
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_MOVEMENT = "extra_movement"
    }
}
