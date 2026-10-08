package com.pausa.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.pausa.domain.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException
import org.json.*

private val Context.store by preferencesDataStore("preferences")
data class Settings(val user: UserPreferences = UserPreferences(), val workout: Workout = Workout("program", "Programa · semana 1",listOf(ProgressionPlan.weeks[0])),
    val progressionWeek: Int = 0, val progressionStarted: Long = 0)
class PreferencesRepository(private val context: Context) {
    private val onboard = booleanPreferencesKey("onboard")
    private val guide = stringPreferencesKey("guidance")
    private val discreet = booleanPreferencesKey("discreet")
    private val theme = stringPreferencesKey("theme")
    private val progression = booleanPreferencesKey("progression")
    private val preparation = booleanPreferencesKey("preparation")
    private val workout = stringPreferencesKey("workout")
    private val week = intPreferencesKey("week")
    private val started = longPreferencesKey("program_started")
    val settings = context.store.data.catch { error ->
        if(error is IOException) emit(emptyPreferences()) else throw error
    }.map { p ->
        Settings(UserPreferences(p[onboard] ?: false,
            runCatching { Guidance.valueOf(p[guide] ?: "BOTH") }.getOrDefault(Guidance.BOTH),
            p[discreet] ?: false,
            runCatching { AppTheme.valueOf(p[theme] ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM),
            p[progression] ?: true, p[preparation] ?: false),
            runCatching { Codec.workout(JSONObject(p[workout] ?: "")) }.getOrDefault(Workout("program", "Programa · semana 1",listOf(ProgressionPlan.weeks[0]))),
            (p[week] ?: 0).coerceIn(0..3), (p[started] ?: 0).coerceAtLeast(0))
    }
    suspend fun user(value: UserPreferences) { context.store.edit {
        it[onboard]=value.onboardingDone; it[guide]=value.guidance.name; it[discreet]=value.discreetScreen
        it[theme]=value.theme.name; it[progression]=value.progressionEnabled; it[preparation]=value.preparationEnabled
    } }
    suspend fun workout(value: Workout) { context.store.edit { it[workout] = Codec.workout(value).toString() } }
    suspend fun restore(value: Settings) { context.store.edit {
        it[onboard]=value.user.onboardingDone; it[guide]=value.user.guidance.name
        it[discreet]=value.user.discreetScreen; it[theme]=value.user.theme.name
        it[progression]=value.user.progressionEnabled; it[preparation]=value.user.preparationEnabled
        it[workout]=Codec.workout(value.workout).toString(); it[week]=value.progressionWeek
        it[started]=value.progressionStarted
    } }
    suspend fun advance(value: Int) { require(value in 0..3); context.store.edit {
        if(value!=(it[week] ?: 0))it[started]=System.currentTimeMillis()
        it[week]=value
        it[workout]=Codec.workout(Workout("program", "Programa · semana ${value+1}",listOf(ProgressionPlan.weeks[value]))).toString()
    } }
}

object Codec {
    fun workout(w:Workout):JSONObject = JSONObject().put("id",w.id).put("name",w.name)
        .put("sets",w.sets).put("rest",w.restSeconds).put("blocks",JSONArray().also { a ->
            w.blocks.forEach { a.put(JSONObject().put("c",it.contractSeconds).put("r",it.relaxSeconds).put("n",it.repetitions)) }
        })
    fun workout(j:JSONObject):Workout = Workout(j.getString("id"),j.getString("name"),
        j.getJSONArray("blocks").let { a -> (0 until a.length()).map {
            a.getJSONObject(it).let { b -> WorkoutBlock(b.getInt("c"),b.getInt("r"),b.getInt("n")) }
        } },j.getInt("sets"),j.getInt("rest"))
    fun session(s:WorkoutSession,programWeek:Int):String=JSONObject().put("id",s.id).put("workout",workout(s.workout))
        .put("started",s.startedAtMillis).put("elapsed",s.elapsedMillis).put("status",s.status.name).put("week",programWeek).toString()
    fun recoverableSession(raw:String):WorkoutSession = session(raw).also {
        require(it.status in listOf(SessionStatus.RUNNING,SessionStatus.PAUSED))
        require(it.elapsedMillis>=0 && it.elapsedMillis<it.workout.durationMillis)
    }
    fun session(raw:String):WorkoutSession=JSONObject(raw).let {
        WorkoutSession(it.getString("id"),workout(it.getJSONObject("workout")),it.getLong("started"),
            it.getLong("elapsed"),SessionStatus.valueOf(it.getString("status")))
    }
}
