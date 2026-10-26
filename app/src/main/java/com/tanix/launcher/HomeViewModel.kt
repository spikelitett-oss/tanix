package com.tanix.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanix.launcher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val cache = app.getSharedPreferences("tanix_cache", Context.MODE_PRIVATE)

    companion object { const val UPDATE_MS = 4L * 24 * 60 * 60 * 1000 } // авто-обновление рекомендаций: 4 дня

    var s by mutableStateOf(store.load()); private set
    var apps by mutableStateOf(listOf<AppItem>()); private set
    var hero by mutableStateOf(listOf(HeroItem("Загрузка…", ""))); private set
    var weather by mutableStateOf<Weather?>(null); private set
    var weatherError by mutableStateOf<String?>(null); private set
    var recoUpdatedAt by mutableStateOf(cache.getLong("ts", 0L)); private set
    var homeSignal by mutableIntStateOf(0); private set

    private var heroJob: Job? = null

    /** Приложения в порядке, выбранном пользователем. */
    val orderedApps: List<AppItem> get() = Repo.orderApps(apps, s)

    /** Приложения, которые показываются на главном экране в ряду «Ваши приложения». */
    val homeAppList: List<AppItem> get() = Repo.homeApps(apps, s)

    init {
        applyNet(s)
        viewModelScope.launch {
            while (true) {
                val ok = loadWeatherNow()
                if (isRecoStale()) loadHero()
                // если погода не загрузилась (например, сеть ещё не поднялась) — повторяем через 30 секунд
                delay(if (ok) 30 * 60_000L else 30_000L)
            }
        }
        loadHero()
    }

    private fun applyNet(c: LauncherSettings) { Net.dohEnabled = c.proxyOn && c.proxyType == 1 }

    fun onHomePressed() { homeSignal++ }

    // ---------------- Настройки ----------------
    fun update(f: (LauncherSettings) -> LauncherSettings) {
        val old = s
        val new = f(old)
        s = new
        store.save(new)
        applyNet(new)
        val heroChanged = old.heroSource != new.heroSource || old.recoType != new.recoType ||
            old.tmdbKey != new.tmdbKey || old.proxyOn != new.proxyOn || old.proxyType != new.proxyType ||
            old.proxyBase != new.proxyBase || old.showHero != new.showHero ||
            (new.heroSource == HeroMode.FAVORITES &&
                (old.favorites != new.favorites || old.recents != new.recents || old.hidden != new.hidden))
        if (heroChanged) loadHero()
        if (old.lat != new.lat || old.lon != new.lon || old.fahrenheit != new.fahrenheit) loadWeather()
    }

    /** Сброс внешнего вида; ключ TMDB, город, избранное, скрытые и выбранные приложения сохраняются. */
    fun resetAppearance() = update {
        LauncherSettings(
            onboarded = true, tmdbKey = it.tmdbKey, city = it.city, lat = it.lat, lon = it.lon,
            tvPkg = it.tvPkg, favorites = it.favorites, hidden = it.hidden, recents = it.recents,
            homeConfigured = it.homeConfigured, homeApps = it.homeApps
        )
    }

    // ---------------- Порядок и главный экран ----------------
    fun freezeOrder() = update { it.copy(appOrder = Repo.orderApps(apps, it).map { a -> a.pkg }, sortMode = 1) }

    fun resetOrder() = update { it.copy(appOrder = emptyList(), sortMode = 0) }

    /** Перемещение значка внутри отображаемого списка [subset] на [delta] позиций (общий порядок). */
    fun moveApp(pkg: String, delta: Int, subset: List<String>) {
        val i = subset.indexOf(pkg)
        if (i < 0) return
        val j = (i + delta).coerceIn(0, subset.lastIndex)
        if (i == j) return
        val reordered = subset.toMutableList().apply { removeAt(i); add(j, pkg) }
        val g = Repo.orderApps(apps, s).map { it.pkg }.toMutableList()
        val set = subset.toSet()
        val slots = g.indices.filter { g[it] in set }
        if (slots.size != reordered.size) return
        slots.forEachIndexed { k, slot -> g[slot] = reordered[k] }
        update { it.copy(appOrder = g, sortMode = 1) }
    }

    /** Добавить/убрать приложение в ряду «Ваши приложения» на главном экране. */
    fun toggleHome(pkg: String) = update {
        val cur = Repo.homeApps(apps, it).map { a -> a.pkg }
        it.copy(homeConfigured = true, homeApps = if (pkg in cur) cur - pkg else cur + pkg)
    }

    /** Переставить приложение в ряду «Ваши приложения». */
    fun moveHome(pkg: String, delta: Int) = update {
        val cur = Repo.homeApps(apps, it).map { a -> a.pkg }.toMutableList()
        val i = cur.indexOf(pkg)
        if (i < 0) return@update it
        val j = (i + delta).coerceIn(0, cur.lastIndex)
        if (i != j) { cur.removeAt(i); cur.add(j, pkg) }
        it.copy(homeConfigured = true, homeApps = cur)
    }

    fun moveRow(i: Int, d: Int) = update {
        val l = it.rowOrder.toMutableList()
        val j = (i + d).coerceIn(0, l.lastIndex)
        if (i != j && i in l.indices) { val x = l.removeAt(i); l.add(j, x) }
        it.copy(rowOrder = l)
    }

    // ---------------- Данные ----------------
    fun refreshApps() {
        viewModelScope.launch {
            apps = withContext(Dispatchers.IO) { Repo.loadApps(getApplication()) }
            if (s.heroSource == HeroMode.FAVORITES || isRecoStale()) loadHero()
        }
    }

    fun loadWeather() { viewModelScope.launch { loadWeatherNow() } }

    private suspend fun loadWeatherNow(): Boolean {
        val r = withContext(Dispatchers.IO) { Repo.loadWeather(s) }
        r.onSuccess { weather = it; weatherError = null }
        r.onFailure { weatherError = it.message ?: "ошибка сети" }
        return r.isSuccess
    }

    private fun fingerprint(c: LauncherSettings) = "v3|${c.tmdbKey}|${c.recoType}|${c.proxyOn}|${c.proxyType}|${c.proxyBase}"

    private fun isRecoStale() =
        s.heroSource == HeroMode.RECO && s.showHero && (s.tmdbKey.isNotBlank() || Repo.keyOptional(s)) &&
            System.currentTimeMillis() - cache.getLong("ts", 0L) >= UPDATE_MS

    fun refreshReco() = loadHero(force = true)

    private suspend fun recoItems(cur: LauncherSettings, force: Boolean): List<HeroItem> {
        if (cur.tmdbKey.isBlank() && !Repo.keyOptional(cur)) {
            return listOf(HeroItem("Подключите рекомендации", "Настройки → TMDB и прокси → API-ключ", color = Color(0xFF455A64)))
        }
        val fp = fingerprint(cur)
        val ts = cache.getLong("ts", 0L)
        val cached = if (cache.getString("fp", "") == fp) Repo.itemsFromJson(cache.getString("json", "") ?: "") else emptyList()
        if (!force && cached.isNotEmpty() && System.currentTimeMillis() - ts < UPDATE_MS) return cached

        val fresh = withContext(Dispatchers.IO) { runCatching { Repo.fetchReco(cur) }.getOrDefault(emptyList()) }
        if (fresh.isNotEmpty()) {
            val now = System.currentTimeMillis()
            cache.edit().putLong("ts", now).putString("fp", fp).putString("json", Repo.itemsToJson(fresh)).apply()
            recoUpdatedAt = now
            return fresh
        }
        return cached.ifEmpty {
            val hint = if (cur.tmdbKey.isBlank()) "Введите свой API-ключ TMDB в настройках" else "Проверьте ключ и прокси TMDB в настройках"
            listOf(HeroItem("Не удалось загрузить", hint, color = Color(0xFF455A64)))
        }
    }

    fun loadHero(force: Boolean = false) {
        heroJob?.cancel()
        heroJob = viewModelScope.launch {
            if (!s.showHero) return@launch
            val cur = s
            hero = when (cur.heroSource) {
                HeroMode.RECO -> recoItems(cur, force)
                HeroMode.FAVORITES -> {
                    val order = (cur.recents + cur.favorites.toList()).distinct()
                    val items = order.mapNotNull { p -> apps.firstOrNull { it.pkg == p && p !in cur.hidden } }.map {
                        HeroItem(
                            title = it.label,
                            subtitle = if (it.pkg in cur.favorites) "Избранное" else "Недавно открывали",
                            packageName = it.pkg, iconPkg = it.pkg, color = Color(0xFF283593)
                        )
                    }
                    items.ifEmpty {
                        listOf(HeroItem("Пока пусто", "Удерживайте OK на приложении и выберите «В избранное»", color = Color(0xFF455A64)))
                    }
                }
            }
        }
    }

    // ---------------- Действия ----------------
    fun toggleFavorite(pkg: String) =
        update { it.copy(favorites = if (pkg in it.favorites) it.favorites - pkg else it.favorites + pkg) }

    /** @return true, если приложение запущено */
    fun launch(pkg: String): Boolean {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        update { it.copy(recents = (listOf(pkg) + it.recents.filter { r -> r != pkg }).take(10)) }
        ctx.startActivity(intent)
        return true
    }

    /** true = нужно показать выбор приложения (не задано или удалено) */
    fun openHero(item: HeroItem): Boolean {
        if (s.heroSource == HeroMode.RECO) {
            val p = s.tvPkg ?: return true
            return !launch(p)
        }
        item.packageName?.let { launch(it) }
        return false
    }

    fun openAppInfo(pkg: String) =
        startSafe(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    fun uninstall(pkg: String) = startSafe(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    private fun startSafe(i: Intent) {
        try { getApplication<Application>().startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    }
}
