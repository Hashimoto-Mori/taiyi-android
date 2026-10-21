package com.jinggong.timer.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 本地日期区间，end 为开区间。 */
data class LocalDateRange(val start: LocalDate, val endExclusive: LocalDate) {
    fun startMillis(): Long = TimeFormatters.startOfDayMillis(start)
    fun endMillis(): Long = TimeFormatters.startOfDayMillis(endExclusive)
}

object DateRanges {

    fun today(): LocalDate = TimeFormatters.today()

    fun day(date: LocalDate): LocalDateRange =
        LocalDateRange(date, date.plusDays(1))

    fun week(date: LocalDate): LocalDateRange {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return LocalDateRange(start, start.plusWeeks(1))
    }

    fun month(date: LocalDate): LocalDateRange {
        val start = date.withDayOfMonth(1)
        return LocalDateRange(start, start.plusMonths(1))
    }

    fun lastDays(date: LocalDate, days: Int): LocalDateRange =
        LocalDateRange(date.minusDays((days - 1).coerceAtLeast(0).toLong()), date.plusDays(1))

    fun allTime(): LocalDateRange =
        LocalDateRange(LocalDate.of(1970, 1, 1), LocalDate.of(2100, 1, 1))
}
