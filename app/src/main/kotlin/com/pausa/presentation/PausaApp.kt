package com.pausa.presentation

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pausa.reminders.ReminderScheduler
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.pausa.R
import com.pausa.BuildConfig
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pausa.data.*
import com.pausa.domain.*
import com.pausa.service.*
import com.pausa.reminders.domain
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PausaApp(vm:AppViewModel,reminderRequest:Int=0,skipStartup:Boolean=false) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var startupFinished by rememberSaveable {mutableStateOf(skipStartup)}
    val history by vm.history.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val savedWorkouts by vm.savedWorkouts.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val operationError by vm.operationError.collectAsStateWithLifecycle()
    val reminderSaved by vm.reminderSaved.collectAsStateWithLifecycle()
    val sessionOptions by vm.sessionOptions.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(reminderRequest,session) {
        if(reminderRequest>0 || session!=null)startupFinished=true
    }
    val navigator=rememberSaveable(saver=AppNavigator.Saver) {AppNavigator()}
    var route by navigator
    var customPayload by rememberSaveable {mutableStateOf<String?>(null)}
    var customDirty by remember {mutableStateOf(false)}
    var discardDraft by remember {mutableStateOf(false)}
    var preparation by rememberSaveable {mutableStateOf<String?>(null)}
    var preparationPocket by rememberSaveable {mutableStateOf(false)}
    var preparationSeconds by rememberSaveable {mutableIntStateOf(3)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var now by remember {mutableStateOf(LocalDateTime.now())}
    LaunchedEffect(lifecycle) {lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while(true) {now=LocalDateTime.now();delay(30000)}
    }}
    LaunchedEffect(preparation,lifecycle) {
        if(preparation!=null)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(preparationSeconds>0) {delay(1000);preparationSeconds--}
            preparation?.let {vm.start(Codec.workout(JSONObject(it)),preparationPocket);preparation=null;route="session"}
        }
    }
    var onboardingPage by rememberSaveable {mutableIntStateOf(0)}
    fun openManual(@Suppress("UNUSED_PARAMETER") from:String) {route="manual"}
    fun backToPrevious() {
        if(route=="finished") {vm.dismissCompleted();route="home";return}
        if(route=="custom" && customDirty)discardDraft=true
        else navigator.back()
    }
    var confirmEnd by remember {mutableStateOf(false)}
    var clearHistory by remember {mutableStateOf(false)}
    var info by remember {mutableStateOf<String?>(null)}
    var handledReminder by rememberSaveable {mutableIntStateOf(0)}
    val snackbar=remember {SnackbarHostState()}
    LaunchedEffect(vm) {vm.feedbackEvents.collect {snackbar.showSnackbar(it)}}
    val context=LocalContext.current
    var notificationDenied by remember {mutableStateOf(false)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->notificationDenied=!granted}
    fun askNotifications() {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            if(notificationDenied)ReminderScheduler.openSettings(context) else permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if(ReminderScheduler.notificationStatus(context)!=null)ReminderScheduler.openSettings(context)
    }
    fun start(w:Workout,pocket:Boolean=false) {
        val vibrates=if(Build.VERSION.SDK_INT>=31)context.getSystemService(VibratorManager::class.java).defaultVibrator.hasVibrator()
            else context.getSystemService(Vibrator::class.java).hasVibrator()
        if(pocket && !vibrates) {info="Este aparelho não possui vibração. Use o treino com tela ou som.";return}
        askNotifications()
        if(settings?.user?.preparationEnabled==true && session==null) {
            preparationPocket=pocket;preparationSeconds=3;preparation=Codec.workout(w).toString()
        } else {vm.start(w,pocket);route="session"}
    }
    LaunchedEffect(reminderRequest,settings?.user?.onboardingDone) {
        if(reminderRequest>handledReminder && settings?.user?.onboardingDone==true) {
            handledReminder=reminderRequest
            if(session==null || session?.session?.status==SessionStatus.COMPLETED) {
                vm.dismissCompleted();start(settings!!.workout)
            }
            if(preparation==null)route="session"
        }
    }
    LaunchedEffect(session?.session?.status) {
        if(session?.session?.status==SessionStatus.COMPLETED)route="finished"
    }
    PausaTheme(settings?.user?.theme ?: AppTheme.SYSTEM) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            val current=settings
            if(!startupFinished && reminderRequest==0 && session==null) {
                StartupScreen(ready=current!=null,onFinished={startupFinished=true})
                return@Surface
            }
            if(current==null) {Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};return@Surface}
            if(!current.user.onboardingDone && route!="manual") {
                Onboarding(onboardingPage,{onboardingPage=it},
                    {vm.updateUser {it.copy(onboardingDone=true)};route="home"},
                    {openManual("onboarding")})
                return@Surface
            }
            BackHandler(route!="home") {
                if(route=="session" && session!=null)confirmEnd=true
                else {if(route=="finished")vm.dismissCompleted();backToPrevious()}
            }
            val destinations=listOf("home","programs","progress","settings")
            Scaffold(containerColor=MaterialTheme.colorScheme.background,
                topBar={if(route!="home")TopAppBar(title={Title(when(route){
                    "session"->"Treino"
                    "programs"->"Treinos";"custom"->"Personalizar treino";"progress"->"Progresso"
                    "reminders"->"Lembretes";"pocket"->"Modo bolso";"finished"->"Treino concluído"
                    "manual"->stringResource(R.string.manual_title);"data"->"Cópia e restauração";"help"->"Ajuda"
                    else->"Ajustes"
                })},navigationIcon={TextButton(onClick={
                    if(route=="session" && session!=null)confirmEnd=true else {
                        if(route=="finished")vm.dismissCompleted()
                        backToPrevious()
                    }
                },modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)) {
                    Fig("23:855","imgIcone",24.dp,description="Voltar",tint=MaterialTheme.colorScheme.onSurface)
                }},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))},
                snackbarHost={SnackbarHost(snackbar)},
                bottomBar={if(route in destinations) {
                    NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
                        destinations.forEachIndexed {i,r->
                            NavigationBarItem(selected=route==r,onClick={route=r},
                                icon={Fig("23:770",listOf("imgDestino","imgDestino1","imgDestino2","imgDestino3")[i],24.dp,
                                    tint=if(route==r)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)},
                                label={Text(listOf("Início","Treinos","Progresso","Ajustes")[i],
                                    style=MaterialTheme.typography.labelMedium)},
                                colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer))
                        }
                    }
                }}
            ) {padding ->
                Box(Modifier.padding(padding)) {
                    when(route) {
                        "home"->Home(current,history,session,{start(current.workout)},
                            {if(it=="manual")openManual("home") else {if(it=="custom")customPayload=null;route=it}},{start(WorkoutPreset.quick)},now)
                        "programs"->Programs({start(it)},{vm.selectWorkout(it){route="home"}},{customPayload=null;route="custom"},current,history,{vm.advance()},
                            savedWorkouts,{customPayload=Codec.workout(it).toString();route="custom"},vm::removeWorkout,vm::resumeProgram,
                            {vm.changeWeek((current.progressionWeek-1).coerceAtLeast(0))})
                        "custom"->CustomWorkout(customPayload?.let {Codec.workout(JSONObject(it))} ?: current.workout,
                            {vm.saveWorkout(it){customDirty=false;route="home"}},{start(it)},{customDirty=it},saving)
                        "session"->SessionScreen(session,sessionOptions ?: current.user,message,
                            {vm.control(if(session?.session?.status==SessionStatus.PAUSED)WorkoutService.RESUME else WorkoutService.PAUSE)},
                            {confirmEnd=true},vm::sessionDiscreet,vm::sessionPocket,vm::sessionScreenGuide)
                        "pocket"->PocketScreen({start(current.workout,true)},{
                            HapticGuidance(context).preview()
                        })
                        "finished"->Finished(session,history,{id,value->vm.feedback(id,value)},{vm.dismissCompleted();route="home"})
                        "progress"->ProgressScreen(history,now.toLocalDate())
                        "reminders"->RemindersScreen(reminders,{askNotifications()},vm::newReminder,vm::saveReminder,vm::removeReminder,
                            saving,reminderSaved,operationError,false,vm::testReminder,now.atZone(ZoneId.systemDefault()))
                        "data"->DataScreen(vm)
                        "help"->HelpScreen(vm)
                        "manual"->ManualScreen()
                        "settings"->SettingsScreen(current,{vm.updateUser(it)},{if(it=="manual")openManual("settings") else route=it},{clearHistory=true},{info=it},saving)
                    }
                    if(route=="home" && message!=null && session==null) {
                        Text(message!!,Modifier.align(Alignment.BottomCenter).padding(16.dp),color=MaterialTheme.colorScheme.error)
                    }
                }
            }
            if(preparation!=null)AlertDialog(onDismissRequest={preparation=null},title={Title("Prepare-se")},
                text={Column {Text(preparationSeconds.toString(),style=MaterialTheme.typography.headlineLarge);Copy("Acomode-se e relaxe a musculatura.")}},
                confirmButton={},dismissButton={TextButton({preparation=null}){Text("Cancelar")}})
            if(discardDraft)AlertDialog(onDismissRequest={discardDraft=false},title={Title("Descartar alterações?")},text={Copy("O treino ainda não foi salvo.")},
                confirmButton={TextButton({customDirty=false;discardDraft=false;navigator.back()}){Text("Descartar")}},dismissButton={TextButton({discardDraft=false}){Text("Continuar editando")}})
            if(confirmEnd)AlertDialog(onDismissRequest={confirmEnd=false},title={Title("Encerrar treino?")},
                text={Copy("A sessão incompleta não será registrada. Você também pode pausá-la e retomar depois.")},
                confirmButton={TextButton(onClick={vm.control(WorkoutService.CANCEL);confirmEnd=false;route="home"}){Text("Encerrar")}},
                dismissButton={TextButton(onClick={confirmEnd=false}){Text("Continuar")}})
            if(clearHistory)AlertDialog(onDismissRequest={clearHistory=false},title={Title("Excluir histórico?")},
                text={Copy("Todos os registros locais de treinos serão excluídos.")},
                confirmButton={TextButton(onClick={vm.clearHistory();clearHistory=false}){Text("Excluir")}},
                dismissButton={TextButton(onClick={clearHistory=false}){Text("Cancelar")}})
            if(info!=null)AlertDialog(onDismissRequest={info=null},title={Title("Kegel Coach")},
                text={Copy(info!!)},confirmButton={TextButton(onClick={info=null}){Text("Entendi")}})
        }
    }
}
