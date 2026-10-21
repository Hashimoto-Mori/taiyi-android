package com.jinggong.timer.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** 前台 Service 的动作与启动入口。 */
object ServiceActions {
    const val ACTION_SYNC = "com.jinggong.timer.action.SYNC"
    const val ACTION_PAUSE = "com.jinggong.timer.action.PAUSE"
    const val ACTION_RESUME = "com.jinggong.timer.action.RESUME"
    const val ACTION_TOGGLE_AUDIO = "com.jinggong.timer.action.TOGGLE_AUDIO"

    const val EXTRA_OPEN_PRACTICE = "com.jinggong.timer.extra.OPEN_PRACTICE"
    const val EXTRA_FINISH_SESSION = "com.jinggong.timer.extra.FINISH_SESSION"

    fun start(context: Context) {
        val intent = Intent(context, PracticeService::class.java).setAction(ACTION_SYNC)
        ContextCompat.startForegroundService(context, intent)
    }

    fun command(context: Context, action: String) {
        val intent = Intent(context, PracticeService::class.java).setAction(action)
        context.startService(intent)
    }

    fun intent(context: Context, action: String): Intent =
        Intent(context, PracticeService::class.java).setAction(action)
}
