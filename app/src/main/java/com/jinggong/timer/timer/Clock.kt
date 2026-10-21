package com.jinggong.timer.timer

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/** 可注入的单调时钟，便于单元测试。 */
interface Clock {
    fun elapsedRealtime(): Long
    fun currentTimeMillis(): Long
}

class SystemClockSource : Clock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/** 当前系统启动次数，用于识别设备重启。 */
interface BootCountProvider {
    fun currentBootCount(): Int
}

class AndroidBootCountProvider(context: Context) : BootCountProvider {
    private val resolver = context.applicationContext.contentResolver

    override fun currentBootCount(): Int = runCatching {
        Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, -1)
    }.getOrDefault(-1)
}
