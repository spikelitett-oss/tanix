package com.tanix.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.HeroItem
import kotlinx.coroutines.delay

/** Цвет выделения плиток: белый на тёмной теме (как в Google TV), акцентный на светлой. */
@Composable
fun tileRing(): Color = if (LocalDark.current) Color.White else MaterialTheme.colorScheme.primary

@Composable
private fun tileShape(): Shape {
    val st = LocalSettings.current
    return if (st.tileStyle == 0) RoundedCornerShape(12.dp) else iconShapeOf(st.tileStyle - 1)
}

private fun tileSize(tileStyle: Int, iconSize: Int): Pair<Dp, Dp> {
    return if (tileStyle == 0) {
        val w = Look.at(Look.TileW, iconSize).dp
        w to w * 9f / 16f
    } else {
        val w = Look.at(Look.IconSizes, iconSize).dp
        w to w
    }
}

/**
 * Плитка приложения. По умолчанию — баннер 16:9 в оригинальных цветах приложения (как в Google TV),
 * подпись показывается под плиткой при фокусе. В режиме перемещения подсвечивается и ловит стрелки.
 */
@Composable
fun AppTile(
    app: AppItem,
    isFav: Boolean,
    moving: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onKey: ((KeyEvent) -> Boolean)?,
    modifier: Modifier = Modifier
) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val banner = st.tileStyle == 0
    val shape = tileShape()
    val (w, h) = tileSize(st.tileStyle, st.iconSize)
    var focused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(moving) { if (moving) { delay(120); runCatching { fr.requestFocus() } } }

    Column(modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusBox(
            Modifier.graphicsLayer { if (moving) { scaleX = 1.08f; scaleY = 1.08f } },
            shape, 1.1f, onClick, onLongClick,
            onFocusChange = { focused = it },
            onKey = onKey, ringColor = if (moving) cs.tertiary else tileRing(), focusRequester = fr
        ) {
            Box(Modifier.width(w).height(h).background(NeutralTile)) {
                when {
                    banner && app.banner != null ->
                        Image(app.banner, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    banner -> Row(
                        Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { Image(it, null, Modifier.size(h * 0.55f).clip(RoundedCornerShape(8.dp))) }
                        Spacer(Modifier.width(6.dp))
                        Text(app.label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    else -> AppIcon(app, w, shape)
                }
                if (isFav) {
                    Text("★", color = Color(0xFFFFD54F), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                }
            }
        }
        val showLabel = when (st.labelMode) { 0 -> true; 1 -> focused || moving; else -> false }
        Box(Modifier.height(22.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
            if (showLabel) {
                Text(
                    app.label, color = if (moving) cs.tertiary else cs.onSurface, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Плитка «+ Добавить» в конце ряда «Ваши приложения». */
@Composable
fun AddTile(onClick: () -> Unit) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val shape = tileShape()
    val (w, h) = tileSize(st.tileStyle, st.iconSize)
    Column(Modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusBox(Modifier, shape, 1.1f, onClick, ringColor = tileRing()) {
            Box(Modifier.width(w).height(h).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Text("+", color = cs.onSurfaceVariant, style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

/** Обработчик стрелок для режима перемещения: onMove получает смещение (-1, +1, ±колонок). */
fun moveKeyHandler(cols: Int, onMove: (Int) -> Unit): (KeyEvent) -> Boolean = { e ->
    if (e.type == KeyEventType.KeyDown) {
        when (e.key) {
            Key.DirectionLeft -> { onMove(-1); true }
            Key.DirectionRight -> { onMove(1); true }
            Key.DirectionUp -> { if (cols > 1) onMove(-cols); true }
            Key.DirectionDown -> { if (cols > 1) onMove(cols); true }
            else -> false
        }
    } else false
}

/** Горизонтальная карточка подборки (16:9) — как «Top picks for you» в Google TV. */
@Composable
fun PickCard(
    item: HeroItem,
    apps: List<AppItem>,
    onFocused: () -> Unit,
    onOpen: (HeroItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val st = LocalSettings.current
    val w = Look.at(Look.PickW, st.posterSize)
    val h = w * 9 / 16
    FocusBox(modifier, cardShape(), 1.07f, onClick = { onOpen(item) }, onFocusChange = { if (it) onFocused() }, ringColor = tileRing()) {
        Box(Modifier.width(w.dp).height(h.dp).background(NeutralTile)) {
            if (item.imageUrl != null) {
                AsyncImage(item.imageUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                apps.firstOrNull { a -> a.pkg == item.iconPkg }?.let { a ->
                    Box(Modifier.align(Alignment.Center)) { AppIcon(a, (h * 0.6f).dp) }
                }
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
            Text(
                item.title, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
