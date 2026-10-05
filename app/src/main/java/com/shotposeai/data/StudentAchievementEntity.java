package com.shotposeai.data;

import androidx.room.Entity;

@Entity(tableName = "student_achievements", primaryKeys = {"studentId", "achievementId"})
public class StudentAchievementEntity {
    public long studentId;
    public long achievementId;
    public long unlockedAt;
    public StudentAchievementEntity(long studentId, long achievementId, long unlockedAt) {
        this.studentId = studentId; this.achievementId = achievementId; this.unlockedAt = unlockedAt;
    }
}
