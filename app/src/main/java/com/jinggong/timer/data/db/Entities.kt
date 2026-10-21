package com.jinggong.timer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次已完成的练习记录。 */
@Entity(
    tableName = "sessions",
    indices = [Index("startTime"), Index("mode")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val startTime: Long,
    val endTime: Long,
    val durationSec: Int,
    val audioTitle: String?,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 已导入到应用私有目录的音频。 */
@Entity(tableName = "audio_files")
data class AudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val fileName: String,
    val modeScope: String,
    val addedAt: Long = System.currentTimeMillis(),
)

/** 按本地日期 + 模式聚合的每日练习。 */
data class DailyModeTotal(
    val day: String,
    val mode: String,
    val totalSec: Long,
    val sessionCount: Int,
)

/** 指定时间区间内按模式聚合的练习。 */
data class ModeTotal(
    val mode: String,
    val totalSec: Long,
    val count: Int,
)

/** 指定时间区间内按日期聚合的练习。 */
data class DailyTotal(
    val day: String,
    val totalSec: Long,
    val sessionCount: Int,
)
