package com.bazinox.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class SocialPost(
    val id: String,
    val authorId: String,
    val caption: String,
    val createdAt: String,
    val likedByMe: Boolean
)

object SocialRepository {
    private const val PREFS = "bazarino_auth"

    private fun auth(context: Context): Pair<String, String> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = p.getString("access_token", null)
            ?: throw IllegalStateException("برای استفاده از بخش اجتماعی ابتدا وارد حساب شو.")
        val userId = p.getString("user_id", null)
            ?: throw IllegalStateException("شناسه حساب پیدا نشد؛ یک‌بار خارج شو و دوباره وارد شو.")
        return token to userId
    }

    private suspend fun request(context: Context, path: String, method: String = "GET", body: JSONObject? = null, prefer: String? = null): String =
        withContext(Dispatchers.IO) {
            val token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("access_token", null)
            val connection = (URL(SupabaseConfig.REST_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer " + (token ?: SupabaseConfig.PUBLISHABLE_KEY))
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                if (prefer != null) setRequestProperty("Prefer", prefer)
            }
            try {
                if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val detail = try { JSONObject(raw).optString("message").ifBlank { raw } } catch (_: Exception) { raw }
                    throw IllegalStateException("خطای سرور ($code): ${detail.take(220)}")
                }
                raw
            } finally {
                connection.disconnect()
            }
        }

    suspend fun loadFeed(context: Context): List<SocialPost> {
        val raw = request(context, "social_posts?select=id,author_id,caption,created_at&status=eq.published&visibility=eq.public&order=created_at.desc&limit=50")
        val rows = JSONArray(raw)
        val userId = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("user_id", null)
        val likedIds = if (userId != null) {
            val likes = JSONArray(request(context, "social_likes?select=post_id&user_id=eq.$userId"))
            (0 until likes.length()).mapNotNull { likes.optJSONObject(it)?.optString("post_id") }.toSet()
        } else emptySet()
        return (0 until rows.length()).mapNotNull { i ->
            rows.optJSONObject(i)?.let { row ->
                val id = row.optString("id")
                SocialPost(id, row.optString("author_id"), row.optString("caption"), row.optString("created_at"), id in likedIds)
            }
        }
    }

    suspend fun createPost(context: Context, caption: String, imageUri: Uri? = null) {
        val (_, userId) = auth(context)
        val postId = UUID.randomUUID().toString()
        request(context, "social_posts", "POST",
            JSONObject().put("id", postId).put("author_id", userId)
                .put("caption", caption.trim()).put("visibility", "public").put("status", "published"),
            "return=minimal")
        if (imageUri != null) {
            val mime = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            if (mime !in listOf("image/jpeg", "image/png", "image/webp")) {
                throw IllegalStateException("فقط تصویر JPG، PNG یا WEBP پشتیبانی می‌شود.")
            }
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IllegalStateException("خواندن تصویر انتخاب‌شده ممکن نیست.")
            if (bytes.size > 10 * 1024 * 1024) throw IllegalStateException("حجم تصویر باید کمتر از ۱۰ مگابایت باشد.")
            val ext = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            val objectPath = "$userId/${UUID.randomUUID()}.$ext"
            withContext(Dispatchers.IO) {
                val connection = (URL(SupabaseConfig.PROJECT_URL.trimEnd('/') + "/storage/v1/object/social-media/" + objectPath)
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                    setRequestProperty("Authorization", "Bearer " + auth(context).first)
                    setRequestProperty("Content-Type", mime)
                    setRequestProperty("x-upsert", "false")
                }
                try {
                    connection.outputStream.use { it.write(bytes) }
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        throw IllegalStateException("بارگذاری تصویر ناموفق بود ($code): ${error.take(160)}")
                    }
                } finally { connection.disconnect() }
            }
            request(context, "social_media", "POST",
                JSONObject().put("id", UUID.randomUUID().toString()).put("post_id", postId)
                    .put("media_key", objectPath).put("media_type", "image").put("sort_order", 0),
                "return=minimal")
        }
    }

    suspend fun toggleLike(context: Context, post: SocialPost) {
        val (_, userId) = auth(context)
        if (post.likedByMe) {
            request(context, "social_likes?post_id=eq.${post.id}&user_id=eq.$userId", "DELETE", prefer = "return=minimal")
        } else {
            request(context, "social_likes", "POST",
                JSONObject().put("post_id", post.id).put("user_id", userId), "return=minimal")
        }
    }

    suspend fun addComment(context: Context, postId: String, body: String) {
        val (_, userId) = auth(context)
        request(context, "social_comments", "POST",
            JSONObject().put("id", UUID.randomUUID().toString()).put("post_id", postId)
                .put("author_id", userId).put("body", body.trim()), "return=minimal")
    }

    suspend fun follow(context: Context, authorId: String) {
        val (_, userId) = auth(context)
        if (authorId == userId) throw IllegalStateException("نمی‌توانی خودت را دنبال کنی.")
        request(context, "user_follows", "POST",
            JSONObject().put("follower_id", userId).put("followed_id", authorId), "return=minimal")
    }
}
