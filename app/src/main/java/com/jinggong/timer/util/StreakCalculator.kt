package com.jinggong.timer.util

import java.time.LocalDate

/** 连续练习天数计算。传入的日期集合不要求有序。 */
object StreakCalculator {

    /**
     * 当前连续天数：今天练过则从今天倒数；今天没练但昨天练过，则从昨天倒数。
     * 这样凌晨尚未练习时不会立即显示断签。
     */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in days) today else today.minusDays(1)
        if (cursor !in days) return 0
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** 历史最长连续天数。 */
    fun longestStreak(days: Set<LocalDate>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var longest = 1
        var current = 1
        for (index in 1 until sorted.size) {
            if (sorted[index] == sorted[index - 1].plusDays(1)) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }

    fun totalDays(days: Set<LocalDate>): Int = days.size
}
