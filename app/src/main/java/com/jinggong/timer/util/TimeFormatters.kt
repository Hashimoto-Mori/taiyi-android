package com.jinggong.timer.util

import android.content.Context
import com.jinggong.timer.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 计时、日期与时长格式化。所有「哪一天」都使用设备本地时区。 */
object TimeFormatters {

    private val zone: ZoneId
        get() = ZoneId.systemDefault()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val timeWithSecondsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val dayGroupFormatter = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)

    /** `HH:MM:SS`；小时可以超过两位。 */
    fun formatTimer(elapsedMs: Long): String {
        val totalSeconds = (elapsedMs.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /** 首页/卡片使用的紧凑时长。 */
    fun formatCompactDuration(context: Context, seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        return when {
            safe <= 0L -> context.getString(R.string.today_minutes, 0)
            safe < 60L -> context.getString(R.string.today_less_than_minute)
            safe < 3600L -> context.getString(R.string.today_minutes, (safe / 60L).toInt())
            else -> context.getString(
                R.string.today_hours,
                (safe / 3600L).toInt(),
                ((safe % 3600L) / 60L).toInt(),
            )
        }
    }

    /** 统计页使用的时长，精确到秒（不足一分钟时）。 */
    fun formatStatDuration(context: Context, seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        return when {
            safe < 60L -> context.getString(R.string.stats_seconds_value, safe.toInt())
            safe < 3600L -> context.getString(R.string.stats_minutes_value, (safe / 60L).toInt())
            else -> context.getString(
                R.string.stats_minutes_value_hours,
                (safe / 3600L).toInt(),
                ((safe % 3600L) / 60L).toInt(),
            )
        }
    }

    /** 例如「今天 21:30」「昨天 06:12」「10月18日 21:30」。 */
    fun formatLatestPractice(context: Context, epochMs: Long?): String {
        if (epochMs == null) return context.getString(R.string.latest_none)
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone)
        val time = dateTime.format(timeFormatter)
        val today = LocalDate.now(zone)
        return when (dateTime.toLocalDate()) {
            today -> context.getString(R.string.time_today_at, time)
            today.minusDays(1) -> context.getString(R.string.time_yesterday_at, time)
            else -> context.getString(
                R.string.time_date_at,
                dateTime.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)),
                time,
            )
        }
    }

    fun formatHistoryDay(day: LocalDate): String = day.format(dayGroupFormatter)

    fun formatShortDate(day: LocalDate): String = day.format(shortDateFormatter)

    fun formatClockTime(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone).format(timeFormatter)

    fun formatCsvDate(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone).format(dateFormatter)

    fun formatCsvTime(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone).format(timeWithSecondsFormatter)

    fun formatFileDate(day: LocalDate = LocalDate.now(zone)): String =
        day.format(DateTimeFormatter.BASIC_ISO_DATE)

    fun startOfDayMillis(day: LocalDate): Long =
        day.atStartOfDay(zone).toInstant().toEpochMilli()

    fun dateOf(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    fun today(): LocalDate = LocalDate.now(zone)
}
