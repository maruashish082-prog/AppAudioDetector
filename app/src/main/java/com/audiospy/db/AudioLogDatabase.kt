package com.audiospy.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AudioLogEntity::class], version = 1, exportSchema = false)
abstract class AudioLogDatabase : RoomDatabase() {

    abstract fun dao(): AudioLogDao

    companion object {
        @Volatile private var INSTANCE: AudioLogDatabase? = null

        fun get(context: Context): AudioLogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AudioLogDatabase::class.java,
                    "audio_log.db"
                ).build().also { INSTANCE = it }
            }
    }
}
