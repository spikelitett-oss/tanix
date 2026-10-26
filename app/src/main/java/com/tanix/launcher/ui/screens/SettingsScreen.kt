package com.tanix.launcher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.R
import com.tanix.launcher.data.HeroMode
import com.tanix.launcher.ui.*

private val Categories = listOf(
    "Внешний вид", "Главный экран", "Значки", "Постеры", "Анимации",
    "Погода и время", "Рекомендации", "Приложения", "Система"
)

/** Настройки в две панели (как в Google TV): слева категории, справа параметры. */
@Composable
fun SettingsScreen(vm: HomeViewModel, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var cat by remember { mutableIntStateOf(0) }
    val ms = animMs(240)

    Box(Modifier.fillMaxSize().background(cs.background)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
            // ---- левая панель ----
            Column(
                Modifier.width(250.dp).fillMaxHeight().clip(RoundedCornerShape(28.dp))
                    .background(cs.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                    FocusBox(Modifier, CircleShape, 1.12f, onClick = onBack) { f ->
                        Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = if (f) cs.onPrimary else cs.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Настройки", color = cs.onSurface, style = MaterialTheme.typography.titleLarge)
                }
                Categories.forEachIndexed { i, name ->
                    FocusBox(Modifier.fillMaxWidth(), CircleShape, 1.0f, onClick = { cat = i }, onFocusChange = { if (it) cat = i }) { f ->
                        Text(
                            name,
                            color = when { f -> cs.onPrimary; i == cat -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.fillMaxWidth()
                                .background(when { f -> cs.primary; i == cat -> cs.secondaryContainer; else -> androidx.compose.ui.graphics.Color.Transparent })
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.weight(1f))
                Image(
                    painterResource(R.drawable.tanix_logo), null,
                    Modifier.padding(top = 16.dp, start = 6.dp).height(18.dp).aspectRatio(720f / 177f).alpha(0.5f),
                    colorFilter = ColorFilter.tint(cs.onSurface)
                )
            }

            Spacer(Modifier.width(24.dp))

            // ---- правая панель ----
            AnimatedContent(
                targetState = cat,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                transitionSpec = { fadeIn(tween(ms, delayMillis = ms / 3)) togetherWith fadeOut(tween(ms / 2)) },
                label = "settingsPane"
            ) { c ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
                    Text(Categories[c], color = cs.onSurface, style = MaterialTheme.typography.headlineMedium)
                    when (c) {
                        0 -> AppearancePane(vm)
                        1 -> HomePane(vm)
                        2 -> IconsPane(vm)
                        3 -> PostersPane(vm)
                        4 -> AnimationsPane(vm)
                        5 -> WeatherTimePane(vm)
                        6 -> SettingsSection("TMDB: российские фильмы и сериалы") { TmdbBlock(vm, advanced = true) }
                        7 -> AppsPane(vm)
                        else -> SystemPane(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearancePane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection("Тема и цвета") {
        ThemeBlock(vm)
        SwitchRow("AMOLED: чисто чёрный фон", "Для тёмной темы", s.amoled) { v -> vm.update { it.copy(amoled = v) } }
    }
    SettingsSection("Размеры и формы") {
        OptionRow("Масштаб интерфейса", listOf("85%", "92%", "100%", "110%", "120%"), s.uiScale) { v -> vm.update { it.copy(uiScale = v) } }
        OptionRow("Размер шрифта", listOf("Мелкий", "Обычный", "Крупный"), s.fontScale) { v -> vm.update { it.copy(fontScale = v) } }
        OptionRow("Скругление карточек", listOf("Малое", "Среднее", "Большое"), s.cornerStyle) { v -> vm.update { it.copy(cornerStyle = v) } }
        OptionRow("Поля экрана (если телевизор обрезает края)", listOf("24", "40", "56", "72"), s.screenPadding) { v -> vm.update { it.copy(screenPadding = v) } }
    }
    SettingsSection("Фон") {
        OptionRow("Стиль фона", listOf("Градиент", "Однотонный", "Как в Google TV"), s.bgStyle) { v -> vm.update { it.copy(bgStyle = v) } }
        OptionRow("Затемнение фона", listOf("Слабое", "Среднее", "Сильное"), s.bgDim) { v -> vm.update { it.copy(bgDim = v) } }
    }
}

@Composable
private fun HomePane(vm: HomeViewModel) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    SettingsSection("Шапка") {
        SwitchRow("Логотип Tanix", null, s.showLogo) { v -> vm.update { it.copy(showLogo = v) } }
        OptionRow("Прозрачность логотипа", listOf("35%", "55%", "75%", "100%"), s.logoAlpha) { v -> vm.update { it.copy(logoAlpha = v) } }
        SwitchRow("Вкладки «Для вас / Приложения / Избранное»", null, s.showTabs) { v -> vm.update { it.copy(showTabs = v) } }
        SwitchRow("Переключать вкладки при наведении", "Как в Google TV; иначе — по нажатию OK", s.tabOnFocus) { v -> vm.update { it.copy(tabOnFocus = v) } }
    }
    SettingsSection("Что показывать на вкладке «Для вас»") {
        SwitchRow("Постеры (рекомендации)", null, s.showHero) { v -> vm.update { it.copy(showHero = v) } }
        SwitchRow("Ряд «Ваши приложения»", null, s.showAppsRow) { v -> vm.update { it.copy(showAppsRow = v) } }
        SwitchRow("Ряд «Избранное»", "Долгое нажатие OK на приложении → «В избранное»", s.showFavRow) { v -> vm.update { it.copy(showFavRow = v) } }
        SwitchRow("Ряд «Недавние»", null, s.showRecentRow) { v -> vm.update { it.copy(showRecentRow = v) } }
    }
    SettingsSection("Порядок рядов") {
        s.rowOrder.forEachIndexed { i, key ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (key) { "posters" -> "Постеры"; "apps" -> "Ваши приложения"; "fav" -> "Избранное"; else -> "Недавние" },
                    color = cs.onSurface, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)
                )
                Chip("▲") { vm.moveRow(i, -1) }
                Spacer(Modifier.width(8.dp))
                Chip("▼") { vm.moveRow(i, 1) }
            }
        }
    }
}

@Composable
private fun IconsPane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection("Значки приложений") { IconsBlock(vm) }
    SettingsSection("Порядок") {
        OptionRow("Сортировка", listOf("По алфавиту", "Свой порядок", "Недавние сверху"), s.sortMode) { v -> vm.update { it.copy(sortMode = v) } }
        SettingRow("Сбросить порядок значков", "Вернуть сортировку по алфавиту", onClick = { vm.resetOrder() })
    }
    SettingsSection("Как переставлять значки") {
        SettingRow(
            "Долгое нажатие OK на значке → «Переместить»",
            "Дальше стрелками на пульте; OK или Назад — готово. В меню также: избранное, скрыть, о приложении, удалить.",
            onClick = {}
        )
    }
}

@Composable
private fun PostersPane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection("Постеры") {
        OptionRow("Источник", HeroMode.values().map { it.title }, s.heroSource.ordinal) { v -> vm.update { it.copy(heroSource = HeroMode.values()[v]) } }
        OptionRow("Тип рекомендаций", listOf("Фильмы и сериалы", "Только фильмы", "Только сериалы"), s.recoType) { v -> vm.update { it.copy(recoType = v) } }
        OptionRow("Размер постеров", listOf("Малые", "Средние", "Большие"), s.posterSize) { v -> vm.update { it.copy(posterSize = v) } }
        SwitchRow("Описание под названием", null, s.posterDesc) { v -> vm.update { it.copy(posterDesc = v) } }
        SwitchRow("Метка «Фильм · ★ 7.8»", null, s.posterBadge) { v -> vm.update { it.copy(posterBadge = v) } }
    }
}

@Composable
private fun AnimationsPane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection("Пресеты") {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip("Как в Google TV") {
                vm.update { it.copy(animOn = true, animSpeed = 1, focusK = 2, focusStyle = 0, enterAnim = true, pivotScroll = true, bgStyle = 2) }
            }
            Chip("Лёгкий режим (слабые приставки)") {
                vm.update { it.copy(animOn = false, enterAnim = false, pivotScroll = false, bgStyle = 0, focusK = 1) }
            }
        }
    }
    SettingsSection("Анимации") {
        SwitchRow("Анимации включены", null, s.animOn) { v -> vm.update { it.copy(animOn = v) } }
        OptionRow("Скорость", listOf("Быстро", "Обычно", "Плавно"), s.animSpeed) { v -> vm.update { it.copy(animSpeed = v) } }
        SwitchRow("Появление рядов каскадом", null, s.enterAnim) { v -> vm.update { it.copy(enterAnim = v) } }
        SwitchRow("Прокрутка как в Google TV", "Фокус удерживается у края, лента плавно едет", s.pivotScroll) { v -> vm.update { it.copy(pivotScroll = v) } }
    }
    SettingsSection("Фокус") {
        OptionRow("Увеличение при фокусе", listOf("Нет", "Малое", "Обычное", "Сильное"), s.focusK) { v -> vm.update { it.copy(focusK = v) } }
        OptionRow("Выделение", listOf("Контур + увеличение", "Только увеличение", "Только контур"), s.focusStyle) { v -> vm.update { it.copy(focusStyle = v) } }
    }
}

@Composable
private fun WeatherTimePane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection("Погода (Open-Meteo)") {
        SwitchRow("Показывать погоду", null, s.showWeather) { v -> vm.update { it.copy(showWeather = v) } }
        WeatherBlock(vm)
        SwitchRow("Описание (ясно, дождь…)", null, s.weatherDesc) { v -> vm.update { it.copy(weatherDesc = v) } }
        SwitchRow("Ощущаемая температура", null, s.weatherFeels) { v -> vm.update { it.copy(weatherFeels = v) } }
    }
    SettingsSection("Время") {
        SwitchRow("Часы", null, s.showClock) { v -> vm.update { it.copy(showClock = v) } }
        SwitchRow("24-часовой формат", null, s.clock24) { v -> vm.update { it.copy(clock24 = v) } }
        SwitchRow("Дата", null, s.showDate) { v -> vm.update { it.copy(showDate = v) } }
    }
}

@Composable
private fun AppsPane(vm: HomeViewModel) {
    val s = vm.s
    var showTv by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    SettingsSection("Кнопка ТВ") {
        SettingRow(
            "Приложение для кнопки ТВ и постеров",
            vm.apps.firstOrNull { it.pkg == s.tvPkg }?.label ?: "Не выбрано (Lampa, ILTV…)",
            onClick = { showTv = true }
        )
    }
    SettingsSection("Скрытые приложения") {
        SettingRow("Выбрать скрытые", "Скрыто: ${s.hidden.size}", onClick = { showHidden = true })
        SettingRow("Показать все", null, onClick = { vm.update { it.copy(hidden = emptySet()) } })
    }
    if (showTv) {
        AppListDialog("Приложение для кнопки ТВ", vm.orderedApps, onClick = { app ->
            vm.update { it.copy(tvPkg = app.pkg) }; showTv = false
        }, onDismiss = { showTv = false })
    }
    if (showHidden) {
        AppListDialog("Скрытые приложения", vm.orderedApps, checked = s.hidden, onClick = { app ->
            vm.update { it.copy(hidden = if (app.pkg in it.hidden) it.hidden - app.pkg else it.hidden + app.pkg) }
        }, onDismiss = { showHidden = false })
    }
}

@Composable
private fun SystemPane(vm: HomeViewModel) {
    val ctx = rememberCtx()
    var confirmReset by remember { mutableStateOf(false) }
    SettingsSection("Главный экран") {
        SettingRow("Сделать Tanix главным экраном", "Откроются системные настройки главного экрана", onClick = { openHomeSettings(ctx) })
        SettingRow("Пройти первоначальную настройку заново", null, onClick = { vm.update { it.copy(onboarded = false) } })
        SettingRow("Сбросить оформление", "Ключ TMDB, город, избранное и скрытые приложения сохранятся", onClick = { confirmReset = true })
    }
    SettingsSection("О приложении") {
        SettingRow("Tanix Launcher 2.0", "Material You · погода · российские фильмы и сериалы", onClick = {})
    }
    if (confirmReset) {
        ConfirmDialog("Сбросить оформление?", "Все параметры внешнего вида вернутся к значениям по умолчанию.", "Сбросить",
            onConfirm = { vm.resetAppearance(); confirmReset = false }, onDismiss = { confirmReset = false })
    }
}
