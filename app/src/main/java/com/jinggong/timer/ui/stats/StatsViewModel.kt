package com.jinggong.timer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinggong.timer.data.db.DailyModeTotal
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.util.DateRanges
import com.jinggong.timer.util.LocalDateRange
import com.jinggong.timer.util.StreakCalculator
import com.jinggong.timer.util.TimeFormatters
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PeriodTotals(
    val quietSec: Long = 0L,
    val standingSec: Long = 0L,
) {
    val totalSec: Long get() = quietSec + standingSec
}

data class DayBar(
    val date: LocalDate,
    val quietSec: Long,
    val standingSec: Long,
) {
    val totalSec: Long get() = quietSec + standingSec
}

data class StatsUiState(
    val today: PeriodTotals = PeriodTotals(),
    val week: PeriodTotals = PeriodTotals(),
    val month: PeriodTotals = PeriodTotals(),
    val all: PeriodTotals = PeriodTotals(),
    val last30Days: List<DayBar> = emptyList(),
    val heatmapMonth: YearMonth = YearMonth.now(),
    val heatmap: Map<LocalDate, Long> = emptyMap(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDays: Int = 0,
)

private data class DayValues(val quietSec: Long = 0L, val standingSec: Long = 0L) {
    fun total(): Long = quietSec + standingSec
}

class StatsViewModel(
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState = combine(
        sessionRepository.observeAllDailyTotals(),
        sessionRepository.observePracticeDays(),
        selectedMonth,
    ) { dailyTotals, practiceDays, month ->
        val values = dailyTotals.toDayValues()
        val today = TimeFormatters.today()
        val days = practiceDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
        val last30Start = today.minusDays(29)
        StatsUiState(
            today = values.periodTotals(DateRanges.day(today)),
            week = values.periodTotals(DateRanges.week(today)),
            month = values.periodTotals(DateRanges.month(today)),
            all = values.periodTotals(DateRanges.allTime()),
            last30Days = (0L..29L).map { offset ->
                val date = last30Start.plusDays(offset)
                val day = values[date] ?: DayValues()
                DayBar(date, day.quietSec, day.standingSec)
            },
            heatmapMonth = month,
            heatmap = buildMap {
                var day = month.atDay(1)
                while (day.month == month.month) {
                    put(day, values[day]?.total() ?: 0L)
                    day = day.plusDays(1)
                }
            },
            currentStreak = StreakCalculator.currentStreak(days, today),
            longestStreak = StreakCalculator.longestStreak(days),
            totalDays = StreakCalculator.totalDays(days),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StatsUiState(),
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = selectedMonth.value.plusMonths(1)
        if (next <= YearMonth.now()) selectedMonth.value = next
    }

    private fun List<DailyModeTotal>.toDayValues(): Map<LocalDate, DayValues> {
        val result = mutableMapOf<LocalDate, DayValues>()
        forEach { row ->
            val day = runCatching { LocalDate.parse(row.day) }.getOrNull() ?: return@forEach
            val current = result[day] ?: DayValues()
            result[day] = when (PracticeMode.fromDb(row.mode)) {
                PracticeMode.QUIET -> current.copy(quietSec = current.quietSec + row.totalSec)
                PracticeMode.STANDING -> current.copy(
                    standingSec = current.standingSec + row.totalSec,
                )
                null -> current
            }
        }
        return result
    }

    private fun Map<LocalDate, DayValues>.periodTotals(range: LocalDateRange): PeriodTotals {
        var quiet = 0L
        var standing = 0L
        forEach { (day, values) ->
            if (day >= range.start && day < range.endExclusive) {
                quiet += values.quietSec
                standing += values.standingSec
            }
        }
        return PeriodTotals(quietSec = quiet, standingSec = standing)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StatsViewModel(container.sessionRepository)
            }
        }
    }
}
