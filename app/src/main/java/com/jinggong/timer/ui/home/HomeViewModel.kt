package com.jinggong.timer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinggong.timer.data.db.SessionEntity
import com.jinggong.timer.data.prefs.SettingsDataStore
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.timer.TimerManager
import com.jinggong.timer.util.StreakCalculator
import com.jinggong.timer.util.TimeFormatters
import com.jinggong.timer.util.intervalClockFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ModeTodaySummary(
    val totalSec: Long = 0L,
    val count: Int = 0,
    val latestStart: Long? = null,
)

data class HomeUiState(
    val ready: Boolean = false,
    val quiet: ModeTodaySummary = ModeTodaySummary(),
    val standing: ModeTodaySummary = ModeTodaySummary(),
    val todayTotalSec: Long = 0L,
    val todayCount: Int = 0,
    val streak: Int = 0,
    val goalMinutes: Int = 30,
    val activeMode: PracticeMode? = null,
    val activeElapsedMs: Long = 0L,
    val activeRunning: Boolean = false,
)

class HomeViewModel(
    sessionRepository: SessionRepository,
    settingsDataStore: SettingsDataStore,
    timerManager: TimerManager,
) : ViewModel() {

    val uiState = combine(
        sessionRepository.observeAll(),
        settingsDataStore.settings,
        timerManager.state,
        intervalClockFlow(1_000L),
    ) { sessions, settings, timer, nowElapsed ->
        val today = TimeFormatters.today()
        val todaySessions = sessions.filter {
            TimeFormatters.dateOf(it.startTime) == today
        }
        val quiet = summaryFor(sessions, PracticeMode.QUIET)
        val standing = summaryFor(sessions, PracticeMode.STANDING)
        val practiceDays = sessions.mapTo(mutableSetOf()) {
            TimeFormatters.dateOf(it.startTime)
        }
        HomeUiState(
            ready = timer.ready,
            quiet = quiet,
            standing = standing,
            todayTotalSec = todaySessions.sumOf { it.durationSec.toLong() },
            todayCount = todaySessions.size,
            streak = StreakCalculator.currentStreak(practiceDays, today),
            goalMinutes = settings.dailyGoalMinutes,
            activeMode = timer.mode.takeIf { timer.isActive },
            activeElapsedMs = if (timer.isActive) timer.elapsedMs(nowElapsed) else 0L,
            activeRunning = timer.isRunning,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState(),
    )

    private fun summaryFor(
        sessions: List<SessionEntity>,
        mode: PracticeMode,
    ): ModeTodaySummary {
        val today = TimeFormatters.today()
        val filtered = sessions.filter {
            it.mode == mode.dbValue && TimeFormatters.dateOf(it.startTime) == today
        }
        val latestOverall = sessions
            .asSequence()
            .filter { it.mode == mode.dbValue }
            .maxByOrNull { it.startTime }
            ?.startTime
        return ModeTodaySummary(
            totalSec = filtered.sumOf { it.durationSec.toLong() },
            count = filtered.size,
            latestStart = latestOverall,
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    sessionRepository = container.sessionRepository,
                    settingsDataStore = container.settingsDataStore,
                    timerManager = container.timerManager,
                )
            }
        }
    }
}
