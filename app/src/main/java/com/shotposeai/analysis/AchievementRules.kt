package com.shotposeai.analysis

object AchievementRules {
    const val REQUIRED_SCORE = 85
    fun qualifies(score: Int) = score >= REQUIRED_SCORE
    fun percentage(completed: Int, total: Int): Int =
        if (total <= 0) 0 else (completed.coerceIn(0, total) * 100 / total)
}
