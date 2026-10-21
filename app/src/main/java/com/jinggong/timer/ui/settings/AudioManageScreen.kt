package com.jinggong.timer.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinggong.timer.R
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.AudioScope
import com.jinggong.timer.ui.components.ConfirmDialog
import com.jinggong.timer.ui.components.EmptyState
import com.jinggong.timer.ui.components.ZenCard
import com.jinggong.timer.ui.components.ZenTopBar

@Composable
fun AudioManageScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: AudioManageViewModel = viewModel(
        factory = AudioManageViewModel.factory(container),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var importName by remember { mutableStateOf("") }
    var importScope by remember { mutableStateOf(AudioScope.ALL) }
    var renaming by remember { mutableStateOf<AudioEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<AudioEntity?>(null) }

    val pickAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            importName = ""
            importScope = AudioScope.ALL
        }
    }

    val imported = stringResource(R.string.audio_import_success)
    val importFailed = stringResource(R.string.audio_import_failed)
    val deleted = stringResource(R.string.audio_deleted)
    val renamed = stringResource(R.string.audio_renamed)
    val nameRequired = stringResource(R.string.audio_name_required)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                AudioManageEvent.Imported -> imported
                AudioManageEvent.ImportFailed -> importFailed
                AudioManageEvent.Deleted -> deleted
                AudioManageEvent.Renamed -> renamed
                AudioManageEvent.NameRequired -> nameRequired
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ZenTopBar(
                title = stringResource(R.string.audio_manage_title),
                onBack = onBack,
                actions = {
                    Button(
                        onClick = { pickAudio.launch(arrayOf("audio/*")) },
                        enabled = !state.importing,
                        modifier = Modifier.padding(end = 10.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.audio_import))
                    }
                },
            )
            if (state.importing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Text(
                    text = stringResource(R.string.audio_importing),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.audio.isEmpty() && !state.importing) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.audio_empty),
                            subtitle = stringResource(R.string.audio_empty_hint),
                        )
                    }
                }
                items(state.audio, key = { it.id }) { audio ->
                    AudioItem(
                        audio = audio,
                        isDefault = state.defaultAudioId == audio.id,
                        isPreviewPlaying = state.previewId == audio.id &&
                            state.playback.isPlaying,
                        onPreview = { viewModel.togglePreview(audio) },
                        onRename = {
                            renaming = audio
                            renameText = audio.displayName
                        },
                        onDelete = { deleting = audio },
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

    pendingUri?.let { uri ->
        ImportAudioDialog(
            name = importName,
            onNameChange = { importName = it },
            scope = importScope,
            onScopeChange = { importScope = it },
            onDismiss = { pendingUri = null },
            onConfirm = {
                viewModel.importAudio(uri, importName, importScope)
                pendingUri = null
            },
        )
    }

    renaming?.let { audio ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.audio_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.audio_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameAudio(audio, renameText)
                        renaming = null
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    deleting?.let { audio ->
        ConfirmDialog(
            title = stringResource(R.string.audio_delete_title),
            message = stringResource(R.string.audio_delete_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteAudio(audio)
                deleting = null
            },
            onDismiss = { deleting = null },
            destructive = true,
        )
    }
}

@Composable
private fun AudioItem(
    audio: AudioEntity,
    isDefault: Boolean,
    isPreviewPlaying: Boolean,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ZenCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = audio.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (isDefault) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = stringResource(R.string.audio_default_badge),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(scopeLabel(audio.modeScope)),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreview) {
                Icon(
                    imageVector = if (isPreviewPlaying) Icons.Outlined.Pause
                    else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(
                        if (isPreviewPlaying) R.string.audio_stop_preview else R.string.audio_preview,
                    ),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(
                        if (isPreviewPlaying) R.string.audio_stop_preview else R.string.audio_preview,
                    ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.rename),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun ImportAudioDialog(
    name: String,
    onNameChange: (String) -> Unit,
    scope: AudioScope,
    onScopeChange: (AudioScope) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.audio_import_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it.take(80)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.audio_name_label)) },
                    placeholder = { Text(stringResource(R.string.audio_name_optional)) },
                    singleLine = true,
                )
                Text(
                    text = stringResource(R.string.audio_scope_label),
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AudioScope.entries.forEach { option ->
                        FilterChip(
                            selected = scope == option,
                            onClick = { onScopeChange(option) },
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.audio_import_confirm),
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun scopeLabel(scope: String): Int = when (scope) {
    "QUIET" -> R.string.audio_scope_quiet
    "STANDING" -> R.string.audio_scope_standing
    else -> R.string.audio_scope_all
}
