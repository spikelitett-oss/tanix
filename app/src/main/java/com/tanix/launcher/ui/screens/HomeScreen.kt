@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.R
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.Repo
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: HomeViewModel, openSettings: () -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }
    var showHomeApps by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var moving by remember { mutableStateOf<String?>(null) }
    var bgUrl by remember { mutableStateOf<String?>(null) }

    val visible = vm.orderedApps.filter { it.pkg !in s.hidden }
    val home = vm.homeAppList

    LaunchedEffect(vm.homeSignal) { tab = 0; moving = null }
    LaunchedEffect(s.showTabs) { if (!s.showTabs) tab = 0 }

    BackHandler(enabled = moving != null) { moving = null }
    BackHandler(enabled = moving == null && tab != 0) { tab = 0 }

    val ms = animMs(320)

    Box(Modifier.fillMaxSize()) {
        HomeBackground(if (tab == 0) bgUrl else null)

        Column(
            Modifier.fillMaxSize().padding(horizontal = Look.at(Look.Paddings, s.screenPadding).dp, vertical = 18.dp)
        ) {
            TopBar(vm, tab, onTab = { tab = it }, openSettings = openSettings, onPick = { showPicker = true })
            Spacer(Modifier.height(6.dp))

            // Переключение вкладок: сдвиг + проявление в сторону движения (как в Google TV)
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(ms, delayMillis = ms / 3)) + slideInHorizontally(tween(ms)) { dir * it / 10 }) togetherWith
                        (fadeOut(tween(ms / 2)) + slideOutHorizontally(tween(ms)) { -dir * it / 10 })
                },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> ForYouPage(
                        vm, home, visible, moving,
                        onMenu = { menuApp = it }, onDone = { moving = null },
                        onBg = { bgUrl = it },
                        onOpenHero = { item -> if (vm.openHero(item)) showPicker = true },
                        onAddApps = { showHomeApps = true }
                    )
                    1 -> AppsGridPage(vm, visible, "Нет приложений", moving, onMenu = { menuApp = it }, onDone = { moving = null })
                    else -> AppsGridPage(
                        vm, visible.filter { it.pkg in s.favorites },
                        "Пусто. Удерживайте OK на приложении → «В избранное»",
                        moving, onMenu = { menuApp = it }, onDone = { moving = null }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = moving != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            enter = fadeIn(tween(ms)) + slideInVertically(tween(ms)) { it },
            exit = fadeOut(tween(ms)) + slideOutVertically(tween(ms)) { it }
        ) {
            Text(
                "◀ ▶ ▲ ▼ — переместить   ·   OK или Назад — готово",
                color = cs.onTertiaryContainer, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).background(cs.tertiaryContainer).padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }

    menuApp?.let { app ->
        AppMenuDialog(app, app.pkg in s.favorites, home.any { it.pkg == app.pkg }, onAction = { a ->
            when (a) {
                AppAction.OPEN -> vm.launch(app.pkg)
                AppAction.MOVE -> { vm.freezeOrder(); moving = app.pkg }
                AppAction.HOME -> vm.toggleHome(app.pkg)
                AppAction.FAVORITE -> vm.toggleFavorite(app.pkg)
                AppAction.HIDE -> vm.update { it.copy(hidden = it.hidden + app.pkg) }
                AppAction.INFO -> vm.openAppInfo(app.pkg)
                AppAction.UNINSTALL -> vm.uninstall(app.pkg)
            }
            menuApp = null
        }, onDismiss = { menuApp = null })
    }

    if (showPicker) {
        AppListDialog("Приложение для кнопки ТВ и подборки", visible, onClick = { app ->
            vm.update { it.copy(tvPkg = app.pkg) }; showPicker = false
        }, onDismiss = { showPicker = false })
    }
    if (showHomeApps) HomeAppsDialog(vm) { showHomeApps = false }
}

/** Медленный наезд фона (эффект Кена Бернса). */
@Composable
private fun KenBurns(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) { Box(Modifier.fillMaxSize()) { content() }; return }
    val inf = rememberInfiniteTransition(label = "kenburns")
    val k by inf.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(18000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "kb"
    )
    Box(Modifier.fillMaxSize().graphicsLayer { scaleX = k; scaleY = k }) { content() }
}

/** Фон: градиент, однотонный или кадр сфокусированной карточки (плавная смена + медленный наезд). */
@Composable
private fun HomeBackground(url: String?) {
    val s = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background)) {
        when (s.bgStyle) {
            1 -> Unit
            0 -> Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(cs.background, cs.primaryContainer.copy(alpha = 0.35f), cs.background))
                )
            )
            else -> {
                var shown by remember { mutableStateOf(url) }
                LaunchedEffect(url) { delay(250); shown = url }
                Crossfade(targetState = shown, animationSpec = tween(animMs(550)), label = "bg") { u ->
                    if (u != null) {
                        KenBurns(s.kenBurns && s.animOn) {
                            AsyncImage(u, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(cs.background, cs.primaryContainer.copy(alpha = 0.35f), cs.background))
                            )
                        )
                    }
                }
                val dim = Look.at(Look.BgDim, s.bgDim)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(cs.background.copy(alpha = 0.92f), cs.background.copy(alpha = dim * 0.45f)))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(cs.background.copy(alpha = dim * 0.5f), cs.background.copy(alpha = dim * 0.7f), cs.background))))
            }
        }
    }
}

@Composable
private fun TopBar(vm: HomeViewModel, tab: Int, onTab: (Int) -> Unit, openSettings: () -> Unit, onPick: () -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (s.showLogo) {
            // Логотип: белый PNG без фона, окрашивается в цвет темы и делается полупрозрачным
            Image(
                painterResource(R.drawable.tanix_logo), "Tanix",
                Modifier.height(26.dp).aspectRatio(720f / 177f).alpha(Look.at(Look.LogoAlphas, s.logoAlpha)),
                colorFilter = ColorFilter.tint(cs.onSurface)
            )
            Spacer(Modifier.width(20.dp))
        }
        if (s.showTabs) {
            // Вкладки в общей «таблетке», как в Google TV
            Row(
                Modifier.clip(CircleShape).background(cs.surfaceContainer.copy(alpha = 0.75f)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Для вас", "Приложения", "Избранное").forEachIndexed { i, name ->
                    TabPill(name, tab == i, onFocus = { if (s.tabOnFocus) onTab(i) }, onClick = { onTab(i) })
                    if (i < 2) Spacer(Modifier.width(4.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))

        if (s.showWeather) { WeatherChip(vm); Spacer(Modifier.width(14.dp)) }

        if (s.showClock || s.showDate) {
            Column(horizontalAlignment = Alignment.End) {
                if (s.showClock) {
                    val f = if (s.clock24) "HH:mm" else "h:mm a"
                    Text(SimpleDateFormat(f, Locale.getDefault()).format(now), color = cs.onSurface, style = MaterialTheme.typography.titleLarge)
                }
                if (s.showDate) {
                    Text(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(14.dp))
        }

        // Кнопка ТВ: открывает выбранное приложение (Lampa, ILTV…). Долгое нажатие — сменить приложение.
        FocusBox(
            Modifier, CircleShape, 1.12f,
            onClick = { val p = s.tvPkg; if (p == null || !vm.launch(p)) onPick() },
            onLongClick = onPick
        ) { f ->
            Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                TvIcon(if (f) cs.onPrimary else cs.onSecondaryContainer)
            }
        }
        Spacer(Modifier.width(10.dp))
        FocusBox(Modifier, CircleShape, 1.12f, onClick = openSettings) { f ->
            Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, "Настройки", tint = if (f) cs.onPrimary else cs.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun WeatherChip(vm: HomeViewModel) {
    val cs = MaterialTheme.colorScheme
    val s = vm.s
    val w = vm.weather
    FocusBox(Modifier, CircleShape, 1.05f, onClick = { vm.loadWeather() }) { f ->
        val fg = if (f) cs.onPrimary else cs.onSecondaryContainer
        Row(
            Modifier.background(if (f) cs.primary else cs.secondaryContainer).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (w == null) {
                // данных ещё нет (идёт загрузка или нет сети) — показываем город; причина видна в настройках погоды
                Text("☁", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(6.dp))
                Text(if (vm.weatherError != null) "—°" else "…", color = fg, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(s.city, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            } else {
                val info = Repo.weatherInfo(w.code)
                val r = w.temp.roundToInt()
                Text(info.first, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(6.dp))
                Text((if (r > 0) "+$r" else "$r") + "°", color = fg, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(s.city, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                if (s.weatherDesc) { Spacer(Modifier.width(8.dp)); Text(info.second, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                if (s.weatherFeels) {
                    val fl = w.feels.roundToInt()
                    Spacer(Modifier.width(8.dp))
                    Text("ощущ. " + (if (fl > 0) "+$fl" else "$fl") + "°", color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}
