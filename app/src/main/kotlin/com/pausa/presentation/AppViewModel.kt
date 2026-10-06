package com.pausa.presentation

import android.app.Application
import androidx.lifecycle.*
import com.pausa.data.*
import com.pausa.domain.*
import com.pausa.reminders.*
import com.pausa.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class AppViewModel(app:Application):AndroidViewModel(app) {
    private val dao=PausaDatabase.get(app).dao()
    private val prefs=PreferencesRepository(app)
    private val lock=Mutex()
    val settings=prefs.settings.map<Settings,Settings?> {it}.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val history=dao.history().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val reminders=dao.reminders().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val session=SessionState.state.asStateFlow()
    val message=SessionState.message.asStateFlow()
    private val feedbackFlow=MutableSharedFlow<String>(extraBufferCapacity=4)
    val feedbackEvents=feedbackFlow.asSharedFlow()
    init {
        viewModelScope.launch {
            if(SessionState.state.value==null) {
                dao.active()?.let { active ->
                    val s=runCatching {Codec.recoverableSession(active.payload)}.getOrNull()
                    if(s==null)dao.deleteActive()
                    if(s!=null && s.status in listOf(SessionStatus.RUNNING,SessionStatus.PAUSED) && SessionState.state.value==null) {
                        SessionState.state.value=WorkoutTimer({android.os.SystemClock.elapsedRealtime()},s.copy(status=SessionStatus.PAUSED)).state()
                        SessionState.message.value="Sessão recuperada em pausa."
                    }
                }
            }
            dao.remindersOnce().forEach {ReminderScheduler(app).schedule(it)}
        }
    }
    fun updateUser(transform:(UserPreferences)->UserPreferences) {viewModelScope.launch {
        lock.withLock {prefs.user(transform(prefs.settings.first().user))}
        if(SessionState.serviceAlive)
            runCatching {WorkoutService.send(getApplication(),WorkoutService.GUIDANCE)}
                .onFailure {SessionState.message.value="Reabra a sessão para aplicar a orientação."}
    }}
    fun selectWorkout(workout:Workout) {viewModelScope.launch {
        runCatching {prefs.workout(workout)}
            .onSuccess {feedbackFlow.emit("Treino salvo: ${workout.name}")}
            .onFailure {feedbackFlow.emit("Não foi possível salvar o treino. Tente novamente.")}
    }}
    fun start(workout:Workout,pocket:Boolean=false) {viewModelScope.launch {
        if(pocket)lock.withLock {prefs.user(prefs.settings.first().user.copy(guidance=Guidance.VIBRATION,discreetScreen=true))}
        runCatching {WorkoutService.send(getApplication(),WorkoutService.START,workout)}
            .onFailure {SessionState.message.value="Não foi possível iniciar o treino. Tente novamente."}
    }}
    fun control(action:String) {runCatching {WorkoutService.send(getApplication(),action)}
        .onFailure {SessionState.message.value="Não foi possível alterar a sessão."}}
    fun feedback(id:String,value:String) {viewModelScope.launch {dao.feedback(id,value)}}
    fun dismissCompleted() {if(session.value?.session?.status==SessionStatus.COMPLETED)SessionState.state.value=null}
    fun clearHistory() {viewModelScope.launch {dao.clearHistory()}}
    fun advance() {viewModelScope.launch {
        val current=prefs.settings.first()
        if(current.progressionWeek<3)prefs.advance(current.progressionWeek+1)
    }}
    fun saveReminder(r:ReminderEntity) {viewModelScope.launch {
        dao.saveReminder(r)
        val scheduler=ReminderScheduler(getApplication())
        if(!r.enabled) {
            scheduler.cancel(r)
            getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(r.id,200)
        } else {scheduler.cancel(r);scheduler.schedule(r)}
        feedbackFlow.emit(if(r.enabled)"Lembrete salvo" else "Lembrete desativado")
    }}
    fun removeReminder(r:ReminderEntity) {viewModelScope.launch {
        ReminderScheduler(getApplication()).cancel(r);dao.deleteReminder(r.id)
        getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(r.id,200)
        feedbackFlow.emit("Lembrete removido")
    }}
    fun newReminder(hour:Int,minute:Int,mask:Int)=saveReminder(ReminderEntity(UUID.randomUUID().toString(),hour,minute,mask))
}
