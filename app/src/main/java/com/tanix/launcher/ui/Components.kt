@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.tanix.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tanix.launcher.data.AppItem

/**
 * Фокусируемый контейнер. Эффекты (все настраиваются): пружинное увеличение, контур,
 * мягкое свечение вокруг и «блик», пробегающий по плитке при получении фокуса.
 */
@Composable
fun FocusBox(
    modifier: Modifier = Modifier,
    shape: Shape,
    focusScale: Float = 1.06f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onKey: ((KeyEvent) -> Boolean)? = null,
    ringColor: Color? = null,
    focusRequester: FocusRequester? = null,
    content: @Composable (Boolean) -> Unit
) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    var f by remember { mutableStateOf(false) }
    val k = Look.at(Look.FocusK, st.focusK)
    val target = 1f + (focusScale - 1f) * k
    val spec: AnimationSpec<Float> =
        if (st.animOn) spring(dampingRatio = 0.75f, stiffness = 420f / Look.at(Look.SpeedK, st.animSpeed)) else snap()
    val sc by animateFloatAsState(if (f) target else 1f, spec, label = "focusScale")
    val glowA by animateFloatAsState(if (f && st.glow) 1f else 0f, tween(animMs(220)), label = "glow")
    val shine = remember { Animatable(0f) }
    LaunchedEffect(f) {
        if (f && st.shine && st.animOn) { shine.snapTo(0f); shine.animateTo(1f, tween(650)) } else shine.snapTo(0f)
    }
    val accent = ringColor ?: cs.primary
    val ring = f && st.focusStyle != 1
    Box(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scale(sc)
            .onFocusChanged { f = it.isFocused; onFocusChange?.invoke(it.isFocused) }
            .onKeyEvent { e -> onKey?.invoke(e) ?: false }
            .drawBehind {
                if (glowA > 0.01f) {
                    val base = if (shape === CircleShape) size.minDimension / 2f else 14.dp.toPx()
                    for (i in 1..3) {
                        val g = i * 5.dp.toPx()
                        drawRoundRect(
                            color = accent.copy(alpha = 0.16f * glowA / i),
                            topLeft = Offset(-g, -g),
                            size = Size(size.width + 2 * g, size.height + 2 * g),
                            cornerRadius = CornerRadius(base + g)
                        )
                    }
                }
            }
            .clip(shape)
            .drawWithContent {
                drawContent()
                val p = shine.value
                if (p > 0f && p < 1f) {
                    val x = size.width * (-0.3f + 1.6f * p)
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                            start = Offset(x - size.width * 0.15f, 0f),
                            end = Offset(x + size.width * 0.15f, size.height)
                        )
                    )
                }
            }
            .border(if (ring) 3.dp else 0.dp, accent, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) { content(f) }
}

/** Material 3 chip (filter chip): выбранный — secondaryContainer с галочкой. */
@Composable
fun Chip(text: String, selected: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusBox(modifier, CircleShape, 1.05f, onClick) { f ->
        val bg = when { f -> cs.primary; selected -> cs.secondaryContainer; else -> cs.surfaceContainerHigh }
        val fg = when { f -> cs.onPrimary; selected -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant }
        Text(
            (if (selected) "✓ " else "") + text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            modifier = Modifier.background(bg).padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/** Пилюля вкладки (как «Для вас / Приложения» в Google TV). */
@Composable
fun TabPill(text: String, selected: Boolean, onFocus: () -> Unit, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusBox(Modifier, CircleShape, 1.04f, onClick, onFocusChange = { if (it) onFocus() }) { f ->
        val bg by animateColorAsState(
            when { f -> cs.primary; selected -> cs.secondaryContainer; else -> Color.Transparent },
            tween(animMs(200)), label = "tabBg"
        )
        val fg = when { f -> cs.onPrimary; selected -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            modifier = Modifier.background(bg).padding(horizontal = 18.dp, vertical = 9.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Text(title, color = cs.primary, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainer).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun SettingRow(title: String, subtitle: String? = null, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    FocusBox(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), 1.01f, onClick) { f ->
        Row(
            Modifier.fillMaxWidth().background(if (f) cs.primaryContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = if (f) cs.onPrimaryContainer else cs.onSurface, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(it, color = if (f) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, subtitle, onClick = { onChange(!checked) }) { Switch(checked = checked, onCheckedChange = null) }
}

@Composable
fun OptionRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = cs.onSurface, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, o -> Chip(o, i == selected) { onSelect(i) } }
        }
    }
}

@Composable
fun AccentPicker(hue: Float, onPick: (Float) -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentHues.forEach { h ->
            FocusBox(Modifier, CircleShape, 1.15f, { onPick(h) }) {
                Box(Modifier.size(38.dp).background(hsl(h, .7f, .6f)), contentAlignment = Alignment.Center) {
                    if (h == hue) Icon(Icons.Filled.Check, null, tint = Color.White)
                }
            }
        }
    }
}

/** Значок «ТВ», нарисованный кодом. */
@Composable
fun TvIcon(tint: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier) {
        val sw = 2.2.dp.toPx()
        drawRoundRect(
            tint, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height * 0.74f),
            cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(sw)
        )
        drawLine(tint, Offset(size.width * 0.28f, size.height - sw / 2), Offset(size.width * 0.72f, size.height - sw / 2),
            strokeWidth = sw, cap = StrokeCap.Round)
    }
}

/** Значок приложения в оригинальных цветах (без оформления Material You). */
@Composable
fun AppIcon(app: AppItem, size: Dp, shape: Shape = iconShapeOf(0)) {
    Box(Modifier.size(size).clip(shape).background(NeutralTile), contentAlignment = Alignment.Center) {
        app.icon?.let {
            if (app.adaptive) Image(it, app.label, Modifier.fillMaxSize().scale(1.5f))
            else Image(it, app.label, Modifier.fillMaxSize().padding(size * 0.18f))
        }
    }
}
