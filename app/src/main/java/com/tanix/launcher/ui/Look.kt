package com.tanix.launcher.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.tanix.launcher.data.LauncherSettings

/** Нейтральный тёмный фон плиток приложений (без цветов Material You). */
val NeutralTile = androidx.compose.ui.graphics.Color(0xFF202124)

val LocalSettings = staticCompositionLocalOf { LauncherSettings() }

/** Таблицы значений для индексов из настроек. */
object Look {
    val UiScales = floatArrayOf(.85f, .92f, 1f, 1.1f, 1.2f)
    val FontScales = floatArrayOf(.9f, 1f, 1.15f)
    val Paddings = intArrayOf(24, 40, 56, 72)
    val Corners = intArrayOf(12, 20, 28)
    val IconSizes = intArrayOf(56, 68, 80, 96)
    val PosterW = intArrayOf(110, 130, 160)
    val LogoAlphas = floatArrayOf(.35f, .55f, .75f, 1f)
    val FocusK = floatArrayOf(0f, .5f, 1f, 1.6f)
    val SpeedK = floatArrayOf(.6f, 1f, 1.5f)
    val BgDim = floatArrayOf(.55f, .7f, .85f)
    val TileW = intArrayOf(112, 136, 160, 192)   // ширина баннерной плитки приложения
    val PickW = intArrayOf(140, 172, 210)         // ширина карточки подборки (16:9)

    fun at(a: FloatArray, i: Int): Float = a[i.coerceIn(0, a.lastIndex)]
    fun at(a: IntArray, i: Int): Int = a[i.coerceIn(0, a.lastIndex)]
}

/** Длительность анимации с учётом настроек (0, если анимации выключены). */
@Composable
fun animMs(base: Int): Int {
    val s = LocalSettings.current
    return if (!s.animOn) 0 else (base * Look.at(Look.SpeedK, s.animSpeed)).toInt()
}

fun iconShapeOf(i: Int): Shape = when (i) {
    0 -> CircleShape
    1 -> RoundedCornerShape(28)
    else -> RoundedCornerShape(10.dp)
}

@Composable
fun cardShape(): Shape = RoundedCornerShape(Look.at(Look.Corners, LocalSettings.current.cornerStyle).dp)
