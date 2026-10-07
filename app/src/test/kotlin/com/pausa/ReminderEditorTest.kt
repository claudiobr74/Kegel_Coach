package com.pausa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pausa.data.ReminderEntity
import com.pausa.domain.AppTheme
import com.pausa.presentation.*
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
class ReminderEditorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun dayEditsAreLocalUntilSavedAndLastDayCanBeCleared() {
        val reminders=mutableStateOf(listOf(ReminderEntity("r",8,30,1)))
        val saved=mutableListOf<ReminderEntity>()
        compose.setContent {PausaTheme(AppTheme.LIGHT) {Surface(Modifier.fillMaxSize()) {
            RemindersScreen(reminders.value,{}, {_,_,_->}, {saved.add(it);reminders.value=listOf(it)}, {})
        }}}
        compose.onNodeWithText("Editar horário e dias").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Dia DOM").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dia SEG").performClick()
        compose.onNodeWithText("Selecione pelo menos um dia.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Dia DOM").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Dia TER").performClick()
        assertTrue(saved.isEmpty())
        compose.onNodeWithText("Salvar").performClick()
        assertEquals(1,saved.size)
        assertEquals(66,saved.single().daysMask)
        compose.onNodeWithText("TER · DOM").assertExists()
        compose.onNodeWithText("Editar horário e dias").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Dia DOM").assertIsSelected()
        compose.onNodeWithContentDescription("Dia TER").assertIsSelected()
        compose.onNodeWithContentDescription("Dia QUA").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1,saved.size)
    }
    @Test fun newReminderSavesChosenDaysWithoutIntermediateWrites() {
        var mask=0
        compose.setContent {PausaTheme(AppTheme.LIGHT) {Surface(Modifier.fillMaxSize()) {
            RemindersScreen(emptyList(),{}, {_,_,days->mask=days}, {}, {})
        }}}
        compose.onNodeWithText("+ Adicionar horário").performScrollTo().performClick()
        compose.onNodeWithText("Sábado e domingo").performScrollTo().performClick()
        assertEquals(0,mask)
        compose.onNodeWithText("Salvar").performClick()
        assertEquals(96,mask)
    }
}
