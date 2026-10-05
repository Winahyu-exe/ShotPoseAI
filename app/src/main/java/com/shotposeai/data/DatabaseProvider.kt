package com.shotposeai.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: ShotPoseDatabase? = null
    fun get(context: Context): ShotPoseDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            ShotPoseDatabase::class.java,
            "shotpose_ai.db"
        ).build().also { instance = it }
    }
}
