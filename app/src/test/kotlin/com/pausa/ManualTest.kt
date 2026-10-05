package com.pausa

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.pausa.domain.AppTheme
import com.pausa.presentation.ManualScreen
import com.pausa.presentation.PausaTheme
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
class ManualTest {
    @get:Rule val compose=createComposeRule()

    @Test fun suppliedAnimationsAndStaticPreviewsDecodeOffline() {
        val context=ApplicationProvider.getApplicationContext<PausaApplication>()
        for(name in listOf("01_prepare.png","02_contraia.png","03_mantenha.png",
            "04_relaxe.png","05_rapida_contraia.png","06_rapida_relaxe.png")) {
            context.assets.open("manual/$name").use {
                val bitmap=requireNotNull(BitmapFactory.decodeStream(it))
                assertEquals(1200,bitmap.width);assertEquals(1500,bitmap.height)
            }
        }

    }

    @Test fun compactLightWithLargeFontAndRestoredSections()=verifyCompact(AppTheme.LIGHT)
    @Test fun compactDarkWithLargeFontAndReducedMotion()=verifyCompact(AppTheme.DARK)

    private fun verifyCompact(theme:AppTheme) {
        val context=ApplicationProvider.getApplicationContext<PausaApplication>()
        Settings.Global.putFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,0f)
        val restoration=StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f,1.6f)) {
                PausaTheme(theme) {
                    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){ManualScreen()}
                }
            }
        }
        compose.onNodeWithTag("manual-section-slow").performScrollTo().performClick()
        val animation=compose.onNodeWithTag("manual-animation-slow").performScrollTo()
        compose.waitUntil(10000) {
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                "Demonstração disponível")).fetchSemanticsNodes().isNotEmpty()
        }
        val bounds=animation.getUnclippedBoundsInRoot()
        assertEquals(.8f,(bounds.right-bounds.left)/(bounds.bottom-bounds.top),.005f)
        animation.assertContentDescriptionContains("Demonstração de contração lenta",substring=true)
        compose.onNodeWithTag("manual-play-slow").performScrollTo().assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("manual-section-slow").performScrollTo().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Expandida"))
        compose.onNodeWithTag("manual-section-quick").performScrollTo().performClick()
        compose.onNodeWithTag("manual-animation-quick").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("manual-references").performScrollTo().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Recolhida"))
        compose.onNodeWithTag("manual-references").performClick()
        compose.onNode(hasText("10.1002/14651858.CD005654.pub4",substring=true)).performScrollTo().assertIsDisplayed()
    }
}
