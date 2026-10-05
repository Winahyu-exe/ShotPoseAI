package com.shotposeai.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "training_sessions")
public class TrainingSessionEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long studentId;
    public long classId;
    public String movementName;
    public long date;
    public String videoPath;
    public int score;
    public String level;
    public String status;

    public TrainingSessionEntity(long studentId, long classId, String movementName, long date,
                                 String videoPath, int score, String level, String status) {
        this.studentId = studentId; this.classId = classId; this.movementName = movementName;
        this.date = date; this.videoPath = videoPath; this.score = score;
        this.level = level; this.status = status;
    }
}
