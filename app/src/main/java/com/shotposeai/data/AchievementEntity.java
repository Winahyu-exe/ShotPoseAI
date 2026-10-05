package com.shotposeai.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "achievements")
public class AchievementEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String description;
    public String icon;
    public String requirementType;
    public int requirementValue;
    public AchievementEntity(String name, String description, String icon, String requirementType, int requirementValue) {
        this.name = name; this.description = description; this.icon = icon;
        this.requirementType = requirementType; this.requirementValue = requirementValue;
    }
}
