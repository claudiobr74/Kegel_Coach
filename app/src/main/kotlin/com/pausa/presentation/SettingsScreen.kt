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

private fun guidanceName(value:Guidance)=when(value) {
    Guidance.BOTH->"Vibração + tela";Guidance.VIBRATION->"Vibração";Guidance.SCREEN->"Tela";Guidance.SOUND->"Som";Guidance.VOICE->"Voz offline"
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun SettingsScreen(settings:Settings,update:((UserPreferences)->UserPreferences)->Unit,
    navigate:(String)->Unit,clear:()->Unit,info:(String)->Unit,busy:Boolean=false) {
    val u=settings.user
    var sheet by rememberSaveable {mutableStateOf<String?>(null)}
    if(sheet!=null)ModalBottomSheet(onDismissRequest={sheet=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Title(if(sheet=="guidance")"Orientação" else "Aparência")
            if(sheet=="guidance") {
                Guidance.entries.forEach {value->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(value==u.guidance,role=Role.RadioButton,enabled=!busy,onClick={update {it.copy(guidance=value)};sheet=null}),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(value==u.guidance,null);Text(guidanceName(value),Modifier.padding(start=8.dp))
                }}
                Copy("Voz usa apenas uma voz em português instalada para uso offline. Na ausência dela, são usados sinais sonoros.")
            } else AppTheme.entries.forEach {value->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(value==u.theme,role=Role.RadioButton,enabled=!busy,onClick={update {it.copy(theme=value)};sheet=null}),verticalAlignment=Alignment.CenterVertically) {
                RadioButton(value==u.theme,null);Text(when(value){AppTheme.SYSTEM->"Seguir sistema";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"},Modifier.padding(start=8.dp))
            }}
        }
    }
    ScrollContent {
        Title("Treino")
        Panel(gap=0.dp,padding=12.dp) {
            ActionRow("Treino selecionado",settings.workout.name,R.drawable.ic_ui_programs,{navigate("programs")})
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){},verticalAlignment=Alignment.CenterVertically) {Text("Sugerir progressão",Modifier.weight(1f));Switch(u.progressionEnabled,{value->update {it.copy(progressionEnabled=value)}},enabled=!busy)}
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){},verticalAlignment=Alignment.CenterVertically) {Text("Preparação de 3 segundos",Modifier.weight(1f));Switch(u.preparationEnabled,{value->update {it.copy(preparationEnabled=value)}},enabled=!busy)}
            Copy("A preparação acontece antes do treino e não entra na duração registrada.")
        }
        Title("Experiência")
        Panel(gap=0.dp,padding=12.dp) {
            ActionRow("Orientação",guidanceName(u.guidance),R.drawable.ic_ui_pocket,{sheet="guidance"})
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){},verticalAlignment=Alignment.CenterVertically) {Text("Tela discreta",Modifier.weight(1f));Switch(u.discreetScreen,{value->update {it.copy(discreetScreen=value)}},enabled=!busy)}
            ActionRow("Modo bolso","Vibrações somente nesta sessão",R.drawable.ic_ui_pocket,{navigate("pocket")})
            ActionRow("Lembretes","Horários, dias e teste de aviso",R.drawable.ic_ui_reminders,{navigate("reminders")})
            ActionRow("Aparência",when(u.theme){AppTheme.SYSTEM->"Seguir sistema";AppTheme.LIGHT->"Claro";AppTheme.DARK->"Escuro"},R.drawable.ic_ui_settings,{sheet="theme"})
        }
        Title("Dados e privacidade")
        Panel {
            Copy("Dados ficam somente neste aparelho. Sem conta, anúncios ou envio de informações. A cópia opcional é salva no local que você escolher.")
            ActionRow("Cópia e restauração","Arquivo protegido por sua senha",R.drawable.ic_ui_programs,{navigate("data")})
            TextButton(onClick=clear,modifier=Modifier.heightIn(min=48.dp)){Text("Excluir histórico",color=MaterialTheme.colorScheme.error)}
        }
        Title("Ajuda")
        ActionRow(stringResource(R.string.manual_entry),"Técnica, demonstrações e cuidados",R.drawable.ic_ui_manual,{navigate("manual")})
        ActionRow("Ajuda e diagnóstico","Dúvidas comuns e funcionamento",R.drawable.ic_ui_settings,{navigate("help")})
        Secondary("Segurança",{info("Pare se houver dor ou desconforto persistente. Procure orientação em caso de sintomas urinários, pélvicos ou pós-operatórios. O app não substitui avaliação médica ou fisioterapêutica. No carro, treine apenas parado.")})
        HeaderBrand()
        Copy("Versão ${BuildConfig.VERSION_NAME} · Cuidado discreto. No seu ritmo.")
    }
}
