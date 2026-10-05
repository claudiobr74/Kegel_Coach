package com.pausa.service

import android.content.Context
import android.os.*
import androidx.test.core.app.ApplicationProvider
import com.pausa.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class HapticGuidanceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    @Test fun everyPhaseUsesAlarmAttributesAndNeverRepeats() {
        shadowOf(vibrator).setHasVibrator(true)
        val guidance=HapticGuidance(context)
        for (phase in Phase.entries) {
            assertTrue(guidance.play(phase))
            assertArrayEquals(HapticGuidance.pattern(phase), shadowOf(vibrator).pattern)
            assertEquals(-1,shadowOf(vibrator).repeat)
            val attributes=shadowOf(vibrator).vibrationAttributesFromLastVibration as VibrationAttributes
            assertEquals(VibrationAttributes.USAGE_ALARM,attributes.usage)
        }
    }
    @Test fun previewUsesSameBackgroundCompatibleAttributes() {
        shadowOf(vibrator).setHasVibrator(true)
        assertTrue(HapticGuidance(context).preview())
        assertEquals(VibrationAttributes.USAGE_ALARM,
            (shadowOf(vibrator).vibrationAttributesFromLastVibration as VibrationAttributes).usage)
    }
    @Test fun cancellationStopsCue() {
        shadowOf(vibrator).setHasVibrator(true)
        val guidance=HapticGuidance(context)
        guidance.play(Phase.CONTRACT);guidance.cancel()
        assertTrue(shadowOf(vibrator).isCancelled)
    }
    @Test fun missingHardwareDoesNotPretendToDeliverCue() {
        shadowOf(vibrator).setHasVibrator(false)
        assertFalse(HapticGuidance(context).play(Phase.CONTRACT))
    }
    @Test fun onlyGuidanceChangesDuringRunningSessionTriggerNewCue() {
        assertTrue(HapticGuidance.changedDuringRunning(Guidance.SCREEN,Guidance.VIBRATION,SessionStatus.RUNNING))
        assertFalse(HapticGuidance.changedDuringRunning(Guidance.BOTH,Guidance.BOTH,SessionStatus.RUNNING))
        assertFalse(HapticGuidance.changedDuringRunning(Guidance.SCREEN,Guidance.VIBRATION,SessionStatus.PAUSED))
        assertFalse(HapticGuidance.changedDuringRunning(Guidance.SCREEN,Guidance.VIBRATION,SessionStatus.COMPLETED))
    }
}
