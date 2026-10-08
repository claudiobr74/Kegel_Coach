package com.pausa.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.pausa.data.HistoryEntity
import com.pausa.domain.Progress
import java.time.*
import java.time.format.DateTimeFormatter

@Composable internal fun ProgressScreen(history:List<HistoryEntity>,today:LocalDate=LocalDate.now()) {
    val dates=remember(history) {history.mapNotNull {runCatching {LocalDate.parse(it.localDate)}.getOrNull()}.toSet()}
    val byDay=remember(history) {history.groupBy {it.localDate}}
    var month by rememberSaveable {mutableStateOf(YearMonth.from(today).toString())}
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    var all by rememberSaveable {mutableStateOf(false)}
    val ym=YearMonth.parse(month)
    val filtered=remember(history,ym,all) {if(all)history else history.filter {it.localDate.startsWith(ym.toString())}}
    if(selected!=null)AlertDialog(onDismissRequest={selected=null},title={Title(LocalDate.parse(selected).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            val sessions=byDay[selected].orEmpty()
            if(sessions.isEmpty())Copy("Nenhuma sessão registrada neste dia.")
            sessions.forEach {Copy("${it.workoutName} · ${duration(it.durationMillis)} · ${it.contractions} contrações");it.feedback?.let {value->Copy(value)}}
        }},confirmButton={TextButton({selected=null}){Text("Fechar")}})
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Panel {
            Title("Prática registrada")
            Text("${Progress.streak(dates,today)} dias seguidos",style=MaterialTheme.typography.headlineMedium)
            Copy("${dates.count {YearMonth.from(it)==ym}} dias praticados · ${history.count {it.localDate.startsWith(ym.toString())}} sessões no mês selecionado")
            Copy("Melhor sequência: ${Progress.bestStreak(dates)} dias")
            Copy("Os registros mostram sua rotina de uso; o app não mede força nem qualidade da contração.")
        }}
        item {Panel {
            Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton({month=ym.minusMonths(1).toString()},Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics {contentDescription="Mês anterior"}){Text("‹")}
                Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy",pt)),Modifier.weight(1f),textAlign=TextAlign.Center)
                TextButton({month=ym.plusMonths(1).toString()},Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics {contentDescription="Próximo mês"}){Text("›")}
            }
            Row {dayNames.forEach {Text(it,Modifier.weight(1f),fontSize=10.sp,textAlign=TextAlign.Center)}}
            val offset=ym.atDay(1).dayOfWeek.value-1
            repeat((offset+ym.lengthOfMonth()+6)/7) {row->Row {
                repeat(7) {col->
                    val day=row*7+col-offset+1
                    val date=if(day in 1..ym.lengthOfMonth())ym.atDay(day) else null
                    val done=date in dates
                    val color=if(done)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    Surface(Modifier.weight(1f).padding(1.dp),color=color,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border=if(date==today)BorderStroke(1.dp,MaterialTheme.colorScheme.primary) else null) {
                        Box(Modifier.heightIn(min=48.dp).then(if(date!=null)Modifier.clickable(role=Role.Button){selected=date.toString()} else Modifier)
                            .semantics {if(date!=null)contentDescription="$date: ${if(done)"concluído" else "sem treino"}"},contentAlignment=Alignment.Center) {
                            Text(if(date==null)"" else "$day${if(done)"✓" else ""}",fontSize=12.sp,color=if(done)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }}
        }}
        item {Panel {
            Title("Últimas quatro semanas")
            val monday=today.minusDays((today.dayOfWeek.value-1).toLong())
            (3 downTo 0).forEach {weeksAgo->val week=monday.minusWeeks(weeksAgo.toLong());val count=(0..6).count {week.plusDays(it.toLong()) in dates}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Copy(if(weeksAgo==0)"Esta semana" else week.format(DateTimeFormatter.ofPattern("dd/MM")));Copy("$count de 7 dias")}
                LinearProgressIndicator(progress={count/7f},modifier=Modifier.fillMaxWidth())
            }
        }}
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Histórico",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            FilterChip(all,{all=!all},label={Text(if(all)"Todos" else "Mês selecionado")})
        }}
        if(filtered.isEmpty())item {Panel {Title("Sua rotina começa com um treino");Copy("Nenhum registro neste período. Conclua uma sessão para acompanhar sua prática.")}}
        items(filtered,key={it.id}) {h->Panel {
            Title(h.workoutName);Copy("${LocalDate.parse(h.localDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${duration(h.durationMillis)} · ${h.contractions} contrações")
            h.feedback?.let {Copy(it)}
        }}
    }
}
