package com.jinggong.timer.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinggong.timer.R
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.ui.components.ZenCard
import com.jinggong.timer.ui.stats.components.MonthHeatmap
import com.jinggong.timer.ui.stats.components.StackedBarChart
import java.time.YearMonth

@Composable
fun StatsScreen(container: AppContainer) {
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        item {
            SummaryCard(state = state)
        }

        item {
            ZenCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = stringResource(R.string.stats_last_30_days),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.stats_last_30_days_hint),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StackedBarChart(
                    days = state.last30Days,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            ZenCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = stringResource(R.string.stats_heatmap_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                MonthHeatmap(
                    month = state.heatmapMonth,
                    values = state.heatmap,
                    canGoNext = state.heatmapMonth < YearMonth.now(),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    value = stringResource(R.string.stats_days_value, state.currentStreak),
                    label = stringResource(R.string.stats_current_streak),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    value = stringResource(R.string.stats_days_value, state.longestStreak),
                    label = stringResource(R.string.stats_longest_streak),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    value = stringResource(R.string.stats_days_value, state.totalDays),
                    label = stringResource(R.string.stats_total_days),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: StatsUiState) {
    ZenCard(contentPadding = PaddingValues(18.dp)) {
        Text(
            text = stringResource(R.string.stats_summary_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1.25f))
            SummaryHeader(stringResource(R.string.stats_quiet), Modifier.weight(1f))
            SummaryHeader(stringResource(R.string.stats_standing), Modifier.weight(1f))
            SummaryHeader(stringResource(R.string.stats_sum), Modifier.weight(1f))
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        SummaryRow(stringResource(R.string.stats_today), state.today)
        SummaryRow(stringResource(R.string.stats_week), state.week)
        SummaryRow(stringResource(R.string.stats_month), state.month)
        SummaryRow(stringResource(R.string.stats_total), state.all)
    }
}

@Composable
private fun SummaryHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun SummaryRow(label: String, totals: PeriodTotals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1.25f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SummaryValue(minutesShort(totals.quietSec), Modifier.weight(1f))
        SummaryValue(minutesShort(totals.standingSec), Modifier.weight(1f))
        SummaryValue(
            minutesShort(totals.totalSec),
            Modifier.weight(1f),
            emphasize = true,
        )
    }
}

@Composable
private fun SummaryValue(text: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = if (emphasize) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (emphasize) FontWeight.Medium else FontWeight.Normal,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun minutesShort(seconds: Long): String {
    return if (seconds in 1..59) {
        stringResource(R.string.stats_seconds_short, seconds.toInt())
    } else {
        stringResource(R.string.stats_minutes_short, (seconds / 60L).toInt())
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    ZenCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}
