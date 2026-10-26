@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.HeroItem
import com.tanix.launcher.data.HeroMode
import com.tanix.launcher.data.normalizeRows
import com.tanix.launcher.ui.*

/**
 * Вкладка «Для вас» в стиле Google TV: вверху название и описание выбранной карточки (фон — её кадр),
 * ниже ряд «Подборка для вас» широкими карточками и ряд «Ваши приложения».
 */
@Composable
fun ForYouPage(
    vm: HomeViewModel,
    home: List<AppItem>,
    visible: List<AppItem>,
    moving: String?,
    onMenu: (AppItem) -> Unit,
    onDone: () -> Unit,
    onBg: (String?) -> Unit,
    onOpenHero: (HeroItem) -> Unit,
    onAddApps: () -> Unit
) {
    val s = vm.s
    val items = vm.hero
    var idx by remember { mutableIntStateOf(0) }
    val cur = items.getOrNull(idx.coerceIn(0, maxOf(0, items.lastIndex)))
    LaunchedEffect(items) { idx = 0; onBg(items.firstOrNull()?.bgUrl) }

    val keys = normalizeRows(s.rowOrder).filter {
        when (it) { "posters" -> s.showHero; "apps" -> s.showAppsRow; "fav" -> s.showFavRow; else -> s.showRecentRow }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (s.showHero) HeroInfo(cur, idx, items.size, Modifier.fillMaxWidth().height(160.dp))
        keys.forEachIndexed { i, key ->
            EnterAnim(i) {
                when (key) {
                    "posters" -> PicksSection(vm, items, onOpenHero) { n -> idx = n; onBg(items.getOrNull(n)?.bgUrl) }
                    "apps" -> HomeAppsSection(home, vm, moving, onMenu, onDone, onAddApps)
                    "fav" -> AppsSection("Избранное", visible.filter { it.pkg in s.favorites }, vm, moving, onMenu, onDone)
                    else -> AppsSection("Недавние", s.recents.mapNotNull { p -> visible.firstOrNull { it.pkg == p } }, vm, null, onMenu, onDone)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/** Верхний блок: метка, крупное название, описание и точки-индикатор (как у «Dune: Part Two» в Google TV). */
@Composable
private fun HeroInfo(item: HeroItem?, index: Int, total: Int, modifier: Modifier) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    Box(modifier) {
        Crossfade(targetState = item, animationSpec = tween(animMs(350)), label = "heroInfo") { cur ->
            Box(Modifier.fillMaxSize()) {
                if (cur != null) {
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.62f).padding(start = 8.dp, bottom = 8.dp)) {
                        if (st.posterBadge) {
                            cur.badge?.let {
                                Text(it, color = cs.primary, style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        Text(cur.title, color = cs.onSurface, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (st.posterDesc && cur.subtitle.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(cur.subtitle, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (total > 1) {
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(minOf(total, 12)) { i ->
                    Box(
                        Modifier.size(if (i == index) 8.dp else 6.dp).clip(CircleShape)
                            .background(if (i == index) cs.onSurface else cs.onSurface.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
private fun PicksSection(vm: HomeViewModel, items: List<HeroItem>, onOpen: (HeroItem) -> Unit, onFocusIdx: (Int) -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (s.heroSource == HeroMode.RECO) "Подборка для вас" else "Недавние и избранные",
            color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
        )
        HeroMode.values().forEach { m ->
            Chip(m.title, s.heroSource == m) { vm.update { it.copy(heroSource = m) } }
            Spacer(Modifier.width(8.dp))
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
        itemsIndexed(items) { i, item ->
            PickCard(item, vm.apps, onFocused = { onFocusIdx(i) }, onOpen = onOpen)
        }
    }
}

/** «Ваши приложения»: выбранные вами приложения (около 15) + плитка «+». */
@Composable
private fun HomeAppsSection(
    list: List<AppItem>, vm: HomeViewModel, moving: String?,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit, onAdd: () -> Unit
) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    Text("Ваши приложения", color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
        items(list, key = { it.pkg }) { app ->
            val isMoving = moving == app.pkg
            AppTile(
                app, app.pkg in s.favorites, isMoving,
                onClick = { if (isMoving) onDone() else vm.launch(app.pkg) },
                onLongClick = { if (!isMoving) onMenu(app) },
                onKey = if (isMoving) moveKeyHandler(1) { d -> vm.moveHome(app.pkg, d) } else null,
                modifier = Modifier.animateItemPlacement(tween(animMs(300)))
            )
        }
        item { AddTile(onAdd) }
    }
}

@Composable
private fun AppsSection(
    title: String, list: List<AppItem>, vm: HomeViewModel, moving: String?,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit
) {
    if (list.isEmpty()) return
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val subset = list.map { it.pkg }
    Text(title, color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
        items(list, key = { it.pkg }) { app ->
            val isMoving = moving == app.pkg
            AppTile(
                app, app.pkg in s.favorites, isMoving,
                onClick = { if (isMoving) onDone() else vm.launch(app.pkg) },
                onLongClick = { if (!isMoving) onMenu(app) },
                onKey = if (isMoving) moveKeyHandler(1) { d -> vm.moveApp(app.pkg, d, subset) } else null,
                modifier = Modifier.animateItemPlacement(tween(animMs(300)))
            )
        }
    }
}

/** Вкладки «Приложения» и «Избранное»: сетка, в которой плитки можно переставлять. */
@Composable
fun AppsGridPage(
    vm: HomeViewModel, list: List<AppItem>, emptyText: String, moving: String?,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit
) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val tileW = (if (s.tileStyle == 0) Look.at(Look.TileW, s.iconSize) else Look.at(Look.IconSizes, s.iconSize)).dp
    val subset = list.map { it.pkg }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = maxOf(1, (maxWidth / (tileW + 24.dp)).toInt())
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { it.pkg }) { app ->
                val isMoving = moving == app.pkg
                Box(Modifier.animateItemPlacement(tween(animMs(300))), contentAlignment = Alignment.Center) {
                    AppTile(
                        app, app.pkg in s.favorites, isMoving,
                        onClick = { if (isMoving) onDone() else vm.launch(app.pkg) },
                        onLongClick = { if (!isMoving) onMenu(app) },
                        onKey = if (isMoving) moveKeyHandler(cols) { d -> vm.moveApp(app.pkg, d, subset) } else null
                    )
                }
            }
        }
    }
}
