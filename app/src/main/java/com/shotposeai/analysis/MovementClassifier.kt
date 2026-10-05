package com.shotposeai.analysis

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MovementClassifier {
    private const val NOSE = 0
    private const val LEFT_ELBOW = 13
    private const val RIGHT_ELBOW = 14
    private const val LEFT_WRIST = 15
    private const val RIGHT_WRIST = 16
    private const val LEFT_SHOULDER = 11
    private const val RIGHT_SHOULDER = 12
    private const val LEFT_HIP = 23
    private const val RIGHT_HIP = 24
    private const val LEFT_KNEE = 25
    private const val RIGHT_KNEE = 26
    private const val LEFT_ANKLE = 27
    private const val RIGHT_ANKLE = 28

    fun classify(result: PoseLandmarkerResult): MovementFeedback {
        if (result.landmarks().size > 1) {
            return MovementFeedback(
                movement = "Lebih dari satu orang terdeteksi",
                score = 0,
                feedback = "Pastikan hanya satu siswa berada di dalam frame kamera.",
                detected = false
            )
        }
        val landmarks = result.landmarks().firstOrNull()
            ?: return MovementFeedback(
                movement = "Pose belum terbaca",
                score = 0,
                feedback = "Pastikan seluruh tubuh pemain terlihat di kamera.",
                detected = false
            )

        if (landmarks.size <= RIGHT_ANKLE) {
            return MovementFeedback(
                movement = "Landmark tidak lengkap",
                score = 0,
                feedback = "Jauhkan kamera sedikit agar kepala sampai kaki masuk frame.",
                detected = false
            )
        }

        val leftKneeAngle = angle(
            landmarks[LEFT_HIP],
            landmarks[LEFT_KNEE],
            landmarks[LEFT_ANKLE]
        )

        val rightKneeAngle = angle(
            landmarks[RIGHT_HIP],
            landmarks[RIGHT_KNEE],
            landmarks[RIGHT_ANKLE]
        )

        val leftLegReach = distance(landmarks[LEFT_HIP], landmarks[LEFT_ANKLE])
        val rightLegReach = distance(landmarks[RIGHT_HIP], landmarks[RIGHT_ANKLE])

        val maxLegReach = max(leftLegReach, rightLegReach)
        val activeSide = if (rightLegReach > leftLegReach) "kanan" else "kiri"
        val activeAngle = max(leftKneeAngle, rightKneeAngle)

        val shoulderTilt = abs(landmarks[LEFT_SHOULDER].y() - landmarks[RIGHT_SHOULDER].y())
        val hipTilt = abs(landmarks[LEFT_HIP].y() - landmarks[RIGHT_HIP].y())

        val reachScore = ((maxLegReach * 140).toInt()).coerceIn(0, 45)
        val angleScore = (((activeAngle - 80.0) / 90.0) * 40).toInt().coerceIn(0, 40)

        val balancePenalty = (shoulderTilt + hipTilt).toDouble()
        val balanceScore = ((1.0 - min(balancePenalty, 0.35) / 0.35) * 15)
            .toInt()
            .coerceIn(0, 15)

        val score = (reachScore + angleScore + balanceScore).coerceIn(0, 100)

        if (score < 25) {
            return MovementFeedback(
                movement = "Pose belum ideal",
                score = score,
                feedback = "Tubuh belum terbaca dengan baik. Pastikan seluruh badan terlihat dan kamera tidak terlalu dekat.",
                detected = false
            )
        }

        if (score in 25..40 && activeAngle < 105) {
            return MovementFeedback(
                movement = "Pose mulai terbaca",
                score = score,
                feedback = "Pose dasar sudah terbaca. Mulai lakukan gerakan shooting atau passing agar sistem bisa menilai ayunan kaki.",
                detected = true
            )
        }

        return when {
            activeAngle >= 145 && maxLegReach >= 0.33f -> {
                MovementFeedback(
                    movement = "Tendangan kaki $activeSide",
                    score = score,
                    feedback = if (balanceScore < 8) {
                        "Tendangan terbaca, tetapi bahu atau pinggul terlalu miring. Jaga badan tetap stabil saat follow-through."
                    } else {
                        "Tendangan terbaca baik. Pertahankan kaki tumpu stabil dan lanjutkan follow-through setelah kontak bola."
                    },
                    detected = true
                )
            }

            activeAngle in 105.0..144.9 -> {
                MovementFeedback(
                    movement = "Persiapan tendangan",
                    score = score,
                    feedback = "Gerakan mulai terbaca. Luruskan ayunan kaki dan arahkan lutut ke target.",
                    detected = true
                )
            }

            shoulderTilt > 0.12f || hipTilt > 0.12f -> {
                MovementFeedback(
                    movement = "Postur kurang seimbang",
                    score = score,
                    feedback = "Badan terlalu miring. Rapikan posisi bahu, pinggul, dan kaki tumpu sebelum menendang.",
                    detected = true
                )
            }

            score in 41..60 -> {
                MovementFeedback(
                    movement = "Gerakan mulai terbaca",
                    score = score,
                    feedback = "Pose sudah terbaca, tetapi ayunan kaki belum kuat. Ambil jarak kamera yang cukup dan lakukan gerakan lebih jelas.",
                    detected = true
                )
            }

            score in 61..80 -> {
                MovementFeedback(
                    movement = "Pose cukup baik",
                    score = score,
                    feedback = "Pose terlihat cukup baik. Tingkatkan stabilitas kaki tumpu dan arahkan badan ke target.",
                    detected = true
                )
            }

            else -> {
                MovementFeedback(
                    movement = "Pose sangat baik",
                    score = score,
                    feedback = "Gerakan terlihat stabil. Pertahankan keseimbangan badan, kaki tumpu, dan follow-through.",
                    detected = true
                )
            }
        }
    }

    /** Movement-specific rule layer kept separate from pose detection. */
    fun classify(result: PoseLandmarkerResult, requestedMovement: String): MovementFeedback {
        val base = classify(result)
        if (!base.detected || requestedMovement.isBlank()) return base
        val l = result.landmarks().firstOrNull() ?: return base
        if (l.size <= RIGHT_ANKLE) return base
        val kneeBend = (180.0 - min(
            angle(l[LEFT_HIP], l[LEFT_KNEE], l[LEFT_ANKLE]),
            angle(l[RIGHT_HIP], l[RIGHT_KNEE], l[RIGHT_ANKLE])
        )).coerceIn(0.0, 90.0)
        val handsAboveShoulders = l[LEFT_WRIST].y() < l[LEFT_SHOULDER].y() && l[RIGHT_WRIST].y() < l[RIGHT_SHOULDER].y()
        val centeredHead = abs(l[NOSE].x() - ((l[LEFT_SHOULDER].x() + l[RIGHT_SHOULDER].x()) / 2f)) < 0.12f
        val adjusted = when {
            requestedMovement.contains("throw", true) -> base.score + if (handsAboveShoulders) 18 else -12
            requestedMovement.contains("drib", true) || requestedMovement.contains("kontrol", true) -> base.score + (kneeBend / 7).toInt()
            requestedMovement.contains("heading", true) -> base.score + if (centeredHead) 10 else -8
            requestedMovement.contains("passing", true) -> base.score + 6
            requestedMovement.contains("shoot", true) || requestedMovement.contains("kick", true) || requestedMovement.contains("penal", true) -> base.score + 8
            else -> base.score
        }.coerceIn(0, 100)
        val tip = when {
            requestedMovement.contains("throw", true) && !handsAboveShoulders -> "Bawa kedua tangan melewati atas kepala dan jaga kedua kaki menyentuh tanah."
            requestedMovement.contains("drib", true) -> "Jaga lutut sedikit menekuk, langkah pendek, dan pusat berat badan tetap stabil."
            requestedMovement.contains("heading", true) -> "Arahkan dahi ke bola dan jaga kepala tetap sejajar dengan bahu."
            requestedMovement.contains("passing", true) -> "Kunci pergelangan kaki, arahkan badan ke target, lalu selesaikan follow-through."
            else -> "Jaga kaki tumpu, arah badan, dan follow-through tetap stabil."
        }
        return base.copy(movement = requestedMovement, score = adjusted, feedback = tip)
    }

    private fun angle(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Double {
        val abX = a.x() - b.x()
        val abY = a.y() - b.y()
        val cbX = c.x() - b.x()
        val cbY = c.y() - b.y()

        val dot = abX * cbX + abY * cbY
        val magAB = sqrt((abX * abX + abY * abY).toDouble()).toFloat()
        val magCB = sqrt((cbX * cbX + cbY * cbY).toDouble()).toFloat()

        if (magAB == 0f || magCB == 0f) return 0.0

        val cos = (dot / (magAB * magCB)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos.toDouble()))
    }

    private fun distance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
