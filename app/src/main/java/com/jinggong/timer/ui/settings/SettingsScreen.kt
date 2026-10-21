package com.jinggong.timer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinggong.timer.R
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.ThemeMode
import com.jinggong.timer.ui.components.ConfirmDialog
import com.jinggong.timer.ui.components.NumberInputDialog
import com.jinggong.timer.ui.components.SectionTitle
import com.jinggong.timer.ui.components.ZenCard
import com.jinggong.timer.util.SystemIntents
import com.jinggong.timer.util.TimeFormatters

@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenAudioManage: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInput by remember { mutableStateOf("") }
    var goalError by remember { mutableStateOf<String?>(null) }
    var targetMode by remember { mutableStateOf<PracticeMode?>(null) }
    var targetInput by remember { mutableStateOf("0") }
    var targetError by remember { mutableStateOf<String?>(null) }
    var showClearFirst by remember { mutableStateOf(false) }
    var showClearSecond by remember { mutableStateOf(false) }
    var showDefaultAudioMenu by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) viewModel.export(uri)
    }

    val exported = stringResource(R.string.csv_export_success)
    val exportFailed = stringResource(R.string.csv_export_failed)
    val noData = stringResource(R.string.csv_no_data)
    val cleared = stringResource(R.string.settings_clear_done)
    val clearBlocked = stringResource(R.string.settings_clear_blocked)
    val clearFailed = stringResource(R.string.settings_clear_failed)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.Exported -> exported
                SettingsEvent.ExportFailed -> exportFailed
                SettingsEvent.NoData -> noData
                SettingsEvent.Cleared -> cleared
                SettingsEvent.ClearBlocked -> clearBlocked
                SettingsEvent.ClearFailed -> clearFailed
                SettingsEvent.Saved -> null
            }
            if (message != null) snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            item {
                SectionTitle(stringResource(R.string.settings_section_appearance))
            }
            item {
                ZenCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(stringResource(mode.labelRes())) },
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.settings_section_practice))
            }
            item {
                ZenCard(contentPadding = PaddingValues(vertical = 2.dp)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_daily_goal),
                        subtitle = stringResource(
                            R.string.settings_daily_goal_summary,
                            state.settings.dailyGoalMinutes,
                        ),
                        onClick = {
                            goalInput = state.settings.dailyGoalMinutes.toString()
                            goalError = null
                            showGoalDialog = true
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_auto_play),
                        subtitle = stringResource(R.string.settings_auto_play_desc),
                        checked = state.settings.autoPlayAudio,
                        onCheckedChange = viewModel::setAutoPlay,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Box {
                        SettingsRow(
                            title = stringResource(R.string.settings_default_audio),
                            subtitle = state.settings.defaultAudioId?.let { id ->
                                state.audio.firstOrNull { it.id == id }?.displayName
                            } ?: stringResource(R.string.settings_default_audio_none),
                            onClick = { showDefaultAudioMenu = true },
                        )
                        DropdownMenu(
                            expanded = showDefaultAudioMenu,
                            onDismissRequest = { showDefaultAudioMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_default_audio_none)) },
                                onClick = {
                                    showDefaultAudioMenu = false
                                    viewModel.setDefaultAudio(null)
                                },
                                trailingIcon = {
                                    if (state.settings.defaultAudioId == null) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                },
                            )
                            state.audio.forEach { audio ->
                                DefaultAudioItem(
                                    audio = audio,
                                    selected = state.settings.defaultAudioId == audio.id,
                                    onClick = {
                                        showDefaultAudioMenu = false
                                        viewModel.setDefaultAudio(audio.id)
                                    },
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_default_loop),
                        subtitle = stringResource(R.string.settings_default_loop_desc),
                        checked = state.settings.loopAudio,
                        onCheckedChange = viewModel::setLoop,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_target_quiet),
                        subtitle = targetSummary(state.settings.quietTargetMinutes),
                        onClick = {
                            targetInput = state.settings.quietTargetMinutes.toString()
                            targetError = null
                            targetMode = PracticeMode.QUIET
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_target_standing),
                        subtitle = targetSummary(state.settings.standingTargetMinutes),
                        onClick = {
                            targetInput = state.settings.standingTargetMinutes.toString()
                            targetError = null
                            targetMode = PracticeMode.STANDING
                        },
                    )
                }
            }

            item {
                SectionTitle(stringResource(R.string.settings_section_audio))
            }
            item {
                ZenCard(contentPadding = PaddingValues(vertical = 2.dp)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_audio_manage),
                        subtitle = stringResource(R.string.settings_audio_count, state.audio.size),
                        onClick = onOpenAudioManage,
                    )
                }
            }

            item {
                SectionTitle(stringResource(R.string.settings_section_data))
            }
            item {
                ZenCard(contentPadding = PaddingValues(vertical = 2.dp)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_export_csv),
                        subtitle = stringResource(R.string.settings_export_csv_desc),
                        onClick = {
                            csvLauncher.launch(
                                context.getString(
                                    R.string.csv_file_name,
                                    TimeFormatters.formatFileDate(),
                                ),
                            )
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingsRow(
                        title = stringResource(R.string.settings_clear_data),
                        subtitle = stringResource(R.string.settings_clear_data_desc),
                        onClick = { showClearFirst = true },
                        destructive = true,
                    )
                }
            }

            item {
                SectionTitle(stringResource(R.string.settings_section_background))
            }
            item {
                ZenCard(contentPadding = PaddingValues(18.dp)) {
                    Text(
                        text = stringResource(R.string.settings_background_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_background_body),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { SystemIntents.openAppDetails(context) },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(stringResource(R.string.settings_open_app_settings))
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.settings_section_about))
            }
            item {
                ZenCard(contentPadding = PaddingValues(vertical = 2.dp)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_summary),
                        onClick = onOpenAbout,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (showGoalDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_goal_dialog_title),
            label = stringResource(R.string.settings_goal_dialog_label),
            initialValue = goalInput,
            confirmText = stringResource(R.string.save),
            onConfirm = { raw ->
                val minutes = raw.toIntOrNull()
                if (minutes == null || minutes !in 5..240) {
                    goalError = context.getString(R.string.settings_goal_invalid)
                } else {
                    viewModel.setDailyGoal(minutes)
                    showGoalDialog = false
                }
            },
            onDismiss = { showGoalDialog = false },
            errorText = goalError,
        )
    }

    targetMode?.let { mode ->
        NumberInputDialog(
            title = stringResource(R.string.target_dialog_title),
            label = stringResource(R.string.target_dialog_label),
            initialValue = targetInput,
            confirmText = stringResource(R.string.save),
            onConfirm = { raw ->
                val minutes = raw.toIntOrNull()
                if (minutes == null || minutes !in 0..240) {
                    targetError = context.getString(R.string.target_invalid)
                } else {
                    viewModel.setTarget(mode, minutes)
                    targetMode = null
                }
            },
            onDismiss = { targetMode = null },
            errorText = targetError,
        )
    }

    if (showClearFirst) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_first_title),
            message = stringResource(R.string.settings_clear_first_message),
            confirmText = stringResource(R.string.confirm),
            onConfirm = {
                showClearFirst = false
                showClearSecond = true
            },
            onDismiss = { showClearFirst = false },
            destructive = true,
        )
    }
    if (showClearSecond) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_second_title),
            message = stringResource(R.string.settings_clear_second_message),
            confirmText = stringResource(R.string.settings_clear_action),
            onConfirm = {
                showClearSecond = false
                viewModel.clearAllSessions()
            },
            onDismiss = { showClearSecond = false },
            destructive = true,
        )
    }
}

@Composable
private fun targetSummary(minutes: Int): String = if (minutes <= 0) {
    stringResource(R.string.settings_target_off)
} else {
    stringResource(R.string.settings_target_summary, minutes)
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DefaultAudioItem(
    audio: AudioEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(audio.displayName, maxLines = 1)
                Text(
                    text = stringResource(
                        when (audio.modeScope) {
                            "QUIET" -> R.string.audio_scope_quiet
                            "STANDING" -> R.string.audio_scope_standing
                            else -> R.string.audio_scope_all
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
        trailingIcon = {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = null)
        },
    )
}
