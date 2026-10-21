package com.jinggong.timer.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.jinggong.timer.R
import com.jinggong.timer.data.db.AudioDao
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.model.AudioScope
import com.jinggong.timer.model.PracticeMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 管理应用私有目录中的音频文件。
 *
 * 导入时立即把 SAF 选中的文件拷贝到 `filesDir/audio/`，之后播放只依赖普通 File，
 * 不再依赖临时的 URI 权限。
 */
class AudioRepository(
    private val context: Context,
    private val dao: AudioDao,
) {
    fun observeAll(): Flow<List<AudioEntity>> = dao.observeAll()

    fun observeForMode(mode: PracticeMode): Flow<List<AudioEntity>> =
        dao.observeForMode(mode.dbValue)

    suspend fun get(id: Long): AudioEntity? = dao.getById(id)

    suspend fun getForModeOnce(mode: PracticeMode): List<AudioEntity> =
        dao.getForModeOnce(mode.dbValue)

    fun fileFor(audio: AudioEntity): File = File(audioDir(), audio.fileName)

    fun fileExists(audio: AudioEntity): Boolean = fileFor(audio).isFile

    suspend fun import(
        uri: Uri,
        displayName: String,
        scope: AudioScope,
    ): Result<AudioEntity> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var destination: File? = null
        runCatching {
            val originalName = queryDisplayName(uri).orEmpty()
            val name = displayName.trim().ifBlank {
                originalName.substringBeforeLast('.').ifBlank {
                    context.getString(R.string.audio_untitled)
                }
            }
            val extension = resolveExtension(originalName, resolver.getType(uri))
            destination = File(
                audioDir(),
                "audio_${System.currentTimeMillis()}_" +
                    "${UUID.randomUUID().toString().take(8)}.$extension",
            )
            val target = destination ?: error("Cannot create audio file")
            val input = resolver.openInputStream(uri)
                ?: error("Cannot read selected file")
            input.use { source ->
                target.outputStream().buffered().use { output ->
                    source.copyTo(output)
                }
            }
            check(target.length() > 0L) { "Audio file is empty" }

            val entity = AudioEntity(
                displayName = name.take(80),
                fileName = target.name,
                modeScope = scope.dbValue,
            )
            entity.copy(id = dao.insert(entity))
        }.onFailure {
            destination?.delete()
        }
    }

    suspend fun rename(audio: AudioEntity, displayName: String) {
        val name = displayName.trim().take(80)
        if (name.isNotEmpty()) dao.rename(audio.id, name)
    }

    suspend fun delete(audio: AudioEntity) {
        dao.delete(audio)
        fileFor(audio).delete()
    }

    /** 清理数据库仍存在但磁盘文件已丢失的记录。 */
    suspend fun removeMissingFiles(): Int {
        var removed = 0
        dao.getAllOnce().forEach { audio ->
            if (!fileExists(audio)) {
                dao.delete(audio)
                removed++
            }
        }
        return removed
    }

    private fun audioDir(): File =
        File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    }

    private fun resolveExtension(originalName: String, mimeType: String?): String {
        val fromName = originalName.substringAfterLast('.', "").lowercase()
        if (fromName in SUPPORTED_EXTENSIONS) return fromName
        val fromMime = mimeType
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.lowercase()
        if (fromMime in SUPPORTED_EXTENSIONS) return fromMime!!
        return when (mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/aac" -> "aac"
            else -> "audio"
        }
    }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "mp4", "3gp",
        )
    }
}
