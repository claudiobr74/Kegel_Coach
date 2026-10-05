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
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pausa.R
import com.pausa.BuildConfig
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pausa.data.*
import com.pausa.domain.*
import com.pausa.service.*
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val pt=Locale.forLanguageTag("pt-BR")
private fun duration(ms:Long):String {
    val s=ms/1000
    return if(s<60)"$s s" else if(s%60==0L)"${s/60} min" else "${s/60} min ${s%60} s"
}
private val dayNames=listOf("SEG","TER","QUA","QUI","SEX","SÁB","DOM")

@Composable fun Fig(node:String,asset:String,size:Dp,modifier:Modifier=Modifier,description:String?=null,tint:Color?=null) {
    val context=LocalContext.current
    val map=remember {JSONObject(context.assets.open("asset-map.json").bufferedReader().use {it.readText()})}
    val filename=map.getJSONObject(node).getString(asset)
    AsyncImage(model="file:///android_asset/figma/$filename",contentDescription=description,
        modifier=modifier.size(size),contentScale=ContentScale.Fit,
        colorFilter=tint?.let {ColorFilter.tint(it)})
}
@Composable internal fun Brand() {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.ic_pausa),contentDescription=null,modifier=Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
        Text("pausa.",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
    }
}
@Composable internal fun Title(text:String) {Text(text,style=MaterialTheme.typography.titleLarge)}
@Composable internal fun Copy(text:String) {Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable internal fun Primary(text:String,onClick:()->Unit,enabled:Boolean=true) {
    Button(onClick,Modifier.fillMaxWidth().heightIn(min=56.dp),enabled=enabled,shape=CircleShape) {Text(text)}
}
@Composable internal fun Secondary(text:String,onClick:()->Unit,modifier:Modifier=Modifier) {
    FilledTonalButton(onClick,modifier.fillMaxWidth().heightIn(min=48.dp),shape=CircleShape,
        colors=ButtonDefaults.filledTonalButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant,
            contentColor=MaterialTheme.colorScheme.primary)) {Text(text)}
}
@Composable internal fun Panel(accent:Boolean=false,padding:Dp=16.dp,gap:Dp=12.dp,content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),
        color=if(accent)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shadowElevation=1.dp) {Column(Modifier.padding(padding),verticalArrangement=Arrangement.spacedBy(gap),content=content)}
}
@Composable internal fun ScrollContent(gap:Dp=12.dp,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(gap),content=content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PausaApp(vm:AppViewModel,reminderRequest:Int=0) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var route by rememberSaveable {mutableStateOf("home")}
    var manualReturn by rememberSaveable {mutableStateOf("settings")}
    var onboardingPage by rememberSaveable {mutableIntStateOf(0)}
    fun openManual(from:String) {manualReturn=from;route="manual"}
    fun backToPrevious() {route=if(route=="manual")manualReturn else "home"}
    var confirmEnd by remember {mutableStateOf(false)}
    var clearHistory by remember {mutableStateOf(false)}
    var info by remember {mutableStateOf<String?>(null)}
    var handledReminder by rememberSaveable {mutableIntStateOf(0)}
    val context=LocalContext.current
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun askNotifications() {if(Build.VERSION.SDK_INT>=33 &&
        ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
        permission.launch(Manifest.permission.POST_NOTIFICATIONS)}
    fun start(w:Workout,pocket:Boolean=false) {
        val vibrates=if(Build.VERSION.SDK_INT>=31)context.getSystemService(VibratorManager::class.java).defaultVibrator.hasVibrator()
            else context.getSystemService(Vibrator::class.java).hasVibrator()
        if(pocket && !vibrates) {info="Este aparelho não possui vibração. Use o treino com tela ou som.";return}
        askNotifications();vm.start(w,pocket);route="session"
    }
    LaunchedEffect(reminderRequest,settings?.user?.onboardingDone) {
        if(reminderRequest>handledReminder && settings?.user?.onboardingDone==true) {
            handledReminder=reminderRequest
            if(session==null || session?.session?.status==SessionStatus.COMPLETED) {
                vm.dismissCompleted();vm.start(settings!!.workout)
            }
            route="session"
        }
    }
    LaunchedEffect(session?.session?.status) {
        if(session?.session?.status==SessionStatus.COMPLETED)route="finished"
    }
    PausaTheme(settings?.user?.theme ?: AppTheme.SYSTEM) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            val current=settings
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
                    "session"->if(current.user.discreetScreen)"Pausa" else "Treino"
                    "programs"->"Programas";"custom"->"Personalizar treino";"progress"->"Progresso"
                    "reminders"->"Lembretes";"pocket"->"Modo bolso";"finished"->"Treino concluído"
                    "manual"->stringResource(R.string.manual_title)
                    else->"Configurações"
                })},navigationIcon={TextButton(onClick={
                    if(route=="session" && session!=null)confirmEnd=true else {
                        if(route=="finished")vm.dismissCompleted()
                        backToPrevious()
                    }
                },modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)) {
                    Fig("23:855","imgIcone",24.dp,description="Voltar",tint=MaterialTheme.colorScheme.onSurface)
                }},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))},
                bottomBar={if(route in destinations) {
                    Surface(color=MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup()) {
                            destinations.forEachIndexed {i,r->
                                val selected=route==r
                                val color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                Column(Modifier.weight(1f).heightIn(min=80.dp)
                                    .selectable(selected=selected,role=Role.Tab,onClick={route=r}).padding(top=8.dp,bottom=12.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(56.dp,32.dp).background(
                                        if(selected)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,CircleShape),
                                        contentAlignment=Alignment.Center) {
                                        Fig("23:770",listOf("imgDestino","imgDestino1","imgDestino2","imgDestino3")[i],20.dp,tint=color)
                                    }
                                    Text(listOf("Home","Treino","Progresso","Configurações")[i],
                                        modifier=Modifier.fillMaxWidth().padding(horizontal=2.dp),textAlign=TextAlign.Center,
                                        style=MaterialTheme.typography.labelSmall,fontSize=if(i==3)10.sp else 11.sp,
                                        fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=color)
                                }
                            }
                        }
                    }
                }}
            ) {padding ->
                Box(Modifier.padding(padding)) {
                    when(route) {
                        "home"->Home(current,history,session,{start(current.workout)},{route=it},{start(WorkoutPreset.quick)})
                        "programs"->Programs({start(it)},{vm.selectWorkout(it);route="home"},{route="custom"},current,history,{vm.advance()})
                        "custom"->CustomWorkout(current.workout,{vm.selectWorkout(it);route="home"},{start(it)})
                        "session"->SessionScreen(session,current.user,message,
                            {vm.control(if(session?.session?.status==SessionStatus.PAUSED)WorkoutService.RESUME else WorkoutService.PAUSE)},
                            {confirmEnd=true},{vm.updateUser {it.copy(discreetScreen=!it.discreetScreen)}},
                            {vm.updateUser {it.copy(guidance=Guidance.VIBRATION,discreetScreen=true)}})
                        "pocket"->PocketScreen({start(current.workout,true)},{
                            HapticGuidance(context).preview()
                        })
                        "finished"->Finished(session,history,{id,value->vm.feedback(id,value)},{vm.dismissCompleted();route="home"})
                        "progress"->ProgressScreen(history)
                        "reminders"->RemindersScreen(reminders,{askNotifications()},vm::newReminder,vm::saveReminder,vm::removeReminder)
                        "manual"->ManualScreen()
                        "settings"->SettingsScreen(current,{vm.updateUser(it)},{if(it=="manual")openManual("settings") else route=it},{clearHistory=true},{info=it})
                    }
                    if(route=="home" && message!=null && session==null) {
                        Text(message!!,Modifier.align(Alignment.BottomCenter).padding(16.dp),color=MaterialTheme.colorScheme.error)
                    }
                }
            }
            if(confirmEnd)AlertDialog(onDismissRequest={confirmEnd=false},title={Title("Encerrar treino?")},
                text={Copy("A sessão incompleta não será registrada. Você também pode pausá-la e retomar depois.")},
                confirmButton={TextButton(onClick={vm.control(WorkoutService.CANCEL);confirmEnd=false;route="home"}){Text("Encerrar")}},
                dismissButton={TextButton(onClick={confirmEnd=false}){Text("Continuar")}})
            if(clearHistory)AlertDialog(onDismissRequest={clearHistory=false},title={Title("Excluir histórico?")},
                text={Copy("Todos os registros locais de treinos serão excluídos.")},
                confirmButton={TextButton(onClick={vm.clearHistory();clearHistory=false}){Text("Excluir")}},
                dismissButton={TextButton(onClick={clearHistory=false}){Text("Cancelar")}})
            if(info!=null)AlertDialog(onDismissRequest={info=null},title={Title("Pausa")},
                text={Copy(info!!)},confirmButton={TextButton(onClick={info=null}){Text("Entendi")}})
        }
    }
}

@Composable private fun Onboarding(page:Int,onPage:(Int)->Unit,onDone:()->Unit,manual:()->Unit) {
    val headlines=listOf("Treinos discretos.\nOnde você estiver.","Contraia. Relaxe. Repita.","Sem precisar olhar para a tela.","Crie uma rotina.")
    val details=listOf(
        "Exercícios rápidos e guiados para fortalecer o assoalho pélvico.",
        "Imagine segurar gases e urina: contraia e eleve suavemente, sem apertar glúteos, coxas ou abdômen. Respire normalmente e relaxe completamente. Sentado ou em pé, mantenha uma postura confortável.",
        "Uma vibração: contrair. Duas: relaxar. Longa: fim da série. Duas curtas e uma longa: concluído. No modo bolso, bloqueie a tela pelo botão do aparelho. No carro, treine apenas parado.",
        "Escolha lembretes depois, nas configurações. Interromper o jato urinário é apenas uma referência de identificação; não faça disso um exercício habitual. Pare se houver dor ou desconforto persistente. Procure orientação profissional em caso de sintomas urinários, pélvicos ou pós-operatórios. O app não substitui avaliação médica ou fisioterapêutica."
    )
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Brand()
        Box(Modifier.fillMaxWidth().height(280.dp),contentAlignment=Alignment.Center) {
            when(page) {
                0 -> {
                    Fig("23:648","imgCampoDeRespiro",280.dp)
                    Fig("23:648","imgExpansao",224.dp)
                    Fig("23:648","imgRitmo",168.dp)
                    Fig("23:648","imgCentro",40.dp)
                }
                1 -> {
                    Fig("23:675","imgCampoDeRespiro",280.dp)
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        Fig("23:675","imgCirculo",112.dp)
                        Text("CONTRAIR 5 s",color=MaterialTheme.colorScheme.primary)
                        Text("RELAXAR 5 s",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                2 -> {
                    Fig("23:703","imgCampoDeRespiro",280.dp)
                    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(160.dp)) {
                        Box(contentAlignment=Alignment.Center){Fig("23:703","imgIcone",76.dp)}
                    }
                }
                3 -> {
                    Fig("23:730","imgCampoDeRespiro",280.dp)
                    Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.width(192.dp)) {
                        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            Fig("23:730","imgIcone",32.dp)
                            listOf("08:00","14:00","21:00").forEach {time ->
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                    Text(time,Modifier.weight(1f));Fig("23:730","imgAtivo",8.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(headlines[page],style=MaterialTheme.typography.headlineLarge)
        Copy(details[page])
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
            repeat(4){Box(Modifier.padding(4.dp).size(if(it==page)24.dp else 8.dp,6.dp)
                .background(if(it==page)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,CircleShape))}
        }
        if(page>0)TextButton(onClick=manual,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
            Text(stringResource(R.string.manual_onboarding_entry))
        }
        Primary(if(page==3)"COMEÇAR" else "Continuar",{if(page<3)onPage(page+1) else onDone()})
        if(page>0)TextButton(onClick={onPage(page-1)},modifier=Modifier.fillMaxWidth()){Text("Voltar")}
    }
}

@Composable private fun Home(settings:Settings,history:List<HistoryEntity>,session:TimerState?,start:()->Unit,
    navigate:(String)->Unit,quick:()->Unit) {
    ScrollContent(gap=10.dp) {
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Copy(when(LocalTime.now().hour){in 5..11->"Bom dia";in 12..17->"Boa tarde";else->"Boa noite"})
                Spacer(Modifier.weight(1f));Fig("23:770","imgIcone",18.dp,tint=MaterialTheme.colorScheme.primary)
            }
            Title("Seu treino de hoje")
        }
        if(session!=null && session.session.status!=SessionStatus.COMPLETED)Panel {
            Text(if(session.session.status==SessionStatus.PAUSED)"Você tem um treino pausado" else "Seu treino está em andamento")
            Primary("VOLTAR AO TREINO",{navigate("session")})
        }
        Panel(true) {
            Text("TREINO DE HOJE",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
            val workout=settings.workout
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    workout.blocks.forEach { block ->
                        Text("${block.contractSeconds} s contração · ${block.relaxSeconds} s relaxamento",color=MaterialTheme.colorScheme.onSurface)
                        Text("${block.repetitions} repetições por série",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${workout.sets} ${if(workout.sets==1) "série" else "séries"} · ${workout.contractions} contrações no total",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.width(82.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    val ms=settings.workout.durationMillis
                    Text(if(ms<60000)"≈ ${ms/1000} s" else "≈ ${(ms+59999)/60000} min",
                        style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
                    Text("no seu ritmo",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Primary("INICIAR TREINO",start,session==null || session.session.status==SessionStatus.COMPLETED)
        }
        WeekPanel(history)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Secondary("Treino rápido",quick,Modifier.weight(1f))
            Secondary("Personalizar",{navigate("custom")},Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(Triple("Programas","programs","imgIcone1"),Triple("Modo bolso","pocket","imgIcone2"),
                Triple("Lembretes","reminders","imgIcone3")).forEach {(label,r,a)->
                Column(Modifier.weight(1f).clickable {navigate(r)}.heightIn(min=56.dp).padding(4.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Fig("23:770",a,20.dp,tint=MaterialTheme.colorScheme.primary)
                    Text(label,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
@Composable private fun WeekPanel(history:List<HistoryEntity>) {
    val today=LocalDate.now();val monday=today.minusDays((today.dayOfWeek.value-1).toLong())
    val dates=history.map {LocalDate.parse(it.localDate)}.toSet()
    Panel(padding=12.dp,gap=10.dp) {
        Text("Esta semana",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            repeat(7) {i->
                val done=monday.plusDays(i.toLong()) in dates
                Surface(shape=RoundedCornerShape(12.dp),color=if(done)MaterialTheme.colorScheme.primary else Color.Transparent,
                    border=BorderStroke(1.dp,if(done)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                    Column(Modifier.widthIn(min=34.dp).heightIn(min=48.dp).padding(4.dp).semantics {
                        contentDescription="${dayNames[i]}: ${if(done)"concluído" else "sem treino"}"
                    },horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(dayNames[i],fontSize=10.sp,lineHeight=18.sp,color=if(done)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if(done)"✓" else "○",fontSize=10.sp,lineHeight=18.sp,color=if(done)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${(0..6).count {monday.plusDays(it.toLong()) in dates}} de 7 dias",style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Sequência / ${Progress.streak(dates,today)} dias",style=MaterialTheme.typography.bodySmall,
                fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun Programs(start:(Workout)->Unit,select:(Workout)->Unit,custom:()->Unit,
    settings:Settings,history:List<HistoryEntity>,advance:()->Unit) {
    ScrollContent {
        Title("Encontre seu ritmo.");Copy("Comece com conforto. Avance aos poucos.")
        val completedDays=history.filter {it.programWeek==settings.progressionWeek && it.completedAt>=settings.progressionStarted}
            .map {it.localDate}.distinct().size
        if(settings.user.progressionEnabled && ProgressionPlan.suggestion(settings.progressionWeek,completedDays)!=null)Panel(true) {
            Text("Você completou seu programa desta semana.")
            Copy("Quer avançar para o próximo nível? Continue no atual se ainda estiver difícil.")
            Secondary("Avançar para semana ${settings.progressionWeek+2}",advance)
        }
        WorkoutPreset.all.forEach {w->Panel {
            Text(w.name.uppercase(pt),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
            w.blocks.forEach {Text("${it.contractSeconds} s contração / ${it.relaxSeconds} s relaxamento · ${it.repetitions} repetições")}
            Copy("${w.sets} série(s) · ${duration(w.durationMillis)}")
            Primary("INICIAR",{start(w)})
            TextButton(onClick={select(w)},modifier=Modifier.fillMaxWidth()){Text("Usar como treino do dia")}
        }}
        Secondary("Personalizar treino",custom)
    }
}
@Composable private fun Stepper(label:String,value:Int,range:IntRange,unit:String="",step:Int=1,onChange:(Int)->Unit) {
    val largeFont=androidx.compose.ui.platform.LocalDensity.current.fontScale>1.3f
    val controls:@Composable ()->Unit = {
        Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface) {
            Row(Modifier.widthIn(min=156.dp).heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={onChange((value-step).coerceAtLeast(range.first))},enabled=value>range.first) {
                    Fig("23:1181","imgIcone1",24.dp,description="Diminuir $label",tint=MaterialTheme.colorScheme.onSurface)
                }
                Text("$value$unit",Modifier.widthIn(min=60.dp),textAlign=TextAlign.Center,
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Medium)
                IconButton(onClick={onChange((value+step).coerceAtMost(range.last))},enabled=value<range.last) {
                    Fig("23:1181","imgIcone2",24.dp,description="Aumentar $label",tint=MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    if(largeFont)Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,style=MaterialTheme.typography.bodyMedium);controls()
    } else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium);controls()
    }
}
@Composable private fun CustomWorkout(initial:Workout,save:(Workout)->Unit,start:(Workout)->Unit) {
    var c by rememberSaveable {mutableIntStateOf(initial.blocks.first().contractSeconds)}
    var r by rememberSaveable {mutableIntStateOf(initial.blocks.first().relaxSeconds)}
    var reps by rememberSaveable {mutableIntStateOf(initial.blocks.first().repetitions)}
    var sets by rememberSaveable {mutableIntStateOf(initial.sets)}
    var rest by rememberSaveable {mutableIntStateOf(initial.restSeconds)}
    val workout=Workout("custom","Personalizado",listOf(WorkoutBlock(c,r,reps)),sets,rest)
    ScrollContent {
        Copy("Ajuste o ritmo. Respeite seu conforto.")
        Stepper("Contração",c,1..30," s"){c=it}
        Stepper("Relaxamento",r,1..30," s"){r=it}
        Stepper("Repetições",reps,1..50){reps=it}
        Stepper("Séries",sets,1..10){sets=it}
        Stepper("Intervalo entre séries",rest,10..300," s",10){rest=it}
        Panel(true) {Copy("Duração calculada");Title(duration(workout.durationMillis))}
        Copy("Inclui contrações, relaxamentos e intervalos entre séries. Sem preparação adicional.")
        Primary("SALVAR TREINO",{save(workout)})
        Secondary("Iniciar agora",{start(workout)})
    }
}

@Composable private fun SessionScreen(state:TimerState?,prefs:UserPreferences,message:String?,pause:()->Unit,
    end:()->Unit,discreet:()->Unit,pocket:()->Unit) {
    if(state==null) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            if(message==null)CircularProgressIndicator() else Copy(message)
        }
        return
    }
    val paused=state.session.status==SessionStatus.PAUSED
    val context=LocalContext.current
    val reduceMotion=AndroidSettings.Global.getFloat(context.contentResolver,AndroidSettings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
    val scale=remember {Animatable(.515f)}
    LaunchedEffect(state.phaseIndex,paused,reduceMotion) {
        val fraction=if(state.phaseDurationMillis==0L)0f else 1f-state.remainingMillis.toFloat()/state.phaseDurationMillis
        val exact=when(state.phase) {
            Phase.CONTRACT->.515f+.485f*fraction
            Phase.RELAX->1f-.485f*fraction
            else->.515f
        }
        scale.snapTo(if(reduceMotion).515f else exact)
        if(!paused && !reduceMotion && state.phase in listOf(Phase.CONTRACT,Phase.RELAX))
            scale.animateTo(if(state.phase==Phase.CONTRACT)1f else .515f,tween(state.remainingMillis.toInt(),easing=LinearEasing))
    }
    val circleNode=if(MaterialTheme.colorScheme.background.luminance()<.2f)"23:2344" else "23:855"
    ScrollContent {
        Text("Série ${state.set} / ${state.session.workout.sets} · ${state.repetition} / ${state.session.workout.blocks.sumOf {it.repetitions}}",
            Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().heightIn(min=280.dp),contentAlignment=Alignment.Center) {
            Fig(circleNode,"imgAmplitudeMaxima",280.dp)
            Fig(circleNode,"imgCirculoDeRitmo",144.dp,modifier=Modifier.scale(scale.value/.515f))
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clearAndSetSemantics {
                contentDescription=(if(paused)"Treino pausado" else when(state.phase) {
                    Phase.CONTRACT->if(prefs.discreetScreen)"Fase um" else "Contraia"
                    Phase.RELAX->if(prefs.discreetScreen)"Fase dois" else "Relaxe"
                    Phase.REST->"Intervalo";Phase.FINISHED->"Concluído"
                }) + ", ${state.secondsRemaining} segundos restantes"
            }) {
                if(!prefs.discreetScreen)Text(if(paused)"PAUSADO" else when(state.phase) {
                    Phase.CONTRACT->"CONTRAIA";Phase.RELAX->"RELAXE";Phase.REST->"DESCANSE";Phase.FINISHED->"CONCLUÍDO"
                },color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold,letterSpacing=1.5.sp)
                Text(state.secondsRemaining.toString(),fontSize=64.sp,color=MaterialTheme.colorScheme.onSurface)
            }
        }
        if(!prefs.discreetScreen)Text(when(state.phase) {
            Phase.CONTRACT->"Suavemente, sem prender a respiração."
            Phase.RELAX->"Relaxe completamente."
            Phase.REST->"Descanse antes da próxima série."
            Phase.FINISHED->"Um cuidado a mais no seu dia."
        },Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(message!=null)Copy(message)
        LinearProgressIndicator(progress={state.progress},modifier=Modifier.fillMaxWidth().height(6.dp))
        Secondary(if(paused)"Retomar" else "Pausar",pause)
        TextButton(onClick=end,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Encerrar")}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Secondary(if(prefs.discreetScreen)"Tela normal" else "Tela discreta",discreet,Modifier.weight(1f))
            Secondary("Modo bolso",pocket,Modifier.weight(1f))
        }
        if(prefs.guidance==Guidance.VIBRATION)Copy("Você pode bloquear a tela pelo botão do aparelho. O ritmo continua por vibrações.")
    }
}
@Composable private fun PocketScreen(start:()->Unit,test:()->Unit) {
    ScrollContent {
        Panel(true) {
            Text("MODO BOLSO",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
            Copy("Você pode guardar o celular e acompanhar o treino apenas pelas vibrações.")
            Fig("23:770","imgIcone2",48.dp,tint=MaterialTheme.colorScheme.primary)
        }
        listOf("1 vibração" to "Contrair","2 vibrações" to "Relaxar","Vibração longa" to "Fim da série",
            "Duas curtas + uma longa" to "Treino concluído").forEach {(a,b)->Panel {Text(a);Copy(b)}}
        Secondary("Experimentar vibrações",test)
        Copy("Inicie e bloqueie a tela pelo botão do aparelho. O app mantém uma notificação discreta com Pausar e Encerrar.")
        Primary("INICIAR MODO BOLSO",start)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Finished(state:TimerState?,history:List<HistoryEntity>,feedback:(String,String)->Unit,done:()->Unit) {
    val s=state?.session
    ScrollContent {
        Title("Um cuidado a mais no seu dia.")
        Panel(true) {
            Title(duration(s?.workout?.durationMillis ?: 0))
            Copy("tempo de treino")
            Text("${s?.workout?.contractions ?: 0} contrações",style=MaterialTheme.typography.titleLarge)
        }
        val dates=history.map {LocalDate.parse(it.localDate)}.toSet()
        Panel {Copy("Sequência atual");Title("${Progress.streak(dates,LocalDate.now())} dias")}
        Copy("Como foi? (opcional)")
        var selected by rememberSaveable {mutableStateOf<String?>(null)}
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Muito fácil","Adequado","Difícil").forEach {value->
                FilterChip(selected==value,{selected=value;if(s!=null)feedback(s.id,value)},label={Text(value)})
            }
        }
        Primary("CONCLUIR",done)
    }
}
@Composable private fun ProgressScreen(history:List<HistoryEntity>) {
    val dates=history.map {LocalDate.parse(it.localDate)}.toSet()
    var month by rememberSaveable {mutableStateOf(YearMonth.now().toString())}
    val ym=YearMonth.parse(month)
    ScrollContent {
        WeekPanel(history)
        Panel {
            Text("Sequência atual: ${Progress.streak(dates,LocalDate.now())} dias")
            Text("Treinos no mês: ${history.count {YearMonth.from(LocalDate.parse(it.localDate))==YearMonth.now()}}")
            Text("Tempo total: ${duration(history.sumOf {it.durationMillis})}")
            Text("Melhor sequência: ${Progress.bestStreak(dates)} dias")
        }
        Panel {
            Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton({month=ym.minusMonths(1).toString()},Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics {contentDescription="Mês anterior"}){Text("‹")}
                Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy",pt)),Modifier.weight(1f),textAlign=TextAlign.Center)
                TextButton({month=ym.plusMonths(1).toString()},Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics {contentDescription="Próximo mês"}){Text("›")}
            }
            Row {dayNames.forEach {Text(it.take(1),Modifier.weight(1f),textAlign=TextAlign.Center)}}
            val offset=ym.atDay(1).dayOfWeek.value-1
            repeat((offset+ym.lengthOfMonth()+6)/7) {row->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) {col->
                        val day=row*7+col-offset+1
                        val date=if(day in 1..ym.lengthOfMonth())ym.atDay(day) else null
                        val done=date in dates
                        Text(if(date==null)"" else "$day${if(done)"✓" else ""}",Modifier.weight(1f).heightIn(min=40.dp)
                            .semantics {if(date!=null)contentDescription="$date: ${if(done)"concluído" else "sem treino"}"},
                            textAlign=TextAlign.Center,color=if(done)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Title("Histórico")
        if(history.isEmpty())Copy("Seus treinos concluídos aparecerão aqui.")
        history.forEach {h->Panel {
            Text(h.workoutName,fontWeight=FontWeight.SemiBold)
            Copy("${LocalDate.parse(h.localDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${duration(h.durationMillis)} · ${h.contractions} contrações")
            if(h.feedback!=null)Copy(h.feedback)
        }}
    }
}

@Composable private fun RemindersScreen(reminders:List<ReminderEntity>,permission:()->Unit,
    create:(Int,Int,Int)->Unit,save:(ReminderEntity)->Unit,remove:(ReminderEntity)->Unit) {
    val context=LocalContext.current
    var days by rememberSaveable {mutableIntStateOf(127)}
    fun pickTime(r:ReminderEntity?) {
        days=r?.daysMask ?: 127
        TimePickerDialog(context,{_,hour,minute->
            if(r==null)create(hour,minute,days) else save(r.copy(hour=hour,minute=minute))
            permission()
        },r?.hour ?: 8,r?.minute ?: 0,true).show()
    }
    ScrollContent {
        Copy("Vários horários, dias à sua escolha e mensagens discretas.")
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)Panel {
            Copy("As notificações precisam de permissão para aparecer.")
            Secondary("Permitir notificações",permission)
        }
        reminders.forEach {r->Panel {
            Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton({pickTime(r)},modifier=Modifier.weight(1f)){Text("%02d:%02d".format(r.hour,r.minute),style=MaterialTheme.typography.titleLarge)}
                Switch(r.enabled,{save(r.copy(enabled=it));if(it)permission()},modifier=Modifier.semantics {contentDescription="Ativar lembrete %02d:%02d".format(r.hour,r.minute)})
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                dayNames.forEachIndexed {i,label->
                    val checked=r.daysMask and (1 shl i)!=0
                    FilterChip(checked,{
                        val mask=r.daysMask xor (1 shl i)
                        if(mask!=0)save(r.copy(daysMask=mask))
                    },label={Text(label)},modifier=Modifier.padding(end=4.dp).sizeIn(minWidth=48.dp,minHeight=48.dp))
                }
            }
            TextButton({remove(r)},Modifier.fillMaxWidth()){Text("Remover horário")}
        }}
        Secondary("+ Adicionar horário",{pickTime(null)})
        Panel {Copy("Mensagem da notificação");Text("Hora de uma pausa rápida")}
        Copy("Os lembretes podem atrasar conforme a economia de bateria do Android. Expanda a notificação para adiar por 10 min, 30 min ou 1 hora.")
    }
}

@Composable private fun SettingsScreen(settings:Settings,update:((UserPreferences)->UserPreferences)->Unit,
    navigate:(String)->Unit,clear:()->Unit,info:(String)->Unit) {
    val u=settings.user
    ScrollContent {
        Title("Treino")
        Secondary("Treino padrão · ${settings.workout.name}",{navigate("programs")})
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Sugerir progressão",Modifier.weight(1f))
            Switch(u.progressionEnabled,{value->update {it.copy(progressionEnabled=value)}},modifier=Modifier.semantics {contentDescription="Sugerir progressão"})
        }
        Title("Experiência")
        Panel {
            Copy("Orientação")
            listOf(Guidance.BOTH to "Vibração + tela",Guidance.VIBRATION to "Vibração",Guidance.SCREEN to "Tela",Guidance.SOUND to "Som").forEach {(guide,label)->
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=guide==u.guidance,role=Role.RadioButton,onClick={update {it.copy(guidance=guide)}}),
                    verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(guide==u.guidance,onClick=null);Text(label,Modifier.padding(start=8.dp))
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Tela discreta",Modifier.weight(1f))
                Switch(u.discreetScreen,{value->update {it.copy(discreetScreen=value)}},modifier=Modifier.semantics {contentDescription="Tela discreta"})
            }
        }
        Secondary("Modo bolso",{navigate("pocket")})
        Secondary("Lembretes",{navigate("reminders")})
        Title("Aparência")
        Panel {AppTheme.entries.forEach {theme->
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=u.theme==theme,role=Role.RadioButton,onClick={update {it.copy(theme=theme)}}),
                verticalAlignment=Alignment.CenterVertically) {
                RadioButton(u.theme==theme,null)
                Text(when(theme){AppTheme.SYSTEM->"Seguir sistema";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"},Modifier.padding(start=8.dp))
            }
        }}
        Title("Privacidade")
        Copy("Dados armazenados somente neste aparelho. Sem conta, anúncios, sensores externos ou compartilhamento de informações de saúde.")
        Secondary("Excluir histórico",clear)
        Title("Sobre")
        Secondary(stringResource(R.string.manual_entry),{navigate("manual")})
        Secondary("Segurança",{info("Interrompa se houver dor ou desconforto persistente. Procure orientação profissional em caso de sintomas urinários, pélvicos ou pós-operatórios. O app não substitui avaliação médica ou fisioterapêutica. No carro, faça o treino apenas parado.")})
        Brand()
        Copy("Versão ${BuildConfig.VERSION_NAME}\nCuidado discreto. No seu ritmo.")
    }
}
