package com.pausa.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.pausa.data.Diagnostics
import com.pausa.BuildConfig

@Composable internal fun HelpScreen(vm:AppViewModel) {
    val context=LocalContext.current
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {uri->if(uri!=null)vm.exportDiagnostics(uri)}
    ScrollContent {
        Title("Dúvidas comuns")
        listOf(
            "O aviso atrasou?" to "Lembretes podem atrasar conforme as restrições do Android. Em Lembretes, confira o horário, os dias, o canal e faça um teste de aviso.",
            "Não senti vibração" to "Experimente os sinais no modo bolso. Confira a orientação escolhida e as restrições do aparelho. A intensidade depende do motor de vibração.",
            "O treino foi interrompido" to "O app recupera o último ponto salvo em pausa. Abra a sessão e escolha Retomar; o período sem orientação não é registrado como treino.",
            "Posso avançar o programa?" to "A sugestão usa dias de prática e relatos recentes. Avance somente com execução confortável e relaxamento completo. Você pode manter ou voltar uma etapa.",
            "O app mede meu fortalecimento?" to "Não. O app guia o tempo e registra sessões. A execução e evolução precisam de avaliação individual.",
            "Como preservar meu histórico?" to "Em Cópia e restauração, exporte um arquivo protegido por senha. Guarde tanto o arquivo quanto a senha."
        ).forEach {(title,copy)->Panel {Title(title);Copy(copy)}}
        Title("Diagnóstico técnico")
        Copy("O arquivo inclui versão do app, Android e eventos de funcionamento. Não inclui nomes de treinos, avaliações pessoais ou senhas. Você escolhe onde salvar.")
        Secondary("Exportar diagnóstico",{export.launch("Kegel_Coach_diagnostico.txt")})
        Copy("Conteúdo educativo consultado em 08/10/2026. Fontes e orientações estão no manual. Não substitui avaliação individual.")
        Copy("Kegel Coach ${BuildConfig.VERSION_NAME}")
    }
}
