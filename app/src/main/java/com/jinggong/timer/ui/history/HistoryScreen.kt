package com.jinggong.timer.ui.history

import android.os.SystemClock
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinggong.timer.R
import com.jinggong.timer.data.db.SessionEntity
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.ui.components.ConfirmDialog
import com.jinggong.timer.ui.components.EmptyState
import com.jinggong.timer.ui.components.ModeBadge
import com.jinggong.timer.ui.components.ZenCard
import com.jinggong.timer.ui.components.accentColor
import com.jinggong.timer.util.TimeFormatters

@Composable
fun HistoryScreen(container: AppContainer) {
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<SessionEntity?>(null) }
    var deleting by remember { mutableStateOf<SessionEntity?>(null) }

    val updatedMessage = stringResource(R.string.history_updated)
    val deletedMessage = stringResource(R.string.history_deleted)
    val failedMessage = stringResource(R.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    HistoryEvent.Updated -> updatedMessage
                    HistoryEvent.Deleted -> deletedMessage
                    HistoryEvent.SaveFailed -> failedMessage
                },
            )
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
                    text = stringResource(R.string.history_title),
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.mode == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text(stringResource(R.string.history_filter_all)) },
                    )
                    FilterChip(
                        selected = state.mode == PracticeMode.QUIET,
                        onClick = { viewModel.setFilter(PracticeMode.QUIET) },
                        label = { Text(stringResource(R.string.mode_quiet)) },
                    )
                    FilterChip(
                        selected = state.mode == PracticeMode.STANDING,
                        onClick = { viewModel.setFilter(PracticeMode.STANDING) },
                        label = { Text(stringResource(R.string.mode_standing)) },
                    )
                }
            }

            if (state.groups.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.history_empty),
                        subtitle = stringResource(R.string.history_empty_hint),
                    )
                }
            }

            state.groups.forEach { group ->
                item(key = "header-${group.day}") {
                    Text(
                        text = TimeFormatters.formatHistoryDay(group.day),
                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp, start = 2.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(group.sessions, key = { it.id }) { session ->
                    HistoryItem(
                        session = session,
                        onEdit = { editing = session },
                        onDelete = { deleting = session },
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

    editing?.let { session ->
        EditSessionDialog(
            session = session,
            onDismiss = { editing = null },
            onConfirm = { minutes, note ->
                viewModel.updateSession(session, minutes, note)
                editing = null
            },
        )
    }

    deleting?.let { session ->
        ConfirmDialog(
            title = stringResource(R.string.history_delete_title),
            message = stringResource(R.string.history_delete_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteSession(session)
                deleting = null
            },
            onDismiss = { deleting = null },
            destructive = true,
        )
    }
}

@Composable
private fun HistoryItem(
    session: SessionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val mode = PracticeMode.fromDb(session.mode) ?: return
    ZenCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModeBadge(mode)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = TimeFormatters.formatClockTime(session.startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_session),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_delete_session),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = TimeFormatters.formatStatDuration(context, session.durationSec.toLong()),
                style = MaterialTheme.typography.titleLarge,
                color = mode.accentColor(),
            )
            if (!session.note.isNullOrBlank()) {
                Text(
                    text = session.note,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        if (!session.audioTitle.isNullOrBlank()) {
            Text(
                text = session.audioTitle,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
