package com.pausa.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
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
    private var settings = Settings()
    private var programWeek = -1
    private var lastCheckpoint: WorkoutSession? = null
    private var tone: ToneGenerator? = null

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
        settings = PreferencesRepository(this).settings.first()
        when(intent.action) {
            START -> {
                if(timer != null) return
                val existing = dao.active()
                if(existing != null) {
                    restore(existing)
                    SessionState.message.value="Seu treino anterior está pausado. Retome ou encerre."
                    publish(); return
                }
                val workout=Codec.workout(JSONObject(intent.getStringExtra("workout") ?: return))
                programWeek=if(workout.id=="program") settings.progressionWeek else -1
                timer=WorkoutTimer({SystemClock.elapsedRealtime()},WorkoutSession(UUID.randomUUID().toString(),workout,System.currentTimeMillis(),0,SessionStatus.RUNNING))
                checkpoint(); begin()
            }
            RESUME -> {
                if(timer?.state()?.session?.status==SessionStatus.RUNNING)return
                if(timer==null) {
                    val active=dao.active()
                    if(active==null){ stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return }
                    restore(active)
                }
                timer?.resume(); checkpoint();begin()
            }
            PAUSE -> {
                ticker?.cancel();timer?.pause();releaseWake();vibrator()?.cancel()
                val state=timer?.state()
                if(state?.session?.status==SessionStatus.COMPLETED)finish(state) else {checkpoint();publish()}
            }
            CANCEL -> {
                ticker?.cancel();timer?.cancel();releaseWake();vibrator()?.cancel()
                dao.deleteActive();SessionState.state.value=null
                stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
            }
            GUIDANCE -> { /* preferences refreshed above; active session remains unchanged */ }
            else -> if(timer==null) {stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
        }
    }
    private fun restore(active:ActiveSessionEntity) {
        val saved=Codec.session(active.payload).copy(status=SessionStatus.PAUSED)
        programWeek=JSONObject(active.payload).optInt("week",-1)
        timer=WorkoutTimer({SystemClock.elapsedRealtime()},saved)
    }
    private suspend fun checkpoint() {
        timer?.state()?.session?.let {
            dao.saveActive(ActiveSessionEntity(payload=Codec.session(it,programWeek)))
            lastCheckpoint=it
        }
    }
    private fun begin() {
        ticker?.cancel(); SessionState.message.value=null
        val initial=timer?.state() ?: return
        if(initial.session.status!=SessionStatus.RUNNING) {publish();return}
        val pm=getSystemService(PowerManager::class.java)
        releaseWake()
        wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"pausa:session").apply {
            setReferenceCounted(false)
            acquire(initial.session.workout.durationMillis-initial.session.elapsedMillis+60_000)
        }
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
                if(now-lastTick>1500L || current.phaseIndex>previous.phaseIndex+1) {
                    timer=WorkoutTimer({SystemClock.elapsedRealtime()},previous.session.copy(status=SessionStatus.PAUSED))
                    releaseWake();vibrator()?.cancel()
                    SessionState.message.value="O ritmo foi interrompido. Retome para continuar com segurança."
                    checkpoint();publish();break
                }
                if(current.session.status==SessionStatus.COMPLETED) {
                    finish(current);break
                }
                SessionState.state.value=current
                if(current.phaseIndex!=previous.phaseIndex) {cue(current.phase);checkpoint();publish()}
                previous=current;lastTick=now
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
    private fun vibrator():Vibrator? = if(Build.VERSION.SDK_INT>=31)
        getSystemService(VibratorManager::class.java)?.defaultVibrator else getSystemService(Vibrator::class.java)
    private fun cue(phase:Phase) {
        if(settings.user.guidance in listOf(Guidance.VIBRATION,Guidance.BOTH)) {
            val pattern=when(phase) {
                Phase.CONTRACT->longArrayOf(0,150)
                Phase.RELAX->longArrayOf(0,150,120,150)
                Phase.REST->longArrayOf(0,600)
                Phase.FINISHED->longArrayOf(0,150,120,150,120,600)
            }
            vibrator()?.takeIf { it.hasVibrator() }?.vibrate(VibrationEffect.createWaveform(pattern,-1))
        }
        if(settings.user.guidance==Guidance.SOUND) {
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
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_pausa).setContentTitle("Pausa")
            .setContentText(if(paused)"Sessão pausada" else "Seu ritmo continua")
            .setContentIntent(content).setOngoing(true).setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_pausa).setContentTitle("Pausa").setContentText("Sessão em andamento").build())
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
        if(SessionState.state.value?.session?.status!=SessionStatus.COMPLETED)vibrator()?.cancel()
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
        fun send(context:Context,action:String,workout:Workout?=null) {
            val intent=Intent(context,WorkoutService::class.java).setAction(action)
            if(workout!=null)intent.putExtra("workout",Codec.workout(workout).toString())
            ContextCompat.startForegroundService(context,intent)
        }
    }
}
