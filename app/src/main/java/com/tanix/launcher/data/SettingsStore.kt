package com.tanix.launcher.data

import android.content.Context

class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tanix", Context.MODE_PRIVATE)

    private fun list(k: String): List<String> = (sp.getString(k, "") ?: "").split(",").filter { it.isNotBlank() }

    fun load(): LauncherSettings {
        val d = LauncherSettings()
        fun b(k: String, def: Boolean) = sp.getBoolean(k, def)
        fun i(k: String, def: Int) = sp.getInt(k, def)
        fun t(k: String, def: String) = sp.getString(k, def) ?: def
        return LauncherSettings(
            onboarded = b("onboarded", d.onboarded),
            themeMode = i("themeMode", d.themeMode),
            dynamicColor = b("dynamicColor", d.dynamicColor),
            accentHue = sp.getFloat("accentHue", d.accentHue),
            amoled = b("amoled", d.amoled),
            uiScale = i("uiScale", d.uiScale),
            fontScale = i("fontScale", d.fontScale),
            cornerStyle = i("cornerStyle", d.cornerStyle),
            screenPadding = i("screenPadding", d.screenPadding),
            bgStyle = i("bgStyle", d.bgStyle),
            bgDim = i("bgDim", d.bgDim),
            showLogo = b("showLogo", d.showLogo),
            logoAlpha = i("logoAlpha", d.logoAlpha),
            showTabs = b("showTabs", d.showTabs),
            tabOnFocus = b("tabOnFocus", d.tabOnFocus),
            showWeather = b("showWeather", d.showWeather),
            weatherDesc = b("weatherDesc", d.weatherDesc),
            weatherFeels = b("weatherFeels", d.weatherFeels),
            showClock = b("showClock", d.showClock),
            clock24 = b("clock24", d.clock24),
            showDate = b("showDate", d.showDate),
            showHero = b("showHero", d.showHero),
            showAppsRow = b("showAppsRow", d.showAppsRow),
            showFavRow = b("showFavRow", d.showFavRow),
            showRecentRow = b("showRecentRow", d.showRecentRow),
            rowOrder = normalizeRows(list("rowOrder")),
            heroSource = runCatching { HeroMode.valueOf(t("heroSource", "RECO")) }.getOrDefault(d.heroSource),
            recoType = i("recoType", d.recoType),
            posterSize = i("posterSize", d.posterSize),
            posterDesc = b("posterDesc", d.posterDesc),
            posterBadge = b("posterBadge", d.posterBadge),
            iconSize = i("iconSize", d.iconSize),
            iconShape = i("iconShape", d.iconShape),
            showLabels = b("showLabels", d.showLabels),
            sortMode = i("sortMode", d.sortMode),
            tileStyle = i("tileStyle", d.tileStyle),
            labelMode = i("labelMode", d.labelMode),
            glow = b("glow", d.glow),
            shine = b("shine", d.shine),
            kenBurns = b("kenBurns", d.kenBurns),
            homeConfigured = b("homeConfigured", d.homeConfigured),
            homeApps = list("homeApps"),
            animOn = b("animOn", d.animOn),
            animSpeed = i("animSpeed", d.animSpeed),
            focusK = i("focusK", d.focusK),
            focusStyle = i("focusStyle", d.focusStyle),
            enterAnim = b("enterAnim", d.enterAnim),
            pivotScroll = b("pivotScroll", d.pivotScroll),
            city = t("city", d.city),
            lat = sp.getString("lat", null)?.toDoubleOrNull() ?: d.lat,
            lon = sp.getString("lon", null)?.toDoubleOrNull() ?: d.lon,
            fahrenheit = b("fahrenheit", d.fahrenheit),
            tmdbKey = t("tmdbKey", ""),
            proxyOn = b("proxyOn", d.proxyOn),
            proxyType = i("proxyType", d.proxyType),
            proxyBase = t("proxyBase", ""),
            tvPkg = sp.getString("tvPkg", null),
            hidden = HashSet(sp.getStringSet("hidden", emptySet()) ?: emptySet()),
            favorites = HashSet(sp.getStringSet("favorites", emptySet()) ?: emptySet()),
            recents = list("recents"),
            appOrder = list("appOrder")
        )
    }

    fun save(s: LauncherSettings) {
        val e = sp.edit()
        e.putBoolean("onboarded", s.onboarded)
        e.putInt("themeMode", s.themeMode)
        e.putBoolean("dynamicColor", s.dynamicColor)
        e.putFloat("accentHue", s.accentHue)
        e.putBoolean("amoled", s.amoled)
        e.putInt("uiScale", s.uiScale)
        e.putInt("fontScale", s.fontScale)
        e.putInt("cornerStyle", s.cornerStyle)
        e.putInt("screenPadding", s.screenPadding)
        e.putInt("bgStyle", s.bgStyle)
        e.putInt("bgDim", s.bgDim)
        e.putBoolean("showLogo", s.showLogo)
        e.putInt("logoAlpha", s.logoAlpha)
        e.putBoolean("showTabs", s.showTabs)
        e.putBoolean("tabOnFocus", s.tabOnFocus)
        e.putBoolean("showWeather", s.showWeather)
        e.putBoolean("weatherDesc", s.weatherDesc)
        e.putBoolean("weatherFeels", s.weatherFeels)
        e.putBoolean("showClock", s.showClock)
        e.putBoolean("clock24", s.clock24)
        e.putBoolean("showDate", s.showDate)
        e.putBoolean("showHero", s.showHero)
        e.putBoolean("showAppsRow", s.showAppsRow)
        e.putBoolean("showFavRow", s.showFavRow)
        e.putBoolean("showRecentRow", s.showRecentRow)
        e.putString("rowOrder", s.rowOrder.joinToString(","))
        e.putString("heroSource", s.heroSource.name)
        e.putInt("recoType", s.recoType)
        e.putInt("posterSize", s.posterSize)
        e.putBoolean("posterDesc", s.posterDesc)
        e.putBoolean("posterBadge", s.posterBadge)
        e.putInt("iconSize", s.iconSize)
        e.putInt("iconShape", s.iconShape)
        e.putBoolean("showLabels", s.showLabels)
        e.putInt("sortMode", s.sortMode)
        e.putInt("tileStyle", s.tileStyle)
        e.putInt("labelMode", s.labelMode)
        e.putBoolean("glow", s.glow)
        e.putBoolean("shine", s.shine)
        e.putBoolean("kenBurns", s.kenBurns)
        e.putBoolean("homeConfigured", s.homeConfigured)
        e.putString("homeApps", s.homeApps.joinToString(","))
        e.putBoolean("animOn", s.animOn)
        e.putInt("animSpeed", s.animSpeed)
        e.putInt("focusK", s.focusK)
        e.putInt("focusStyle", s.focusStyle)
        e.putBoolean("enterAnim", s.enterAnim)
        e.putBoolean("pivotScroll", s.pivotScroll)
        e.putString("city", s.city)
        e.putString("lat", s.lat.toString())
        e.putString("lon", s.lon.toString())
        e.putBoolean("fahrenheit", s.fahrenheit)
        e.putString("tmdbKey", s.tmdbKey)
        e.putBoolean("proxyOn", s.proxyOn)
        e.putInt("proxyType", s.proxyType)
        e.putString("proxyBase", s.proxyBase)
        e.putString("tvPkg", s.tvPkg)
        e.putStringSet("hidden", HashSet(s.hidden))
        e.putStringSet("favorites", HashSet(s.favorites))
        e.putString("recents", s.recents.joinToString(","))
        e.putString("appOrder", s.appOrder.joinToString(","))
        e.apply()
    }
}
