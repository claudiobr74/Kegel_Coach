package com.pausa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pausa.domain.*
import com.pausa.presentation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ProfessionalUxTest {
    @get:Rule val compose=createComposeRule()
    @Test fun mixedEditorKeepsBothBlocksAndChosenName() {
        var saved:Workout?=null
        compose.setContent {PausaTheme(AppTheme.LIGHT) {Surface(Modifier.fillMaxSize()) {CustomWorkout(WorkoutPreset.mixed,{saved=it},{})}}}
        compose.onNode(hasSetTextAction()).performTextReplacement("Meu misto")
        compose.onNodeWithText("Salvar treino").performClick()
        assertEquals("Meu misto",saved!!.name)
        assertEquals(WorkoutPreset.mixed.blocks,saved!!.blocks)
    }
    @Test fun pausedTrainingInstructsRelaxationInsteadOfContraction() {
        val state=WorkoutTimer({0L},WorkoutSession("p",WorkoutPreset.beginner,0,0,SessionStatus.PAUSED)).state()
        compose.setContent {PausaTheme(AppTheme.DARK) {Surface(Modifier.fillMaxSize()) {SessionScreen(state,UserPreferences(),null,{},{},{},{})}}}
        compose.onNodeWithText("Relaxe completamente. Retome quando estiver pronto.").assertExists()
        compose.onNodeWithText("Contraia suavemente. Respire normalmente.").assertDoesNotExist()
    }
    @Test fun navigationReturnsToOriginAndRestoresStack() {
        val navigator=AppNavigator();var route by navigator
        route="settings";route="reminders";navigator.back();assertEquals("settings",route)
        route="manual";navigator.back();assertEquals("settings",route)
        route="programs";route="custom";navigator.back();assertEquals("programs",route)
    }
}
