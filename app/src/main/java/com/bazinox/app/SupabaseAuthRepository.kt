package com.bazinox.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthResult(val message: String, val signedIn: Boolean)

object SupabaseAuthRepository {
    private const val PREFS = "bazarino_auth"

    fun savedEmail(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("email", null)

    fun signOut(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    suspend fun signUp(context: Context, email: String, password: String, displayName: String): AuthResult =
        requestAuth(
            context,
            "auth/v1/signup",
            JSONObject()
                .put("email", email)
                .put("password", password)
                .put("data", JSONObject().put("display_name", displayName))
        )

    suspend fun signIn(context: Context, email: String, password: String): AuthResult =
        requestAuth(
            context,
            "auth/v1/token?grant_type=password",
            JSONObject().put("email", email).put("password", password)
        )

    private suspend fun requestAuth(context: Context, path: String, body: JSONObject): AuthResult =
        withContext(Dispatchers.IO) {
            val connection = (URL(SupabaseConfig.PROJECT_URL.trimEnd('/') + "/" + path)
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer " + SupabaseConfig.PUBLISHABLE_KEY)
            }

            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = if (raw.isBlank()) JSONObject() else JSONObject(raw)

                if (code !in 200..299) {
                    val message = json.optString("msg")
                        .ifBlank { json.optString("message") }
                        .ifBlank { json.optString("error_description") }
                        .ifBlank { "خطا در ارتباط با سرویس احراز هویت (HTTP $code)" }
                    throw IllegalStateException(message)
                }

                val token = json.optString("access_token")
                if (token.isNotBlank()) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString("access_token", token)
                        .putString("refresh_token", json.optString("refresh_token"))
                        .putString("user_id", json.optJSONObject("user")?.optString("id").orEmpty())
                        .putString("email", json.optJSONObject("user")?.optString("email") ?: body.optString("email"))
                        .apply()
                    AuthResult("ورود موفق بود.", true)
                } else {
                    AuthResult("ثبت‌نام انجام شد. ایمیل تأیید را بررسی کن؛ بعد از تأیید می‌توانی وارد شوی.", false)
                }
            } finally {
                connection.disconnect()
            }
        }
}
