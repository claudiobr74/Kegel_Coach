package com.pausa.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable internal fun DataScreen(vm:AppViewModel) {
    val busy by vm.saving.collectAsStateWithLifecycle()
    val preview by vm.backupPreview.collectAsStateWithLifecycle()
    val error by vm.operationError.collectAsStateWithLifecycle()
    var direction by remember {mutableStateOf<String?>(null)}
    var password by remember {mutableStateOf("")}
    var confirmation by remember {mutableStateOf("")}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {uri->
        vm.finishBackupTransfer(uri,false)
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        vm.finishBackupTransfer(uri,true)
    }
    if(direction!=null)AlertDialog(onDismissRequest={direction=null;password="";confirmation=""},title={Title(if(direction=="export")"Proteger a cópia" else "Abrir cópia protegida")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Copy(if(direction=="export")"Crie uma senha de pelo menos 8 caracteres e guarde-a. Não há recuperação dessa senha." else "Informe a senha usada para criar o arquivo.")
            OutlinedTextField(password,{password=it.take(256)},label={Text("Senha")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
            if(direction=="export")OutlinedTextField(confirmation,{confirmation=it.take(256)},label={Text("Confirmar senha")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
        }},confirmButton={TextButton({
            vm.prepareBackupTransfer(password)
            if(direction=="export")export.launch("Kegel_Coach_${LocalDate.now()}.kcb") else restore.launch(arrayOf("*/*"))
            direction=null;password="";confirmation=""
        },enabled=password.length>=8 && (direction!="export" || password==confirmation)){Text("Escolher arquivo")}},dismissButton={TextButton({direction=null;password="";confirmation=""}){Text("Cancelar")}})
    if(preview!=null)AlertDialog(onDismissRequest={if(!busy)vm.cancelRestore()},title={Title("Restaurar esta cópia?")},
        text={Copy("${preview!!.history.size} sessões, ${preview!!.workouts.size} treinos salvos e ${preview!!.reminders.size} lembretes. Os dados e ajustes atuais serão substituídos. Exporte uma cópia atual antes se desejar preservá-los.")},
        confirmButton={TextButton({vm.restoreBackup()},enabled=!busy){Text(if(busy)"Restaurando…" else "Substituir e restaurar")}},dismissButton={TextButton({vm.cancelRestore()},enabled=!busy){Text("Cancelar")}})
    ScrollContent {
        Panel(true) {Title("Seus dados, sua cópia");Copy("Exporte sessões, treinos salvos, ajustes e lembretes para continuar em outro aparelho. O arquivo é protegido pela senha escolhida por você.")}
        Primary(if(busy)"Processando…" else "Exportar cópia protegida",{direction="export"},!busy)
        Secondary("Restaurar uma cópia",{if(!busy)direction="restore"})
        if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
        Copy("Sessões em andamento e registros técnicos não entram na cópia. Encerre o treino antes de restaurar. Os lembretes restaurados seguem o horário local do aparelho.")
        Copy("Sem uma cópia exportada, a desinstalação ou limpeza dos dados pode apagar seu histórico. O app não envia a cópia automaticamente.")
    }
}
