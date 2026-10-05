package com.shotposeai.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analysis_results")
public class AnalysisResultEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long trainingSessionId;
    public String correctPoints;
    public String improvementPoints;
    public String coachNote;
    public String rawAnalysisData;

    public AnalysisResultEntity(long trainingSessionId, String correctPoints, String improvementPoints,
                                String coachNote, String rawAnalysisData) {
        this.trainingSessionId = trainingSessionId; this.correctPoints = correctPoints;
        this.improvementPoints = improvementPoints; this.coachNote = coachNote;
        this.rawAnalysisData = rawAnalysisData;
    }
}
