package com.tanix.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

object Repo {

    // ---------- Приложения ----------
    fun loadApps(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val result = LinkedHashMap<String, AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName || result.containsKey(pkg)) continue
                runCatching {
                    val d = ri.loadIcon(pm)
                    // Баннер 16:9, который приложение предоставляет для Android TV (как плитки Netflix, YouTube…)
                    val bannerDrawable = runCatching {
                        ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
                    }.getOrNull()
                    val banner = bannerDrawable?.let { b -> runCatching { b.toBitmap(320, 180).asImageBitmap() }.getOrNull() }
                    result[pkg] = AppItem(
                        ri.loadLabel(pm).toString(), pkg,
                        d.toBitmap(216, 216).asImageBitmap(), d is AdaptiveIconDrawable, banner
                    )
                }
            }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    /** Порядок приложений по выбранному режиму сортировки. */
    fun orderApps(apps: List<AppItem>, s: LauncherSettings): List<AppItem> = when (s.sortMode) {
        1 -> {
            val idx = HashMap<String, Int>()
            s.appOrder.forEachIndexed { i, p -> idx[p] = i }
            apps.sortedWith(compareBy<AppItem> { idx[it.pkg] ?: Int.MAX_VALUE }.thenBy { it.label.lowercase() })
        }
        2 -> {
            val r = s.recents
            apps.sortedWith(compareBy<AppItem> { r.indexOf(it.pkg).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it.label.lowercase() })
        }
        else -> apps.sortedBy { it.label.lowercase() }
    }

    /** Приложения на главном экране: выбранные вручную или (пока не настроено) первые 15. */
    fun homeApps(apps: List<AppItem>, s: LauncherSettings): List<AppItem> {
        val visible = orderApps(apps, s).filter { it.pkg !in s.hidden }
        return if (s.homeConfigured) s.homeApps.mapNotNull { p -> visible.firstOrNull { it.pkg == p } } else visible.take(15)
    }

    // ---------- Погода ----------
    fun searchCity(q: String): List<City> {
        if (q.isBlank()) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(q, "UTF-8")}&count=6&language=ru&format=json"
        val arr = JSONObject(Net.get(url)).optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            City(
                o.optString("name"),
                listOf(o.optString("admin1"), o.optString("country")).filter { s -> s.isNotBlank() }.joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude")
            )
        }
    }

    /** Сначала Open-Meteo, при ошибке — MET Norway. В ошибке указано, что именно не сработало. */
    fun loadWeather(s: LauncherSettings): Result<Weather> {
        val a = runCatching { openMeteo(s) }
        if (a.isSuccess) return a
        val b = runCatching { metNo(s) }
        if (b.isSuccess) return b
        return Result.failure(
            Exception("Open-Meteo: ${a.exceptionOrNull()?.message ?: "?"}; MET.no: ${b.exceptionOrNull()?.message ?: "?"}")
        )
    }

    private fun openMeteo(s: LauncherSettings): Weather {
        val unit = if (s.fahrenheit) "&temperature_unit=fahrenheit" else ""
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${s.lat}&longitude=${s.lon}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&wind_speed_unit=ms&timezone=auto$unit"
        val c = JSONObject(Net.get(url)).getJSONObject("current")
        return Weather(c.getDouble("temperature_2m"), c.getDouble("apparent_temperature"), c.getInt("weather_code"), c.getDouble("wind_speed_10m"))
    }

    private fun metNo(s: LauncherSettings): Weather {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" +
            String.format(Locale.US, "%.4f", s.lat) + "&lon=" + String.format(Locale.US, "%.4f", s.lon)
        val data = JSONObject(Net.get(url, "TanixLauncher/2.1 github.com/tanix-launcher"))
            .getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val d = data.getJSONObject("instant").getJSONObject("details")
        var t = d.getDouble("air_temperature")
        val wind = d.optDouble("wind_speed", 0.0)
        val sym = data.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: data.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: "cloudy"
        if (s.fahrenheit) t = t * 9.0 / 5.0 + 32.0
        return Weather(t, t, symbolToCode(sym), wind)
    }

    private fun symbolToCode(sym: String): Int {
        val b = sym.substringBefore('_')
        return when {
            b.startsWith("clearsky") -> 0
            b.startsWith("fair") -> 1
            b.startsWith("partlycloudy") -> 2
            b == "cloudy" -> 3
            b.contains("fog") -> 45
            b.contains("thunder") -> 95
            b.contains("snow") || b.contains("sleet") -> 73
            b.contains("lightrain") -> 61
            b.contains("rain") -> 63
            else -> 3
        }
    }

    fun weatherInfo(code: Int): Pair<String, String> = when (code) {
        0 -> "☀️" to "Ясно"
        1 -> "🌤️" to "Малооблачно"
        2 -> "⛅" to "Облачно с прояснениями"
        3 -> "☁️" to "Пасмурно"
        45, 48 -> "🌫️" to "Туман"
        in 51..57 -> "🌦️" to "Морось"
        in 61..67 -> "🌧️" to "Дождь"
        in 71..77 -> "❄️" to "Снег"
        in 80..82 -> "🌧️" to "Ливень"
        85, 86 -> "🌨️" to "Снегопад"
        in 95..99 -> "⛈️" to "Гроза"
        else -> "☁️" to "—"
    }

    // ---------- TMDB ----------
    @Volatile var rootuScheme = "http"
    private const val ROOTU_API = "tmdb-api.rootu.top"
    private const val ROOTU_IMG = "tmdb-img.rootu.top"

    /** Зеркало Rootu (из плагина Lampa) можно пробовать и без своего ключа. */
    fun keyOptional(s: LauncherSettings) = s.proxyOn && s.proxyType == 0

    private fun keyParam(s: LauncherSettings) = if (s.tmdbKey.isBlank()) "" else "&api_key=${s.tmdbKey.trim()}"

    fun tmdbApiBase(s: LauncherSettings): String = when {
        !s.proxyOn -> "https://api.themoviedb.org"
        s.proxyType == 0 -> "$rootuScheme://$ROOTU_API"
        s.proxyType == 2 && s.proxyBase.isNotBlank() -> s.proxyBase.trim().trimEnd('/')
        else -> "https://api.tmdb.org"
    }

    fun tmdbImageBase(s: LauncherSettings): String = when {
        !s.proxyOn -> "https://image.tmdb.org/t/p"
        s.proxyType == 0 -> "$rootuScheme://$ROOTU_IMG/t/p"
        s.proxyType == 2 && s.proxyBase.isNotBlank() -> s.proxyBase.trim().trimEnd('/') + "/t/p"
        else -> "https://image.tmdb.org/t/p"
    }

    /** GET к TMDB; path начинается с /3/… Для зеркала Rootu пробует http и https. */
    fun tmdbGet(s: LauncherSettings, path: String): String {
        if (s.proxyOn && s.proxyType == 0) {
            var last: Exception? = null
            val order = if (rootuScheme == "http") listOf("http", "https") else listOf("https", "http")
            for (scheme in order) {
                try {
                    val r = Net.get("$scheme://$ROOTU_API$path")
                    rootuScheme = scheme
                    return r
                } catch (e: Exception) { last = e }
            }
            throw last ?: java.io.IOException("нет ответа")
        }
        return Net.get(tmdbApiBase(s) + path)
    }

    private fun ok(x: String) = x.isNotBlank() && x != "null"

    /** Только российские фильмы и сериалы, свежие за 3 года, по популярности. Карточки — горизонтальные (backdrop). */
    fun fetchReco(s: LauncherSettings): List<HeroItem> {
        val since = java.time.LocalDate.now().minusYears(3).toString()

        fun discover(kind: String): List<HeroItem> {
            val dateParam = if (kind == "movie") "primary_release_date.gte" else "first_air_date.gte"
            val url = "/3/discover/$kind?language=ru-RU&sort_by=popularity.desc" +
                "&with_original_language=ru&with_origin_country=RU&vote_count.gte=3&include_adult=false&$dateParam=$since" +
                keyParam(s)
            val arr = JSONObject(tmdbGet(s, url)).getJSONArray("results")
            val img = tmdbImageBase(s)
            val out = ArrayList<HeroItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val poster = o.optString("poster_path")
                val back = o.optString("backdrop_path")
                if (!ok(poster) && !ok(back)) continue
                val rating = o.optDouble("vote_average", 0.0)
                val year = o.optString("release_date").ifBlank { o.optString("first_air_date") }.take(4)
                out += HeroItem(
                    title = o.optString("title").ifBlank { o.optString("name") },
                    subtitle = o.optString("overview").trim().ifBlank { "Описание отсутствует" }.take(260),
                    imageUrl = if (ok(back)) "$img/w300$back" else "$img/w342$poster",
                    bgUrl = if (ok(back)) "$img/w780$back" else null,
                    badge = listOfNotNull(
                        if (kind == "tv") "Сериал" else "Фильм",
                        year.ifBlank { null },
                        if (rating > 0) "★ %.1f".format(rating) else null
                    ).joinToString(" · ")
                )
            }
            return out
        }

        val movies = if (s.recoType != 2) discover("movie") else emptyList()
        val tv = if (s.recoType != 1) discover("tv") else emptyList()
        val mixed = ArrayList<HeroItem>()
        for (i in 0 until maxOf(movies.size, tv.size)) {
            movies.getOrNull(i)?.let { mixed += it }
            tv.getOrNull(i)?.let { mixed += it }
        }
        return mixed.take(24)
    }

    fun itemsToJson(items: List<HeroItem>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject().put("t", it.title).put("s", it.subtitle).put("i", it.imageUrl ?: "")
                    .put("g", it.bgUrl ?: "").put("b", it.badge ?: "")
            )
        }
        return arr.toString()
    }

    fun itemsFromJson(json: String): List<HeroItem> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            HeroItem(
                title = o.getString("t"), subtitle = o.getString("s"),
                imageUrl = o.optString("i").ifBlank { null },
                bgUrl = o.optString("g").ifBlank { null },
                badge = o.optString("b").ifBlank { null }
            )
        }
    }.getOrDefault(emptyList())

    fun testTmdb(s: LauncherSettings): String {
        if (s.tmdbKey.isBlank() && !keyOptional(s)) return "Сначала введите API-ключ"
        return runCatching {
            tmdbGet(s, "/3/configuration?" + keyParam(s).removePrefix("&"))
            "✓ Соединение работает" + if (s.proxyOn && s.proxyType == 0) " ($rootuScheme)" else ""
        }.getOrElse {
            "✗ Ошибка: ${it.message ?: it.javaClass.simpleName}" +
                if (s.tmdbKey.isBlank()) " — введите свой API-ключ TMDB" else ""
        }
    }
}
