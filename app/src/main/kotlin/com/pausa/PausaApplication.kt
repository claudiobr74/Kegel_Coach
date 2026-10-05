package com.pausa

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build

class PausaApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).components {
        add(SvgDecoder.Factory())
        if(Build.VERSION.SDK_INT>=28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
    }.build()
}
