package com.jinggong.timer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 设置与计时快照共用同一个 Preferences DataStore。 */
internal val Context.jingGongDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jinggong_prefs",
)

class SettingsDataStore(private val context: Context) {

    val settings: Flow<Settings> = context.jingGongDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            Settings(
                dailyGoalMinutes = prefs[Keys.DAILY_GOAL] ?: 30,
                autoPlayAudio = prefs[Keys.AUTO_PLAY] ?: true,
                defaultAudioId = prefs[Keys.DEFAULT_AUDIO_ID]?.takeIf { it > 0L },
                loopAudio = prefs[Keys.LOOP_AUDIO] ?: true,
                quietTargetMinutes = prefs[Keys.QUIET_TARGET] ?: 0,
                standingTargetMinutes = prefs[Keys.STANDING_TARGET] ?: 0,
                themeMode = ThemeMode.fromDb(prefs[Keys.THEME_MODE]),
            )
        }

    suspend fun current(): Settings = settings.first()

    suspend fun setDailyGoal(minutes: Int) {
        context.jingGongDataStore.edit { it[Keys.DAILY_GOAL] = minutes.coerceIn(5, 240) }
    }

    suspend fun setAutoPlay(enabled: Boolean) {
        context.jingGongDataStore.edit { it[Keys.AUTO_PLAY] = enabled }
    }

    suspend fun setDefaultAudio(id: Long?) {
        context.jingGongDataStore.edit { prefs ->
            if (id == null || id <= 0L) prefs.remove(Keys.DEFAULT_AUDIO_ID)
            else prefs[Keys.DEFAULT_AUDIO_ID] = id
        }
    }

    suspend fun setLoopAudio(enabled: Boolean) {
        context.jingGongDataStore.edit { it[Keys.LOOP_AUDIO] = enabled }
    }

    suspend fun setTargetMinutes(mode: PracticeMode, minutes: Int) {
        val safe = minutes.coerceIn(0, 240)
        context.jingGongDataStore.edit { prefs ->
            when (mode) {
                PracticeMode.QUIET -> prefs[Keys.QUIET_TARGET] = safe
                PracticeMode.STANDING -> prefs[Keys.STANDING_TARGET] = safe
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.jingGongDataStore.edit { it[Keys.THEME_MODE] = mode.dbValue }
    }

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
        // 新键让已有安装也默认开启自动播放；用户仍可在设置中关闭。
        val AUTO_PLAY = booleanPreferencesKey("auto_play_audio_v2")
        val DEFAULT_AUDIO_ID = longPreferencesKey("default_audio_id")
        val LOOP_AUDIO = booleanPreferencesKey("loop_audio")
        val QUIET_TARGET = intPreferencesKey("quiet_target_minutes")
        val STANDING_TARGET = intPreferencesKey("standing_target_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
