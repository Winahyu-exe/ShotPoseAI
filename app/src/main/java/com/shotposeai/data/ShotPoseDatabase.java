package com.shotposeai.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {UserEntity.class, StudentEntity.class, ClassRoomEntity.class,
        ClassStudentEntity.class, TrainingSessionEntity.class, AnalysisResultEntity.class,
        AchievementEntity.class, StudentAchievementEntity.class}, version = 1, exportSchema = false)
public abstract class ShotPoseDatabase extends RoomDatabase {
    public abstract AppDao appDao();
}
