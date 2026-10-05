package com.pausa.domain

import org.junit.Assert.*
import org.junit.Test

class ManualDemoTest {
    @Test fun slowCountdownHasNoSkippedSecondsAtPhaseBoundaries() {
        val expected = listOf(1,3,2,1,6,5,4,3,2,1)
        assertEquals(expected, (0..9).map { manualDemoFrame(false,it*1_000L).seconds })
        assertEquals(DemoPhase.CONTRACT,manualDemoFrame(false,1_999).phase)
        assertEquals(DemoPhase.HOLD,manualDemoFrame(false,2_000).phase)
        assertEquals(DemoPhase.RELAX,manualDemoFrame(false,4_000).phase)
        assertEquals(DemoPhase.PREPARE,manualDemoFrame(false,10_000).phase)
    }
    @Test fun quickAndDelayedCallbacksUseRealElapsedTime() {
        assertEquals("05_rapida_contraia.png",manualDemoFrame(true,0).asset)
        assertEquals("06_rapida_relaxe.png",manualDemoFrame(true,1_000).asset)
        assertEquals(DemoPhase.RELAX,manualDemoFrame(true,123_456).phase)
        assertEquals(5,manualDemoFrame(false,25_800).seconds)
        assertEquals(manualDemoFrame(false,0),manualDemoFrame(false,-1))
    }
}
