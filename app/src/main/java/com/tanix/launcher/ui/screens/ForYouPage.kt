@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.HeroItem
import com.tanix.launcher.data.HeroMode
import com.tanix.launcher.data.normalizeRows
import com.tanix.launcher.ui.*

/** Вкладка «Для вас»: ряды в порядке, заданном в настройках. */
@Composable
fun ForYouPage(
    vm: HomeViewModel,
    visible: List<AppItem>,
    moving: String?,
    onMenu: (AppItem) -> Unit,
    onDone: () -> Unit,
    onBg: (String?) -> Unit,
    onOpenHero: (HeroItem) -> Unit
) {
    val s = vm.s
    val keys = normalizeRows(s.rowOrder).filter {
        when (it) { "posters" -> s.showHero; "apps" -> s.showAppsRow; "fav" -> s.showFavRow; else -> s.showRecentRow }
    }
    PivotScope(0.30f) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            keys.forEachIndexed { idx, key ->
                EnterAnim(idx) {
                    when (key) {
                        "posters" -> PostersSection(vm, onBg, onOpenHero)
                        "apps" -> AppsSection("Ваши приложения", visible, vm, moving, true, onMenu, onDone)
                        "fav" -> AppsSection("Избранное", visible.filter { it.pkg in s.favorites }, vm, moving, true, onMenu, onDone)
                        else -> AppsSection(
                            "Недавние",
                            s.recents.mapNotNull { p -> visible.firstOrNull { it.pkg == p } },
                            vm, moving, false, onMenu, onDone
                        )
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun PostersSection(vm: HomeViewModel, onBg: (String?) -> Unit, onOpenHero: (HeroItem) -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (s.heroSource == HeroMode.RECO) "Российские фильмы и сериалы" else "Недавние и избранные",
            color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
        )
        HeroMode.values().forEach { m ->
            Chip(m.title, s.heroSource == m) { vm.update { it.copy(heroSource = m) } }
            Spacer(Modifier.width(8.dp))
        }
    }
    PivotScope(0.10f) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
            items(vm.hero) { item ->
                PosterCard(item, vm.apps, onFocused = { onBg(item.bgUrl) }, onOpen = onOpenHero)
            }
        }
    }
}

@Composable
private fun AppsSection(
    title: String, list: List<AppItem>, vm: HomeViewModel, moving: String?, reorderable: Boolean,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit
) {
    if (list.isEmpty()) return
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val size = Look.at(Look.IconSizes, s.iconSize).dp
    val subset = list.map { it.pkg }
    Text(title, color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
    PivotScope(0.10f) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
            items(list, key = { it.pkg }) { app ->
                val isMoving = moving == app.pkg
                AppTile(
                    app, size, app.pkg in s.favorites, isMoving,
                    onClick = { if (isMoving) onDone() else vm.launch(app.pkg) },
                    onLongClick = { if (!isMoving) onMenu(app) },
                    onKey = if (isMoving && reorderable) moveKeyHandler(vm, app.pkg, subset, 1) else null,
                    modifier = Modifier.animateItemPlacement(tween(animMs(300)))
                )
            }
        }
    }
}

/** Вкладки «Приложения» и «Избранное»: сетка, в которой значки можно переставлять. */
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
    val size = Look.at(Look.IconSizes, s.iconSize).dp
    val subset = list.map { it.pkg }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = maxOf(1, (maxWidth / (size + 40.dp)).toInt())
        PivotScope(0.30f) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(list, key = { it.pkg }) { app ->
                    val isMoving = moving == app.pkg
                    Box(Modifier.animateItemPlacement(tween(animMs(300))), contentAlignment = Alignment.Center) {
                        AppTile(
                            app, size, app.pkg in s.favorites, isMoving,
                            onClick = { if (isMoving) onDone() else vm.launch(app.pkg) },
                            onLongClick = { if (!isMoving) onMenu(app) },
                            onKey = if (isMoving) moveKeyHandler(vm, app.pkg, subset, cols) else null
                        )
                    }
                }
            }
        }
    }
}
