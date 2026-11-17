package com.bazinox.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object ListingRepository {
    private const val PREFS = "bazarino_auth"

    suspend fun createPendingListing(
        context: Context,
        title: String,
        description: String,
        categorySlug: String,
        priceToman: Long,
        planDays: Int,
        province: String,
        city: String
    ): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
            ?: throw IllegalStateException("ابتدا وارد حساب کاربری شو.")
        val sellerId = prefs.getString("user_id", null)
            ?: throw IllegalStateException("شناسه حساب پیدا نشد؛ یک‌بار خارج شو و دوباره وارد شو.")

        require(title.isNotBlank()) { "عنوان آگهی را وارد کن." }
        require(description.isNotBlank()) { "توضیحات آگهی را وارد کن." }
        require(priceToman >= 0) { "قیمت معتبر نیست." }
        val feeRial = when (planDays) {
            30 -> 750000L
            60 -> 1500000L
            90 -> 2250000L
            else -> throw IllegalArgumentException("پلن انتشار معتبر نیست.")
        }
        require(province.isNotBlank() && city.isNotBlank()) { "استان و شهر را وارد کن." }

        val body = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("seller_id", sellerId)
            .put("title", title.trim())
            .put("description", description.trim())
            .put("category_slug", categorySlug)
            .put("price_irr", Math.multiplyExact(priceToman, 10L))
            .put("plan_days", planDays)
            .put("listing_fee_irr", feeRial)
            .put("province", province.trim())
            .put("city", city.trim())
            .put("status", "pending_fee")

        val connection = (URL(SupabaseConfig.REST_URL + "listings")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Prefer", "return=representation")
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val error = try { JSONObject(raw).optString("message").ifBlank { raw } } catch (_: Exception) { raw }
                throw IllegalStateException("ثبت آگهی ناموفق بود (HTTP $code): $error")
            }
            "آگهی با موفقیت در سرور ذخیره شد و در انتظار پرداخت هزینه انتشار است."
        } finally {
            connection.disconnect()
        }
    }
}
