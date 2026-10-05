package com.pausa

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class BrandTest {
    @Test fun installedLauncherUsesAdaptiveIconAndCorrectVersion() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context.getDrawable(R.mipmap.ic_launcher) is AdaptiveIconDrawable)
        assertEquals("1.0.2",BuildConfig.VERSION_NAME)
        assertEquals(3,BuildConfig.VERSION_CODE)
        assertNotNull(context.getDrawable(R.drawable.ic_notification))
    }
}
