package com.shotposeai.data

import com.shotposeai.analysis.AchievementRules
import com.shotposeai.analysis.MovementCatalog

/** Call inside a Room transaction on the IO dispatcher. */
object AchievementRepository {
    fun unlockForScore(dao: AppDao, studentId: Long, movementName: String, score: Int): Long? {
        if (!AchievementRules.qualifies(score)) return null
        val movement = MovementCatalog.all.firstOrNull { it.name == movementName } ?: return null
        val badge = dao.getAchievementByName(movement.badgeName)
        val badgeId = badge?.id ?: dao.insertAchievement(AchievementEntity(
            movement.badgeName, "Capai skor minimal 8,5/10 pada ${movement.name}.",
            "trophy", "SCORE", AchievementRules.REQUIRED_SCORE
        ))
        return if (dao.unlock(StudentAchievementEntity(studentId, badgeId, System.currentTimeMillis())) != -1L) badgeId else null
    }
}
