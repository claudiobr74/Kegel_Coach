package com.pausa.domain

import java.time.*

data class WorkoutBlock(val contractSeconds: Int, val relaxSeconds: Int, val repetitions: Int) {
    init {
        require(contractSeconds in 1..30 && relaxSeconds in 1..30)
        require(repetitions in 1..50)
    }
}

data class Workout(
    val id: String,
    val name: String,
    val blocks: List<WorkoutBlock>,
    val sets: Int = 1,
    val restSeconds: Int = 30
) {
    init { require(blocks.isNotEmpty() && blocks.size <= 2); require(sets in 1..10); require(restSeconds in 10..300) }
    val durationMillis: Long get() = timeline().sumOf { it.durationMillis }
    val contractions: Int get() = blocks.sumOf { it.repetitions } * sets
    fun timeline(): List<WorkoutPhase> = buildList {
        for (set in 1..sets) {
            var rep = 0
            for (block in blocks) repeat(block.repetitions) {
                rep++
                add(WorkoutPhase(Phase.CONTRACT, block.contractSeconds * 1000L, set, rep))
                add(WorkoutPhase(Phase.RELAX, block.relaxSeconds * 1000L, set, rep))
            }
            if (set < sets) add(WorkoutPhase(Phase.REST, restSeconds * 1000L, set, rep))
        }
    }
}

enum class Phase { CONTRACT, RELAX, REST, FINISHED }
data class WorkoutPhase(val phase: Phase, val durationMillis: Long, val set: Int, val repetition: Int)
object WorkoutPreset {
    val beginner = Workout("beginner", "Iniciante", listOf(WorkoutBlock(3, 6, 10)))
    val intermediate = Workout("intermediate", "Intermediário", listOf(WorkoutBlock(5, 5, 10)), 2)
    val advanced = Workout("advanced", "Avançado", listOf(WorkoutBlock(8, 8, 10)), 3)
    val quick = Workout("quick", "Contrações rápidas", listOf(WorkoutBlock(1, 1, 10)))
    val mixed = Workout("mixed", "Treino misto", listOf(WorkoutBlock(3, 6, 10), WorkoutBlock(1, 1, 10)))
    val all = listOf(beginner, intermediate, advanced, quick, mixed)
}

enum class SessionStatus { RUNNING, PAUSED, COMPLETED, CANCELLED }
data class WorkoutSession(
    val id: String, val workout: Workout, val startedAtMillis: Long,
    val elapsedMillis: Long, val status: SessionStatus
)
data class TimerState(
    val session: WorkoutSession, val phase: Phase, val phaseIndex: Int,
    val remainingMillis: Long, val phaseDurationMillis: Long, val set: Int, val repetition: Int
) {
    val secondsRemaining: Int get() = ((remainingMillis + 999) / 1000).toInt()
    val progress: Float get() = (session.elapsedMillis.toFloat() / session.workout.durationMillis).coerceIn(0f, 1f)
}

/** Clock must be monotonic (SystemClock.elapsedRealtime on Android), never wall clock. */
fun interface MonotonicClock { fun nowMillis(): Long }
class WorkoutTimer(private val clock: MonotonicClock, session: WorkoutSession) {
    private var saved = session
    private var anchor = clock.nowMillis()
    private val phases = session.workout.timeline()
    init { require(session.elapsedMillis in 0..session.workout.durationMillis) }
    fun state(): TimerState {
        val elapsed = if (saved.status == SessionStatus.RUNNING)
            (saved.elapsedMillis + (clock.nowMillis() - anchor).coerceAtLeast(0)).coerceAtMost(saved.workout.durationMillis)
        else saved.elapsedMillis
        val status = if (saved.status == SessionStatus.RUNNING && elapsed == saved.workout.durationMillis)
            SessionStatus.COMPLETED else saved.status
        val snapshot = saved.copy(elapsedMillis = elapsed, status = status)
        var boundary = 0L
        phases.forEachIndexed { index, p ->
            boundary += p.durationMillis
            if (elapsed < boundary) return TimerState(snapshot, p.phase, index, boundary - elapsed, p.durationMillis, p.set, p.repetition)
        }
        return TimerState(snapshot, Phase.FINISHED, phases.size, 0, 0, saved.workout.sets, phases.last().repetition)
    }
    fun pause(): TimerState {
        saved = state().session.let { if (it.status == SessionStatus.RUNNING) it.copy(status = SessionStatus.PAUSED) else it }
        return state()
    }
    fun resume(): TimerState {
        if (saved.status == SessionStatus.PAUSED) { saved = saved.copy(status = SessionStatus.RUNNING); anchor = clock.nowMillis() }
        return state()
    }
    fun cancel(): TimerState { saved = state().session.copy(status = SessionStatus.CANCELLED); return state() }
}

data class WorkoutHistory(val id: String, val date: LocalDate, val durationMillis: Long, val contractions: Int, val feedback: String? = null)
data class Reminder(val id: String, val hour: Int, val minute: Int, val days: Set<DayOfWeek>, val enabled: Boolean = true) {
    init { require(hour in 0..23 && minute in 0..59); require(days.isNotEmpty()) }
    /** ZonedDateTime resolves DST gaps/overlaps; an alarm always lies strictly in the future. */
    fun nextAfter(now: ZonedDateTime): ZonedDateTime =
        (0..7).map { now.toLocalDate().plusDays(it.toLong()).atTime(hour, minute).atZone(now.zone) }
            .first { it.dayOfWeek in days && it.isAfter(now) }
}
enum class Guidance { SCREEN, VIBRATION, BOTH, SOUND, VOICE }
enum class AppTheme { SYSTEM, LIGHT, DARK }
data class UserPreferences(val onboardingDone: Boolean = false, val guidance: Guidance = Guidance.BOTH,
    val discreetScreen: Boolean = false, val theme: AppTheme = AppTheme.SYSTEM, val progressionEnabled: Boolean = true, val preparationEnabled: Boolean = false)
object ProgressionPlan {
    val weeks = listOf(WorkoutBlock(3,6,10), WorkoutBlock(4,6,10), WorkoutBlock(5,5,10), WorkoutBlock(6,6,10))
    fun suggestion(week: Int, completionsThisWeek: Int, tolerated: Boolean = true): WorkoutBlock? =
        if (tolerated && completionsThisWeek >= 7 && week in 0..2) weeks[week + 1] else null
}
object Progress {
    fun streak(dates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in dates) today else today.minusDays(1)
        var count = 0
        while (cursor in dates) { count++; cursor = cursor.minusDays(1) }
        return count
    }
    fun bestStreak(dates: Set<LocalDate>): Int {
        var best = 0; var current = 0; var previous: LocalDate? = null
        for (date in dates.sorted()) {
            current = if (previous?.plusDays(1) == date) current + 1 else 1
            best = maxOf(best, current); previous = date
        }
        return best
    }
}
