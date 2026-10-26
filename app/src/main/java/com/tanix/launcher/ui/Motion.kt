@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Прокрутка «как в Google TV»: сфокусированный элемент подтягивается к опорной точке
 * (доля размера контейнера), а прокрутка идёт плавной пружиной.
 */
class PivotSpec(private val fraction: Float, stiffness: Float) : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness)

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - containerSize * fraction
}

@Composable
fun PivotScope(fraction: Float, content: @Composable () -> Unit) {
    val s = LocalSettings.current
    if (s.pivotScroll) {
        val stiffness = if (s.animOn) 260f / Look.at(Look.SpeedK, s.animSpeed) else Spring.StiffnessHigh
        val spec = remember(fraction, stiffness) { PivotSpec(fraction, stiffness) }
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) { content() }
    } else {
        content()
    }
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
