package com.jinggong.timer.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.ui.theme.LocalZenPalette

@Composable
fun PracticeMode.accentColor(): Color {
    val palette = LocalZenPalette.current
    return when (this) {
        PracticeMode.QUIET -> palette.quiet
        PracticeMode.STANDING -> palette.standing
    }
}

@Composable
fun ModeBadge(mode: PracticeMode, modifier: Modifier = Modifier) {
    val color = mode.accentColor()
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Row(modifier = Modifier.padding(PaddingValues(horizontal = 10.dp, vertical = 4.dp))) {
            Text(
                text = stringResource(mode.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}
