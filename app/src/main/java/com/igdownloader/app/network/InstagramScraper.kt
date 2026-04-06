package com.igdownloader.app.network

import com.igdownloader.app.domain.model.InstagramMedia
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class InstagramScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Extract the shortcode from any Instagram URL format:
    // https://www.instagram.com/p/{shortcode}/
    // https://www.instagram.com/reel/{shortcode}/
    // https://www.instagram.com/tv/{shortcode}/
    fun extractShortcode(url: String): String? {
        val pattern = Pattern.compile(
            "instagram\\.com/(?:p|reel|tv|reels)/([A-Za-z0-9_-]+)"
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    suspend fun fetchMediaInfo(postUrl: String): Result<InstagramMedia> {
        val shortcode = extractShortcode(postUrl)
            ?: return Result.failure(IllegalArgumentException("Invalid Instagram URL. Please use a post, reel, or TV link."))

        // Try the embed page first — it works without login
        return tryEmbedPage(shortcode, postUrl)
            .recoverCatching { tryOEmbedApi(shortcode, postUrl).getOrThrow() }
    }

    // Strategy 1: Fetch the Instagram embed page and parse the video URL from it
    private fun tryEmbedPage(shortcode: String, postUrl: String): Result<InstagramMedia> {
        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Referer", "https://www.instagram.com/")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            parseEmbedHtml(html, shortcode, postUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseEmbedHtml(html: String, shortcode: String, postUrl: String): Result<InstagramMedia> {
        val doc = Jsoup.parse(html)

        // Try to find the video element
        val videoEl = doc.selectFirst("video")
        val videoUrl = videoEl?.attr("src")?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("video source")?.attr("src")?.takeIf { it.isNotEmpty() }

        // Try to find the thumbnail (poster attribute on video, or og:image)
        val thumbnail = videoEl?.attr("poster")?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst("img.EmbedIgImage")?.attr("src")

        // Try to extract caption
        val caption = doc.selectFirst(".Caption")?.text()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")

        // Also try to extract video URL from inline JSON scripts
        val extractedVideoUrl = videoUrl ?: extractVideoUrlFromScripts(html)

        return if (extractedVideoUrl != null || thumbnail != null) {
            Result.success(
                InstagramMedia(
                    shortcode = shortcode,
                    videoUrl = extractedVideoUrl,
                    thumbnailUrl = thumbnail,
                    caption = caption,
                    isVideo = extractedVideoUrl != null,
                    postUrl = postUrl
                )
            )
        } else {
            Result.failure(Exception("Could not extract media. The post may be private or require login."))
        }
    }

    // Extract video URL from JSON data embedded in <script> tags
    private fun extractVideoUrlFromScripts(html: String): String? {
        // Look for video_url in JSON blobs
        val videoUrlPattern = Pattern.compile("\"video_url\":\\s*\"([^\"]+)\"")
        val matcher = videoUrlPattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.replace("\\/", "/")
        }

        // Look for contentUrl in JSON-LD
        val contentUrlPattern = Pattern.compile("\"contentUrl\":\\s*\"([^\"]+)\"")
        val matcher2 = contentUrlPattern.matcher(html)
        if (matcher2.find()) {
            return matcher2.group(1)?.replace("\\/", "/")
        }

        return null
    }

    // Strategy 2: Use Instagram's oEmbed API for thumbnail fallback
    private fun tryOEmbedApi(shortcode: String, postUrl: String): Result<InstagramMedia> {
        val oEmbedUrl = "https://api.instagram.com/oembed/?url=https://www.instagram.com/p/$shortcode/&omitscript=true"
        val request = Request.Builder()
            .url(oEmbedUrl)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return Result.failure(Exception("Empty oEmbed response"))
            val json = JSONObject(body)
            val thumbnail = json.optString("thumbnail_url").takeIf { it.isNotEmpty() }
            val title = json.optString("title").takeIf { it.isNotEmpty() }

            Result.success(
                InstagramMedia(
                    shortcode = shortcode,
                    videoUrl = null,
                    thumbnailUrl = thumbnail,
                    caption = title,
                    isVideo = false,
                    postUrl = postUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
