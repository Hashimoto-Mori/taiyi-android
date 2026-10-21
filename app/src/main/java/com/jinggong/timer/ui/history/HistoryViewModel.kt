package com.jinggong.timer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinggong.timer.data.db.SessionEntity
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.util.TimeFormatters
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryDayGroup(
    val day: LocalDate,
    val sessions: List<SessionEntity>,
)

data class HistoryUiState(
    val mode: PracticeMode? = null,
    val groups: List<HistoryDayGroup> = emptyList(),
)

sealed interface HistoryEvent {
    data object Updated : HistoryEvent
    data object Deleted : HistoryEvent
    data object SaveFailed : HistoryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<PracticeMode?>(null)
    private val _events = MutableSharedFlow<HistoryEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val sessions = filter.flatMapLatest { mode ->
        if (mode == null) sessionRepository.observeAll()
        else sessionRepository.observeByMode(mode.dbValue)
    }

    val uiState = combine(filter, sessions) { mode, list ->
        val groups = list
            .groupBy { TimeFormatters.dateOf(it.startTime) }
            .toSortedMap(compareByDescending { it })
            .map { (day, items) ->
                HistoryDayGroup(
                    day = day,
                    sessions = items.sortedByDescending { it.startTime },
                )
            }
        HistoryUiState(mode = mode, groups = groups)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HistoryUiState(),
    )

    fun setFilter(mode: PracticeMode?) {
        filter.value = mode
    }

    fun updateSession(session: SessionEntity, minutes: Double, note: String) {
        viewModelScope.launch {
            val durationSec = (minutes * 60.0).roundToInt().coerceAtLeast(1)
            val updated = session.copy(
                durationSec = durationSec,
                note = note.trim().takeIf { it.isNotEmpty() },
            )
            runCatching { sessionRepository.update(updated) }
                .onSuccess { _events.emit(HistoryEvent.Updated) }
                .onFailure { _events.emit(HistoryEvent.SaveFailed) }
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            runCatching { sessionRepository.delete(session) }
                .onSuccess { _events.emit(HistoryEvent.Deleted) }
                .onFailure { _events.emit(HistoryEvent.SaveFailed) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(container.sessionRepository)
            }
        }
    }
}
