package com.jinggong.timer.timer

import com.jinggong.timer.model.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTest {

    @Test
    fun runningElapsedUsesMonotonicDelta() {
        val state = TimerState(
            phase = TimerPhase.RUNNING,
            accumulatedMs = 5_000L,
            resumeElapsedMs = 1_000L,
        )
        assertEquals(12_000L, state.elapsedMs(8_000L))
    }

    @Test
    fun pausedElapsedDoesNotAdvance() {
        val state = TimerState(
            phase = TimerPhase.PAUSED,
            accumulatedMs = 42_000L,
            resumeElapsedMs = 1_000L,
        )
        assertEquals(42_000L, state.elapsedMs(999_999L))
    }

    @Test
    fun idleElapsedIsZero() {
        assertEquals(0L, TimerState().elapsedMs(123_456L))
    }

    @Test
    fun clockGoingBackwardsClampsToZero() {
        val state = TimerState(
            phase = TimerPhase.RUNNING,
            accumulatedMs = 1_000L,
            resumeElapsedMs = 10_000L,
        )
        assertEquals(1_000L, state.elapsedMs(9_000L))
    }
}
