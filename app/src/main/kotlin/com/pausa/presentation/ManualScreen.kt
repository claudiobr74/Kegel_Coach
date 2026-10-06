package com.pausa.presentation

import android.database.ContentObserver
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pausa.R
import com.pausa.domain.DemoPhase
import com.pausa.domain.manualDemoFrame
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign

private data class ManualSection(val id:String,@StringRes val title:Int,@ArrayRes val steps:Int)
private val sections=listOf(
    ManualSection("identify",R.string.manual_identify,R.array.manual_identify_steps),
    ManualSection("position",R.string.manual_position,R.array.manual_position_steps),
    ManualSection("slow",R.string.manual_slow,R.array.manual_slow_steps),
    ManualSection("quick",R.string.manual_quick,R.array.manual_quick_steps),
    ManualSection("check",R.string.manual_check,R.array.manual_check_steps),
    ManualSection("errors",R.string.manual_errors,R.array.manual_error_steps),
    ManualSection("safety",R.string.manual_safety,R.array.manual_safety_steps)
)

@Composable internal fun ManualScreen() {
    var expanded by rememberSaveable {mutableStateOf<String?>("identify")}
    var references by rememberSaveable {mutableStateOf(false)}
    ScrollContent(gap=16.dp) {
        Text("Aprenda a técnica",style=MaterialTheme.typography.headlineMedium)
        Copy(stringResource(R.string.manual_intro))
        sections.forEach {section->
            val open=expanded==section.id
            Panel {
                SectionHeader(stringResource(section.title),open,"manual-section-${section.id}") {
                    expanded=if(open)null else section.id
                }
                if(open) {
                    if(section.id in listOf("slow","quick"))ManualAnimation(section.id)
                    stringArrayResource(section.steps).forEachIndexed {index,step->
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text("${index+1}.",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                            Text(step,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                        }
                    }
                    when(section.id) {
                        "identify"->Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                            Text(stringResource(R.string.manual_urine_note),Modifier.padding(16.dp),
                                style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
                        }
                        "slow"->Copy(stringResource(R.string.manual_metaphor))
                        "quick"->{Copy(stringResource(R.string.manual_quality));Copy(stringResource(R.string.manual_metaphor))}
                        "check"->Copy(stringResource(R.string.manual_no_detection))
                        "safety"->{Copy(stringResource(R.string.manual_individual));Copy(stringResource(R.string.manual_disclaimer))}
                    }
                }
            }
        }
        Panel {
            SectionHeader(stringResource(R.string.manual_references),references,"manual-references") {references=!references}
            if(references)SelectionContainer {
                Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    stringArrayResource(R.array.manual_reference_items).forEach {Text(it,style=MaterialTheme.typography.bodySmall)}
                }
            }
        }
        Copy(stringResource(R.string.manual_disclaimer))
    }
}

@Composable private fun SectionHeader(title:String,expanded:Boolean,tag:String,onClick:()->Unit) {
    val state=stringResource(if(expanded)R.string.manual_expanded else R.string.manual_collapsed)
    val action=stringResource(if(expanded)R.string.manual_close else R.string.manual_open)
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag(tag)
        .clickable(role=Role.Button,onClickLabel=action,onClick=onClick)
        .semantics(mergeDescendants=true){stateDescription=state},
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
        Icon(androidx.compose.ui.res.painterResource(if(expanded)R.drawable.ic_ui_expand_less else R.drawable.ic_ui_expand_more),
            contentDescription=null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(24.dp))
    }
}

/** Both background lifecycle and the system animation preference stop educational playback. */
@Composable private fun rememberManualMotion():Pair<Boolean,Boolean> {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    fun enabled()=Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f
    var motion by remember {mutableStateOf(enabled())}
    var resumed by remember {mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
    DisposableEffect(lifecycle,context) {
        val observer=object:ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange:Boolean){motion=enabled()}
        }
        val events=LifecycleEventObserver {_,_->
            resumed=lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            motion=enabled()
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        lifecycle.addObserver(events)
        onDispose {context.contentResolver.unregisterContentObserver(observer);lifecycle.removeObserver(events)}
    }
    return motion to resumed
}

@Composable private fun ManualAnimation(id:String) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    var playing by rememberSaveable(id){mutableStateOf(false)}
    var failed by remember(id){mutableStateOf(false)}
    var loaded by remember {mutableStateOf(false)}
    val (motion,resumed)=rememberManualMotion()
    val active=playing && motion && resumed
    var elapsed by rememberSaveable(id) { mutableLongStateOf(0L) }
    // Freeze on pause/background; monotonic elapsed time drives both image and countdown.
    LaunchedEffect(active) {
        if(active) {
            val origin=SystemClock.elapsedRealtime()-elapsed
            try {
                while(true) {
                    elapsed=SystemClock.elapsedRealtime()-origin
                    delay(50)
                }
            } finally { elapsed=SystemClock.elapsedRealtime()-origin }
        }
    }
    val frame=manualDemoFrame(id=="quick",if(motion)elapsed else 0L)
    val file=frame.asset
    val request=remember(file,lifecycle) {
        ImageRequest.Builder(context).data("file:///android_asset/manual/$file")
            .allowHardware(false).crossfade(false).lifecycle(lifecycle).build()
    }
    val assetState=stringResource(if(loaded)R.string.manual_asset_ready else R.string.manual_asset_loading)
    val description=stringResource(if(id=="slow")R.string.manual_animation_slow_desc else R.string.manual_animation_quick_desc)
    val label=when(frame.phase) {
        DemoPhase.PREPARE->R.string.manual_phase_prepare
        DemoPhase.CONTRACT,DemoPhase.HOLD->R.string.manual_phase_contract
        DemoPhase.RELAX->R.string.manual_phase_relax
    }
    val enlargedFont=LocalDensity.current.fontScale>1.2f
    @Composable fun Clock(overlay:Boolean) {
        Column(Modifier.fillMaxWidth().testTag("manual-clock-$id"),
            horizontalAlignment=Alignment.CenterHorizontally) {
            val color=if(overlay)Color(0xFF172C2D) else MaterialTheme.colorScheme.onSurface
            Text(stringResource(label),style=MaterialTheme.typography.labelMedium,color=color)
            Text(frame.seconds.toString(),Modifier.fillMaxWidth(),
                style=MaterialTheme.typography.displaySmall.copy(
                    fontSize=28.sp,lineHeight=32.sp,fontFeatureSettings="tnum"),
                color=color,textAlign=TextAlign.Center)
        }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(.8f)) {
        AsyncImage(model=request,contentDescription=description,contentScale=ContentScale.Fit,
            modifier=Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                .testTag("manual-animation-$id").semantics {stateDescription=assetState},
            onLoading={loaded=false},onError={failed=true;loaded=false},onSuccess={failed=false;loaded=true})
        // Blank space above the head is reserved for the native caption/countdown.
        // With large fonts move it below the image to preserve posture visibility.
        if(!enlargedFont)Box(Modifier.align(Alignment.TopCenter).padding(top=6.dp)) { Clock(true) }
    }
    if(enlargedFont)Clock(false)
    Text(stringResource(when(frame.phase) {
        DemoPhase.PREPARE->R.string.manual_cue_prepare
        DemoPhase.CONTRACT->R.string.manual_cue_contract
        DemoPhase.HOLD->R.string.manual_cue_hold
        DemoPhase.RELAX->R.string.manual_cue_relax
    }),Modifier.fillMaxWidth().heightIn(min=48.dp),
        style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
    if(failed)Copy(stringResource(R.string.manual_load_error))
    FilledTonalButton(onClick={playing=!playing},enabled=motion,
        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("manual-play-$id"),
        colors=ButtonDefaults.filledTonalButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant,
            contentColor=MaterialTheme.colorScheme.primary)) {
        Text(stringResource(if(playing && motion)R.string.manual_pause else R.string.manual_play),fontWeight=FontWeight.SemiBold)
    }
    if(!motion)Copy(stringResource(R.string.manual_reduced_motion))
}
