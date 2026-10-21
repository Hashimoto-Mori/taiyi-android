package com.jinggong.timer.timer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jinggong.timer.data.prefs.jingGongDataStore
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.TimerPhase
import kotlinx.coroutines.flow.first

/** 把计时快照写入 DataStore，进程被杀后可恢复。 */
class TimerStore(private val context: Context) {

    suspend fun load(): TimerSnapshot? {
        val prefs = context.jingGongDataStore.data.first()
        val mode = PracticeMode.fromDb(prefs[Keys.MODE]) ?: return null
        val phase = when (prefs[Keys.PHASE]) {
            TimerPhase.RUNNING.name -> TimerPhase.RUNNING
            TimerPhase.PAUSED.name -> TimerPhase.PAUSED
            else -> return null
        }
        return TimerSnapshot(
            mode = mode,
            phase = phase,
            accumulatedMs = prefs[Keys.ACCUMULATED] ?: 0L,
            resumeElapsedMs = prefs[Keys.RESUME_ELAPSED] ?: 0L,
            resumeWallMs = prefs[Keys.RESUME_WALL] ?: 0L,
            startWallMs = prefs[Keys.START_WALL] ?: 0L,
            targetMinutes = prefs[Keys.TARGET_MINUTES] ?: 0,
            bootCount = prefs[Keys.BOOT_COUNT] ?: -1,
            checkpointTotalMs = prefs[Keys.CHECKPOINT_TOTAL] ?: 0L,
            checkpointWallMs = prefs[Keys.CHECKPOINT_WALL] ?: 0L,
        )
    }

    suspend fun save(snapshot: TimerSnapshot) {
        context.jingGongDataStore.edit { prefs ->
            prefs[Keys.MODE] = snapshot.mode.dbValue
            prefs[Keys.PHASE] = snapshot.phase.name
            prefs[Keys.ACCUMULATED] = snapshot.accumulatedMs
            prefs[Keys.RESUME_ELAPSED] = snapshot.resumeElapsedMs
            prefs[Keys.RESUME_WALL] = snapshot.resumeWallMs
            prefs[Keys.START_WALL] = snapshot.startWallMs
            prefs[Keys.TARGET_MINUTES] = snapshot.targetMinutes
            prefs[Keys.BOOT_COUNT] = snapshot.bootCount
            prefs[Keys.CHECKPOINT_TOTAL] = snapshot.checkpointTotalMs
            prefs[Keys.CHECKPOINT_WALL] = snapshot.checkpointWallMs
        }
    }

    suspend fun clear() {
        context.jingGongDataStore.edit { prefs ->
            prefs.remove(Keys.MODE)
            prefs.remove(Keys.PHASE)
            prefs.remove(Keys.ACCUMULATED)
            prefs.remove(Keys.RESUME_ELAPSED)
            prefs.remove(Keys.RESUME_WALL)
            prefs.remove(Keys.START_WALL)
            prefs.remove(Keys.TARGET_MINUTES)
            prefs.remove(Keys.BOOT_COUNT)
            prefs.remove(Keys.CHECKPOINT_TOTAL)
            prefs.remove(Keys.CHECKPOINT_WALL)
        }
    }

    private object Keys {
        val MODE = stringPreferencesKey("timer_mode")
        val PHASE = stringPreferencesKey("timer_phase")
        val ACCUMULATED = longPreferencesKey("timer_accumulated_ms")
        val RESUME_ELAPSED = longPreferencesKey("timer_resume_elapsed_ms")
        val RESUME_WALL = longPreferencesKey("timer_resume_wall_ms")
        val START_WALL = longPreferencesKey("timer_start_wall_ms")
        val TARGET_MINUTES = intPreferencesKey("timer_target_minutes")
        val BOOT_COUNT = intPreferencesKey("timer_boot_count")
        val CHECKPOINT_TOTAL = longPreferencesKey("timer_checkpoint_total_ms")
        val CHECKPOINT_WALL = longPreferencesKey("timer_checkpoint_wall_ms")
    }
}
