package com.pausa.service

import android.content.Context
import android.media.AudioAttributes
import android.os.*
import com.pausa.domain.*

/** Timed training cues are alarms, not touch feedback; this also permits background delivery. */
internal class HapticGuidance(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else context.getSystemService(Vibrator::class.java)

    fun available():Boolean = vibrator?.hasVibrator()==true
    fun play(phase: Phase): Boolean = play(pattern(phase))
    @Suppress("DEPRECATION")
    fun play(pattern: LongArray): Boolean {
        val device = vibrator?.takeIf { it.hasVibrator() } ?: return false
        val effect = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= 33) {
            device.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            device.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
        return true
    }
    fun preview() = play(longArrayOf(0,150,1000,150,120,150,1000,600,1000,150,120,150,120,600))
    fun cancel() { vibrator?.cancel() }

    companion object {
        fun pattern(phase: Phase): LongArray = when (phase) {
            Phase.CONTRACT -> longArrayOf(0,150)
            Phase.RELAX -> longArrayOf(0,150,120,150)
            Phase.REST -> longArrayOf(0,600)
            Phase.FINISHED -> longArrayOf(0,150,120,150,120,600)
        }
        fun changedDuringRunning(before: Guidance, after: Guidance, status: SessionStatus?) =
            before != after && status == SessionStatus.RUNNING
    }
}
