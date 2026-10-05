package com.shotposeai.ui.analysis

import android.Manifest
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.os.CountDownTimer
import android.view.View
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.shotposeai.analysis.MovementClassifier
import com.shotposeai.analysis.MovementFeedback
import com.shotposeai.analysis.PoseLandmarkerHelper
import com.shotposeai.analysis.VideoPoseAnalyzer
import com.shotposeai.data.AnalysisResultEntity
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.data.TrainingSessionEntity
import com.shotposeai.data.AchievementRepository
import java.util.concurrent.Callable
import com.shotposeai.databinding.ActivityAnalysisBinding
import com.shotposeai.databinding.DialogProcessingBinding
import com.shotposeai.ui.home.StudentPickerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class AnalysisActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var cameraExecutor: ExecutorService

    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null

    private var latestFeedback: MovementFeedback? = null
    private var stableFeedback: MovementFeedback? = null
    private var smoothedScore: Float? = null

    private var lastSavedResultSignature: String? = null
    private var isSavingResult = false

    private var lastPanelUpdateAt = 0L
    private var lostPoseFrameCount = 0

    private var pendingMovement: String? = null
    private var pendingMovementCount = 0

    private var mode: String = MODE_CAMERA
    private var selectedMovement: String = "Gerakan sepak bola"
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    private var recordingTimer: CountDownTimer? = null
    private var isCameraRunning = false
    private var importedPreviewBitmap: Bitmap? = null
    private var countdownTimer: CountDownTimer? = null
    private var recording: Recording? = null
    private var stoppingRecording = false
    private var cameraGeneration = 0
    private var currentVideoUri: Uri? = null
    private var recordedFile: File? = null
    private var keepRecordedFile = false
    private var replayController: VideoReplayController? = null
    private var analysisJob: Job? = null
    private var processingDialog: Dialog? = null
    private var foreground = false
    private var restoredPosition = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraMode()
        } else {
            showStatus("Izin kamera ditolak. Aktifkan permission kamera untuk memakai mode live.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CAMERA
        selectedMovement = intent.getStringExtra(EXTRA_MOVEMENT) ?: selectedMovement
        binding.tvTitle.text = "Pantau Pose"

        binding.btnClose.setOnClickListener { finish() }
        binding.btnSwitchCamera.setOnClickListener { switchCamera() }
        binding.btnRecordToggle.setOnClickListener { toggleCameraSession() }
        binding.btnSaveResult.setOnClickListener { saveLatestResult() }
        binding.emptyPoseHint.isVisible = true

        restoredPosition = savedInstanceState?.getInt("replay_position") ?: 0
        recordedFile = savedInstanceState?.getString("recorded_file")?.let(::File)
        val restoredUri = savedInstanceState?.getString("replay_uri")
        if (restoredUri != null || mode == MODE_VIDEO) {
            startVideoMode(restoredUri ?: intent.getStringExtra(EXTRA_VIDEO_URI))
        } else {
            requestCameraPermissionIfNeeded()
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startCameraMode()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraMode() {
        countdownTimer?.cancel()
        replayController?.release()
        replayController = null
        analysisJob?.cancel()
        processingDialog?.dismiss()
        if (!keepRecordedFile) recordedFile?.delete()
        recordedFile = null
        currentVideoUri = null
        binding.replayControls.isVisible = false
        positionRecordButton(forReplay = false)
        binding.overlayView.clear()
        binding.tvLiveBadge.text = "LIVE"
        binding.btnSwitchCamera.visibility = View.VISIBLE
        binding.previewView.visibility = View.VISIBLE
        binding.videoView.visibility = View.GONE
        binding.videoFrameView.visibility = View.GONE
        binding.overlayView.visibility = View.VISIBLE
        binding.emptyPoseHint.isVisible = true
        binding.btnRecordToggle.isVisible = false
        binding.tvRecordTimer.isVisible = true
        binding.tvRecordTimer.text = "00:30"

        resetLiveResultState()
        binding.btnSwitchCamera.isEnabled = true
        showStatus("Mulai dalam 3 detik...")
        countdownTimer = object : CountDownTimer(3_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                showStatus("Mulai dalam ${(millisUntilFinished / 1_000L) + 1L} detik...")
            }
            override fun onFinish() {
                countdownTimer = null
                if (!isFinishing && !isDestroyed && foreground) openCameraPreview()
            }
        }.start()
    }

    private fun openCameraPreview() {
        val generation = ++cameraGeneration
        binding.btnSwitchCamera.isEnabled = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            if (generation != cameraGeneration || isFinishing || isDestroyed || !foreground) return@addListener
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                val recorder = Recorder.Builder().setQualitySelector(
                    QualitySelector.fromOrderedList(listOf(Quality.HD, Quality.SD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
                ).build()
                val capture = VideoCapture.withOutput(recorder)
                capture.targetRotation = binding.previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                cameraExecutor.execute {
                    poseLandmarkerHelper?.close()
                    poseLandmarkerHelper = PoseLandmarkerHelper(this, this)
                }
                imageAnalyzer.setAnalyzer(cameraExecutor) { proxy ->
                    val helper = poseLandmarkerHelper
                    if (helper != null) helper.detectLiveStream(proxy) else proxy.close()
                }
                provider.unbindAll()
                var liveOverlayAvailable = true
                try {
                    provider.bindToLifecycle(this, cameraSelector, preview, capture, imageAnalyzer)
                } catch (_: IllegalArgumentException) {
                    // Some devices cannot stream Preview + VideoCapture + ImageAnalysis together.
                    provider.unbindAll()
                    imageAnalyzer.clearAnalyzer()
                    provider.bindToLifecycle(this, cameraSelector, preview, capture)
                    liveOverlayAvailable = false
                }
                val directory = File(filesDir, "analysis_videos").apply { mkdirs() }
                val file = File.createTempFile("recording_", ".mp4", directory)
                recordedFile = file
                keepRecordedFile = false
                stoppingRecording = false
                isCameraRunning = true
                binding.btnRecordToggle.isVisible = true
                binding.btnRecordToggle.isEnabled = false
                binding.btnRecordToggle.text = "Menyiapkan..."
                val output = FileOutputOptions.Builder(file)
                    .setDurationLimitMillis(MAX_VIDEO_DURATION_MS.toLong()).build()
                recording = recorder.prepareRecording(this, output)
                    .start(ContextCompat.getMainExecutor(this)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                if (!isFinishing && !isDestroyed && !stoppingRecording) {
                                    binding.btnRecordToggle.text = "Berhenti"
                                    binding.btnRecordToggle.isEnabled = true
                                    startRecordingTimer()
                                    showStatus(if (liveOverlayAvailable) "Merekam video. Arahkan ke seluruh tubuh pemain."
                                        else "Merekam video. Landmark tersedia setelah perekaman selesai.")
                                }
                            }
                            is VideoRecordEvent.Finalize -> {
                                recordingTimer?.cancel()
                                recordingTimer = null
                                recording = null
                                stoppingRecording = false
                                isCameraRunning = false
                                cameraProvider?.unbindAll()
                                closeLiveLandmarker()
                                if (isFinishing || isDestroyed) {
                                    if (!isChangingConfigurations) file.delete()
                                } else {
                                    val usable = event.error == VideoRecordEvent.Finalize.ERROR_NONE ||
                                        event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ||
                                        event.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
                                    if (usable && file.length() > 0L) {
                                        startVideoMode(Uri.fromFile(file).toString())
                                    } else {
                                        file.delete()
                                        resetLiveResultState()
                                        binding.btnRecordToggle.text = "Rekam Lagi"
                                        binding.btnRecordToggle.isEnabled = true
                                        binding.btnSwitchCamera.isEnabled = true
                                        showStatus("Video gagal direkam. Silakan rekam ulang (kode ${event.error}).")
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                isCameraRunning = false
                cameraProvider?.unbindAll()
                closeLiveLandmarker()
                binding.btnRecordToggle.isVisible = true
                binding.btnRecordToggle.isEnabled = true
                binding.btnRecordToggle.text = "Rekam Lagi"
                binding.btnSwitchCamera.isEnabled = true
                showStatus("Gagal membuka kamera: ${e.message.orEmpty()}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun closeLiveLandmarker() {
        if (!cameraExecutor.isShutdown) cameraExecutor.execute {
            poseLandmarkerHelper?.close()
            poseLandmarkerHelper = null
        }
    }

    private fun switchCamera() {
        if (recording != null || stoppingRecording) return
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        Toast.makeText(this, if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
            "Rekaman berikutnya memakai kamera depan" else "Rekaman berikutnya memakai kamera belakang",
            Toast.LENGTH_SHORT).show()
    }

    private fun toggleCameraSession() {
        if (isSavingResult) return
        if (recording != null) {
            stopCameraSession("Menyiapkan video untuk diputar ulang...")
        } else if (!stoppingRecording) {
            startCameraMode()
        }
    }

    private fun startRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = object : CountDownTimer(MAX_VIDEO_DURATION_MS.toLong(), 1_000L) {
            override fun onTick(remaining: Long) {
                val seconds = (remaining / 1_000L).toInt().coerceAtLeast(0)
                binding.tvRecordTimer.text = String.format("00:%02d", seconds)
            }
            override fun onFinish() {
                binding.tvRecordTimer.text = "00:00"
                stopCameraSession("Batas perekaman 30 detik tercapai.")
            }
        }.start()
    }

    private fun stopCameraSession(message: String) {
        if (stoppingRecording) return
        recordingTimer?.cancel()
        recordingTimer = null
        isCameraRunning = false
        stoppingRecording = recording != null
        binding.btnRecordToggle.isEnabled = false
        binding.btnSwitchCamera.isEnabled = false
        binding.btnSaveResult.isEnabled = false
        showStatus(message)
        // The MP4 must be finalized before unbinding or opening it for analysis/playback.
        recording?.stop()
    }

    private fun startVideoMode(uriString: String?) {
        recordingTimer?.cancel()
        countdownTimer?.cancel()
        replayController?.release()
        replayController = null
        analysisJob?.cancel()
        processingDialog?.dismiss()
        isCameraRunning = false
        binding.btnRecordToggle.isVisible = false
        binding.tvRecordTimer.isVisible = false
        binding.replayControls.isVisible = false
        binding.tvLiveBadge.text = "VIDEO"
        binding.btnSwitchCamera.isVisible = false
        binding.previewView.isVisible = false
        binding.videoView.isVisible = false
        binding.videoFrameView.isVisible = true
        binding.overlayView.isVisible = true
        binding.overlayView.clear()
        binding.emptyPoseHint.isVisible = false
        resetLiveResultState()

        val uri = uriString?.let(Uri::parse)
        if (uri == null) {
            showStatus("Video tidak valid. Pilih ulang video dari beranda.")
            return
        }
        currentVideoUri = uri
        val readable = runCatching { contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
        if (!readable) {
            showStatus("Video tidak dapat diakses. Pilih ulang video dari perangkat.")
            binding.emptyPoseHint.isVisible = true
            return
        }
        showStatus("Menganalisis frame video...")
        val progressBinding = DialogProcessingBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(progressBinding.root)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        processingDialog = dialog
        if (foreground) showProcessingDialog()

        analysisJob = lifecycleScope.launch {
            try {
                val samples = mutableListOf<ReplayPoseSample>()
                val feedback = VideoPoseAnalyzer(this@AnalysisActivity).analyze(
                    uri = uri,
                    movement = selectedMovement,
                    onProgress = { progress ->
                        withContext(Dispatchers.Main) {
                            showStatus("Menganalisis video... $progress%")
                            progressBinding.progressProcessing.progress = progress
                            progressBinding.tvProcessing.text = "Menganalisis gerakan… $progress%"
                        }
                    },
                    onFrameAnalyzed = { frame, result, frameWidth, frameHeight ->
                        withContext(Dispatchers.Main) {
                            val previous = importedPreviewBitmap
                            importedPreviewBitmap = frame
                            binding.videoFrameView.setImageBitmap(frame)
                            if (previous !== frame && previous?.isRecycled == false) previous.recycle()
                            binding.overlayView.setResults(result, frameWidth, frameHeight,
                                isMirrored = false, scaleMode = PoseOverlayView.ScaleMode.FIT_CENTER)
                        }
                    },
                    onPoseSample = { timestamp, result, width, height ->
                        samples += ReplayPoseSample(timestamp, result, width, height)
                    }
                )
                showFeedback(feedback, "Analisis video selesai")
                binding.emptyPoseHint.isVisible = false
                binding.overlayView.clear()
                binding.videoView.isVisible = true
                binding.videoFrameView.isVisible = false
                binding.videoFrameView.setImageDrawable(null)
                importedPreviewBitmap?.recycle()
                importedPreviewBitmap = null
                replayController = VideoReplayController(binding, samples.toList(), ::showStatus, allowRetake = mode == MODE_CAMERA).also {
                    it.load(uri, restoredPosition)
                    if (!foreground) it.onPause()
                }
                restoredPosition = 0
                if (mode == MODE_CAMERA) {
                    positionRecordButton(forReplay = true)
                    binding.btnRecordToggle.isVisible = true
                    binding.btnRecordToggle.text = "Rekam Lagi"
                    binding.btnRecordToggle.isEnabled = true
                    binding.btnSwitchCamera.isVisible = true
                    binding.btnSwitchCamera.isEnabled = true
                }
            } finally {
                dialog.dismiss()
                if (processingDialog === dialog) processingDialog = null
            }
        }
    }

    private fun positionRecordButton(forReplay: Boolean) {
        val margin = (12 * resources.displayMetrics.density).roundToInt()
        binding.btnRecordToggle.layoutParams = (binding.btnRecordToggle.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = if (forReplay) Gravity.TOP or Gravity.START else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            topMargin = if (forReplay) margin else 0
            marginStart = if (forReplay) margin else 0
        }
    }

    private fun showProcessingDialog() {
        val dialog = processingDialog ?: return
        if (!dialog.isShowing && !isFinishing && !isDestroyed) {
            dialog.show()
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9f).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        val rawFeedback = MovementClassifier.classify(resultBundle.poseLandmarkerResult, selectedMovement)

        runOnUiThread {
            if (!isCameraRunning || isFinishing || isDestroyed) return@runOnUiThread
            binding.overlayView.setResults(
                resultBundle.poseLandmarkerResult,
                resultBundle.inputImageWidth,
                resultBundle.inputImageHeight,
                isMirrored = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA,
                scaleMode = PoseOverlayView.ScaleMode.FILL_CENTER
            )

            handleLiveFeedback(
                rawFeedback = rawFeedback,
                inferenceText = "Inference: ${resultBundle.inferenceTime} ms"
            )
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            if (!isCameraRunning || isFinishing || isDestroyed) return@runOnUiThread
            resetLiveResultState()
            showStatus(error)
            binding.emptyPoseHint.isVisible = true
            binding.tvFeedback.text = error
            binding.btnSaveResult.isEnabled = false
        }
    }

    private fun handleLiveFeedback(
        rawFeedback: MovementFeedback,
        inferenceText: String
    ) {
        binding.tvStatus.text = inferenceText

        if (rawFeedback.detected) {
            lostPoseFrameCount = 0
        } else {
            lostPoseFrameCount++
        }

        val now = SystemClock.elapsedRealtime()
        val currentStableFeedback = stableFeedback

        val isFirstResult = currentStableFeedback == null
        val becameDetected = rawFeedback.detected && currentStableFeedback?.detected != true
        val poseLostConfirmed = !rawFeedback.detected && lostPoseFrameCount >= POSE_LOST_CONFIRMATION_FRAMES
        val intervalPassed = now - lastPanelUpdateAt >= RESULT_PANEL_UPDATE_INTERVAL_MS

        val shouldUpdatePanel = isFirstResult ||
                becameDetected ||
                poseLostConfirmed ||
                intervalPassed

        if (!shouldUpdatePanel) return

        val displayFeedback = buildStableFeedback(rawFeedback)

        showFeedback(
            feedback = displayFeedback,
            inferenceText = inferenceText
        )

        lastPanelUpdateAt = now
    }

    private fun buildStableFeedback(rawFeedback: MovementFeedback): MovementFeedback {
        if (!rawFeedback.detected) {
            smoothedScore = null
            pendingMovement = null
            pendingMovementCount = 0
            stableFeedback = rawFeedback
            return rawFeedback
        }

        val previousFeedback = stableFeedback

        val previousScore = smoothedScore ?: rawFeedback.score.toFloat()
        val nextScore = previousScore + ((rawFeedback.score - previousScore) * SCORE_SMOOTHING_FACTOR)
        smoothedScore = nextScore

        val roundedScore = nextScore
            .roundToInt()
            .coerceIn(0, 100)

        if (
            previousFeedback == null ||
            !previousFeedback.detected ||
            previousFeedback.movement == rawFeedback.movement
        ) {
            pendingMovement = null
            pendingMovementCount = 0

            val nextFeedback = rawFeedback.copy(score = roundedScore)
            stableFeedback = nextFeedback
            return nextFeedback
        }

        if (pendingMovement == rawFeedback.movement) {
            pendingMovementCount++
        } else {
            pendingMovement = rawFeedback.movement
            pendingMovementCount = 1
        }

        val movementConfirmed = pendingMovementCount >= MOVEMENT_CONFIRMATION_COUNT

        val nextFeedback = if (movementConfirmed) {
            pendingMovement = null
            pendingMovementCount = 0
            rawFeedback.copy(score = roundedScore)
        } else {
            previousFeedback.copy(score = roundedScore)
        }

        stableFeedback = nextFeedback
        return nextFeedback
    }

    private fun showFeedback(
        feedback: MovementFeedback,
        inferenceText: String
    ) {
        latestFeedback = feedback

        binding.tvStatus.text = inferenceText
        val resultStatus = if (feedback.score >= PASSING_SCORE) "Benar" else "Perlu Perbaikan"
        binding.tvStatus.text = "$resultStatus • $inferenceText"
        binding.tvResultStatus.text = resultStatus.uppercase()
        binding.tvMovement.text = selectedMovement
        binding.tvScore.text = "${feedback.score}/100"
        binding.tvDetailSummary.text = if (feedback.detected) {
            "Pose terbaca • Posisi kaki, lutut, bahu, dan keseimbangan dianalisis"
        } else {
            "Pose belum cukup terbaca untuk menghitung detail gerakan"
        }
        binding.tvFeedback.text = feedback.feedback
        binding.emptyPoseHint.isVisible = !feedback.detected

        updateSaveButtonState(feedback)
    }

    private fun showStatus(message: String) {
        binding.tvStatus.text = message
    }

    private fun saveLatestResult() {
        val feedback = latestFeedback ?: return

        if (!feedback.detected) return
        if (isSavingResult) return

        val currentSignature = buildFeedbackSignature(feedback)
        if (currentSignature == lastSavedResultSignature) {
            Toast.makeText(
                this,
                "Hasil ini sudah disimpan",
                Toast.LENGTH_SHORT
            ).show()
            updateSaveButtonState(feedback)
            return
        }

        val studentId = intent.getLongExtra(StudentPickerActivity.EXTRA_STUDENT_ID, 0L)
        val studentName = intent.getStringExtra(StudentPickerActivity.EXTRA_STUDENT_NAME) ?: "Siswa"
        if (studentId <= 0L) {
            Toast.makeText(this, "Pilih siswa sebelum menyimpan hasil", Toast.LENGTH_SHORT).show()
            return
        }

        val videoUriToSave = currentVideoUri?.toString() ?: ""
        isSavingResult = true
        // Keep the source while the transaction is running, even if the user leaves.
        keepRecordedFile = recordedFile != null
        binding.btnRecordToggle.isEnabled = false
        updateSaveButtonState(feedback)

        lifecycleScope.launch(Dispatchers.IO) {
            val saveResult = runCatching {
                val database = DatabaseProvider.get(this@AnalysisActivity)
                database.runInTransaction(Callable {
                val dao = database.appDao()
                check(dao.getStudent(studentId) != null) { "Siswa sudah dihapus" }
                val level = when { feedback.score >= 85 -> "Excellent"; feedback.score >= 70 -> "Good"; else -> "Beginner" }
                val sessionId = dao.insertTraining(TrainingSessionEntity(
                    studentId, 0L, selectedMovement, System.currentTimeMillis(),
                    videoUriToSave, feedback.score, level, "COMPLETED"
                ))
                dao.insertAnalysis(AnalysisResultEntity(
                    sessionId,
                    if (feedback.score >= PASSING_SCORE) "Posisi dasar dan keseimbangan terbaca baik\nGerakan berhasil dianalisis" else "Pose tubuh berhasil terdeteksi",
                    if (feedback.score >= PASSING_SCORE) "Pertahankan konsistensi dan follow-through" else feedback.feedback,
                    "Perhatikan kaki tumpu, arah pandangan, dan stabilitas badan.",
                    "{\"detected\":${feedback.detected},\"score\":${feedback.score}}"
                ))
                val newAchievementId = AchievementRepository.unlockForScore(dao, studentId, selectedMovement, feedback.score)
                sessionId to newAchievementId
                })
            }

            withContext(Dispatchers.Main) {
                isSavingResult = false

                saveResult
                    .onSuccess { (sessionId, achievementId) ->
                        keepRecordedFile = true
                        lastSavedResultSignature = currentSignature
                        recordingTimer?.cancel()
                        cameraProvider?.unbindAll()
                        binding.videoView.pause()
                        startActivity(android.content.Intent(this@AnalysisActivity, AnalysisResultActivity::class.java).apply {
                            putExtra(AnalysisResultActivity.EXTRA_SESSION_ID, sessionId)
                            putExtra(AnalysisResultActivity.EXTRA_STUDENT_NAME, studentName)
                            putExtra(AnalysisResultActivity.EXTRA_NEW_ACHIEVEMENT_ID, achievementId ?: 0L)
                        })
                        finish()
                    }
                    .onFailure {
                        keepRecordedFile = false
                        if (replayController != null) binding.btnRecordToggle.isEnabled = true
                        Toast.makeText(
                            this@AnalysisActivity,
                            "Gagal menyimpan hasil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                updateSaveButtonState(latestFeedback)
            }
        }
    }

    private fun updateSaveButtonState(feedback: MovementFeedback? = latestFeedback) {
        if (isSavingResult) {
            binding.btnSaveResult.text = "Menyimpan..."
            binding.btnSaveResult.isEnabled = false
            return
        }

        if (isCameraRunning || recording != null || stoppingRecording || feedback?.detected != true) {
            binding.btnSaveResult.text = "Simpan & Lihat Hasil"
            binding.btnSaveResult.isEnabled = false
            return
        }

        val currentSignature = buildFeedbackSignature(feedback)
        val alreadySaved = currentSignature == lastSavedResultSignature

        binding.btnSaveResult.text = if (alreadySaved) {
            "Tersimpan"
        } else {
            "Simpan & Lihat Hasil"
        }

        binding.btnSaveResult.isEnabled = !alreadySaved
    }

    private fun buildFeedbackSignature(feedback: MovementFeedback): String {
        return listOf(
            mode,
            feedback.movement,
            feedback.score.toString(),
            feedback.feedback
        ).joinToString(separator = "|")
    }

    private fun resetLiveResultState() {
        latestFeedback = null
        stableFeedback = null
        smoothedScore = null
        lastSavedResultSignature = null
        isSavingResult = false
        lastPanelUpdateAt = 0L
        lostPoseFrameCount = 0
        pendingMovement = null
        pendingMovementCount = 0

        binding.btnSaveResult.text = "Simpan & Lihat Hasil"
        binding.btnSaveResult.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        replayController?.onResume()
        showProcessingDialog()
    }

    override fun onPause() {
        foreground = false
        countdownTimer?.cancel()
        countdownTimer = null
        cameraGeneration++
        replayController?.onPause()
        processingDialog?.dismiss()
        if (recording != null) stopCameraSession("Perekaman berhenti. Menyiapkan video...")
        else if (mode == MODE_CAMERA && currentVideoUri == null) {
            binding.btnRecordToggle.isVisible = true
            binding.btnRecordToggle.isEnabled = true
            binding.btnRecordToggle.text = "Rekam Lagi"
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("replay_uri", currentVideoUri?.toString())
        outState.putString("recorded_file", recordedFile?.absolutePath)
        outState.putInt("replay_position", replayController?.positionMs ?: restoredPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        recordingTimer?.cancel()
        analysisJob?.cancel()
        processingDialog?.dismiss()
        replayController?.release()
        val pendingRecording = recording
        pendingRecording?.close()
        cameraProvider?.unbindAll()
        closeLiveLandmarker()
        binding.videoFrameView.setImageDrawable(null)
        importedPreviewBitmap?.recycle()
        importedPreviewBitmap = null
        if (pendingRecording == null && !keepRecordedFile && !isChangingConfigurations) recordedFile?.delete()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_MOVEMENT = "extra_movement"
        const val MODE_CAMERA = "camera"
        const val MODE_VIDEO = "video"

        private const val RESULT_PANEL_UPDATE_INTERVAL_MS = 550L
        private const val SCORE_SMOOTHING_FACTOR = 0.35f
        private const val POSE_LOST_CONFIRMATION_FRAMES = 6
        private const val MOVEMENT_CONFIRMATION_COUNT = 2
        private const val MAX_VIDEO_DURATION_MS = 30_000
        private const val PASSING_SCORE = 70
    }
}
