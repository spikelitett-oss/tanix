package com.tanix.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Общие блоки настроек: используются и в экране настроек, и в первом запуске.

@Composable
fun ThemeBlock(vm: HomeViewModel) {
    val s = vm.s
    OptionRow("Тема", listOf("Системная", "Тёмная", "Светлая"), s.themeMode) { v -> vm.update { it.copy(themeMode = v) } }
    val dynOk = android.os.Build.VERSION.SDK_INT >= 31
    SwitchRow(
        "Динамические цвета Material You",
        if (dynOk) "Цвета подбираются из обоев" else "Нужен Android 12+ — на этой приставке работает выбранный акцент",
        checked = s.dynamicColor && dynOk
    ) { v -> if (dynOk) vm.update { it.copy(dynamicColor = v) } }
    Text("Акцентный цвет", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp))
    AccentPicker(s.accentHue) { h -> vm.update { it.copy(accentHue = h, dynamicColor = false) } }
}

@Composable
fun IconsBlock(vm: HomeViewModel) {
    val s = vm.s
    OptionRow("Форма значков", listOf("Круг", "Скруглённый", "Квадрат"), s.iconShape) { v -> vm.update { it.copy(iconShape = v) } }
    OptionRow("Размер значков", listOf("Мелкие", "Средние", "Крупные", "Огромные"), s.iconSize) { v -> vm.update { it.copy(iconSize = v) } }
    SwitchRow("Подписи под значками", null, s.showLabels) { v -> vm.update { it.copy(showLabels = v) } }
}

@Composable
fun WeatherBlock(vm: HomeViewModel) {
    val s = vm.s
    var showCity by remember { mutableStateOf(false) }
    SettingRow("Город", s.city, onClick = { showCity = true })
    OptionRow("Единицы температуры", listOf("°C", "°F"), if (s.fahrenheit) 1 else 0) { v -> vm.update { it.copy(fahrenheit = v == 1) } }
    SettingRow(
        "Обновить сейчас",
        vm.weather?.let { Repo.weatherInfo(it.code).second + ", ощущается как " + it.feels.toInt() + "°" },
        onClick = { vm.loadWeather() }
    )
    if (showCity) CityDialog({ showCity = false }) { c ->
        vm.update { it.copy(city = c.name, lat = c.lat, lon = c.lon) }
        showCity = false
    }
}

@Composable
fun TmdbBlock(vm: HomeViewModel, advanced: Boolean) {
    val s = vm.s
    var showKey by remember { mutableStateOf(false) }
    var showMirror by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SettingRow(
        "API-ключ TMDB",
        if (s.tmdbKey.isBlank()) "Не задан — нажмите, чтобы ввести" else "Задан (••••${s.tmdbKey.takeLast(4)})",
        onClick = { showKey = true }
    )
    SwitchRow("Включить прокси TMDB", "Обход блокировки TMDB в России", checked = s.proxyOn) { v -> vm.update { it.copy(proxyOn = v) } }
    if (s.proxyOn) {
        OptionRow("Тип прокси", listOf("Rootu (как в Lampa)", "DNS-over-HTTPS", "Своё зеркало"), s.proxyType) { v ->
            vm.update { it.copy(proxyType = v) }
        }
    }
    if (advanced) {
        val upd = vm.recoUpdatedAt
        val sub = if (upd == 0L) "Ещё не обновлялось · авто раз в 4 дня"
        else "Обновлено " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(upd)) + " · авто раз в 4 дня"
        SettingRow("Обновить рекомендации сейчас", sub, onClick = { vm.refreshReco() })
    }
    if (advanced && s.proxyOn && s.proxyType == 2) {
        SettingRow("Адрес своего зеркала", s.proxyBase.ifBlank { "Не задан — будет использован api.tmdb.org" }, onClick = { showMirror = true })
    }
    SettingRow("Проверить соединение", status ?: "Нажмите для проверки", onClick = {
        status = "Проверяем…"
        val snap = vm.s
        scope.launch { status = withContext(Dispatchers.IO) { Repo.testTmdb(snap) } }
    })

    if (showKey) TextInputDialog("API-ключ TMDB (v3)", "Ключ", s.tmdbKey, { showKey = false }) { v ->
        vm.update { it.copy(tmdbKey = v.trim()) }; showKey = false
    }
    if (showMirror) TextInputDialog("Адрес зеркала TMDB", "https://tmdb.example.workers.dev", s.proxyBase, { showMirror = false }) { v ->
        vm.update { it.copy(proxyBase = v.trim()) }; showMirror = false
    }
}

fun openHomeSettings(ctx: Context) {
    for (a in listOf(android.provider.Settings.ACTION_HOME_SETTINGS, android.provider.Settings.ACTION_SETTINGS)) {
        try { ctx.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) { }
    }
}

@Composable
fun rememberCtx(): Context = LocalContext.current
