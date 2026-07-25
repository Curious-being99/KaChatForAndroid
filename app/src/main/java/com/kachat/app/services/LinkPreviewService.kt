package com.kachat.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Open Graph metadata scraped from a message link, for rendering a rich preview card. */
data class LinkPreviewData(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String?
)

/**
 * Fetches Open Graph preview metadata for links sent in chat messages (private/group only -
 * broadcast rooms never call this). Each recipient's own device does this fetch when the message
 * renders, rather than the sender embedding preview data in the encrypted message payload, so
 * link previews never bloat the on-chain/indexer payload.
 *
 * Plain object, not Hilt-injected - [com.kachat.app.ui.screens.MessageBubble] and
 * `GroupMessageBubble` are presentational composables with no ViewModel threaded through, so this
 * mirrors the existing no-DI utility pattern [com.kachat.app.util.TextLinkify] already uses,
 * rather than changing those composables' signatures. Owns its own short-timeout client, separate
 * from [com.kachat.app.di.AppModule]'s REST-API client - arbitrary user-supplied URLs need
 * tighter timeouts than trusted API hosts.
 */
object LinkPreviewService {
    private const val FETCH_TIMEOUT_SECONDS = 8L
    private const val MAX_BODY_BYTES = 1_000_000L
    private const val CACHE_LIMIT = 2_048

    private val client = OkHttpClient.Builder()
        .connectTimeout(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // `null` value = "fetched, but no preview data found" - still worth caching so a bad/plain
    // link isn't refetched on every scroll. Bounded FIFO eviction, not LRU - simplicity over
    // optimality for a cosmetic, cheap-to-refetch-on-relaunch cache.
    private val cache = LinkedHashMap<String, LinkPreviewData?>()
    private val cacheLock = Any()

    private val titleTagRegex = Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE)

    private val youTubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")

    suspend fun fetchPreview(url: String): LinkPreviewData? {
        synchronized(cacheLock) {
            if (cache.containsKey(url)) return cache[url]
        }

        val result = withContext(Dispatchers.IO) { fetchAndParse(url) }

        synchronized(cacheLock) {
            if (!cache.containsKey(url) && cache.size >= CACHE_LIMIT) {
                val oldestKey = cache.keys.firstOrNull()
                if (oldestKey != null) cache.remove(oldestKey)
            }
            cache[url] = result
        }
        return result
    }

    private fun fetchAndParse(url: String): LinkPreviewData? {
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme != "http" && scheme != "https") return null

        // YouTube serves a cookie-consent-wall page (no Open Graph tags at all) to plain scraper
        // requests instead of the real video page, so the generic scrape below never finds
        // anything for a youtube.com/youtu.be link. YouTube's own oEmbed endpoint is built
        // exactly for this (no consent wall, no API key needed).
        val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull()
        if (host != null && host in youTubeHosts) {
            fetchYouTubeOEmbed(url)?.let { return it }
            // Fall through to the generic scrape only if oEmbed itself failed (e.g. a
            // private/deleted video) - unlikely to succeed either, but no harm trying.
        }

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; KaChatLinkPreview/1.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val source = response.body?.source() ?: return null

                val buffer = Buffer()
                while (buffer.size < MAX_BODY_BYTES) {
                    val read = source.read(buffer, MAX_BODY_BYTES - buffer.size)
                    if (read == -1L) break
                }
                val html = buffer.readString(Charsets.UTF_8)
                parseHtml(html, url)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchYouTubeOEmbed(url: String): LinkPreviewData? {
        val oEmbedUrl = "https://www.youtube.com/oembed".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("url", url)
            ?.addQueryParameter("format", "json")
            ?.build() ?: return null

        return try {
            val request = Request.Builder()
                .url(oEmbedUrl)
                .header("User-Agent", "Mozilla/5.0 (compatible; KaChatLinkPreview/1.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val title = json.optString("title").takeIf { it.isNotEmpty() }
                val thumbnailUrl = json.optString("thumbnail_url").takeIf { it.isNotEmpty() }
                if (title == null && thumbnailUrl == null) return null

                LinkPreviewData(
                    url = url,
                    title = title,
                    description = null,
                    imageUrl = thumbnailUrl,
                    siteName = "YouTube"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHtml(html: String, url: String): LinkPreviewData? {
        val title = metaContent("og:title", html, useProperty = true) ?: titleTag(html)
        val description = metaContent("og:description", html, useProperty = true) ?: metaContent("description", html, useProperty = false)
        val imageUrl = metaContent("og:image", html, useProperty = true)?.let { resolveUrl(it, url) }
        val siteName = metaContent("og:site_name", html, useProperty = true) ?: runCatching { java.net.URI(url).host }.getOrNull()

        if (title == null && description == null && imageUrl == null) return null

        return LinkPreviewData(
            url = url,
            title = title?.decodeHtmlEntities(),
            description = description?.decodeHtmlEntities(),
            imageUrl = imageUrl,
            siteName = siteName
        )
    }

    /** Matches `<meta property="og:title" content="...">` in either attribute order, single or
     *  double quotes - real-world OG tags aren't consistent about ordering/quoting. */
    private fun metaContent(tagValue: String, html: String, useProperty: Boolean): String? {
        val attribute = if (useProperty) "property" else "name"
        val escaped = Regex.escape(tagValue)
        val patterns = listOf(
            """<meta[^>]+$attribute=["']$escaped["'][^>]+content=["']([^"']*)["']""",
            """<meta[^>]+content=["']([^"']*)["'][^>]+$attribute=["']$escaped["']"""
        )
        for (pattern in patterns) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(html)
            val raw = match?.groupValues?.getOrNull(1)?.trim()
            if (!raw.isNullOrEmpty()) return raw
        }
        return null
    }

    private fun titleTag(html: String): String? {
        val raw = titleTagRegex.find(html)?.groupValues?.getOrNull(1)?.trim()
        return raw?.takeIf { it.isNotEmpty() }
    }

    private fun resolveUrl(raw: String, base: String): String {
        return try {
            java.net.URI(base).resolve(raw).toString()
        } catch (e: Exception) {
            raw
        }
    }

    private fun String.decodeHtmlEntities(): String {
        return this
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
