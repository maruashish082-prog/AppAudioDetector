package com.audiospy.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_log")
data class AudioLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val state: String,
    val timestamp: Long,
    val isAlert: Boolean = false
)
