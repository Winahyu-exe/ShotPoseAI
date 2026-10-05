package com.shotposeai.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "students", indices = {@Index(value = {"attendanceNumber"}, unique = true)})
public class StudentEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String attendanceNumber;
    public String baseLevel;
    public String profilePhotoPath;
    public long createdAt;

    public StudentEntity(String name, String attendanceNumber, String baseLevel,
                         String profilePhotoPath, long createdAt) {
        this.name = name; this.attendanceNumber = attendanceNumber;
        this.baseLevel = baseLevel; this.profilePhotoPath = profilePhotoPath;
        this.createdAt = createdAt;
    }
}
