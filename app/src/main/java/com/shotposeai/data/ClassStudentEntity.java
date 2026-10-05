package com.shotposeai.data;

import androidx.room.Entity;

@Entity(tableName = "class_students", primaryKeys = {"classId", "studentId"})
public class ClassStudentEntity {
    public long classId;
    public long studentId;
    public ClassStudentEntity(long classId, long studentId) { this.classId = classId; this.studentId = studentId; }
}
