package com.shotposeai.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "classrooms")
public class ClassRoomEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String category;
    public String iconType;
    public long coachId;
    public long createdAt;

    public ClassRoomEntity(String name, String category, String iconType, long coachId, long createdAt) {
        this.name = name; this.category = category; this.iconType = iconType;
        this.coachId = coachId; this.createdAt = createdAt;
    }
}
