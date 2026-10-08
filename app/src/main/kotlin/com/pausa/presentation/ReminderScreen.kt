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

@OptIn(ExperimentalLayoutApi::class)
@Composable fun RemindersScreen(reminders:List<ReminderEntity>,permission:()->Unit,
    create:(Int,Int,Int)->Unit,save:(ReminderEntity)->Unit,remove:(ReminderEntity)->Unit,
    saving:Boolean=false,savedSignal:Int=0,error:String?=null,autoClose:Boolean=true,test:()->Unit={},now:ZonedDateTime=ZonedDateTime.now()) {
    val context=LocalContext.current
    var editing by rememberSaveable {mutableStateOf(false)}
    LaunchedEffect(savedSignal) {if(savedSignal>0)editing=false}
    var editingId by rememberSaveable {mutableStateOf<String?>(null)}
    var hourText by rememberSaveable {mutableStateOf("08")}
    var minuteText by rememberSaveable {mutableStateOf("00")}
    var days by rememberSaveable {mutableIntStateOf(127)}
    fun edit(r:ReminderEntity?) {
        editingId=r?.id
        hourText="%02d".format(r?.hour ?: 8)
        minuteText="%02d".format(r?.minute ?: 0)
        days=r?.daysMask ?: 127
        editing=true
    }
    val hour=hourText.toIntOrNull()
    val minute=minuteText.toIntOrNull()
    val validTime=hour!=null && hour in 0..23 && minute!=null && minute in 0..59
    if(editing)AlertDialog(
        onDismissRequest={if(!saving)editing=false},
        title={Title(if(editingId==null)"Novo lembrete" else "Editar lembrete")},
        text={Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(hourText,{hourText=it.filter {c->c in '0'..'9'}.take(2)},
                    label={Text("Hora (00–23)")},singleLine=true,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                    isError=hour==null || hour !in 0..23,
                    modifier=Modifier.weight(1f).testTag("reminder-hour"))
                OutlinedTextField(minuteText,{minuteText=it.filter {c->c in '0'..'9'}.take(2)},
                    label={Text("Minuto (00–59)")},singleLine=true,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                    isError=minute==null || minute !in 0..59,
                    modifier=Modifier.weight(1f).testTag("reminder-minute"))
            }
            if(validTime)Text("Horário escolhido: %02d:%02d".format(hour,minute),fontWeight=FontWeight.SemiBold)
            else Text("Informe uma hora de 00 a 23 e minutos de 00 a 59.",color=MaterialTheme.colorScheme.error)
            Copy("O Android pode atrasar a entrega conforme as restrições de bateria.")
            if(error!=null)Text(error,color=MaterialTheme.colorScheme.error)
            Copy("Dias da semana")
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                dayNames.forEachIndexed {i,label->
                    FilterChip(days and (1 shl i)!=0,{days=days xor (1 shl i)},
                        label={Text(label)},modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)
                            .semantics {contentDescription="Dia $label"})
                }
            }
            TextButton({days=127}) {Text("Todos os dias")}
            TextButton({days=31}) {Text("Segunda a sexta")}
            TextButton({days=96}) {Text("Sábado e domingo")}
            if(days==0)Text("Selecione pelo menos um dia.",color=MaterialTheme.colorScheme.error)
        }},
        confirmButton={TextButton({
            val existing=reminders.firstOrNull {it.id==editingId}
            if(editingId==null)create(requireNotNull(hour),requireNotNull(minute),days)
            else if(existing!=null)save(existing.copy(hour=requireNotNull(hour),minute=requireNotNull(minute),daysMask=days))
            if(autoClose)editing=false
            if(existing?.enabled!=false)permission()
        },enabled=days!=0 && validTime && !saving) {Text(if(saving)"Salvando…" else "Salvar")}},
        dismissButton={TextButton({editing=false},enabled=!saving) {Text("Cancelar")}}
    )
    ScrollContent {
        Copy("Escolha o horário e os dias de cada lembrete.")
        val blocked=ReminderScheduler.notificationStatus(context)
        Panel(accent=blocked!=null) {
            Title(if(blocked==null)"Notificações disponíveis" else "Avisos bloqueados")
            Copy(blocked ?: "Faça um teste para verificar som e exibição neste aparelho.")
            if(blocked!=null)Secondary("Permitir notificações",permission)
            else Secondary("Testar aviso",test)
            if(blocked!=null)TextButton({ReminderScheduler.openSettings(context)}){Text("Abrir configurações de notificações")}
        }
        if(error!=null)Text(error,color=MaterialTheme.colorScheme.error)
        reminders.forEach {r->key(r.id) {Panel {
            Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton({edit(r)},enabled=!saving,modifier=Modifier.weight(1f)){Text("%02d:%02d".format(r.hour,r.minute),style=MaterialTheme.typography.titleLarge)}
                Switch(r.enabled,{save(r.copy(enabled=it));if(it)permission()},enabled=!saving,modifier=Modifier.semantics {contentDescription="Ativar lembrete %02d:%02d".format(r.hour,r.minute)})
            }
            Text(if(r.daysMask==127)"Todos os dias" else dayNames.filterIndexed {i,_->r.daysMask and (1 shl i)!=0}.joinToString(" · "))
            if(r.enabled && r.daysMask and 127 != 0) {
                val next=r.domain().nextAfter(now)
                Text("Próximo: "+next.format(DateTimeFormatter.ofPattern("EEE, dd/MM 'às' HH:mm",pt)),
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(r.snoozedUntil>System.currentTimeMillis())Text("Adiado até "+Instant.ofEpochMilli(r.snoozedUntil).atZone(now.zone).format(DateTimeFormatter.ofPattern("HH:mm",pt)),style=MaterialTheme.typography.bodySmall)
            } else Text("Lembrete desativado",style=MaterialTheme.typography.bodySmall)
            Row {
                TextButton({edit(r)},Modifier.weight(1f),enabled=!saving){Text("Editar horário e dias")}
                TextButton({remove(r)},Modifier.weight(1f),enabled=!saving){Text("Remover")}
            }
        }}}
        Secondary("+ Adicionar horário",{edit(null)})
        Panel {Copy("Mensagem da notificação");Text(stringResource(R.string.reminder_title))}
        Copy("Os lembretes podem atrasar conforme a economia de bateria do Android. Expanda a notificação para adiar por 10 min, 30 min ou 1 hora.")
    }
}
