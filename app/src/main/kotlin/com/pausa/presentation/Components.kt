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

@Composable fun Fig(node:String,asset:String,size:Dp,modifier:Modifier=Modifier,description:String?=null,tint:Color?=null) {
    val nativeIcon=when("$node/$asset") {
        "23:770/imgDestino" -> R.drawable.ic_ui_home
        "23:770/imgDestino1" -> R.drawable.ic_ui_play
        "23:770/imgDestino2" -> R.drawable.ic_ui_progress
        "23:770/imgDestino3" -> R.drawable.ic_ui_settings
        "23:770/imgIcone1" -> R.drawable.ic_ui_programs
        "23:770/imgIcone2" -> R.drawable.ic_ui_pocket
        "23:770/imgIcone3" -> R.drawable.ic_ui_reminders
        "23:855/imgIcone" -> R.drawable.ic_ui_back
        "23:1181/imgIcone1" -> R.drawable.ic_ui_minus
        "23:1181/imgIcone2" -> R.drawable.ic_ui_plus
        else -> null
    }
    if(nativeIcon!=null) {
        Icon(painterResource(nativeIcon), contentDescription=description, modifier=modifier.size(size),
            tint=tint ?: MaterialTheme.colorScheme.primary)
        return
    }
    val context=LocalContext.current
    val map=remember {JSONObject(context.assets.open("asset-map.json").bufferedReader().use {it.readText()})}
    val filename=map.getJSONObject(node).getString(asset)
    AsyncImage(model="file:///android_asset/figma/$filename",contentDescription=description,
        modifier=modifier.size(size),contentScale=ContentScale.Fit,
        colorFilter=tint?.let {ColorFilter.tint(it)})
}
@Composable internal fun Brand() {
    Column(Modifier.widthIn(max=280.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        BrandSymbol(Modifier.size(64.dp))
        BrandName()
    }
}
@Composable internal fun HeaderBrand() {
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription="Kegel Coach" },
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        BrandSymbol(Modifier.size(40.dp))
        BrandName(Modifier.weight(1f),compact=true)
    }
}
@Composable internal fun Title(text:String) {Text(text,style=MaterialTheme.typography.titleLarge)}
@Composable internal fun Copy(text:String) {Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable internal fun Primary(text:String,onClick:()->Unit,enabled:Boolean=true) {
    Button(onClick,Modifier.fillMaxWidth().heightIn(min=56.dp),enabled=enabled,shape=RoundedCornerShape(16.dp)) {Text(text)}
}
@Composable internal fun Secondary(text:String,onClick:()->Unit,modifier:Modifier=Modifier) {
    FilledTonalButton(onClick,modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp),
        colors=ButtonDefaults.filledTonalButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant,
            contentColor=MaterialTheme.colorScheme.primary)) {Text(text)}
}
@Composable internal fun Panel(accent:Boolean=false,padding:Dp=16.dp,gap:Dp=12.dp,content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),
        color=if(accent)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shadowElevation=0.dp) {Column(Modifier.padding(padding),verticalArrangement=Arrangement.spacedBy(gap),content=content)}
}
@Composable internal fun ScrollContent(gap:Dp=12.dp,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(gap),content=content)
}
