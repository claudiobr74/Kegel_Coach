package com.pausa.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pausa.MainActivity
import com.pausa.R
import com.pausa.data.*
import com.pausa.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.time.*
import java.util.UUID

object SessionState {
    val state = MutableStateFlow<TimerState?>(null)
    val message = MutableStateFlow<String?>(null)
    val options = MutableStateFlow<UserPreferences?>(null)
    var serviceAlive = false
}

class WorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, _ ->
        SessionState.message.value="A sessão foi interrompida. Reabra o app para retomar."
        releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    })
    private val commands = Channel<Intent>(Channel.UNLIMITED)
    private var timer: WorkoutTimer? = null
    private var ticker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var dao: PausaDao
    private val haptics by lazy { HapticGuidance(this) }
    private var settings = Settings()
    private var programWeek = -1
    private var lastCheckpoint: WorkoutSession? = null
    private var tone: ToneGenerator? = null
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var audioFocus: AudioFocusRequest? = null
    private var lastObserved: TimerState? = null
    private var lastObservedAt = 0L

    override fun onCreate() {
        super.onCreate()
        SessionState.serviceAlive=true
        dao = PausaDatabase.get(this).dao()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Sessão em andamento", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null,null); enableVibration(false); lockscreenVisibility=Notification.VISIBILITY_PRIVATE
        })
        ServiceCompat.startForeground(this, NOTIFICATION, notification(false),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        scope.launch {
            for (command in commands) try { handle(command) } catch (e: Exception) {
                if(e is CancellationException) throw e
                ticker?.cancel(); releaseWake()
                SessionState.message.value = "Não foi possível manter a sessão. Abra o app para retomar."
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent != null) commands.trySend(intent) else stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent:Intent?):IBinder?=null

    private suspend fun handle(intent:Intent) {
        val previousGuidance = settings.user.guidance
        settings = PreferencesRepository(this).settings.first()
        SessionState.options.value?.let { settings=settings.copy(user=it) }
        when(intent.action) {
            START -> {
                if(timer != null) return
                val existing = dao.active()
                if(existing != null) {
                    if(!restore(existing))return
                    SessionState.message.value="Seu treino anterior está pausado. Retome ou encerre."
                    publish(); return
                }
                val workout=Codec.workout(JSONObject(intent.getStringExtra("workout") ?: return))
                SessionState.options.value=PreferencesRepository(this).settings.first().user.let {
                    if(intent.getBooleanExtra("pocket",false))it.copy(guidance=Guidance.VIBRATION,discreetScreen=true) else it
                }
                settings=settings.copy(user=SessionState.options.value!!)
                programWeek=if(workout.id=="program") settings.progressionWeek else -1
                timer=WorkoutTimer({SystemClock.elapsedRealtime()},WorkoutSession(UUID.randomUUID().toString(),workout,System.currentTimeMillis(),0,SessionStatus.RUNNING))
                checkpoint(); begin()
            }
            RESUME -> {
                if(timer?.state()?.session?.status==SessionStatus.RUNNING)return
                if(timer==null) {
                    val active=dao.active()
                    if(active==null){ stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return }
                    if(!restore(active))return
                }
                timer?.resume(); checkpoint();begin()
            }
            PAUSE -> {
                ticker?.cancel()
                val current=timer?.state()
                val previous=lastObserved
                if(previous!=null && current!=null && previous.session.status==SessionStatus.RUNNING && current.session.status in listOf(SessionStatus.RUNNING,SessionStatus.COMPLETED) &&
                    CueContinuity.interrupted(lastObservedAt,SystemClock.elapsedRealtime(),previous.phaseIndex,current.phaseIndex)) {
                    timer=WorkoutTimer({SystemClock.elapsedRealtime()},previous.session.copy(status=SessionStatus.PAUSED))
                    SessionState.message.value="O ritmo foi interrompido. Retome para continuar com segurança."
                } else timer?.pause()
                releaseWake();releaseAudioFocus();haptics.cancel();tone?.stopTone();speech?.stop()
                val state=timer?.state()
                if(state?.session?.status==SessionStatus.COMPLETED)finish(state) else {checkpoint();publish()}
            }
            CANCEL -> {
                ticker?.cancel();timer?.cancel();releaseWake();releaseAudioFocus();haptics.cancel();tone?.stopTone();speech?.stop()
                dao.deleteActive();SessionState.state.value=null;SessionState.options.value=null
                stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
            }
            GUIDANCE -> {
                val user=settings.user
                settings=settings.copy(user=user.copy(
                    guidance=intent.getStringExtra("guidance")?.let {Guidance.valueOf(it)} ?: user.guidance,
                    discreetScreen=if(intent.hasExtra("discreet"))intent.getBooleanExtra("discreet",false) else user.discreetScreen))
                SessionState.options.value=settings.user
                checkpoint()
                if (previousGuidance != settings.user.guidance) {
                    haptics.cancel(); tone?.stopTone(); speech?.stop()
                    val current = timer?.state()
                    releaseAudioFocus()
                    if(current?.session?.status==SessionStatus.RUNNING && !requestAudioFocus()) {
                        ticker?.cancel();timer?.pause();releaseWake();checkpoint();publish()
                        SessionState.message.value="O áudio está indisponível. Retome ou escolha orientação por tela."
                        return
                    }
                    prepareSpeech()
                    if (HapticGuidance.changedDuringRunning(previousGuidance, settings.user.guidance, current?.session?.status))
                        current?.let { cue(it.phase) }
                }
            }
            else -> if(timer==null) {stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
        }
    }
    private suspend fun restore(active:ActiveSessionEntity):Boolean {
        val decoded=runCatching {Codec.recoverableSession(active.payload)}.getOrNull()
        if(decoded==null) {
            dao.deleteActive();SessionState.state.value=null;SessionState.options.value=null
            SessionState.message.value="A sessão salva não pôde ser recuperada. Inicie um novo treino."
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return false
        }
        val json=JSONObject(active.payload)
        programWeek=json.optInt("week",-1)
        SessionState.options.value=settings.user.copy(
            guidance=runCatching {Guidance.valueOf(json.optString("guidance",settings.user.guidance.name))}.getOrDefault(settings.user.guidance),
            discreetScreen=json.optBoolean("discreet",settings.user.discreetScreen))
        settings=settings.copy(user=SessionState.options.value!!)
        timer=WorkoutTimer({SystemClock.elapsedRealtime()},decoded.copy(status=SessionStatus.PAUSED))
        return true
    }
    private suspend fun checkpoint() {
        timer?.state()?.session?.let {
            dao.saveActive(ActiveSessionEntity(payload=JSONObject(Codec.session(it,programWeek)).put("guidance",settings.user.guidance.name).put("discreet",settings.user.discreetScreen).toString()))
            lastCheckpoint=it
        }
    }
    private suspend fun begin() {
        ticker?.cancel(); SessionState.message.value=null
        val initial=timer?.state() ?: return
        if(initial.session.status!=SessionStatus.RUNNING) {publish();return}
        if(!requestAudioFocus()) {
            timer?.pause();checkpoint();publish()
            SessionState.message.value="O áudio está indisponível. Retome ou escolha orientação por tela."
            return
        }
        val pm=getSystemService(PowerManager::class.java)
        releaseWake()
        wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"pausa:session").apply {
            setReferenceCounted(false)
            acquire(initial.session.workout.durationMillis-initial.session.elapsedMillis+60_000)
        }
        lastObserved=initial;lastObservedAt=SystemClock.elapsedRealtime()
        prepareSpeech()
        cue(initial.phase)
        ticker=scope.launch {
            var previous=initial
            var lastTick=SystemClock.elapsedRealtime()
            publish()
            while(isActive) {
                // Wake only for the next display second or phase boundary, not every animation frame.
                delay(minOf(1000L, previous.remainingMillis % 1000L.let { if(it==0L)1000L else it }).coerceAtLeast(1))
                val now=SystemClock.elapsedRealtime()
                val current=timer?.state() ?: break
                if(CueContinuity.interrupted(lastTick,now,previous.phaseIndex,current.phaseIndex)) {
                    timer=WorkoutTimer({SystemClock.elapsedRealtime()},previous.session.copy(status=SessionStatus.PAUSED))
                    releaseWake();releaseAudioFocus();haptics.cancel();tone?.stopTone();speech?.stop()
                    SessionState.message.value="O ritmo foi interrompido. Retome para continuar com segurança."
                    checkpoint();publish();break
                }
                if(current.session.status==SessionStatus.COMPLETED) {
                    finish(current);break
                }
                SessionState.state.value=current
                if(current.phaseIndex!=previous.phaseIndex) {cue(current.phase);checkpoint();publish()}
                previous=current;lastTick=now
                lastObserved=current;lastObservedAt=now
            }
        }
    }
    private suspend fun finish(state:TimerState) {
        val s=state.session
        dao.complete(HistoryEntity(s.id,s.workout.name,System.currentTimeMillis(),LocalDate.now().toString(),
            s.workout.durationMillis,s.workout.contractions,programWeek=programWeek))
        SessionState.state.value=state
        cue(Phase.FINISHED);releaseWake()
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    }
    private fun publish() {
        val state=timer?.state()
        SessionState.state.value=state
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION,notification(state?.session?.status==SessionStatus.PAUSED))
    }
    private fun requestAudioFocus():Boolean {
        if(settings.user.guidance !in listOf(Guidance.SOUND,Guidance.VOICE))return true
        if(audioFocus!=null)return true
        val manager=getSystemService(AudioManager::class.java)
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener {change->
                if(change in listOf(AudioManager.AUDIOFOCUS_LOSS,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
                    commands.trySend(Intent().setAction(PAUSE))
            }.build()
        if(manager.requestAudioFocus(request)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return false
        audioFocus=request
        return true
    }
    private fun releaseAudioFocus() {
        audioFocus?.let {getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)}
        audioFocus=null
    }
    private fun prepareSpeech() {
        if(settings.user.guidance!=Guidance.VOICE || speech!=null)return
        speech=TextToSpeech(this) {status ->
            if(status==TextToSpeech.SUCCESS) {
                val offline=speech?.voices?.firstOrNull {it.locale.language=="pt" && !it.isNetworkConnectionRequired}
                if(offline!=null) {speech?.voice=offline;speechReady=true}
                else SessionState.message.value="Voz em português offline indisponível. O treino usará sinais sonoros."
            } else SessionState.message.value="Voz indisponível. O treino usará sinais sonoros."
        }
    }
    private fun cue(phase:Phase) {
        if(settings.user.guidance in listOf(Guidance.VIBRATION,Guidance.BOTH)) haptics.play(phase)
        if(settings.user.guidance==Guidance.VOICE && speechReady) {
            speech?.speak(when(phase) {
                Phase.CONTRACT->"Contrair";Phase.RELAX->"Relaxar";Phase.REST->"Descanse";Phase.FINISHED->"Treino concluído"
            },TextToSpeech.QUEUE_FLUSH,null,"phase")
        }
        if(settings.user.guidance==Guidance.SOUND || (settings.user.guidance==Guidance.VOICE && !speechReady)) {
            if(tone==null) tone=ToneGenerator(AudioManager.STREAM_MUSIC,35)
            tone?.startTone(when(phase) {
                Phase.CONTRACT->ToneGenerator.TONE_PROP_BEEP
                Phase.RELAX->ToneGenerator.TONE_PROP_BEEP2
                Phase.REST->ToneGenerator.TONE_PROP_ACK
                Phase.FINISHED->ToneGenerator.TONE_PROP_PROMPT
            },if(phase==Phase.FINISHED)600 else 200)
        }
    }
    private fun notification(paused:Boolean):Notification {
        val content=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(a:String)=PendingIntent.getForegroundService(this,a.hashCode(),Intent(this,WorkoutService::class.java).setAction(a),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("Pausa")
            .setContentText(if(paused)"Sessão pausada" else "Seu ritmo continua")
            .setContentIntent(content).setOngoing(true).setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("Pausa").setContentText("Sessão em andamento").build())
            .addAction(0,if(paused)"Retomar" else "Pausar",action(if(paused)RESUME else PAUSE))
            .addAction(0,"Encerrar",action(CANCEL)).build()
    }
    private fun releaseWake(){wakeLock?.let {if(it.isHeld)it.release()};wakeLock=null}
    override fun onDestroy() {
        SessionState.serviceAlive=false
        if(SessionState.state.value?.session?.status==SessionStatus.RUNNING) {
            lastCheckpoint?.let {
                SessionState.state.value=WorkoutTimer({SystemClock.elapsedRealtime()},it.copy(status=SessionStatus.PAUSED)).state()
                SessionState.message.value="Sessão interrompida. Retome a partir do último ponto salvo."
            }
        }
        releaseWake();scope.cancel();commands.close()
        if(SessionState.state.value?.session?.status==SessionStatus.COMPLETED)
            Handler(Looper.getMainLooper()).postDelayed({releaseAudioFocus()},2000) else releaseAudioFocus()
        speech?.let { if(SessionState.state.value?.session?.status==SessionStatus.COMPLETED)
            Handler(Looper.getMainLooper()).postDelayed({it.shutdown()},2000) else it.shutdown() }
        if(SessionState.state.value?.session?.status!=SessionStatus.COMPLETED)haptics.cancel()
        tone?.let {
            if(SessionState.state.value?.session?.status==SessionStatus.COMPLETED)
                Handler(Looper.getMainLooper()).postDelayed({it.release()},650)
            else it.release()
        }
        super.onDestroy()
    }
    companion object {
        const val START="com.pausa.START";const val PAUSE="com.pausa.PAUSE";const val RESUME="com.pausa.RESUME"
        const val CANCEL="com.pausa.CANCEL";const val GUIDANCE="com.pausa.GUIDANCE"
        private const val CHANNEL="session";private const val NOTIFICATION=100
        fun send(context:Context,action:String,workout:Workout?=null,pocket:Boolean=false,options:UserPreferences?=null) {
            val intent=Intent(context,WorkoutService::class.java).setAction(action)
            intent.putExtra("pocket",pocket)
            options?.let {intent.putExtra("guidance",it.guidance.name);intent.putExtra("discreet",it.discreetScreen)}
            if(workout!=null)intent.putExtra("workout",Codec.workout(workout).toString())
            ContextCompat.startForegroundService(context,intent)
        }
    }
}
