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
        compose.onNodeWithContentDescription("Dia DOM").performScrollTo().assertIsDisplayed()
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
    @Test fun correctingSevenToEightSavesEightAndShowsItWhenReopened() {
        val reminders=mutableStateOf(listOf(ReminderEntity("time",7,0,127)))
        val saved=mutableListOf<ReminderEntity>()
        compose.setContent {PausaTheme(AppTheme.LIGHT) {Surface(Modifier.fillMaxSize()) {
            RemindersScreen(reminders.value,{}, {_,_,_->}, {saved.add(it);reminders.value=listOf(it)}, {})
        }}}
        compose.onNodeWithText("Editar horário e dias").performScrollTo().performClick()
        compose.onNodeWithTag("reminder-hour").performTextReplacement("08")
        compose.onNodeWithTag("reminder-minute").performTextReplacement("00")
        compose.onNodeWithText("O aviso será às 08:00").assertExists()
        compose.onNodeWithText("Salvar").performClick()
        assertEquals(8,saved.single().hour)
        assertEquals(0,saved.single().minute)
        assertEquals(127,saved.single().daysMask)
        compose.onNodeWithText("08:00").assertExists()
        compose.onNodeWithText("Editar horário e dias").performScrollTo().performClick()
        compose.onNodeWithTag("reminder-hour").assertTextContains("08")
        compose.onNodeWithTag("reminder-minute").assertTextContains("00")
        compose.onNodeWithTag("reminder-hour").performTextReplacement("09")
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1,saved.size)
        compose.onNodeWithText("08:00").assertExists()
    }
    @Test fun invalidOrIncompleteTimesCannotBeSaved() {
        var created=0
        compose.setContent {PausaTheme(AppTheme.LIGHT) {Surface(Modifier.fillMaxSize()) {
            RemindersScreen(emptyList(),{}, {_,_,_->created++}, {}, {})
        }}}
        compose.onNodeWithText("+ Adicionar horário").performScrollTo().performClick()
        compose.onNodeWithTag("reminder-hour").performTextReplacement("24")
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithTag("reminder-hour").performTextReplacement("08")
        compose.onNodeWithTag("reminder-minute").performTextReplacement("60")
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithTag("reminder-minute").performTextReplacement("")
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithTag("reminder-minute").performTextReplacement("30")
        compose.onNodeWithText("Salvar").assertIsEnabled().performClick()
        assertEquals(1,created)
    }
}
