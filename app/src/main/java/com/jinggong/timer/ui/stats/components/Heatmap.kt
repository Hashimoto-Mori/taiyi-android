package com.jinggong.timer.ui.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jinggong.timer.R
import com.jinggong.timer.ui.theme.LocalZenPalette
import com.jinggong.timer.ui.theme.ZenPalette
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthHeatmap(
    month: YearMonth,
    values: Map<LocalDate, Long>,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalZenPalette.current
    val weekdays = listOf(
        stringResource(R.string.weekday_mon),
        stringResource(R.string.weekday_tue),
        stringResource(R.string.weekday_wed),
        stringResource(R.string.weekday_thu),
        stringResource(R.string.weekday_fri),
        stringResource(R.string.weekday_sat),
        stringResource(R.string.weekday_sun),
    )
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val totalCells = offset + dayCount
    val rows = (totalCells + 6) / 7

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.stats_month_title, month.year, month.monthValue),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(R.string.stats_prev_month),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.stats_next_month),
                    tint = if (canGoNext) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val cell = row * 7 + column
                    val dayOfMonth = cell - offset + 1
                    if (dayOfMonth in 1..dayCount) {
                        val date = month.atDay(dayOfMonth)
                        HeatCell(
                            day = dayOfMonth,
                            totalSec = values[date] ?: 0L,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(R.string.stats_heatmap_less),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            listOf(
                palette.heatmapEmpty,
                palette.heatmapLevel1,
                palette.heatmapLevel2,
                palette.heatmapLevel3,
                palette.heatmapLevel4,
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.stats_heatmap_more),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeatCell(
    day: Int,
    totalSec: Long,
    palette: ZenPalette,
    modifier: Modifier = Modifier,
) {
    val color = heatColor(totalSec, palette)
    val high = totalSec > 3600L
    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (high) palette.onHeatmapHigh
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (totalSec > 0L) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

private fun heatColor(totalSec: Long, palette: ZenPalette): Color = when {
    totalSec <= 0L -> palette.heatmapEmpty
    totalSec <= 15 * 60L -> palette.heatmapLevel1
    totalSec <= 30 * 60L -> palette.heatmapLevel2
    totalSec <= 60 * 60L -> palette.heatmapLevel3
    else -> palette.heatmapLevel4
}
