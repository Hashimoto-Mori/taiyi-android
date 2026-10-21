package com.jinggong.timer.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jinggong.timer.R
import com.jinggong.timer.data.db.SessionEntity
import java.util.Locale

@Composable
fun EditSessionDialog(
    session: SessionEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
) {
    val initialMinutes = remember(session.id) {
        val minutes = session.durationSec / 60.0
        if (minutes % 1.0 == 0.0) {
            minutes.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", minutes)
        }
    }
    var minutesText by remember(session.id) { mutableStateOf(initialMinutes) }
    var note by remember(session.id) { mutableStateOf(session.note.orEmpty()) }
    var error by remember(session.id) { mutableStateOf<String?>(null) }
    val errorMessage = stringResource(R.string.history_invalid_duration)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.history_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = {
                        minutesText = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.history_duration_label)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(stringResource(R.string.summary_note_label)) },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = minutesText.replace(',', '.').toDoubleOrNull()
                    if (minutes == null || minutes !in 0.1..1440.0) {
                        error = errorMessage
                    } else {
                        onConfirm(minutes, note)
                    }
                },
            ) {
                Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
