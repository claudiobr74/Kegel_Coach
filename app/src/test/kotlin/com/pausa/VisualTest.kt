package com.pausa

import android.graphics.Bitmap
import android.graphics.Canvas
import com.pausa.domain.WorkoutTimer
import com.pausa.domain.WorkoutSession
import com.pausa.domain.SessionStatus
import com.pausa.domain.WorkoutPreset
import com.pausa.service.SessionState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class VisualTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun bitmap(): Bitmap {
        compose.waitForIdle()
        lateinit var bitmap: Bitmap
        compose.runOnUiThread {
            val view=compose.activity.window.decorView
            bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
        }
        return bitmap
    }
    private fun capture(name:String) {
        val file=File("build/visuals/$name.png").also {it.parentFile?.mkdirs()}
        file.outputStream().use {bitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun renderOnboardingHomeCustomAndDark() {
        compose.waitUntil(15000){compose.onAllNodesWithText("Continuar").fetchSemanticsNodes().isNotEmpty()}
        capture("onboarding")
        repeat(3){i->
            compose.onNodeWithText("Continuar").performScrollTo().performClick()
            capture("onboarding-${i+2}")
            if(i==0) {
                compose.onNodeWithText("Ver manual de execução").performScrollTo().performClick()
                compose.onNodeWithText("Manual de execução").assertExists()
                capture("manual-light")
                compose.onNodeWithContentDescription("Voltar").performClick()
                compose.onNodeWithText("Contraia. Relaxe. Repita.").assertExists()
            }
        }
        compose.onNodeWithText("COMEÇAR").performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Iniciar treino").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Kegel Coach").assertIsDisplayed()
        capture("home")
        compose.onNodeWithText("Personalizar").performScrollTo().performClick()
        capture("custom")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Treinos").performClick()
        capture("programs")
        compose.onNodeWithText("Progresso").performClick()
        capture("progress")
        compose.onNodeWithText("Ajustes").performClick()
        capture("settings")
        compose.onNodeWithText("Escuro").performScrollTo().performClick()
        compose.onNodeWithText("Início").performClick()
        compose.waitUntil(10000) {bitmap().getPixel(4,4)==android.graphics.Color.rgb(17,30,36)}
        compose.onNodeWithContentDescription("Kegel Coach").assertIsDisplayed()
        capture("home-dark")
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Como fazer os exercícios").performScrollTo().performClick()
        capture("manual-dark")
        compose.onNodeWithTag("manual-section-slow").performScrollTo().performClick()
        compose.onNodeWithTag("manual-animation-slow").performScrollTo()
        compose.waitUntil(10000) {
            compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Demonstração disponível")).fetchSemanticsNodes().isNotEmpty()
        }
        capture("manual-slow-dark")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onAllNodesWithText("Ajustes").assertCountEquals(2)
        compose.onNodeWithText("Início").performClick()
        compose.onNodeWithText("Modo bolso").performScrollTo().performClick()
        capture("pocket")
        compose.onNodeWithContentDescription("Voltar").performClick()
        var now=0L
        val timer=WorkoutTimer({now},WorkoutSession("visual",WorkoutPreset.beginner,0,0,SessionStatus.RUNNING))
        SessionState.state.value=timer.state()
        compose.onNodeWithText("Continuar treino").performClick()
        compose.onNodeWithContentDescription("Contraia, 3 segundos restantes").assertExists()
        capture("session-contract-dark")
        now=500L
        val half=timer.state()
        SessionState.state.value=half.copy(session=half.session.copy(status=SessionStatus.PAUSED))
        compose.onNodeWithContentDescription("Treino pausado, 3 segundos restantes").assertExists()
        capture("session-counter-half-dark")
        now=3000L
        SessionState.state.value=timer.state()
        compose.onNodeWithContentDescription("Relaxe, 6 segundos restantes").assertExists()
        capture("session-relax-dark")
        SessionState.state.value=null
    }
}
