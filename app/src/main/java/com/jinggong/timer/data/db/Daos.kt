package com.jinggong.timer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE mode = :mode ORDER BY startTime DESC")
    fun observeByMode(mode: String): Flow<List<SessionEntity>>

    @Query(
        "SELECT * FROM sessions WHERE startTime >= :from AND startTime < :to " +
            "ORDER BY startTime DESC",
    )
    fun observeRange(from: Long, to: Long): Flow<List<SessionEntity>>

    @Query(
        "SELECT date(startTime / 1000, 'unixepoch', 'localtime') AS day, " +
            "mode, SUM(durationSec) AS totalSec, COUNT(*) AS sessionCount " +
            "FROM sessions WHERE startTime >= :from AND startTime < :to " +
            "GROUP BY day, mode ORDER BY day ASC",
    )
    fun observeDailyTotals(from: Long, to: Long): Flow<List<DailyModeTotal>>

    @Query(
        "SELECT date(startTime / 1000, 'unixepoch', 'localtime') AS day, " +
            "mode, SUM(durationSec) AS totalSec, COUNT(*) AS sessionCount " +
            "FROM sessions GROUP BY day, mode ORDER BY day ASC",
    )
    fun observeAllDailyTotals(): Flow<List<DailyModeTotal>>

    @Query(
        "SELECT date(startTime / 1000, 'unixepoch', 'localtime') AS day " +
            "FROM sessions GROUP BY day ORDER BY day DESC",
    )
    fun observePracticeDays(): Flow<List<String>>

    @Query("SELECT MAX(startTime) FROM sessions WHERE mode = :mode")
    fun observeLatestStart(mode: String): Flow<Long?>

    @Query(
        "SELECT mode, SUM(durationSec) AS totalSec, COUNT(*) AS count " +
            "FROM sessions WHERE startTime >= :from AND startTime < :to GROUP BY mode",
    )
    fun observeModeTotals(from: Long, to: Long): Flow<List<ModeTotal>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM sessions ORDER BY startTime ASC")
    suspend fun getAllOnce(): List<SessionEntity>
}

@Dao
interface AudioDao {

    @Query("SELECT * FROM audio_files ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<AudioEntity>>

    @Query(
        "SELECT * FROM audio_files WHERE modeScope = 'ALL' OR modeScope = :mode " +
            "ORDER BY addedAt DESC",
    )
    fun observeForMode(mode: String): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AudioEntity?

    @Query(
        "SELECT * FROM audio_files WHERE modeScope = 'ALL' OR modeScope = :mode " +
            "ORDER BY addedAt DESC",
    )
    suspend fun getForModeOnce(mode: String): List<AudioEntity>

    @Query("SELECT * FROM audio_files")
    suspend fun getAllOnce(): List<AudioEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(audio: AudioEntity): Long

    @Query("UPDATE audio_files SET displayName = :displayName WHERE id = :id")
    suspend fun rename(id: Long, displayName: String)

    @Delete
    suspend fun delete(audio: AudioEntity)

    @Query("DELETE FROM audio_files")
    suspend fun deleteAll()
}
