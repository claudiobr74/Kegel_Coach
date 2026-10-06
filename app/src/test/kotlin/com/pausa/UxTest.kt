package com.pausa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pausa.domain.*
import com.pausa.presentation.*
import com.pausa.data.Settings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w320dp-h568dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class UxTest {
    @get:Rule val compose=createComposeRule()

    @Test fun activeWorkoutReplacesStartAndQuickActions() {
        val session=WorkoutSession("ux",WorkoutPreset.beginner,0,0,SessionStatus.PAUSED)
        val state=WorkoutTimer({0L},session).state()
        var continued=false
        compose.setContent {PausaTheme(AppTheme.LIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Home(Settings(),emptyList(),state,{}, {continued=it=="session"},{})
            }
        }}
        compose.onNodeWithText("Continuar treino").assertIsDisplayed().performClick()
        assertTrue(continued)
        compose.onNodeWithText("Iniciar treino").assertDoesNotExist()
        compose.onNodeWithText("Treino rápido").assertDoesNotExist()
    }

    @Test fun numericEditingRejectsOutOfRangeAndSavesEnteredValue() {
        var saved:Workout?=null
        compose.setContent {PausaTheme(AppTheme.LIGHT) {
            Surface(Modifier.fillMaxSize()) {CustomWorkout(WorkoutPreset.beginner,{saved=it},{})}
        }}
        compose.onNodeWithText("3 s").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("99")
        compose.onNodeWithText("Aplicar").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("7")
        compose.onNodeWithText("Aplicar").performClick()
        compose.onNodeWithText("Salvar treino").assertIsDisplayed().performClick()
        assertEquals(7,saved!!.blocks.first().contractSeconds)
    }

    @Test fun compactLargeFontKeepsPauseAndEndAccessible() {
        val session=WorkoutSession("ux-large",WorkoutPreset.beginner,0,0,SessionStatus.PAUSED)
        val state=WorkoutTimer({0L},session).state()
        var pauses=0
        compose.setContent {CompositionLocalProvider(LocalDensity provides Density(2f,1.6f)) {
            PausaTheme(AppTheme.DARK) {Surface(Modifier.fillMaxSize()) {
                SessionScreen(state,UserPreferences(),null,{pauses++},{},{},{})
            }}
        }}
        compose.onNodeWithText("Retomar").assertIsDisplayed().performClick()
        assertEquals(1,pauses)
        compose.onNodeWithText("Encerrar").assertIsDisplayed()
        compose.onNodeWithText("Opções").performClick()
        compose.onNodeWithText("Modo bolso").assertExists()
    }
}
