package com.audiospy.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: AudioLogEntity)

    @Query("SELECT * FROM audio_log ORDER BY timestamp DESC LIMIT 200")
    fun observeAll(): Flow<List<AudioLogEntity>>

    @Query("""
        SELECT COUNT(*) FROM audio_log 
        WHERE packageName = :pkg AND state = :state 
        AND timestamp > :since
    """)
    suspend fun recentCount(pkg: String, state: String, since: Long): Int

    @Query("DELETE FROM audio_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
