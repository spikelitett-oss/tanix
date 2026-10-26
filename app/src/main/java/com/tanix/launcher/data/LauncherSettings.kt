package com.tanix.launcher.data

val DefaultRowOrder = listOf("posters", "apps", "fav", "recent")

fun normalizeRows(l: List<String>): List<String> =
    (l.filter { it in DefaultRowOrder }.distinct() + DefaultRowOrder).distinct()

/** Все настройки лаунчера. Числовые значения — индексы вариантов (см. ui/Look.kt). */
data class LauncherSettings(
    val onboarded: Boolean = false,

    // ---- Оформление ----
    val themeMode: Int = 1,              // 0 системная, 1 тёмная, 2 светлая
    val dynamicColor: Boolean = true,    // Material You из обоев (Android 12+)
    val accentHue: Float = 250f,         // акцент для Android 11 и ниже
    val amoled: Boolean = false,
    val uiScale: Int = 2,                // 85 / 92 / 100 / 110 / 120 %
    val fontScale: Int = 1,
    val cornerStyle: Int = 1,
    val screenPadding: Int = 1,
    val bgStyle: Int = 2,                // 0 градиент, 1 однотонный, 2 по фокусу как в Google TV
    val bgDim: Int = 1,

    // ---- Шапка ----
    val showLogo: Boolean = true,
    val logoAlpha: Int = 2,
    val showTabs: Boolean = true,
    val tabOnFocus: Boolean = true,
    val showWeather: Boolean = true,
    val weatherDesc: Boolean = false,
    val weatherFeels: Boolean = false,
    val showClock: Boolean = true,
    val clock24: Boolean = true,
    val showDate: Boolean = false,

    // ---- Ряды ----
    val showHero: Boolean = true,
    val showAppsRow: Boolean = true,
    val showFavRow: Boolean = true,
    val showRecentRow: Boolean = true,
    val rowOrder: List<String> = DefaultRowOrder,

    // ---- Постеры ----
    val heroSource: HeroMode = HeroMode.RECO,
    val recoType: Int = 0,               // 0 всё, 1 фильмы, 2 сериалы
    val posterSize: Int = 1,
    val posterDesc: Boolean = true,
    val posterBadge: Boolean = true,

    // ---- Значки ----
    val iconSize: Int = 2,
    val iconShape: Int = 0,              // 0 круг, 1 скруглённый, 2 квадрат
    val showLabels: Boolean = true,
    val sortMode: Int = 1,               // 0 алфавит, 1 свой порядок, 2 недавние

    // ---- Анимации ----
    val animOn: Boolean = true,
    val animSpeed: Int = 1,              // 0 быстро, 1 обычно, 2 плавно
    val focusK: Int = 2,                 // сила увеличения при фокусе
    val focusStyle: Int = 0,             // 0 контур+увеличение, 1 только увеличение, 2 только контур
    val enterAnim: Boolean = true,
    val pivotScroll: Boolean = true,     // «как в Google TV»: фокус держится у края при прокрутке

    // ---- Погода ----
    val city: String = "Москва",
    val lat: Double = 55.7558,
    val lon: Double = 37.6176,
    val fahrenheit: Boolean = false,

    // ---- TMDB ----
    val tmdbKey: String = "",
    val proxyOn: Boolean = true,
    val proxyType: Int = 0,              // 0 Rootu (как в Lampa), 1 DNS-over-HTTPS, 2 своё зеркало
    val proxyBase: String = "",

    // ---- Приложения ----
    val tvPkg: String? = null,
    val hidden: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val appOrder: List<String> = emptyList()
)
