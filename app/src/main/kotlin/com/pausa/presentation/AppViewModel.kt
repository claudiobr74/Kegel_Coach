package com.pausa.presentation

import android.app.Application
import android.net.Uri
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
import org.json.JSONObject

class AppViewModel(app:Application):AndroidViewModel(app) {
    private val repository=AppRepository(app)
    private val dao=repository.dao
    private val prefs=repository.preferences
    private val lock=Mutex()
    val settings=prefs.settings.map<Settings,Settings?> {it}.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val history=dao.history().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val reminders=dao.reminders().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val savedWorkouts=dao.savedWorkouts().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val session=SessionState.state.asStateFlow()
    val sessionOptions=SessionState.options.asStateFlow()
    val message=SessionState.message.asStateFlow()
    val saving=MutableStateFlow(false)
    val operationError=MutableStateFlow<String?>(null)
    val reminderSaved=MutableStateFlow(0)
    val backupPreview=MutableStateFlow<BackupSnapshot?>(null)
    private var transferPassword:String?=null
    fun prepareBackupTransfer(password:String) {transferPassword=password}
    fun finishBackupTransfer(uri:Uri?,restore:Boolean) {
        val password=transferPassword;transferPassword=null
        if(uri==null)return
        if(password==null)operation("") {throw IllegalStateException("A seleção foi interrompida. Escolha o arquivo novamente.")}
        else if(restore)previewBackup(uri,password) else exportBackup(uri,password)
    }
    private val feedbackFlow=MutableSharedFlow<String>(extraBufferCapacity=4)
    val feedbackEvents=feedbackFlow.asSharedFlow()
    private fun operation(success:String,block:suspend ()->Unit) {
        if(saving.value)return
        saving.value=true;operationError.value=null
        viewModelScope.launch {
            try {block();if(success.isNotBlank())feedbackFlow.emit(success)}
            catch(e:Exception) {
                if(e is CancellationException)throw e
                val text=when(e) {is IllegalArgumentException,is IllegalStateException->e.message ?: "Não foi possível concluir. Tente novamente."
                    else->"Não foi possível concluir. Tente novamente."}
                operationError.value=text;feedbackFlow.emit(text)
                Diagnostics.record(getApplication(),"Operação local não concluída")
            } finally {saving.value=false}
        }
    }
    init {
        operation("") {
            if(SessionState.state.value==null)dao.active()?.let {active->
                val s=runCatching {Codec.recoverableSession(active.payload)}.getOrNull()
                if(s==null)dao.deleteActive()
                else if(SessionState.state.value==null) {
                    SessionState.state.value=WorkoutTimer({android.os.SystemClock.elapsedRealtime()},s.copy(status=SessionStatus.PAUSED)).state()
                    val user=prefs.settings.first().user;val j=JSONObject(active.payload)
                    SessionState.options.value=user.copy(
                        guidance=runCatching {Guidance.valueOf(j.optString("guidance",user.guidance.name))}.getOrDefault(user.guidance),
                        discreetScreen=j.optBoolean("discreet",user.discreetScreen))
                    SessionState.message.value="Sessão recuperada em pausa."
                }
            }
            dao.remindersOnce().forEach {ReminderScheduler(app).schedule(it)}
        }
    }
    fun updateUser(transform:(UserPreferences)->UserPreferences)=operation("") {
        val (before,user)=lock.withLock {
            val previous=prefs.settings.first().user;val updated=transform(previous)
            prefs.user(updated);previous to updated
        }
        if(SessionState.serviceAlive) {
            val active=SessionState.options.value ?: before
            WorkoutService.send(getApplication(),WorkoutService.GUIDANCE,options=user.copy(
                guidance=if(user.guidance!=before.guidance)user.guidance else active.guidance,
                discreetScreen=if(user.discreetScreen!=before.discreetScreen)user.discreetScreen else active.discreetScreen))
        }
    }
    fun selectWorkout(workout:Workout,onSuccess:()->Unit={})=operation("Treino selecionado: ${workout.name}") {prefs.workout(workout);onSuccess()}
    fun saveWorkout(workout:Workout,onSuccess:()->Unit={})=operation("Treino salvo: ${workout.name}") {
        require(dao.savedWorkoutsOnce().any {it.id==workout.id} || dao.savedWorkoutsOnce().size<100) {"A biblioteca já possui 100 treinos. Remova um para adicionar outro."}
        require(workout.name.isNotBlank() && workout.name.length<=80)
        val w=if(workout.id.startsWith("saved-"))workout else workout.copy(id="saved-${UUID.randomUUID()}")
        dao.saveWorkout(SavedWorkoutEntity(w.id,w.name,Codec.workout(w).toString()));prefs.workout(w);onSuccess()
    }
    fun removeWorkout(id:String)=operation("Treino removido da biblioteca") {dao.deleteWorkout(id)}
    fun start(workout:Workout,pocket:Boolean=false) {
        if(saving.value) {SessionState.message.value="Aguarde a operação atual antes de iniciar.";return}
        if((pocket || settings.value?.user?.guidance==Guidance.VIBRATION) && !HapticGuidance(getApplication()).available()) {
            SessionState.message.value="Este aparelho não possui vibração. Escolha orientação por tela ou som.";return
        }
        runCatching {WorkoutService.send(getApplication(),WorkoutService.START,workout,pocket)}
            .onFailure {SessionState.message.value="Não foi possível iniciar o treino. Tente novamente."}
    }
    fun sessionDiscreet() { val user=settings.value?.user ?: return; changeSessionOptions(user) {it.copy(discreetScreen=!it.discreetScreen)} }
    fun sessionScreenGuide() {val user=settings.value?.user ?: return;changeSessionOptions(user) {it.copy(guidance=Guidance.SCREEN,discreetScreen=false)}}
    fun sessionPocket() {
        if(!HapticGuidance(getApplication()).available()) {SessionState.message.value="Este aparelho não possui vibração. Mantenha orientação por tela ou som.";return}
        val user=settings.value?.user ?: return; changeSessionOptions(user) {it.copy(guidance=Guidance.VIBRATION,discreetScreen=true)} }
    private fun changeSessionOptions(default:UserPreferences,transform:(UserPreferences)->UserPreferences) {
        val user=transform(SessionState.options.value ?: default)
        SessionState.options.value=user
        if(SessionState.serviceAlive)runCatching {WorkoutService.send(getApplication(),WorkoutService.GUIDANCE,options=user)}
            .onFailure {SessionState.message.value="Não foi possível alterar a orientação."}
        else operation("") {
            dao.active()?.let {active->
                val payload=JSONObject(active.payload).put("guidance",user.guidance.name).put("discreet",user.discreetScreen).toString()
                dao.saveActive(active.copy(payload=payload))
            }
        }
    }
    fun control(action:String) {
        if(saving.value && action==WorkoutService.RESUME) {SessionState.message.value="Aguarde a operação atual.";return}
        runCatching {WorkoutService.send(getApplication(),action)}
        .onFailure {SessionState.message.value="Não foi possível alterar a sessão."}}
    fun feedback(id:String,value:String)=operation("Avaliação registrada") {dao.feedback(id,value)}
    fun dismissCompleted() {if(session.value?.session?.status==SessionStatus.COMPLETED){SessionState.state.value=null;SessionState.options.value=null}}
    fun clearHistory()=operation("Histórico excluído") {dao.clearHistory()}
    fun changeWeek(week:Int)=operation("Programa atualizado") {require(week in 0..3);prefs.advance(week)}
    fun advance()=changeWeek((settings.value?.progressionWeek ?: 0)+1)
    fun resumeProgram()=changeWeek(settings.value?.progressionWeek ?: 0)
    fun saveReminder(r:ReminderEntity)=operation("Lembrete salvo") {
        r.domain();require(r.daysMask in 1..127)
        require(dao.reminder(r.id)!=null || dao.remindersOnce().size<100) {"Você já possui 100 lembretes."}
        val saved=r.copy(snoozedUntil=0)
        dao.saveReminder(saved)
        val scheduler=ReminderScheduler(getApplication());scheduler.cancel(saved)
        if(saved.enabled)try {scheduler.schedule(saved)} catch(e:Exception) {
            dao.saveReminder(saved.copy(enabled=false))
            throw IllegalStateException("Horário salvo, mas o aviso não foi agendado. Ative-o para tentar novamente.",e)
        }
        getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(r.id,200)
        reminderSaved.value++
    }
    fun removeReminder(r:ReminderEntity)=operation("Lembrete removido") {
        dao.deleteReminder(r.id);ReminderScheduler(getApplication()).cancel(r)
        getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(r.id,200)
    }
    fun newReminder(hour:Int,minute:Int,mask:Int)=saveReminder(ReminderEntity(UUID.randomUUID().toString(),hour,minute,mask))
    fun testReminder()=operation("Aviso de teste enviado") {
        ReminderScheduler.notificationStatus(getApplication())?.let {throw IllegalStateException(it)}
        ReminderScheduler(getApplication()).notify(ReminderEntity("test",8,0))
    }
    fun exportDiagnostics(uri:Uri)=operation("Diagnóstico exportado") {
        withContext(Dispatchers.IO) {
            val text="Kegel Coach ${com.pausa.BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.SDK_INT}\n"+Diagnostics.read(getApplication())
            getApplication<Application>().contentResolver.openOutputStream(uri,"wt")?.use {it.write(text.toByteArray(Charsets.UTF_8))} ?: error("Não foi possível abrir o arquivo.")
        }
    }
    fun exportBackup(uri:Uri,password:String)=operation("Cópia protegida exportada") {
        withContext(Dispatchers.IO) {
            val bytes=BackupCodec.encrypt(repository.snapshot(),password)
            getApplication<Application>().contentResolver.openOutputStream(uri,"wt")?.use {it.write(bytes)} ?: error("Não foi possível abrir o arquivo.")
        }
    }
    fun previewBackup(uri:Uri,password:String)=operation("") {
        withContext(Dispatchers.IO) {
            check(dao.active()==null) {"Encerre o treino ativo antes de restaurar uma cópia."}
            val bytes=getApplication<Application>().contentResolver.openInputStream(uri)?.use {stream->
                val result=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val count=stream.read(buffer);if(count<0)break
                    require(result.size()+count<=BackupCodec.MAX_BYTES) {"Arquivo muito grande."};result.write(buffer,0,count)}
                result.toByteArray()
            } ?: error("Não foi possível abrir o arquivo.")
            backupPreview.value=BackupCodec.decrypt(bytes,password)
        }
    }
    fun restoreBackup()=operation("Dados restaurados") {
        val backup=backupPreview.value ?: return@operation
        val previous=dao.remindersOnce()
        repository.restore(backup)
        previous.forEach {
            ReminderScheduler(getApplication()).cancel(it)
            getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(it.id,200)
        }
        backupPreview.value=null
        backup.reminders.forEach {r->runCatching {ReminderScheduler(getApplication()).schedule(r)}.onFailure {
            dao.saveReminder(r.copy(enabled=false));feedbackFlow.emit("Dados restaurados. Um lembrete precisa ser reativado.")
        }}
    }
    fun cancelRestore() {backupPreview.value=null}
}
