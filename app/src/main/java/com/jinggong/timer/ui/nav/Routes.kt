package com.jinggong.timer.ui.nav

import com.jinggong.timer.model.PracticeMode

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val AUDIO_MANAGE = "audio"
    const val ABOUT = "about"

    const val PRACTICE_PATTERN = "practice/{mode}"

    fun practice(mode: PracticeMode): String = "practice/${mode.dbValue}"

    val topLevel = setOf(HOME, HISTORY, STATS, SETTINGS)
}
