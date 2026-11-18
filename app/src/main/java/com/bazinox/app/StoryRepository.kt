package com.bazinox.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class SocialStory(
    val id: String,
    val authorId: String,
    val mediaUrl: String,
    val expiresAt: String
)

object StoryRepository {
    private fun credentials(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences("bazarino_auth", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
            ?: throw IllegalStateException("برای انتشار استوری ابتدا وارد حساب شو.")
        val userId = prefs.getString("user_id", null)
            ?: throw IllegalStateException("شناسه حساب پیدا نشد؛ دوباره وارد شو.")
        return token to userId
    }

    suspend fun loadStories(context: Context): List<SocialStory> = withContext(Dispatchers.IO) {
        val (token, _) = credentials(context)
        val url = URL(SupabaseConfig.REST_URL +
            "social_stories?select=id,author_id,media_key,expires_at&expires_at=gt.${Uri.encode(Instant.now().toString())}&order=created_at.desc&limit=40")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("دریافت استوری ناموفق بود ($code): ${raw.take(180)}")
            val rows = JSONArray(raw)
            (0 until rows.length()).mapNotNull { index ->
                rows.optJSONObject(index)?.let { row ->
                    val key = row.optString("media_key")
                    val safe = key.split("/").joinToString("/") { Uri.encode(it) }
                    SocialStory(
                        row.optString("id"),
                        row.optString("author_id"),
                        "${SupabaseConfig.PROJECT_URL.trimEnd('/')}/storage/v1/object/public/social-media/$safe",
                        row.optString("expires_at")
                    )
                }
            }
        } finally { connection.disconnect() }
    }

    suspend fun createImageStory(context: Context, imageUri: Uri) = withContext(Dispatchers.IO) {
        val (token, userId) = credentials(context)
        val mime = context.contentResolver.getType(imageUri) ?: "image/jpeg"
        if (mime !in listOf("image/jpeg", "image/png", "image/webp")) {
            throw IllegalStateException("استوری فعلاً فقط تصویر JPG، PNG یا WEBP می‌پذیرد.")
        }
        val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("خواندن تصویر انتخاب‌شده ممکن نیست.")
        if (bytes.size > 10 * 1024 * 1024) throw IllegalStateException("حجم تصویر باید کمتر از ۱۰ مگابایت باشد.")
        val ext = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val objectPath = "$userId/stories/${UUID.randomUUID()}.$ext"
        val upload = (URL(SupabaseConfig.PROJECT_URL.trimEnd('/') + "/storage/v1/object/social-media/" + objectPath.split("/").joinToString("/") { Uri.encode(it) })
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", mime)
            setRequestProperty("x-upsert", "false")
        }
        try {
            upload.outputStream.use { it.write(bytes) }
            val code = upload.responseCode
            if (code !in 200..299) {
                val error = upload.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("بارگذاری استوری ناموفق بود ($code): ${error.take(160)}")
            }
        } finally { upload.disconnect() }

        val expiresAt = Instant.now().plus(24, ChronoUnit.HOURS).toString()
        val body = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("author_id", userId)
            .put("media_key", objectPath)
            .put("media_type", "image")
            .put("expires_at", expiresAt)
        val insert = (URL(SupabaseConfig.REST_URL + "social_stories").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        try {
            insert.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = insert.responseCode
            if (code !in 200..299) {
                val error = insert.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("ثبت استوری ناموفق بود ($code): ${error.take(180)}")
            }
        } finally { insert.disconnect() }
    }
}
