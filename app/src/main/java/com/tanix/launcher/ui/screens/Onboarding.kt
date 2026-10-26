package com.tanix.launcher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.R
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay

/** Первый запуск: пошаговая настройка основных функций. */
@Composable
fun Onboarding(vm: HomeViewModel) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val ctx = rememberCtx()
    var step by remember { mutableIntStateOf(0) }
    var showTvApp by remember { mutableStateOf(false) }
    var showHome by remember { mutableStateOf(false) }
    val last = 6
    val next = remember { FocusRequester() }
    LaunchedEffect(step) { delay(200); runCatching { next.requestFocus() } }

    Box(Modifier.fillMaxSize().background(cs.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(R.drawable.tanix_logo), null,
                Modifier.height(if (step == 0) 54.dp else 30.dp).aspectRatio(720f / 177f).alpha(0.85f),
                colorFilter = ColorFilter.tint(cs.onBackground)
            )
            Spacer(Modifier.height(10.dp))
            Text("Шаг ${step + 1} из ${last + 1}", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(14.dp))

            when (step) {
                0 -> {
                    Text("Добро пожаловать!", color = cs.onBackground, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Tanix — лаунчер в стиле Material You для вашей приставки.\n\n" +
                            "• главный экран как в Google TV: большой блок и подборка\n" +
                            "• ваши приложения баннерами (до 15, можно менять)\n" +
                            "• вкладки «Для вас», «Приложения», «Избранное»\n" +
                            "• значки можно переставлять (долгое нажатие OK)\n" +
                            "• погода на главном экране\n" +
                            "• российские фильмы и сериалы\n" +
                            "• кнопка ТВ: быстрый запуск Lampa, ILTV и др.\n" +
                            "• десятки настроек оформления и анимаций\n\n" +
                            "Настроим всё за минуту.",
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start
                    )
                }
                1 -> {
                    StepTitle("Внешний вид")
                    SettingsSection("Тема и цвета") { ThemeBlock(vm) }
                }
                2 -> {
                    StepTitle("Плитки приложений")
                    SettingsSection("Вид и размер") { IconsBlock(vm) }
                }
                3 -> {
                    StepTitle("Что показывать на главном экране")
                    SettingsSection("Можно изменить позже в настройках") {
                        SwitchRow("Погода", null, s.showWeather) { v -> vm.update { it.copy(showWeather = v) } }
                        SwitchRow("Часы", null, s.showClock) { v -> vm.update { it.copy(showClock = v) } }
                        SwitchRow("Постеры (рекомендации)", null, s.showHero) { v -> vm.update { it.copy(showHero = v) } }
                        SwitchRow("Ряд «Избранное»", null, s.showFavRow) { v -> vm.update { it.copy(showFavRow = v) } }
                        SwitchRow("Ряд «Недавние»", null, s.showRecentRow) { v -> vm.update { it.copy(showRecentRow = v) } }
                        OptionRow("Фон", listOf("Градиент", "Однотонный", "Как в Google TV"), s.bgStyle) { v -> vm.update { it.copy(bgStyle = v) } }
                    }
                }
                4 -> {
                    StepTitle("Погода")
                    SettingsSection("Город и единицы") { WeatherBlock(vm) }
                }
                5 -> {
                    StepTitle("Рекомендации: российские фильмы и сериалы")
                    Text(
                        "Нужен бесплатный ключ TMDB (themoviedb.org → Настройки → API). Шаг можно пропустить.",
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                    )
                    SettingsSection("TMDB") { TmdbBlock(vm, advanced = false) }
                }
                else -> {
                    StepTitle("Почти готово")
                    Text(
                        "Выберите приложение для кнопки ТВ и сделайте Tanix главным экраном (кнопка Home).",
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                    )
                    SettingsSection("Последний шаг") {
                        SettingRow("Приложения на главном экране", "Выберите около 15", onClick = { showHome = true })
                        SettingRow(
                            "Приложение для кнопки ТВ",
                            vm.apps.firstOrNull { it.pkg == s.tvPkg }?.label ?: "Например Lampa или ILTV",
                            onClick = { showTvApp = true }
                        )
                        SettingRow("Открыть настройки главного экрана", "Выберите Tanix в списке", onClick = { openHomeSettings(ctx) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Если системного пункта нет — через ADB:\nadb shell cmd package set-home-activity com.tanix.launcher/.MainActivity",
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                    )
                }
            }

            if (showHome) HomeAppsDialog(vm) { showHome = false }
            if (showTvApp) AppListDialog("Приложение для кнопки ТВ", vm.orderedApps, onClick = { app ->
                vm.update { it.copy(tvPkg = app.pkg) }; showTvApp = false
            }, onDismiss = { showTvApp = false })

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) Chip("Назад") { step-- }
                Chip(
                    if (step == last) "Готово" else if (step == 0) "Начать" else "Далее",
                    selected = true, modifier = Modifier.focusRequester(next)
                ) {
                    if (step == last) vm.update { it.copy(onboarded = true) } else step++
                }
            }
        }
    }
}

@Composable
private fun StepTitle(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
}
