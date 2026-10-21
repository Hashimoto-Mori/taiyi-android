package com.jinggong.timer.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jinggong.timer.R
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.ui.components.ZenCard

@Composable
fun AudioPanel(
    state: PracticeUiState,
    onToggleAudio: () -> Unit,
    onSelectAudio: (Long) -> Unit,
    onToggleLoop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ZenCard(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.practice_audio_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleLoop, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Repeat,
                    contentDescription = stringResource(R.string.practice_audio_loop),
                    tint = if (state.playback.loop) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                )
            }
        }

        if (state.audioOptions.isEmpty()) {
            Text(
                text = stringResource(R.string.practice_no_audio),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.practice_no_audio_hint),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
            TextButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.settings_audio_manage))
            }
            return@ZenCard
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp),
            ) {
                IconButton(onClick = onToggleAudio, enabled = state.selectedAudio != null) {
                    Icon(
                        imageVector = if (state.playback.isPlaying) {
                            Icons.Outlined.Pause
                        } else {
                            Icons.Outlined.PlayArrow
                        },
                        contentDescription = stringResource(
                            if (state.playback.isPlaying) R.string.practice_audio_pause
                            else R.string.practice_audio_play,
                        ),
                        tint = if (state.selectedAudio != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = state.selectedAudio?.displayName
                        ?: stringResource(R.string.practice_audio_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = if (state.playback.isPlaying) {
                        stringResource(R.string.practice_audio_playing)
                    } else {
                        stringResource(R.string.practice_audio_paused)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(stringResource(R.string.practice_audio_switch))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.practice_audio_empty_option)) },
                        onClick = {
                            expanded = false
                            onSelectAudio(0L)
                        },
                        trailingIcon = {
                            if (state.selectedAudio == null) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        },
                    )
                    state.audioOptions.forEach { audio ->
                        AudioMenuItem(
                            audio = audio,
                            selected = state.selectedAudio?.id == audio.id,
                            onClick = {
                                expanded = false
                                onSelectAudio(audio.id)
                            },
                        )
                    }
                }
            }
        }

        if (state.playback.hasError) {
            Text(
                text = stringResource(R.string.audio_file_missing),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AudioMenuItem(
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
