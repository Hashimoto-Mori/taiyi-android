package com.jinggong.timer.data.prefs

/** 用户偏好设置。时长单位均为分钟。 */
data class Settings(
    val dailyGoalMinutes: Int = 30,
    val autoPlayAudio: Boolean = true,
    val defaultAudioId: Long? = null,
    val loopAudio: Boolean = true,
    val quietTargetMinutes: Int = 0,
    val standingTargetMinutes: Int = 0,
    val themeMode: com.jinggong.timer.model.ThemeMode =
        com.jinggong.timer.model.ThemeMode.SYSTEM,
) {
    fun targetMinutesFor(mode: com.jinggong.timer.model.PracticeMode): Int = when (mode) {
        com.jinggong.timer.model.PracticeMode.QUIET -> quietTargetMinutes
        com.jinggong.timer.model.PracticeMode.STANDING -> standingTargetMinutes
    }
}
