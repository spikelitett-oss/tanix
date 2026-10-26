package com.tanix.launcher.data

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Сетевой клиент. При включённом DoH адреса tmdb-хостов берутся через Cloudflare/Google DNS-over-HTTPS. */
object Net {
    @Volatile var dohEnabled = false

    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()

    private val cache = ConcurrentHashMap<String, Pair<Long, List<InetAddress>>>()

    private val dohEndpoints = listOf(
        "https://1.1.1.1/dns-query",
        "https://1.0.0.1/dns-query",
        "https://8.8.8.8/resolve",
        "https://8.8.4.4/resolve"
    )

    private fun resolveDoh(host: String): List<InetAddress> {
        for (ep in dohEndpoints) {
            try {
                val req = Request.Builder().url("$ep?name=$host&type=A")
                    .header("accept", "application/dns-json").build()
                dohClient.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@use
                    val answers = JSONObject(r.body!!.string()).optJSONArray("Answer") ?: return@use
                    val list = (0 until answers.length()).map { answers.getJSONObject(it) }
                        .filter { it.optInt("type") == 1 }
                        .map { InetAddress.getByName(it.getString("data")) }
                    if (list.isNotEmpty()) return list
                }
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    private val dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val isTmdb = hostname.contains("tmdb") || hostname.contains("themoviedb")
            if (!dohEnabled || !isTmdb) return Dns.SYSTEM.lookup(hostname)
            val now = System.currentTimeMillis()
            cache[hostname]?.takeIf { it.first > now }?.let { return it.second }
            val res = resolveDoh(hostname)
            if (res.isNotEmpty()) { cache[hostname] = (now + 10 * 60_000) to res; return res }
            return Dns.SYSTEM.lookup(hostname)
        }
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .dns(dns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun get(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", "TanixLauncher/2.0").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            return r.body!!.string()
        }
    }
}
