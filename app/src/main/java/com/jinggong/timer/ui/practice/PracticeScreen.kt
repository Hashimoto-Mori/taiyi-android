package com.jinggong.timer.ui.practice

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinggong.timer.R
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.TimerPhase
import com.jinggong.timer.ui.components.NumberInputDialog
import com.jinggong.timer.ui.components.ZenCard
import com.jinggong.timer.ui.components.ZenTopBar
import com.jinggong.timer.util.SystemIntents
import com.jinggong.timer.util.TimeFormatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min

@Composable
fun PracticeScreen(
    container: AppContainer,
    mode: PracticeMode,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActiveMode: (PracticeMode) -> Unit,
) {
    val viewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.factory(container, mode),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.timer.phase) {
        nowElapsed = SystemClock.elapsedRealtime()
        if (state.timer.phase == TimerPhase.RUNNING) {
            while (isActive) {
                nowElapsed = SystemClock.elapsedRealtime()
                delay(200L)
            }
        }
    }
    val elapsedMs = state.timer.elapsedMs(nowElapsed)

    val activity = remember(context) { context.findActivity() }
    DisposableEffect(state.timer.isRunning) {
        if (state.timer.isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var notificationGranted by remember {
        mutableStateOf(SystemIntents.canPostNotifications(context))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationGranted = SystemIntents.canPostNotifications(context)
    }

    var showTargetDialog by remember { mutableStateOf(false) }
    var targetInput by remember { mutableStateOf("0") }
    var targetError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.summary) {
        if (state.summary != null) showTargetDialog = false
    }

    val savedMessage = stringResource(R.string.summary_saved)
    val discardedMessage = stringResource(R.string.summary_discarded)
    val saveFailedMessage = stringResource(R.string.summary_save_failed)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                PracticeEvent.Saved -> savedMessage
                PracticeEvent.Discarded -> discardedMessage
                PracticeEvent.SaveFailed -> saveFailedMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            ZenTopBar(
                title = stringResource(
                    when (mode) {
                        PracticeMode.QUIET -> R.string.practice_title_quiet
                        PracticeMode.STANDING -> R.string.practice_title_standing
                    },
                ),
                onBack = onBack,
            )

            if (state.isOtherModeActive) {
                OtherModeWarning(
                    activeMode = state.timer.mode,
                    onOpenActiveMode = onOpenActiveMode,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.timer.recoveredAfterReboot) {
                RecoveryBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val timerFontSize = min(70f, maxWidth.value / 4.9f).sp
                Text(
                    text = TimeFormatters.formatTimer(elapsedMs),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = timerFontSize,
                        lineHeight = timerFontSize * 1.08f,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = stringResource(
                    when (state.timer.phase) {
                        TimerPhase.IDLE -> R.string.practice_status_ready
                        TimerPhase.RUNNING -> R.string.practice_status_running
                        TimerPhase.PAUSED -> R.string.practice_status_paused
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state.timer.isActive) {
                Text(
                    text = stringResource(R.string.practice_back_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            AssistChip(
                onClick = {
                    targetInput = state.targetMinutes.toString()
                    targetError = null
                    showTargetDialog = true
                },
                label = {
                    Text(
                        text = if (state.targetMinutes > 0) {
                            stringResource(R.string.practice_target, state.targetMinutes)
                        } else {
                            stringResource(R.string.practice_target_none)
                        },
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(50),
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        when (state.timer.phase) {
                            TimerPhase.IDLE -> viewModel.start()
                            TimerPhase.RUNNING -> viewModel.pause()
                            TimerPhase.PAUSED -> viewModel.resume()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    enabled = state.timer.ready && !state.isOtherModeActive,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = when (state.timer.phase) {
                            TimerPhase.IDLE -> Icons.Outlined.PlayArrow
                            TimerPhase.RUNNING -> Icons.Outlined.Pause
                            TimerPhase.PAUSED -> Icons.Outlined.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            when (state.timer.phase) {
                                TimerPhase.IDLE -> R.string.practice_start
                                TimerPhase.RUNNING -> R.string.practice_pause
                                TimerPhase.PAUSED -> R.string.practice_resume
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (state.timer.isActive) {
                    OutlinedButton(
                        onClick = { viewModel.end() },
                        modifier = Modifier.height(60.dp),
                        enabled = elapsedMs >= 1_000L,
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.practice_end))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            AudioPanel(
                state = state,
                onToggleAudio = viewModel::toggleAudio,
                onSelectAudio = viewModel::selectAudio,
                onToggleLoop = viewModel::toggleLoop,
                onOpenSettings = onOpenSettings,
            )

            if (!notificationGranted) {
                Spacer(modifier = Modifier.height(14.dp))
                NotificationPermissionCard(
                    onOpenSettings = { SystemIntents.openNotificationSettings(context) },
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (showTargetDialog) {
        NumberInputDialog(
            title = stringResource(R.string.target_dialog_title),
            label = stringResource(R.string.target_dialog_label),
            initialValue = targetInput,
            confirmText = stringResource(R.string.confirm),
            onConfirm = { raw ->
                val minutes = raw.toIntOrNull()
                if (minutes == null || minutes !in 0..240) {
                    targetError = context.getString(R.string.target_invalid)
                } else {
                    viewModel.setTarget(minutes)
                    showTargetDialog = false
                }
            },
            onDismiss = { showTargetDialog = false },
            errorText = targetError,
            supportingText = stringResource(R.string.target_dialog_label),
        )
    }

    state.summary?.let { result ->
        FinishSummaryDialog(
            result = result,
            onSave = viewModel::save,
            onDiscard = viewModel::discard,
        )
    }
}

@Composable
private fun OtherModeWarning(
    activeMode: PracticeMode?,
    onOpenActiveMode: (PracticeMode) -> Unit,
) {
    val mode = activeMode ?: return
    ZenCard(borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)) {
        Text(
            text = stringResource(
                R.string.practice_other_mode,
                stringResource(mode.labelRes()),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            onClick = { onOpenActiveMode(mode) },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.practice_other_mode_action))
        }
    }
}

@Composable
private fun RecoveryBanner() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.practice_recovered),
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NotificationPermissionCard(onOpenSettings: () -> Unit) {
    ZenCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f)) {
        Text(
            text = stringResource(R.string.permission_notification_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.permission_notification_body),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(stringResource(R.string.permission_open_settings))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
