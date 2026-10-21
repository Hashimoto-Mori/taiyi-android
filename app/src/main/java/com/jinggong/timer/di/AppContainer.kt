package com.jinggong.timer.di

import android.app.Application
import androidx.room.Room
import com.jinggong.timer.data.db.AppDatabase
import com.jinggong.timer.data.prefs.SettingsDataStore
import com.jinggong.timer.data.repo.AudioRepository
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.service.PlaybackController
import com.jinggong.timer.service.PracticeTimerAudioSync
import com.jinggong.timer.timer.AndroidBootCountProvider
import com.jinggong.timer.timer.SystemClockSource
import com.jinggong.timer.timer.TimerManager
import com.jinggong.timer.timer.TimerStore
import com.jinggong.timer.ui.nav.NavigationEvents
import com.jinggong.timer.util.CsvExporter
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 手动依赖注入容器，应用进程内单例。 */
class AppContainer(application: Application) {

    private val app = application.applicationContext
    val appContext: android.content.Context get() = app
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase = Room.databaseBuilder(
        app,
        AppDatabase::class.java,
        AppDatabase.NAME,
    ).build()

    val sessionRepository = SessionRepository(database.sessionDao())
    val audioRepository = AudioRepository(app, database.audioDao())
    val settingsDataStore = SettingsDataStore(app)

    val timerManager = TimerManager(
        store = TimerStore(app),
        clock = SystemClockSource(),
        bootCounts = AndroidBootCountProvider(app),
        scope = applicationScope,
    )

    val playbackController = PlaybackController(
        context = app,
        audioRepository = audioRepository,
        settingsDataStore = settingsDataStore,
        scope = applicationScope,
    )

    /** 计时暂停/继续/结束时联动音频。 */
    val practiceTimerAudioSync = PracticeTimerAudioSync(
        timerManager = timerManager,
        playbackController = playbackController,
        scope = applicationScope,
    )

    val csvExporter = CsvExporter(app, sessionRepository)
    val navigationEvents = NavigationEvents()

    private val initialized = AtomicBoolean(false)

    /** 启动一次性恢复：计时快照与失效音频记录。 */
    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        applicationScope.launch {
            timerManager.restore()
            val removed = runCatching { audioRepository.removeMissingFiles() }.getOrDefault(0)
            if (removed > 0) {
                val current = settingsDataStore.current()
                val defaultId = current.defaultAudioId
                if (defaultId != null && audioRepository.get(defaultId) == null) {
                    settingsDataStore.setDefaultAudio(null)
                }
            }
        }
    }

    suspend fun awaitReady() {
        timerManager.state.filter { it.ready }.first()
    }
}
