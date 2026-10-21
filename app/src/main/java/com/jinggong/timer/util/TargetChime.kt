package com.jinggong.timer.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 目标时长到达时的轻柔提示：短振动 + 两声轻音，不打断练习。 */
object TargetChime {

    fun play(context: Context) {
        runCatching { vibrate(context) }
        runCatching { tone() }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return

        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0L, 180L, 120L, 180L), -1),
        )
    }

    private fun tone() {
        val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35)
        generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
        Handler(Looper.getMainLooper()).postDelayed(
            { runCatching { generator.release() } },
            900L,
        )
    }
}
