package com.bazinox.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class BazarinoConversation(val id: String, val listingId: String?, val createdAt: String?)
data class BazarinoMessage(val id: String, val conversationId: String, val senderId: String, val body: String, val createdAt: String?)

object ChatRepository {
    private fun auth(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences("bazarino_auth", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
            ?: throw IllegalStateException("برای استفاده از پیام‌رسان ابتدا وارد حساب شو.")
        val userId = prefs.getString("user_id", null)
            ?: throw IllegalStateException("شناسه حساب پیدا نشد؛ خارج شو و دوباره وارد شو.")
        return token to userId
    }

    private suspend fun request(
        context: Context,
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        val (token, _) = auth(context)
        val connection = (URL(SupabaseConfig.REST_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
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
                val detail = try {
                    JSONObject(raw).optString("message").ifBlank { JSONObject(raw).optString("hint") }
                } catch (_: Exception) { raw }
                throw IllegalStateException("ارتباط با پیام‌رسان ناموفق بود (HTTP $code): $detail")
            }
            raw
        } finally {
            connection.disconnect()
        }
    }

    suspend fun conversations(context: Context): List<BazarinoConversation> {
        val (_, userId) = auth(context)
        val query = "conversation_members?select=conversation_id,conversations(id,listing_id,created_at)" +
            "&user_id=eq.${enc(userId)}&order=conversation_id.desc"
        val array = JSONArray(request(context, query))
        return (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            val conversation = row.optJSONObject("conversations") ?: return@mapNotNull null
            BazarinoConversation(
                id = conversation.optString("id"),
                listingId = conversation.optString("listing_id").takeIf { it != "null" && it.isNotBlank() },
                createdAt = conversation.optString("created_at")
            )
        }
    }

    suspend fun messages(context: Context, conversationId: String): List<BazarinoMessage> {
        requireUuid(conversationId)
        val query = "messages?select=id,conversation_id,sender_id,body,created_at" +
            "&conversation_id=eq.${enc(conversationId)}&deleted_at=is.null&order=created_at.asc&limit=200"
        val array = JSONArray(request(context, query))
        return (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            BazarinoMessage(
                row.optString("id"),
                row.optString("conversation_id"),
                row.optString("sender_id"),
                row.optString("body"),
                row.optString("created_at")
            )
        }
    }

    suspend fun startForListing(context: Context, listingId: String): String {
        requireUuid(listingId)
        val result = request(
            context,
            "rpc/create_listing_conversation",
            method = "POST",
            body = JSONObject().put("p_listing_id", listingId)
        )
        return result.trim().removeSurrounding("\"")
    }

    suspend fun send(context: Context, conversationId: String, message: String) {
        requireUuid(conversationId)
        require(message.isNotBlank() && message.length <= 5000) { "پیام باید بین ۱ تا ۵۰۰۰ نویسه باشد." }
        val (_, userId) = auth(context)
        val body = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("conversation_id", conversationId)
            .put("sender_id", userId)
            .put("message_type", "text")
            .put("body", message.trim())
        request(context, "messages", method = "POST", body = body, prefer = "return=minimal")
    }

    private fun requireUuid(value: String) {
        try { UUID.fromString(value) } catch (_: Exception) {
            throw IllegalArgumentException("شناسه معتبر نیست.")
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
