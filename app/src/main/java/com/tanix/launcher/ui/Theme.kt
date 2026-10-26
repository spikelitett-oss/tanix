package com.tanix.launcher.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.tanix.launcher.data.LauncherSettings

val AccentHues = listOf(250f, 220f, 195f, 170f, 140f, 95f, 60f, 35f, 12f, 345f, 315f, 285f)

val LocalDark = staticCompositionLocalOf { true }

fun hsl(h: Float, s: Float, l: Float) = Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))

/** Палитра Material 3 от выбранного оттенка (для Android 11 и ниже, где нет динамических цветов). */
fun hueScheme(h: Float, dark: Boolean): ColorScheme {
    val t = (h + 60f) % 360f
    return if (dark) darkColorScheme(
        primary = hsl(h, .85f, .80f), onPrimary = hsl(h, .90f, .18f),
        primaryContainer = hsl(h, .55f, .30f), onPrimaryContainer = hsl(h, .90f, .90f),
        secondary = hsl(h, .30f, .75f), onSecondary = hsl(h, .30f, .20f),
        secondaryContainer = hsl(h, .22f, .28f), onSecondaryContainer = hsl(h, .40f, .90f),
        tertiary = hsl(t, .50f, .78f), onTertiary = hsl(t, .50f, .18f),
        tertiaryContainer = hsl(t, .30f, .28f), onTertiaryContainer = hsl(t, .50f, .90f),
        background = hsl(h, .12f, .08f), onBackground = hsl(h, .10f, .90f),
        surface = hsl(h, .12f, .08f), onSurface = hsl(h, .10f, .90f),
        surfaceVariant = hsl(h, .10f, .26f), onSurfaceVariant = hsl(h, .10f, .78f),
        surfaceContainerLowest = hsl(h, .12f, .06f), surfaceContainerLow = hsl(h, .12f, .10f),
        surfaceContainer = hsl(h, .12f, .12f), surfaceContainerHigh = hsl(h, .11f, .16f),
        surfaceContainerHighest = hsl(h, .10f, .20f),
        outline = hsl(h, .08f, .55f)
    ) else lightColorScheme(
        primary = hsl(h, .60f, .40f), onPrimary = Color.White,
        primaryContainer = hsl(h, .90f, .90f), onPrimaryContainer = hsl(h, .90f, .15f),
        secondary = hsl(h, .30f, .40f), onSecondary = Color.White,
        secondaryContainer = hsl(h, .40f, .88f), onSecondaryContainer = hsl(h, .40f, .15f),
        tertiary = hsl(t, .40f, .40f), onTertiary = Color.White,
        tertiaryContainer = hsl(t, .60f, .90f), onTertiaryContainer = hsl(t, .60f, .15f),
        background = hsl(h, .25f, .98f), onBackground = hsl(h, .10f, .10f),
        surface = hsl(h, .25f, .98f), onSurface = hsl(h, .10f, .10f),
        surfaceVariant = hsl(h, .20f, .90f), onSurfaceVariant = hsl(h, .10f, .30f),
        surfaceContainerLowest = Color.White, surfaceContainerLow = hsl(h, .25f, .96f),
        surfaceContainer = hsl(h, .25f, .94f), surfaceContainerHigh = hsl(h, .22f, .92f),
        surfaceContainerHighest = hsl(h, .20f, .90f),
        outline = hsl(h, .08f, .50f)
    )
}

private val TanixTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun TanixTheme(s: LauncherSettings, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = when (s.themeMode) { 0 -> isSystemInDarkTheme(); 1 -> true; else -> false }
    var scheme = if (s.dynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else hueScheme(s.accentHue, dark)
    if (dark && s.amoled) {
        scheme = scheme.copy(
            background = Color.Black, surface = Color.Black,
            surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF080808),
            surfaceContainer = Color(0xFF101010), surfaceContainerHigh = Color(0xFF181818),
            surfaceContainerHighest = Color(0xFF202020)
        )
    }
    val base = LocalDensity.current
    val dens = Density(
        base.density * Look.at(Look.UiScales, s.uiScale),
        base.fontScale * Look.at(Look.FontScales, s.fontScale)
    )
    CompositionLocalProvider(LocalSettings provides s, LocalDark provides dark, LocalDensity provides dens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TanixTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(32.dp)
            ),
            content = content
        )
    }
}
