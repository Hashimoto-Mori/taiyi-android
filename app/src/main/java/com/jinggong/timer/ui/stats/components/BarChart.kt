package com.jinggong.timer.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinggong.timer.R
import com.jinggong.timer.ui.stats.DayBar
import com.jinggong.timer.ui.theme.LocalZenPalette
import com.jinggong.timer.util.TimeFormatters

@Composable
fun StackedBarChart(
    days: List<DayBar>,
    modifier: Modifier = Modifier,
) {
    val maxTotal = days.maxOfOrNull { it.totalSec } ?: 0L
    val palette = LocalZenPalette.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = palette.quiet)
            Text(
                text = stringResource(R.string.stats_quiet),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            LegendDot(color = palette.standing)
            Text(
                text = stringResource(R.string.stats_standing),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (maxTotal > 0L) {
                Text(
                    text = stringResource(
                        R.string.stats_chart_max,
                        (maxTotal / 60L).toInt().coerceAtLeast(1),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }

        if (maxTotal <= 0L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.stats_chart_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(top = 16.dp),
            ) {
                for (line in 0..3) {
                    val y = size.height * line / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                val slot = size.width / days.size.coerceAtLeast(1)
                val barWidth = slot * 0.58f
                val radius = CornerRadius(3f, 3f)
                days.forEachIndexed { index, day ->
                    if (day.totalSec <= 0L) return@forEachIndexed
                    val totalHeight = (day.totalSec.toFloat() / maxTotal.toFloat()) *
                        size.height * 0.92f
                    val barHeight = totalHeight.coerceAtLeast(2.dp.toPx())
                    val quietHeight = barHeight * (day.quietSec.toFloat() / day.totalSec)
                    val standingHeight = barHeight - quietHeight
                    val x = slot * index + (slot - barWidth) / 2f
                    val bottom = size.height

                    if (quietHeight > 0f) {
                        drawRoundRect(
                            color = palette.quiet,
                            topLeft = Offset(x, bottom - quietHeight),
                            size = Size(barWidth, quietHeight),
                            cornerRadius = radius,
                        )
                    }
                    if (standingHeight > 0f) {
                        drawRoundRect(
                            color = palette.standing,
                            topLeft = Offset(x, bottom - barHeight),
                            size = Size(barWidth, standingHeight),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }

        if (days.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = TimeFormatters.formatShortDate(days.first().date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = TimeFormatters.formatShortDate(days.last().date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}
