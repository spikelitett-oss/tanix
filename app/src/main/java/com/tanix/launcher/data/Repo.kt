package com.tanix.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

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
                    result[pkg] = AppItem(ri.loadLabel(pm).toString(), pkg, d.toBitmap(216, 216).asImageBitmap(), d is AdaptiveIconDrawable)
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

    // ---------- Погода (Open-Meteo, без ключа) ----------
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

    fun loadWeather(s: LauncherSettings): Weather? = runCatching {
        val unit = if (s.fahrenheit) "&temperature_unit=fahrenheit" else ""
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${s.lat}&longitude=${s.lon}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&wind_speed_unit=ms&timezone=auto$unit"
        val c = JSONObject(Net.get(url)).getJSONObject("current")
        Weather(c.getDouble("temperature_2m"), c.getDouble("apparent_temperature"), c.getInt("weather_code"), c.getDouble("wind_speed_10m"))
    }.getOrNull()

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

    /** Только российские фильмы и сериалы (язык оригинала ru, страна RU), свежие за 3 года, по популярности. */
    fun fetchReco(s: LauncherSettings): List<HeroItem> {
        val key = s.tmdbKey.trim()
        val since = java.time.LocalDate.now().minusYears(3).toString()

        fun discover(kind: String): List<HeroItem> {
            val dateParam = if (kind == "movie") "primary_release_date.gte" else "first_air_date.gte"
            val url = "/3/discover/$kind?api_key=$key&language=ru-RU&sort_by=popularity.desc" +
                "&with_original_language=ru&with_origin_country=RU&vote_count.gte=3&include_adult=false&$dateParam=$since"
            val arr = JSONObject(tmdbGet(s, url)).getJSONArray("results")
            val out = ArrayList<HeroItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val poster = o.optString("poster_path")
                if (poster.isBlank() || poster == "null") continue
                val back = o.optString("backdrop_path")
                val rating = o.optDouble("vote_average", 0.0)
                out += HeroItem(
                    title = o.optString("title").ifBlank { o.optString("name") },
                    subtitle = o.optString("overview").trim().ifBlank { "Описание отсутствует" }.take(200),
                    imageUrl = "${tmdbImageBase(s)}/w342$poster",
                    bgUrl = if (back.isNotBlank() && back != "null") "${tmdbImageBase(s)}/w780$back" else null,
                    badge = (if (kind == "tv") "Сериал" else "Фильм") + if (rating > 0) " · ★ %.1f".format(rating) else ""
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
        if (s.tmdbKey.isBlank()) return "Сначала введите API-ключ"
        return runCatching {
            tmdbGet(s, "/3/configuration?api_key=${s.tmdbKey.trim()}")
            "✓ Соединение работает" + if (s.proxyOn && s.proxyType == 0) " ($rootuScheme)" else ""
        }.getOrElse { "✗ Ошибка: ${it.message ?: it.javaClass.simpleName}" }
    }
}
