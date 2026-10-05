package com.pausa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class SmokeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun onboardingLeadsToOneTapWorkout() {
        compose.waitUntil(10000){compose.onAllNodesWithText("Continuar").fetchSemanticsNodes().isNotEmpty()}
        repeat(3){compose.onNodeWithText("Continuar").performScrollTo().performClick()}
        compose.onNodeWithText("COMEÇAR").performScrollTo().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("INICIAR TREINO").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("INICIAR TREINO").assertIsDisplayed()
        compose.onNodeWithText("Configurações").performClick()
        compose.onNodeWithText("Como fazer os exercícios").performScrollTo().performClick()
        compose.onNodeWithTag("manual-section-slow").performScrollTo().performClick()
        compose.onNodeWithTag("manual-play-slow").performScrollTo().performClick()
        compose.onNodeWithText("Pausar demonstração").assertExists().performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Treino rápido").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Pausar").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Pausar").performClick()
        compose.onNodeWithText("Retomar").assertExists()
    }
}
