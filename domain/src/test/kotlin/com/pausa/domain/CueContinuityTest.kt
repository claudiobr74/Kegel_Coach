package com.pausa.domain

import org.junit.Assert.*
import org.junit.Test

class CueContinuityTest {
    @Test fun normalPhaseBoundariesAreAccepted() {
        assertFalse(CueContinuity.interrupted(1000,2000,0,1))
        assertFalse(CueContinuity.interrupted(1000,2450,0,1))
    }
    @Test fun delayedPauseAtCompletionCannotCreditMissedTraining() {
        var now=0L
        val workout=Workout("one","One",listOf(WorkoutBlock(1,1,1)))
        val timer=WorkoutTimer({now},WorkoutSession("id",workout,0,0,SessionStatus.RUNNING))
        val observed=timer.state();now=10000
        val late=timer.state()
        assertEquals(SessionStatus.COMPLETED,late.session.status)
        assertTrue(CueContinuity.interrupted(0,now,observed.phaseIndex,late.phaseIndex))
        val recovered=WorkoutTimer({now},observed.session.copy(status=SessionStatus.PAUSED))
        assertEquals(SessionStatus.PAUSED,recovered.state().session.status)
        assertEquals(0L,recovered.state().session.elapsedMillis)
    }
    @Test fun missingMoreThanOnePhaseInterruptsEvenWithShortClockGap() {
        assertTrue(CueContinuity.interrupted(0,1000,1,3))
        assertTrue(CueContinuity.interrupted(0,1501,1,1))
    }
}
