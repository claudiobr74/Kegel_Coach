package com.pausa

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import java.io.File
import org.robolectric.annotation.GraphicsMode
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BrandTest {
    @Test fun installedLauncherUsesAdaptiveIconAndCorrectVersion() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context.getDrawable(R.mipmap.ic_launcher) is AdaptiveIconDrawable)
        assertEquals("Kegel Coach",context.applicationInfo.loadLabel(context.packageManager).toString())
        assertEquals("1.1.5",BuildConfig.VERSION_NAME)
        assertEquals(11,BuildConfig.VERSION_CODE)
        assertNotNull(context.getDrawable(R.drawable.ic_notification))
        val canvasBitmap=Bitmap.createBitmap(432,432,Bitmap.Config.ARGB_8888)
        context.getDrawable(R.mipmap.ic_launcher)!!.apply {setBounds(0,0,432,432);draw(Canvas(canvasBitmap))}
        File("build/visuals/launcher.png").apply {parentFile?.mkdirs()}.outputStream().use {
            canvasBitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
        val logo=BitmapFactory.decodeResource(context.resources,R.drawable.kegel_coach_logo)
        assertEquals(1536,logo.width);assertEquals(1024,logo.height)
    }
}
