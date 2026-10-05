package com.shotposeai.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"email"}, unique = true)})
public class UserEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String email;
    public String passwordHash;
    public String role;
    public String employeeNumber;
    public String profilePhotoPath;
    public long createdAt;

    public UserEntity(String name, String email, String passwordHash, String role,
                      String employeeNumber, String profilePhotoPath, long createdAt) {
        this.name = name; this.email = email; this.passwordHash = passwordHash;
        this.role = role; this.employeeNumber = employeeNumber;
        this.profilePhotoPath = profilePhotoPath; this.createdAt = createdAt;
    }
}
