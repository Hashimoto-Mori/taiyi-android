package com.jinggong.timer

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.jinggong.timer.data.prefs.Settings
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.ThemeMode
import com.jinggong.timer.service.ServiceActions
import com.jinggong.timer.ui.nav.AppNavHost
import com.jinggong.timer.ui.theme.JingGongTheme
import com.jinggong.timer.util.SystemIntents
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 拒绝时仍允许计时，练习页会显示提示 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        container = (application as JingGongApp).container
        setContent {
            val settings by container.settingsDataStore.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            JingGongTheme(darkTheme = darkTheme) {
                AppNavHost(container = container)
            }
        }
        requestNotificationPermissionOnce()
        lifecycleScope.launch {
            container.awaitReady()
            if (container.timerManager.state.value.isActive) {
                ServiceActions.start(this@MainActivity)
            }
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            container.awaitReady()
            if (container.timerManager.state.value.isActive) {
                ServiceActions.start(this@MainActivity)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val finish = intent.getBooleanExtra(ServiceActions.EXTRA_FINISH_SESSION, false)
        val open = finish || intent.getBooleanExtra(ServiceActions.EXTRA_OPEN_PRACTICE, false)
        intent.removeExtra(ServiceActions.EXTRA_FINISH_SESSION)
        intent.removeExtra(ServiceActions.EXTRA_OPEN_PRACTICE)
        if (!open) return
        lifecycleScope.launch {
            container.awaitReady()
            if (container.timerManager.state.value.isActive) {
                container.navigationEvents.requestOpenPractice(finish)
            }
        }
    }

    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (SystemIntents.canPostNotifications(this)) return
        val prefs = getSharedPreferences("ui_flags", MODE_PRIVATE)
        if (prefs.getBoolean("notification_asked", false)) return
        prefs.edit().putBoolean("notification_asked", true).apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
