package com.jinggong.timer.timer

import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.TimerPhase

/** 持久化的计时快照。阶段只可能是 RUNNING 或 PAUSED。 */
data class TimerSnapshot(
    val mode: PracticeMode,
    val phase: TimerPhase,
    val accumulatedMs: Long,
    val resumeElapsedMs: Long,
    val resumeWallMs: Long,
    val startWallMs: Long,
    val targetMinutes: Int,
    val bootCount: Int,
    val checkpointTotalMs: Long,
    val checkpointWallMs: Long,
)

/** UI 与 Service 共享的计时状态。 */
data class TimerState(
    val ready: Boolean = false,
    val phase: TimerPhase = TimerPhase.IDLE,
    val mode: PracticeMode? = null,
    val accumulatedMs: Long = 0L,
    val resumeElapsedMs: Long = 0L,
    val startWallMs: Long = 0L,
    val targetMinutes: Int = 0,
    val recoveredAfterReboot: Boolean = false,
) {
    val isActive: Boolean get() = phase != TimerPhase.IDLE
    val isRunning: Boolean get() = phase == TimerPhase.RUNNING
    val isPaused: Boolean get() = phase == TimerPhase.PAUSED

    /** 当前累计毫秒数，使用单调时钟，不受系统时间调整影响。 */
    fun elapsedMs(nowElapsed: Long): Long = when (phase) {
        TimerPhase.IDLE -> 0L
        TimerPhase.PAUSED -> accumulatedMs
        TimerPhase.RUNNING -> accumulatedMs + (nowElapsed - resumeElapsedMs).coerceAtLeast(0L)
    }
}

/** 结束练习、等待保存时使用的不可变结果。 */
data class TimerResult(
    val mode: PracticeMode,
    val startWallMs: Long,
    val endWallMs: Long,
    val durationSec: Int,
    val targetMinutes: Int,
)
