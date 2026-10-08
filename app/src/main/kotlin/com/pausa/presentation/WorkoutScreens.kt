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

@Composable internal fun Onboarding(page:Int,onPage:(Int)->Unit,onDone:()->Unit,manual:()->Unit) {
    val context=LocalContext.current
    val headlines=listOf("Seu momento de cuidado","Aprenda o movimento","Reconheça os sinais","Comece no seu ritmo")
    val details=listOf(
        "Treinos breves e discretos, sem conta e sem internet. Seu histórico fica neste aparelho.",
        "Contraia e eleve suavemente, como ao segurar gases. Respire normalmente, sem apertar glúteos, coxas ou abdômen. Depois, relaxe completamente.",
        "Uma vibração: contrair. Duas: relaxar. Longa: intervalo. Duas curtas e uma longa: concluído. Experimente antes do primeiro treino.",
        "Pare se houver dor ou desconforto persistente. Em caso de sintomas urinários, pélvicos ou pós-operatórios, procure orientação profissional. Evite treinar interrompendo o jato urinário."
    )
    ScrollContent(gap=14.dp) {
        HeaderBrand()
        Text("${page+1} de 4",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        Icon(painterResource(when(page){0->R.drawable.ic_brand_symbol;1->R.drawable.ic_ui_manual;2->R.drawable.ic_ui_pocket;else->R.drawable.ic_ui_programs}),
            contentDescription=null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(72.dp))
        Text(headlines[page],style=MaterialTheme.typography.headlineLarge)
        Copy(details[page])
        if(page==1)Panel(true) {Title("Contraia 3 s · Relaxe 6 s");Copy("Pratique a técnica antes de acompanhar o contador.")}
        if(page==2)Secondary("Experimentar vibrações",{HapticGuidance(context).preview()})
        if(page==3)Copy("Você poderá escolher lembretes e revisar a técnica a qualquer momento. O app orienta o tempo e não mede a contração muscular. No carro, treine apenas parado.")
        if(page>0)TextButton(onClick=manual,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(stringResource(R.string.manual_onboarding_entry))}
        Primary(if(page==3)"COMEÇAR" else "Continuar",{if(page<3)onPage(page+1) else onDone()})
        if(page>0)TextButton(onClick={onPage(page-1)},modifier=Modifier.fillMaxWidth()){Text("Voltar")}
    }
}

@Composable internal fun Home(settings:Settings,history:List<HistoryEntity>,session:TimerState?,start:()->Unit,
    navigate:(String)->Unit,quick:()->Unit,now:LocalDateTime=LocalDateTime.now()) {
    val active=session!=null && session.session.status!=SessionStatus.COMPLETED
    ScrollContent(gap=14.dp) {
        HeaderBrand()
        Copy(when(now.hour){in 5..11->"Bom dia. Vamos cuidar da sua rotina?";
            in 12..17->"Boa tarde. Um momento para você.";else->"Boa noite. Um momento para você."})
        Panel(true,padding=16.dp,gap=10.dp) {
            Text(if(active)"Treino em andamento" else if(history.any {it.localDate==now.toLocalDate().toString()})"Treino concluído hoje" else "Treino de hoje",style=MaterialTheme.typography.labelLarge,
                color=MaterialTheme.colorScheme.primary)
            val workout=if(active)session!!.session.workout else settings.workout
            Text(workout.name,style=MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(duration(workout.durationMillis),style=MaterialTheme.typography.titleLarge,
                    color=MaterialTheme.colorScheme.primary)
                Text("${workout.sets} ${if(workout.sets==1)"série" else "séries"} · ${workout.contractions} contrações",
                    style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
            }
            if(active) {
                Copy(if(session!!.session.status==SessionStatus.PAUSED)"Pausado. Retome quando estiver pronto."
                    else "Sua sessão continua em andamento.")
                Primary("Continuar treino",{navigate("session")})
            } else {
                workout.blocks.forEach {Copy("Contraia ${it.contractSeconds} s · Relaxe ${it.relaxSeconds} s")}
                Primary(if(history.any {it.localDate==now.toLocalDate().toString()})"Iniciar outra sessão" else "Iniciar treino",start)
            }
        }
        WeekPanel(history,now.toLocalDate())
        if(!active)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Secondary("Treino rápido",quick,Modifier.weight(1f))
            Secondary("Personalizar",{navigate("custom")},Modifier.weight(1f))
        }
        Panel(gap=0.dp,padding=12.dp) {
            ActionRow("Como fazer os exercícios","Técnica e demonstrações",R.drawable.ic_ui_manual,{navigate("manual")})
            ActionRow("Modo bolso","Ritmo por vibrações",R.drawable.ic_ui_pocket,{navigate("pocket")})
            ActionRow("Lembretes","Horários da sua rotina",R.drawable.ic_ui_reminders,{navigate("reminders")})
        }
    }
}

@Composable internal fun ActionRow(title:String,detail:String,icon:Int,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(role=Role.Button,onClick=onClick).heightIn(min=64.dp).padding(vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon),contentDescription=null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(24.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(detail,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_ui_chevron),contentDescription=null,
            tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))
    }
}

@Composable internal fun WeekPanel(history:List<HistoryEntity>,today:LocalDate=LocalDate.now()) {
    val monday=today.minusDays((today.dayOfWeek.value-1).toLong())
    val dates=history.map {LocalDate.parse(it.localDate)}.toSet()
    Panel(padding=12.dp,gap=10.dp) {
        Text("Esta semana",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            repeat(7) {i->
                val done=monday.plusDays(i.toLong()) in dates
                Surface(modifier=Modifier.weight(1f).padding(horizontal=2.dp),shape=RoundedCornerShape(12.dp),color=if(done)MaterialTheme.colorScheme.primary else Color.Transparent,
                    border=BorderStroke(if(monday.plusDays(i.toLong())==today)2.dp else 1.dp,if(done || monday.plusDays(i.toLong())==today)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                    Column(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(vertical=6.dp).semantics {
                        contentDescription="${dayNames[i]}: ${if(done)"concluído" else "sem treino"}"
                    },horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(dayNames[i],fontSize=10.sp,lineHeight=20.sp,color=if(done)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if(done)"✓" else "·",fontSize=14.sp,lineHeight=20.sp,color=if(done)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${(0..6).count {monday.plusDays(it.toLong()) in dates}} de 7 dias",style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${Progress.streak(dates,today)} dias seguidos",style=MaterialTheme.typography.bodySmall,
                fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable internal fun Programs(start:(Workout)->Unit,select:(Workout)->Unit,custom:()->Unit,
    settings:Settings,history:List<HistoryEntity>,advance:()->Unit,
    saved:List<SavedWorkoutEntity> = emptyList(),edit:(Workout)->Unit={},remove:(String)->Unit={},resumeProgram:()->Unit={},reduce:()->Unit={}) {
    var confirmAdvance by remember {mutableStateOf(false)}
    var confirmRemove by remember {mutableStateOf<String?>(null)}
    if(confirmRemove!=null)AlertDialog(onDismissRequest={confirmRemove=null},title={Title("Remover treino salvo?")},text={Copy("O histórico das sessões será preservado.")},confirmButton={TextButton({remove(confirmRemove!!);confirmRemove=null}){Text("Remover")}},dismissButton={TextButton({confirmRemove=null}){Text("Cancelar")}})
    if(confirmAdvance)AlertDialog(onDismissRequest={confirmAdvance=false},title={Title("Avançar o ritmo?")},text={Copy("Avance somente se conseguiu executar e relaxar completamente, sem dor ou desconforto. Você pode manter ou reduzir o nível.")},confirmButton={TextButton({advance();confirmAdvance=false}){Text("Avançar")}},dismissButton={TextButton({confirmAdvance=false}){Text("Manter nível")}})
    var expanded by rememberSaveable {mutableStateOf<String?>(null)}
    ScrollContent(gap=20.dp) {
        Copy("Escolha um ritmo confortável. Você pode ajustar depois.")
        Panel(true,padding=20.dp) {
            Text("Seu treino selecionado",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Title(settings.workout.name)
            Copy("${duration(settings.workout.durationMillis)} · ${settings.workout.contractions} contrações")
            Primary("Iniciar treino",{start(settings.workout)})
        }
        Title("Meu programa")
        Panel {
            Title("Etapa ${settings.progressionWeek+1} de 4")
            Copy("Um ritmo por vez. A passagem de etapa depende da sua escolha.")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                repeat(4) {step->Surface(Modifier.weight(1f),shape=RoundedCornerShape(10.dp),color=if(step==settings.progressionWeek)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                    Text("${step+1}",Modifier.padding(12.dp),textAlign=TextAlign.Center)
                }}
            }
            if(settings.workout.id!="program")Secondary("Retomar programa",resumeProgram)
            if(settings.progressionWeek>0)TextButton(reduce){Text("Voltar uma etapa")}
        }
        val completedDays=history.filter {it.programWeek==settings.progressionWeek && it.completedAt>=settings.progressionStarted}
            .map {it.localDate}.distinct().size
        val tolerated=history.filter {it.programWeek==settings.progressionWeek && it.completedAt>=settings.progressionStarted}
            .take(7).none {it.feedback in listOf("Difícil","Desconforto")}
        if(settings.user.progressionEnabled && ProgressionPlan.suggestion(settings.progressionWeek,completedDays,tolerated)!=null)Panel {
            Title("Pronto para o próximo ritmo?")
            Copy("Você também pode continuar no nível atual.")
            Secondary("Avançar para etapa ${settings.progressionWeek+2}",{confirmAdvance=true})
        }
        if(!tolerated)Panel {Copy("Mantenha ou reduza o ritmo. Reveja a técnica; se houver desconforto persistente, procure orientação profissional.")}
        if(saved.isNotEmpty()) {
            Title("Meus treinos salvos")
            saved.forEach {item->Panel {
                val w=Codec.workout(JSONObject(item.payload))
                Title(w.name);Copy("${duration(w.durationMillis)} · ${w.contractions} contrações")
                Secondary("Treinar agora",{start(w)})
                Row {TextButton({select(w)},Modifier.weight(1f)){Text("Selecionar")};TextButton({edit(w)},Modifier.weight(1f)){Text("Editar")};TextButton({confirmRemove=item.id},Modifier.weight(1f)){Text("Remover")}}
            }}
        }
        Title("Treinos avulsos")
        WorkoutPreset.all.forEach {w->
            val selected=w.id==settings.workout.id
            Panel(gap=12.dp) {
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.Button){expanded=if(expanded==w.id)null else w.id},
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(w.name,style=MaterialTheme.typography.titleMedium)
                        Copy("${duration(w.durationMillis)} · ${w.sets} ${if(w.sets==1)"série" else "séries"}")
                    }
                    if(selected)Text("Selecionado",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    Icon(painterResource(if(expanded==w.id)R.drawable.ic_ui_expand_less else R.drawable.ic_ui_expand_more),
                        contentDescription=if(expanded==w.id)"Recolher ${w.name}" else "Detalhes de ${w.name}",modifier=Modifier.size(20.dp))
                }
                if(expanded==w.id) {
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    w.blocks.forEach {Copy("Contraia ${it.contractSeconds} s · Relaxe ${it.relaxSeconds} s · ${it.repetitions} repetições")}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={start(w)},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Treinar agora")}
                        if(!selected)TextButton(onClick={select(w)},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Selecionar")}
                    }
                }
            }
        }
        Secondary("Personalizar treino",custom)
    }
}
@Composable internal fun Stepper(label:String,value:Int,range:IntRange,unit:String="",step:Int=1,onChange:(Int)->Unit) {
    var editing by rememberSaveable {mutableStateOf(false)}
    var draft by rememberSaveable {mutableStateOf(value.toString())}
    if(editing) {
        val entered=draft.toIntOrNull()
        AlertDialog(onDismissRequest={editing=false},title={Title(label)},text={
            OutlinedTextField(value=draft,onValueChange={draft=it.filter(Char::isDigit).take(4)},
                label={Text("${range.first} a ${range.last}${unit}")},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                isError=entered==null || entered !in range)
        },confirmButton={TextButton(onClick={entered?.takeIf {it in range}?.let(onChange);editing=false},
            enabled=entered!=null && entered in range){Text("Aplicar")}},
            dismissButton={TextButton(onClick={editing=false}){Text("Cancelar")}})
    }
    val largeFont=androidx.compose.ui.platform.LocalDensity.current.fontScale>1.3f
    val controls:@Composable ()->Unit = {
        Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface) {
            Row(Modifier.widthIn(min=156.dp).heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={onChange((value-step).coerceAtLeast(range.first))},enabled=value>range.first) {
                    Fig("23:1181","imgIcone1",24.dp,description="Diminuir $label",tint=MaterialTheme.colorScheme.onSurface)
                }
                Text("$value$unit",Modifier.widthIn(min=60.dp).heightIn(min=48.dp)
                    .clickable(role=Role.Button,onClickLabel="Editar $label"){draft=value.toString();editing=true}
                    .wrapContentHeight(),textAlign=TextAlign.Center,
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
@Composable internal fun CustomWorkout(initial:Workout,save:(Workout)->Unit,start:(Workout)->Unit,
    onDirty:(Boolean)->Unit={},busy:Boolean=false) {
    val initialName=if(initial.id.startsWith("saved-"))initial.name else "Meu treino"
    var name by rememberSaveable(initial.id) {mutableStateOf(initialName)}
    var c by rememberSaveable(initial.id) {mutableIntStateOf(initial.blocks.first().contractSeconds)}
    var r by rememberSaveable(initial.id) {mutableIntStateOf(initial.blocks.first().relaxSeconds)}
    var reps by rememberSaveable(initial.id) {mutableIntStateOf(initial.blocks.first().repetitions)}
    var sets by rememberSaveable(initial.id) {mutableIntStateOf(initial.sets)}
    var rest by rememberSaveable(initial.id) {mutableIntStateOf(initial.restSeconds)}
    var second by rememberSaveable(initial.id) {mutableStateOf(initial.blocks.size==2)}
    var c2 by rememberSaveable(initial.id) {mutableIntStateOf(initial.blocks.getOrNull(1)?.contractSeconds ?: 1)}
    var r2 by rememberSaveable(initial.id) {mutableIntStateOf(initial.blocks.getOrNull(1)?.relaxSeconds ?: 1)}
    var reps2 by rememberSaveable(initial.id) {mutableIntStateOf(initial.blocks.getOrNull(1)?.repetitions ?: 10)}
    var reviewed by rememberSaveable {mutableStateOf(false)}
    val blocks=listOf(WorkoutBlock(c,r,reps))+if(second)listOf(WorkoutBlock(c2,r2,reps2)) else emptyList()
    val workout=Workout(if(initial.id.startsWith("saved-"))initial.id else "custom",name.trim().ifBlank {"Meu treino"},blocks,sets,rest)
    val dirty=name!=initialName || blocks!=initial.blocks || sets!=initial.sets || rest!=initial.restSeconds
    LaunchedEffect(dirty) {onDirty(dirty)}
    val extensive=blocks.any {it.contractSeconds>10 || it.repetitions>20 || it.relaxSeconds<it.contractSeconds} || sets>3 || workout.durationMillis>900000
    LaunchedEffect(c,r,reps,c2,r2,reps2,sets,rest,second) {reviewed=false}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name,{name=it.take(80)},label={Text("Nome do treino")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Copy("Toque em um valor para digitá-lo. Mantenha um ritmo que permita relaxar completamente.")
            if(second)Title("Bloco 1")
            Stepper("Contração",c,1..30," s"){c=it}
            Stepper("Relaxamento",r,1..30," s"){r=it}
            Stepper("Repetições",reps,1..50){reps=it}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Adicionar bloco rápido ou misto",Modifier.weight(1f));Switch(second,{second=it})
            }
            if(second) {
                Title("Bloco 2")
                Stepper("Contração do bloco 2",c2,1..30," s"){c2=it}
                Stepper("Relaxamento do bloco 2",r2,1..30," s"){r2=it}
                Stepper("Repetições do bloco 2",reps2,1..50){reps2=it}
            }
            Stepper("Séries",sets,1..10){sets=it}
            if(sets>1)Stepper("Intervalo entre séries",rest,10..300," s",10){rest=it}
            if(extensive)Panel(true) {
                Title("Reveja esta combinação")
                Copy("O tempo de contração, volume ou descanso está fora da faixa dos treinos básicos. O app não determina a dose adequada para você. Ajuste os valores ou siga um plano profissional individualizado.")
                Row(Modifier.fillMaxWidth().selectable(reviewed,role=Role.Checkbox,onClick={reviewed=!reviewed}),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(reviewed,null);Text("Revisei os valores e sigo orientação individual.",Modifier.weight(1f))
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Copy("Duração total");Title(duration(workout.durationMillis))}
        val enabled=name.isNotBlank() && !busy && (!extensive || reviewed)
        Primary(if(busy)"Salvando…" else "Salvar treino",{save(workout)},enabled)
        TextButton(onClick={start(workout)},enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Treinar agora")}
    }
}

/** One clockwise sweep over the entire phase; the service owns the timer. */
@Composable internal fun PhaseSweepRing(state:TimerState,paused:Boolean,reduceMotion:Boolean,modifier:Modifier=Modifier) {
    val elapsed=remember(state.session.id,state.phaseIndex) {
        Animatable((state.phaseDurationMillis-state.remainingMillis).coerceAtLeast(0L).toFloat())
    }
    LaunchedEffect(state.session.id,state.phaseIndex,state.remainingMillis,paused,reduceMotion) {
        val exact=(state.phaseDurationMillis-state.remainingMillis).coerceAtLeast(0L).toFloat()
        elapsed.snapTo(exact)
        if(!paused && !reduceMotion && state.phase!=Phase.FINISHED && state.remainingMillis>0L) {
            // Resynchronize at every service tick, and stop exactly on a phase boundary.
            val untilTick=minOf(1000L,state.remainingMillis % 1000L.let {if(it==0L)1000L else it})
            elapsed.animateTo(exact+untilTick.toFloat(),tween(untilTick.toInt(),easing=LinearEasing))
        }
    }
    val color=when(state.phase) {Phase.RELAX->MaterialTheme.colorScheme.secondary;Phase.REST->MaterialTheme.colorScheme.onSurfaceVariant;else->MaterialTheme.colorScheme.primary}
    Canvas(modifier) {
        val progress=if(reduceMotion || state.phase==Phase.FINISHED)1f else
            (elapsed.value/state.phaseDurationMillis.coerceAtLeast(1L).toFloat()).coerceIn(0f,1f)
        val diameter=size.minDimension*.80f
        val inset=androidx.compose.ui.geometry.Offset((size.width-diameter)/2f,(size.height-diameter)/2f)
        val arcSize=androidx.compose.ui.geometry.Size(diameter,diameter)
        val sweep=progress*360f
        val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=size.minDimension*.06f,cap=StrokeCap.Butt)
        drawCircle(color=color.copy(alpha=.10f),radius=diameter/2f,style=stroke)
        // A single gradient stroke keeps the arc smooth without segment seams.
        if(progress>0f) {
            if(reduceMotion || state.phase==Phase.FINISHED) {
                drawCircle(color=color,radius=diameter/2f,style=stroke)
            } else {
                val fadeStart=(progress-43f/360f).coerceAtLeast(0f)
                val brush=Brush.sweepGradient(0f to color,fadeStart to color,
                    progress to color.copy(alpha=0f),1f to color.copy(alpha=0f),center=center)
                rotate(-90f) {
                    drawArc(brush=brush,startAngle=0f,sweepAngle=sweep,useCenter=false,
                        topLeft=inset,size=arcSize,style=stroke)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun SessionScreen(state:TimerState?,prefs:UserPreferences,message:String?,pause:()->Unit,
    end:()->Unit,discreet:()->Unit,pocket:()->Unit,screen:()->Unit={}) {
    if(state==null) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            if(message==null)CircularProgressIndicator() else Copy(message)
        }
        return
    }
    val paused=state.session.status==SessionStatus.PAUSED
    val view=LocalView.current
    val keepScreenOn=!paused && prefs.guidance in listOf(Guidance.SCREEN,Guidance.BOTH)
    DisposableEffect(view,keepScreenOn) {
        val previous=view.keepScreenOn;view.keepScreenOn=keepScreenOn
        onDispose {view.keepScreenOn=previous}
    }
    val context=LocalContext.current
    val reduceMotion=AndroidSettings.Global.getFloat(context.contentResolver,AndroidSettings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
    var options by rememberSaveable {mutableStateOf(false)}
    if(options)ModalBottomSheet(onDismissRequest={options=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Title("Opções do treino")
            ActionRow(if(prefs.discreetScreen)"Tela normal" else "Tela discreta","Altere as informações exibidas",
                R.drawable.ic_ui_settings,{discreet();options=false})
            ActionRow("Modo bolso","Ative vibrações e bloqueie a tela pelo botão do aparelho",
                R.drawable.ic_ui_pocket,{pocket();options=false})
            ActionRow("Orientação por tela","Acompanhe as fases pelo contador",
                R.drawable.ic_ui_play,{screen();options=false})
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val largeFont=androidx.compose.ui.platform.LocalDensity.current.fontScale>1.3f
        val dialSize=if(largeFont)280.dp else if(maxHeight<540.dp)220.dp else 260.dp
        Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Série ${state.set} de ${state.session.workout.sets} · Repetição ${state.repetition} de ${state.session.workout.blocks.sumOf {it.repetitions}}",
                Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium)
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(dialSize),contentAlignment=Alignment.Center) {
                    PhaseSweepRing(state,paused,reduceMotion,Modifier.fillMaxSize())
                    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.semantics(mergeDescendants=true) {
                        contentDescription=(if(paused)"Treino pausado" else when(state.phase) {
                            Phase.CONTRACT->"Contrair"
                            Phase.RELAX->"Relaxar"
                            Phase.REST->"Intervalo";Phase.FINISHED->"Concluído"
                        }) + ", ${state.secondsRemaining} segundos restantes"
                    }) {
                        Text(if(paused)"Pausado" else when(state.phase) {
                            Phase.CONTRACT->"Contrair";Phase.RELAX->"Relaxar";Phase.REST->"Descanse";Phase.FINISHED->"Concluído"
                        },style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                        Text(state.secondsRemaining.toString(),modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp),textAlign=TextAlign.Center,
                            style=MaterialTheme.typography.headlineLarge.copy(fontSize=64.sp,lineHeight=76.sp,fontFeatureSettings="tnum"),
                            color=MaterialTheme.colorScheme.primary)
                        Text("segundos",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if(!prefs.discreetScreen)Text(if(paused)"Relaxe completamente. Retome quando estiver pronto." else when(state.phase) {
                    Phase.CONTRACT->"Contraia suavemente. Respire normalmente."
                    Phase.RELAX->"Solte completamente a musculatura."
                    Phase.REST->"Descanse antes da próxima série."
                    Phase.FINISHED->"Um cuidado a mais no seu dia."
                },Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(message!=null)Copy(message)
            }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Progresso da sessão",style=MaterialTheme.typography.bodySmall)
                Text("${duration((state.session.workout.durationMillis-state.session.elapsedMillis).coerceAtLeast(0L))} restantes",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress={state.progress},modifier=Modifier.fillMaxWidth().height(4.dp))
            Primary(if(paused)"Retomar" else "Pausar",pause)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={options=true},modifier=Modifier.heightIn(min=48.dp)){Text("Opções")}
                TextButton(onClick=end,modifier=Modifier.heightIn(min=48.dp)){Text("Encerrar")}
            }
        }
    }
}

@Composable internal fun PocketScreen(start:()->Unit,test:()->Unit) {
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
        Primary("Iniciar modo bolso",start)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun Finished(state:TimerState?,history:List<HistoryEntity>,feedback:(String,String)->Unit,done:()->Unit) {
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
            listOf("Muito fácil","Adequado","Difícil","Desconforto").forEach {value->
                FilterChip(selected==value,{selected=value;if(s!=null)feedback(s.id,value)},label={Text(value)})
            }
        }
        if(selected=="Desconforto")Copy("Interrompa os exercícios e procure orientação se o desconforto persistir. A progressão não será sugerida com esse relato recente.")
        Primary("Concluir",done)
    }
}
