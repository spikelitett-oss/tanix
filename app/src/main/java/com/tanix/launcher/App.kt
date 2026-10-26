package com.tanix.launcher

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tanix.launcher.data.Net

class App : Application(), ImageLoaderFactory {
    // Картинки идут через общий клиент (DNS-over-HTTPS / зеркало TMDB)
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this).okHttpClient(Net.client).crossfade(true).build()
}
