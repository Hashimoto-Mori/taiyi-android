package com.jinggong.timer.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun currentStreakIncludesToday() {
        val days = setOf(
            today,
            today.minusDays(1),
            today.minusDays(2),
            today.minusDays(4),
        )
        assertEquals(3, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreakFallsBackToYesterday() {
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun currentStreakIsZeroWhenNoRecentPractice() {
        val days = setOf(today.minusDays(3))
        assertEquals(0, StreakCalculator.currentStreak(days, today))
    }

    @Test
    fun longestStreakFindsLargestRun() {
        val days = setOf(
            today.minusDays(10),
            today.minusDays(9),
            today.minusDays(8),
            today.minusDays(4),
            today.minusDays(3),
        )
        assertEquals(3, StreakCalculator.longestStreak(days))
    }

    @Test
    fun emptyStreakIsZero() {
        assertEquals(0, StreakCalculator.longestStreak(emptySet()))
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }
}
