package com.pausa.domain

import kotlin.test.*
import java.time.*

class WorkoutTest {
    @Test fun durationsIncludeOnlyRealPhases() {
        assertEquals(90_000L, WorkoutPreset.beginner.durationMillis)
        assertEquals(230_000L, WorkoutPreset.intermediate.durationMillis)
        assertEquals(540_000L, WorkoutPreset.advanced.durationMillis)
        assertEquals(20_000L, WorkoutPreset.quick.durationMillis)
        assertEquals(110_000L, WorkoutPreset.mixed.durationMillis)
    }
    @Test fun maxAndInvalidSettings() {
        assertEquals(32700000L, Workout("max","Max",listOf(WorkoutBlock(30,30,50)),10,300).durationMillis)
        assertFailsWith<IllegalArgumentException> { WorkoutBlock(0, 5, 10) }
        assertFailsWith<IllegalArgumentException> { WorkoutBlock(5, 31, 10) }
        assertFailsWith<IllegalArgumentException> { WorkoutBlock(5, 5, 51) }
        assertFailsWith<IllegalArgumentException> { Workout("x","x",emptyList()) }
        assertFailsWith<IllegalArgumentException> { Workout("x","x",listOf(WorkoutBlock(1,1,1)),11) }
    }
    @Test fun realElapsedAndBoundaryTransitions() {
        var now = 100L
        val timer = WorkoutTimer({ now }, session(WorkoutPreset.beginner))
        assertEquals(Phase.CONTRACT, timer.state().phase)
        now += 2999
        assertEquals(1, timer.state().secondsRemaining)
        now++
        assertEquals(Phase.RELAX, timer.state().phase)
        now += 6000
        assertEquals(2, timer.state().repetition)
        assertEquals(Phase.CONTRACT, timer.state().phase)
        now += 41000 // delayed callback/background: phase uses real time
        assertEquals(50_000L, timer.state().session.elapsedMillis)
        assertEquals(Phase.RELAX, timer.state().phase)
    }
    @Test fun pauseResumeAndRecovery() {
        var now = 0L
        val timer = WorkoutTimer({ now }, session(WorkoutPreset.beginner))
        now = 1500; val paused = timer.pause()
        now = 99999
        assertEquals(1500L, timer.state().session.elapsedMillis)
        val restored = WorkoutTimer({ now }, paused.session)
        restored.resume(); now += 1500
        assertEquals(Phase.RELAX, restored.state().phase)
        restored.pause(); restored.pause()
        restored.resume(); restored.resume()
        now += 6000
        assertEquals(2, restored.state().repetition)
    }
    @Test fun restOnlyBetweenSetsAndCompletionAfterLastRelaxation() {
        var now = 0L
        val w = Workout("x","x",listOf(WorkoutBlock(1,1,1)),2,10)
        val timer = WorkoutTimer({now},session(w))
        now=2000; assertEquals(Phase.REST,timer.state().phase)
        now=12000; assertEquals(2,timer.state().set)
        now=13000; assertEquals(SessionStatus.RUNNING,timer.state().session.status)
        now=14000; assertEquals(SessionStatus.COMPLETED,timer.state().session.status)
        assertEquals(Phase.FINISHED,timer.state().phase)
        now=20000; assertEquals(14000L,timer.state().session.elapsedMillis)
    }
    @Test fun cancellationNeverCompletes() {
        var now=0L
        val timer=WorkoutTimer({now},session(WorkoutPreset.quick))
        now=2000;timer.cancel();now=90000
        assertEquals(SessionStatus.CANCELLED,timer.state().session.status)
    }
    @Test fun progressionRequiresAdherenceAndExplicitNextWeek() {
        assertNull(ProgressionPlan.suggestion(0,6))
        assertEquals(WorkoutBlock(4,6,10),ProgressionPlan.suggestion(0,7))
        assertNull(ProgressionPlan.suggestion(3,7))
    }
    @Test fun recentDifficultyPreventsProgressionSuggestion() {
        assertNull(ProgressionPlan.suggestion(0,7,tolerated=false))
        assertNotNull(ProgressionPlan.suggestion(0,7,tolerated=true))
    }
    @Test fun streakDeduplicatesDaysAndAllowsTodayPending() {
        val today=LocalDate.of(2026,10,5)
        val dates=setOf(today.minusDays(1),today.minusDays(2),today.minusDays(4))
        assertEquals(2,Progress.streak(dates,today))
        assertEquals(2,Progress.bestStreak(dates))
    }
    @Test fun reminderNextDayAndWeekRollover() {
        val now=ZonedDateTime.of(2026,10,5,8,0,0,0,ZoneId.of("America/Sao_Paulo"))
        val r=Reminder("x",8,0,setOf(DayOfWeek.MONDAY))
        assertEquals(now.plusWeeks(1),r.nextAfter(now))
        assertEquals(now.plusHours(1),r.copy(hour=9).nextAfter(now))
        assertFailsWith<IllegalArgumentException>{r.copy(days=emptySet())}
    }
    @Test fun daylightSavingGapResolvesToValidFutureInstant() {
        val now=ZonedDateTime.of(2026,3,8,0,0,0,0,ZoneId.of("America/New_York"))
        val next=Reminder("dst",2,30,setOf(DayOfWeek.SUNDAY)).nextAfter(now)
        assertEquals(3,next.hour)
        assertTrue(next.isAfter(now))
    }
    private fun session(w:Workout)=WorkoutSession("test",w,0,0,SessionStatus.RUNNING)
}
