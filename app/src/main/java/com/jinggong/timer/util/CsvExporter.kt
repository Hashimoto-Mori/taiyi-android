package com.jinggong.timer.util

import android.content.Context
import android.net.Uri
import com.jinggong.timer.R
import com.jinggong.timer.data.db.SessionEntity
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.model.PracticeMode
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 把全部练习记录导出为带 UTF-8 BOM 的 CSV。 */
class CsvExporter(
    private val context: Context,
    private val sessionRepository: SessionRepository,
) {
    suspend fun export(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sessions = sessionRepository.getAllOnce()
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Cannot open export file")
            output.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write('\uFEFF'.code)
                    writer.write(context.getString(R.string.csv_header))
                    writer.write("\r\n")
                    sessions.forEach { session ->
                        writer.write(session.toCsvRow())
                        writer.write("\r\n")
                    }
                }
            }
        }
    }

    private fun SessionEntity.toCsvRow(): String {
        val modeLabel = when (PracticeMode.fromDb(mode)) {
            PracticeMode.QUIET -> context.getString(R.string.mode_quiet)
            PracticeMode.STANDING -> context.getString(R.string.mode_standing)
            null -> mode
        }
        return listOf(
            TimeFormatters.formatCsvDate(startTime),
            TimeFormatters.formatCsvTime(startTime),
            modeLabel,
            durationSec.toString(),
            note.orEmpty(),
        ).joinToString(",") { escape(it) }
    }

    private fun escape(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
