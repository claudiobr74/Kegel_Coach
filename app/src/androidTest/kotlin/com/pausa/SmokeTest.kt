package com.pausa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

class SmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun onboardingLeadsToOneTapWorkout() {
        compose.waitUntil(10000){compose.onAllNodesWithText("Continuar").fetchSemanticsNodes().isNotEmpty()}
        repeat(3){compose.onNodeWithText("Continuar").performScrollTo().performClick()}
        compose.onNodeWithText("COMEÇAR").performScrollTo().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Iniciar treino").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Iniciar treino").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Como fazer os exercícios").performScrollTo().performClick()
        compose.onNodeWithTag("manual-section-slow").performScrollTo().performClick()
        compose.onNodeWithTag("manual-play-slow").performScrollTo().performClick()
        compose.onNodeWithText("Pausar demonstração").assertExists().performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Início").performClick()
        compose.onNodeWithText("Treino rápido").performScrollTo().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Pausar").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Pausar").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Retomar").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Retomar").assertExists()
        compose.onNodeWithText("Encerrar").performClick()
        compose.onNode(hasText("Encerrar") and hasAnyAncestor(isDialog()),useUnmergedTree=true).performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Iniciar treino").fetchSemanticsNodes().isNotEmpty()}
        val vm=androidx.lifecycle.ViewModelProvider(compose.activity)[com.pausa.presentation.AppViewModel::class.java]
        val prefs=com.pausa.data.PreferencesRepository(compose.activity)
        val before=kotlinx.coroutines.runBlocking {prefs.settings.first().user}
        val brief=com.pausa.domain.Workout("smoke","Sessão de teste",listOf(com.pausa.domain.WorkoutBlock(1,1,1)))
        compose.runOnIdle {vm.start(brief,true)}
        compose.waitUntil(10000){vm.session.value?.session?.status==com.pausa.domain.SessionStatus.COMPLETED}
        val after=kotlinx.coroutines.runBlocking {prefs.settings.first().user}
        org.junit.Assert.assertEquals(before,after)
        compose.onNodeWithText("Treino concluído").assertExists()
        compose.onNodeWithText("Concluir").performScrollTo().performClick()
        compose.onNodeWithText("Treino concluído hoje").assertExists()

    }
}
