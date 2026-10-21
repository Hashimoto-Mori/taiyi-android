package com.jinggong.timer.model

import androidx.annotation.StringRes
import com.jinggong.timer.R

/** 练习模式。数据库中使用稳定的英文枚举值。 */
enum class PracticeMode(val dbValue: String) {
    QUIET("QUIET"),
    STANDING("STANDING"),
    ;

    @StringRes
    fun labelRes(): Int = when (this) {
        QUIET -> R.string.mode_quiet
        STANDING -> R.string.mode_standing
    }

    companion object {
        fun fromDb(value: String?): PracticeMode? =
            entries.firstOrNull { it.dbValue == value }

        fun fromRoute(value: String?): PracticeMode? = fromDb(value)
    }
}

/** 音频适用的练习模式。 */
enum class AudioScope(val dbValue: String) {
    QUIET("QUIET"),
    STANDING("STANDING"),
    ALL("ALL"),
    ;

    @StringRes
    fun labelRes(): Int = when (this) {
        QUIET -> R.string.audio_scope_quiet
        STANDING -> R.string.audio_scope_standing
        ALL -> R.string.audio_scope_all
    }

    fun matches(mode: PracticeMode): Boolean = when (this) {
        QUIET -> mode == PracticeMode.QUIET
        STANDING -> mode == PracticeMode.STANDING
        ALL -> true
    }

    companion object {
        fun fromDb(value: String?): AudioScope =
            entries.firstOrNull { it.dbValue == value } ?: ALL
    }
}

/** 计时器当前阶段。 */
enum class TimerPhase {
    IDLE,
    RUNNING,
    PAUSED,
}

/** 应用皮肤模式。 */
enum class ThemeMode(val dbValue: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK"),
    ;

    @StringRes
    fun labelRes(): Int = when (this) {
        SYSTEM -> R.string.theme_system
        LIGHT -> R.string.theme_light
        DARK -> R.string.theme_dark
    }

    companion object {
        fun fromDb(value: String?): ThemeMode =
            entries.firstOrNull { it.dbValue == value } ?: SYSTEM
    }
}
