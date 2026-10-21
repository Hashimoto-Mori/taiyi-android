package com.jinggong.timer.timer

import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 计时器的唯一状态源。
 *
 * 运行中使用 [Clock.elapsedRealtime] 计算时长；每次状态变化与每 15 秒检查点都写入
 * [TimerStore]。设备重启后恢复到最近检查点并暂停，避免把关机时间计入练习。
 */
class TimerManager(
    private val store: TimerStore,
    private val clock: Clock,
    private val bootCounts: BootCountProvider,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var checkpointJob: Job? = null

    /** 从 DataStore 恢复计时；重复调用安全。 */
    suspend fun restore() {
        mutex.withLock {
            if (_state.value.ready) return
            val snapshot = runCatching { store.load() }.getOrNull()
            if (snapshot == null) {
                _state.value = TimerState(ready = true)
                return
            }

            val nowElapsed = clock.elapsedRealtime()
            val currentBoot = bootCounts.currentBootCount()
            val bootChanged = snapshot.bootCount >= 0 &&
                currentBoot >= 0 &&
                snapshot.bootCount != currentBoot
            val elapsedWentBackwards = snapshot.phase == TimerPhase.RUNNING &&
                nowElapsed < snapshot.resumeElapsedMs
            val cannotTrustElapsed = bootChanged || elapsedWentBackwards

            if (snapshot.phase == TimerPhase.RUNNING && cannotTrustElapsed) {
                val restoredMs = snapshot.checkpointTotalMs
                    .coerceAtLeast(snapshot.accumulatedMs)
                val restored = TimerState(
                    ready = true,
                    phase = TimerPhase.PAUSED,
                    mode = snapshot.mode,
                    accumulatedMs = restoredMs,
                    resumeElapsedMs = nowElapsed,
                    startWallMs = snapshot.startWallMs,
                    targetMinutes = snapshot.targetMinutes,
                    recoveredAfterReboot = true,
                )
                _state.value = restored
                store.save(
                    restored.toSnapshot(
                        checkpointTotalMs = restoredMs,
                        checkpointWallMs = clock.currentTimeMillis(),
                    ),
                )
            } else {
                _state.value = TimerState(
                    ready = true,
                    phase = snapshot.phase,
                    mode = snapshot.mode,
                    accumulatedMs = snapshot.accumulatedMs,
                    resumeElapsedMs = snapshot.resumeElapsedMs,
                    startWallMs = snapshot.startWallMs,
                    targetMinutes = snapshot.targetMinutes,
                )
                if (snapshot.phase == TimerPhase.RUNNING) startCheckpointLoop()
            }
        }
    }

    /** 开始新的练习。已有活动练习时忽略。 */
    suspend fun start(mode: PracticeMode, targetMinutes: Int) {
        mutex.withLock {
            if (!_state.value.ready || _state.value.phase != TimerPhase.IDLE) return
            val nowElapsed = clock.elapsedRealtime()
            val nowWall = clock.currentTimeMillis()
            val target = targetMinutes.coerceIn(0, 240)
            val state = TimerState(
                ready = true,
                phase = TimerPhase.RUNNING,
                mode = mode,
                accumulatedMs = 0L,
                resumeElapsedMs = nowElapsed,
                startWallMs = nowWall,
                targetMinutes = target,
            )
            _state.value = state
            store.save(
                state.toSnapshot(
                    checkpointTotalMs = 0L,
                    checkpointWallMs = nowWall,
                ),
            )
            startCheckpointLoop()
        }
    }

    /** 暂停并把当前片段累加进 accumulatedMs。 */
    suspend fun pause() {
        mutex.withLock {
            val current = _state.value
            if (!current.isRunning) return
            val nowElapsed = clock.elapsedRealtime()
            val nowWall = clock.currentTimeMillis()
            val total = current.elapsedMs(nowElapsed)
            val paused = current.copy(
                phase = TimerPhase.PAUSED,
                accumulatedMs = total,
                resumeElapsedMs = nowElapsed,
                recoveredAfterReboot = false,
            )
            _state.value = paused
            stopCheckpointLoop()
            store.save(
                paused.toSnapshot(
                    checkpointTotalMs = total,
                    checkpointWallMs = nowWall,
                ),
            )
        }
    }

    /** 从暂停继续。 */
    suspend fun resume() {
        mutex.withLock {
            val current = _state.value
            if (!current.isPaused) return
            val nowElapsed = clock.elapsedRealtime()
            val nowWall = clock.currentTimeMillis()
            val resumed = current.copy(
                phase = TimerPhase.RUNNING,
                resumeElapsedMs = nowElapsed,
                recoveredAfterReboot = false,
            )
            _state.value = resumed
            store.save(
                resumed.toSnapshot(
                    checkpointTotalMs = resumed.accumulatedMs,
                    checkpointWallMs = nowWall,
                ),
            )
            startCheckpointLoop()
        }
    }

    /**
     * 结束并暂停当前练习，返回待保存结果。
     *
     * 计时状态保持 PAUSED，直到调用方保存或放弃，避免小结弹窗期间丢失时长。
     */
    suspend fun finish(): TimerResult? {
        return mutex.withLock {
            val current = _state.value
            if (!current.isActive || current.mode == null) return@withLock null
            val nowElapsed = clock.elapsedRealtime()
            val nowWall = clock.currentTimeMillis()
            val total = current.elapsedMs(nowElapsed)
            val paused = current.copy(
                phase = TimerPhase.PAUSED,
                accumulatedMs = total,
                resumeElapsedMs = nowElapsed,
                recoveredAfterReboot = false,
            )
            _state.value = paused
            stopCheckpointLoop()
            store.save(
                paused.toSnapshot(
                    checkpointTotalMs = total,
                    checkpointWallMs = nowWall,
                ),
            )
            TimerResult(
                mode = current.mode,
                startWallMs = current.startWallMs,
                endWallMs = nowWall,
                durationSec = (total / 1000L).toInt(),
                targetMinutes = current.targetMinutes,
            )
        }
    }

    /** 修改当前练习的目标时长；0 表示取消目标。 */
    suspend fun updateTarget(minutes: Int) {
        mutex.withLock {
            val current = _state.value
            if (!current.isActive) return
            val safe = minutes.coerceIn(0, 240)
            val updated = current.copy(targetMinutes = safe)
            _state.value = updated
            val nowWall = clock.currentTimeMillis()
            store.save(
                updated.toSnapshot(
                    checkpointTotalMs = updated.elapsedMs(clock.elapsedRealtime()),
                    checkpointWallMs = nowWall,
                ),
            )
        }
    }

    /** 保存或放弃后清空计时状态。 */
    suspend fun reset() {
        mutex.withLock {
            stopCheckpointLoop()
            _state.value = TimerState(ready = true)
            store.clear()
        }
    }

    private fun startCheckpointLoop() {
        stopCheckpointLoop()
        checkpointJob = scope.launch {
            while (isActive) {
                delay(CHECKPOINT_INTERVAL_MS)
                checkpoint()
            }
        }
    }

    private fun stopCheckpointLoop() {
        checkpointJob?.cancel()
        checkpointJob = null
    }

    private suspend fun checkpoint() {
        mutex.withLock {
            val current = _state.value
            if (!current.isRunning) return
            val nowElapsed = clock.elapsedRealtime()
            val total = current.elapsedMs(nowElapsed)
            store.save(
                current.toSnapshot(
                    checkpointTotalMs = total,
                    checkpointWallMs = clock.currentTimeMillis(),
                ),
            )
        }
    }

    private fun TimerState.toSnapshot(checkpointTotalMs: Long, checkpointWallMs: Long): TimerSnapshot {
        val mode = requireNotNull(mode) { "Active timer must have a mode" }
        return TimerSnapshot(
            mode = mode,
            phase = phase,
            accumulatedMs = accumulatedMs,
            resumeElapsedMs = resumeElapsedMs,
            resumeWallMs = clock.currentTimeMillis(),
            startWallMs = startWallMs,
            targetMinutes = targetMinutes,
            bootCount = bootCounts.currentBootCount(),
            checkpointTotalMs = checkpointTotalMs,
            checkpointWallMs = checkpointWallMs,
        )
    }

    private companion object {
        const val CHECKPOINT_INTERVAL_MS = 15_000L
    }
}
