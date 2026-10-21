package com.jinggong.timer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinggong.timer.R
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.ui.components.GoalRing
import com.jinggong.timer.ui.components.ModeBadge
import com.jinggong.timer.ui.components.ZenCard
import com.jinggong.timer.ui.components.accentColor
import com.jinggong.timer.util.TimeFormatters
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenMode: (PracticeMode) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = TimeFormatters.today()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.today_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = TimeFormatters.formatHistoryDay(today),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            TodayOverviewCard(state = state)
        }

        val activeMode = state.activeMode
        if (activeMode != null) {
            item {
                ActiveSessionBanner(
                    mode = activeMode,
                    elapsedMs = state.activeElapsedMs,
                    running = state.activeRunning,
                    onClick = { onOpenMode(activeMode) },
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_section_practice),
                modifier = Modifier.padding(top = 10.dp, start = 2.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            ModeCard(
                mode = PracticeMode.QUIET,
                summary = state.quiet,
                subtitle = stringResource(R.string.home_quiet_subtitle),
                onClick = { onOpenMode(PracticeMode.QUIET) },
            )
        }

        item {
            ModeCard(
                mode = PracticeMode.STANDING,
                summary = state.standing,
                subtitle = stringResource(R.string.home_standing_subtitle),
                onClick = { onOpenMode(PracticeMode.STANDING) },
            )
        }

    }
}

@Composable
private fun TodayOverviewCard(state: HomeUiState) {
    val goalSec = state.goalMinutes.coerceAtLeast(1) * 60L
    val progress = (state.todayTotalSec.toFloat() / goalSec.toFloat()).coerceIn(0f, 1f)
    val percent = (progress * 100).roundToInt()
    ZenCard(contentPadding = PaddingValues(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_total_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = TimeFormatters.formatCompactDuration(
                        androidx.compose.ui.platform.LocalContext.current,
                        state.todayTotalSec,
                    ),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.today_sessions, state.todayCount),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GoalRing(
                progress = progress,
                centerTop = "$percent%",
                centerBottom = stringResource(R.string.goal_ring_label),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.today_streak_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.streak_days, state.streak),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ActiveSessionBanner(
    mode: PracticeMode,
    elapsedMs: Long,
    running: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = stringResource(
                        if (running) R.string.notification_running else R.string.notification_paused,
                        stringResource(mode.labelRes()),
                        TimeFormatters.formatTimer(elapsedMs),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.active_session_open),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: PracticeMode,
    summary: ModeTodaySummary,
    subtitle: String,
    onClick: () -> Unit,
) {
    val accent = mode.accentColor()
    val shape = RoundedCornerShape(30.dp)
    val context = androidx.compose.ui.platform.LocalContext.current
    val latest = if (summary.latestStart != null) {
        stringResource(
            R.string.latest_practice,
            TimeFormatters.formatLatestPractice(context, summary.latestStart),
        )
    } else {
        stringResource(R.string.latest_none)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 178.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        accent.copy(alpha = 0.10f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.20f), shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            text = stringResource(mode.labelRes()).take(1),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 26.dp),
            fontSize = 116.sp,
            color = accent.copy(alpha = 0.055f),
            fontWeight = FontWeight.Light,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModeBadge(mode)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.today_sessions, summary.count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(mode.labelRes()),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = TimeFormatters.formatCompactDuration(context, summary.totalSec),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = latest,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
