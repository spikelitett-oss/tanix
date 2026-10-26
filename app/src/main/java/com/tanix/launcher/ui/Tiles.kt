package com.tanix.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.FocusRequester
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
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.HeroItem
import kotlinx.coroutines.delay

/** Значок приложения с подписью. В режиме перемещения подсвечивается и ловит стрелки. */
@Composable
fun AppTile(
    app: AppItem,
    size: Dp,
    isFav: Boolean,
    moving: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onKey: ((KeyEvent) -> Boolean)?,
    modifier: Modifier = Modifier
) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val shape = iconShapeOf(st.iconShape)
    val fr = remember { FocusRequester() }
    LaunchedEffect(moving) { if (moving) { delay(120); runCatching { fr.requestFocus() } } }

    Column(modifier.width(size + 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusBox(
            Modifier.graphicsLayer { if (moving) { scaleX = 1.1f; scaleY = 1.1f } },
            shape, 1.1f, onClick, onLongClick,
            onKey = onKey, ringColor = if (moving) cs.tertiary else null, focusRequester = fr
        ) {
            Box {
                AppIcon(app, size, shape)
                if (isFav) {
                    Text("★", color = cs.primary, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
        if (st.showLabels) {
            Spacer(Modifier.height(6.dp))
            Text(
                app.label, color = if (moving) cs.tertiary else cs.onSurface, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}

/** Обработчик стрелок для режима перемещения значка. */
fun moveKeyHandler(vm: HomeViewModel, pkg: String, subset: List<String>, cols: Int): (KeyEvent) -> Boolean = { e ->
    if (e.type == KeyEventType.KeyDown) {
        when (e.key) {
            Key.DirectionLeft -> { vm.moveApp(pkg, -1, subset); true }
            Key.DirectionRight -> { vm.moveApp(pkg, 1, subset); true }
            Key.DirectionUp -> { if (cols > 1) vm.moveApp(pkg, -cols, subset); true }
            Key.DirectionDown -> { if (cols > 1) vm.moveApp(pkg, cols, subset); true }
            else -> false
        }
    } else false
}

/** Прямоугольный постер: обложка, под ней название и мелкое описание. */
@Composable
fun PosterCard(
    item: HeroItem,
    apps: List<AppItem>,
    onFocused: () -> Unit,
    onOpen: (HeroItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val w = Look.at(Look.PosterW, st.posterSize)
    val h = w * 3 / 2
    val shape = cardShape()
    Column(modifier.width(w.dp)) {
        FocusBox(Modifier, shape, 1.07f, onClick = { onOpen(item) }, onFocusChange = { if (it) onFocused() }) {
            Box(Modifier.width(w.dp).height(h.dp).background(cs.surfaceContainerHigh)) {
                if (item.imageUrl != null) {
                    AsyncImage(item.imageUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    apps.firstOrNull { a -> a.pkg == item.iconPkg }?.let { a ->
                        Box(Modifier.align(Alignment.Center)) { AppIcon(a, 72.dp) }
                    }
                }
                if (st.posterBadge) {
                    item.badge?.let { b ->
                        Text(
                            b, color = cs.onSecondaryContainer, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(CircleShape)
                                .background(cs.secondaryContainer.copy(alpha = 0.92f)).padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, color = cs.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (st.posterDesc && item.subtitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(item.subtitle, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
