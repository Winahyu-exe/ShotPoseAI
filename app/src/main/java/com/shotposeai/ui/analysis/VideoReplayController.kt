package com.shotposeai.ui.analysis

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.shotposeai.R
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.shotposeai.databinding.ActivityAnalysisBinding
import java.util.Locale

/** Results are cached, never inferred again when replaying or seeking backwards. */
internal data class ReplayPoseSample(
    val timestampMs: Long,
    val result: PoseLandmarkerResult?,
    val width: Int,
    val height: Int
)

internal class VideoReplayController(
    private val binding: ActivityAnalysisBinding,
    private val samples: List<ReplayPoseSample>,
    private val onPlaybackError: (String) -> Unit,
    private val allowRetake: Boolean = false
) {
    private val handler = Handler(Looper.getMainLooper())
    private val timestamps = samples.map { it.timestampMs }
    private var player: MediaPlayer? = null
    private var prepared = false
    private var disposed = false
    private var foreground = true
    private var dragging = false
    private var seeking = false
    private var resumeAfterSeek = false
    private var savedPosition = 0
    private var completed = false
    private var shownIndex = Int.MIN_VALUE
    private val accessibilityManager = binding.root.context
        .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private val hideControls: Runnable = Runnable {
        if (!disposed && foreground && prepared && !dragging && !seeking &&
            binding.videoView.isPlaying && !accessibilityManager.isTouchExplorationEnabled) {
            setControlsVisible(false)
        }
    }

    private fun setControlsVisible(visible: Boolean) {
        handler.removeCallbacks(hideControls)
        binding.replayChrome.isVisible = visible
        binding.replayControls.contentDescription = if (visible)
            "Sembunyikan kontrol video" else "Tampilkan kontrol video"
        if (allowRetake) binding.btnRecordToggle.isVisible = visible
        if (visible && prepared && foreground && !dragging && !seeking && binding.videoView.isPlaying &&
            !accessibilityManager.isTouchExplorationEnabled) {
            handler.postDelayed(hideControls, 2500L)
        }
    }

    val positionMs: Int
        get() = if (prepared && !seeking) binding.videoView.currentPosition.coerceAtLeast(0) else savedPosition

    private val ticker = object : Runnable {
        override fun run() {
            if (!foreground || disposed || !prepared) return
            if (!dragging && !seeking) updateFrame(positionMs)
            handler.postDelayed(this, 33L)
        }
    }

    fun load(uri: Uri, initialPosition: Int = 0) {
        savedPosition = initialPosition
        binding.replayControls.isVisible = true
        setControlsVisible(true)
        binding.replayControls.setOnClickListener {
            setControlsVisible(!binding.replayChrome.isVisible)
        }
        setControlsEnabled(false)
        binding.videoView.setOnPreparedListener { mp ->
            if (disposed) return@setOnPreparedListener
            player = mp
            prepared = true
            completed = false
            mp.isLooping = false
            binding.videoFrameView.isVisible = false
            binding.emptyPoseHint.isVisible = false
            binding.replaySeek.max = binding.videoView.duration.coerceAtLeast(1)
            mp.setOnSeekCompleteListener {
                if (!disposed) {
                    seeking = false
                    setControlsEnabled(true)
                    updateFrame(binding.videoView.currentPosition.coerceAtLeast(0))
                    if (resumeAfterSeek && foreground) binding.videoView.start()
                    resumeAfterSeek = false
                    updatePlayLabel()
                }
            }
            setControlsEnabled(true)
            seek(savedPosition.coerceAtLeast(1), false)
            handler.removeCallbacks(ticker)
            if (foreground) handler.post(ticker)
        }
        binding.videoView.setOnCompletionListener {
            completed = true
            savedPosition = binding.videoView.duration.coerceAtLeast(0)
            updateFrame(savedPosition)
            updatePlayLabel()
        }
        binding.videoView.setOnErrorListener { _, _, _ ->
            prepared = false
            handler.removeCallbacks(ticker)
            binding.overlayView.clear()
            binding.tvReplayPose.isVisible = true
            setControlsVisible(true)
            binding.tvReplayPose.text = "Video tidak dapat diputar. Gunakan video MP4/H.264 atau rekam ulang."
            setControlsEnabled(false)
            onPlaybackError(binding.tvReplayPose.text.toString())
            true
        }
        binding.btnPlayPause.setOnClickListener {
            if (prepared && !seeking) {
                when {
                    binding.videoView.isPlaying -> pause()
                    completed -> seek(0, true)
                    else -> { binding.videoView.start(); updatePlayLabel() }
                }
            }
        }
        binding.replaySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                dragging = true
                handler.removeCallbacks(hideControls)
                resumeAfterSeek = binding.videoView.isPlaying
                binding.videoView.pause()
                binding.overlayView.clear()
                shownIndex = Int.MIN_VALUE
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvReplayTime.text = timeText(progress)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                dragging = false
                seek(seekBar.progress, resumeAfterSeek)
            }
        })
        binding.videoView.setVideoURI(uri)
        binding.videoView.requestFocus()
    }

    private fun seek(position: Int, resume: Boolean) {
        if (!prepared) return
        savedPosition = position.coerceIn(0, binding.replaySeek.max)
        completed = false
        seeking = true
        setControlsEnabled(false)
        resumeAfterSeek = resume
        binding.videoView.pause()
        binding.overlayView.clear()
        shownIndex = Int.MIN_VALUE
        // Wait for the decoded frame, rather than displaying landmarks at the requested
        // position over a video frame that still belongs to the old playback position.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            player?.seekTo(savedPosition.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            player?.seekTo(savedPosition)
        }
        updatePlayLabel()
    }

    private fun updateFrame(position: Int) {
        savedPosition = position
        binding.replaySeek.progress = position
        binding.tvReplayTime.text = timeText(position)
        val index = if (position >= ReplayTimeline.ANALYSIS_LIMIT_MS) -2
            else ReplayTimeline.indexAt(timestamps, position.toLong())
        if (index != shownIndex) {
            shownIndex = index
            val sample = samples.getOrNull(index)
            val result = sample?.result
            if (sample != null && result != null && result.landmarks().firstOrNull()?.isNotEmpty() == true) {
                binding.overlayView.setResults(result, sample.width, sample.height,
                    isMirrored = false, scaleMode = PoseOverlayView.ScaleMode.FIT_CENTER)
                binding.tvReplayPose.isVisible = false
            } else {
                binding.overlayView.clear()
                binding.tvReplayPose.isVisible = true
                binding.tvReplayPose.text = if (position >= ReplayTimeline.ANALYSIS_LIMIT_MS) {
                    "Bagian ini di luar 30 detik pertama yang dianalisis."
                } else {
                    "Pose tidak terbaca pada bagian video ini."
                }
            }
        }
    }

    private fun timeText(position: Int) = "${formatTime(position)} / ${formatTime(binding.replaySeek.max)}"
    private fun formatTime(ms: Int): String = String.format(Locale.US, "%02d:%02d", ms / 60000, (ms / 1000) % 60)
    private fun updatePlayLabel() {
        val playing = binding.videoView.isPlaying
        binding.btnPlayPause.setImageResource(when {
            completed -> R.drawable.ic_analysis_replay
            playing -> android.R.drawable.ic_media_pause
            else -> android.R.drawable.ic_media_play
        })
        binding.btnPlayPause.contentDescription = when {
            completed -> "Putar ulang video"
            playing -> "Jeda video"
            else -> "Putar video"
        }
        setControlsVisible(true)
    }
    private fun setControlsEnabled(enabled: Boolean) {
        binding.btnPlayPause.isEnabled = enabled
        binding.replaySeek.isEnabled = enabled
    }
    fun pause() {
        savedPosition = positionMs
        resumeAfterSeek = false
        binding.videoView.pause()
        updatePlayLabel()
    }
    fun onPause() {
        foreground = false
        pause()
        handler.removeCallbacks(ticker)
        // SurfaceView can be destroyed while backgrounded. Reopen explicitly so a
        // transient currentPosition=0 cannot overwrite the saved playback position.
        player?.setOnSeekCompleteListener(null)
        binding.videoView.suspend()
        player = null
        prepared = false
        seeking = false
        setControlsEnabled(false)
        binding.overlayView.clear()
        shownIndex = Int.MIN_VALUE
    }
    fun onResume() {
        foreground = true
        handler.removeCallbacks(ticker)
        if (prepared) handler.post(ticker) else binding.videoView.resume()
    }
    fun release() {
        disposed = true
        handler.removeCallbacks(hideControls)
        binding.replayControls.setOnClickListener(null)
        handler.removeCallbacks(ticker)
        player?.setOnSeekCompleteListener(null)
        binding.videoView.setOnPreparedListener(null)
        binding.videoView.setOnCompletionListener(null)
        binding.videoView.setOnErrorListener(null)
        binding.videoView.stopPlayback()
        player = null
        prepared = false
    }
}
