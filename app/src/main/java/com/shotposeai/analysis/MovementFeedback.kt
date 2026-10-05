package com.shotposeai.analysis

data class MovementFeedback(
    val movement: String,
    val score: Int,
    val feedback: String,
    val detected: Boolean
)
