package com.jinggong.timer.util

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 固定间隔发出单调时钟读数，用于首页等活动状态的小幅刷新。 */
fun intervalClockFlow(intervalMs: Long = 1_000L): Flow<Long> = flow {
    while (true) {
        emit(SystemClock.elapsedRealtime())
        delay(intervalMs)
    }
}
