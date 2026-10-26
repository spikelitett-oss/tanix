package com.tanix.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Раньше здесь подменялась прокрутка через LocalBringIntoViewSpec — этого API нет в Compose 1.6.
 * Теперь используется стандартная прокрутка Compose (она тоже плавная).
 */
@Composable
fun PivotScope(fraction: Float, content: @Composable () -> Unit) {
    content()
}

/** Плавное появление блока (с задержкой по индексу) — каскад, как при загрузке Google TV. */
@Composable
fun EnterAnim(index: Int, content: @Composable () -> Unit) {
    val s = LocalSettings.current
    if (!s.animOn || !s.enterAnim) { content(); return }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 70L); shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(animMs(420), easing = FastOutSlowInEasing),
        label = "enter"
    )
    Box(Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 36.dp.toPx() }) { content() }
}
