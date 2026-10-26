package com.tanix.launcher.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap?, val adaptive: Boolean)

enum class HeroMode(val title: String) { RECO("Рекомендации"), FAVORITES("Избранное") }

data class HeroItem(
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val bgUrl: String? = null,
    val badge: String? = null,
    val packageName: String? = null,
    val iconPkg: String? = null,
    val color: Color = Color(0xFF3F51B5)
)

data class Weather(val temp: Double, val feels: Double, val code: Int, val wind: Double)

data class City(val name: String, val region: String, val lat: Double, val lon: Double)
