package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Direct client for Razorpay QR Code REST API and local ESP32 HTTP dispatching.
 * Operates standalone on-device without requiring any external middle-tier server.
 */
object DirectKioskClient {

    private const val TAG = "DirectKioskClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    data class RazorpayQrResponse(
        val qrId: String,
        val imageUrl: String?,
        val payloadString: String?,
        val status: String,
        val amountInPaise: Long
    )

    data class PaymentCheckResult(
        val isPaid: Boolean,
        val paymentId: String?,
        val status: String,
        val amountReceived: Long
    )

    /**
     * Directly creates a single-use Dynamic UPI QR Code using Razorpay's QR Codes API.
     * POST https://api.razorpay.com/v1/payments/qr_codes
     */
    suspend fun createDynamicQr(
        keyId: String,
        keySecret: String,
        amountInr: Double,
        slotId: Int,
        productTitle: String,
        merchantName: String
    ): Result<RazorpayQrResponse> = withContext(Dispatchers.IO) {
        try {
            val amountPaise = (amountInr * 100).toLong().coerceAtLeast(100L)
            val authHeader = Credentials.basic(keyId.trim(), keySecret.trim())

            val payloadJson = JSONObject().apply {
                put("type", "upi_qr")
                put("name", merchantName.ifBlank { "Smart Vending Kiosk" })
                put("usage", "single_use")
                put("fixed_amount", true)
                put("payment_amount", amountPaise)
                put("description", "Slot #$slotId - $productTitle")
                val notes = JSONObject().apply {
                    put("slot_id", slotId.toString())
                    put("product", productTitle)
                }
                put("notes", notes)
            }

            val request = Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes")
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .post(payloadJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Razorpay QR creation failed: HTTP ${response.code} - $bodyString")
                    return@withContext Result.failure(Exception("Razorpay API Error (${response.code}): $bodyString"))
                }

                val json = JSONObject(bodyString)
                val qrId = json.optString("id", "")
                val imageUrl = json.optString("image_url", null)
                val payloadString = json.optString("payload_string", "")
                    .ifBlank { json.optString("intent_url", "") }
                    .ifBlank { json.optString("qr_code", "") }
                    .takeIf { it.isNotBlank() }
                val status = json.optString("status", "active")
                val amount = json.optLong("payment_amount", amountPaise)

                Result.success(
                    RazorpayQrResponse(
                        qrId = qrId,
                        imageUrl = imageUrl,
                        payloadString = payloadString,
                        status = status,
                        amountInPaise = amount
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createDynamicQr: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Directly queries Razorpay API to check if the specific QR code has received payments.
     * Checks:
     * 1. GET https://api.razorpay.com/v1/payments/qr_codes/{qr_id}/payments
     * 2. GET https://api.razorpay.com/v1/payments/qr_codes/{qr_id}
     */
    suspend fun pollQrPaymentStatus(
        keyId: String,
        keySecret: String,
        qrId: String
    ): Result<PaymentCheckResult> = withContext(Dispatchers.IO) {
        try {
            if (qrId.isBlank()) {
                return@withContext Result.failure(Exception("Missing QR ID"))
            }

            val authHeader = Credentials.basic(keyId.trim(), keySecret.trim())

            // 1. Query payments associated with this QR Code
            val paymentsRequest = Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes/$qrId/payments")
                .header("Authorization", authHeader)
                .get()
                .build()

            httpClient.newCall(paymentsRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val count = json.optInt("count", 0)
                    val items: JSONArray? = json.optJSONArray("items")

                    if (count > 0 && items != null && items.length() > 0) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            val status = item.optString("status", "").lowercase()
                            val paymentId = item.optString("id", "")
                            val amount = item.optLong("amount", 0L)

                            if (status == "captured" || status == "authorized" || status == "processed") {
                                return@withContext Result.success(
                                    PaymentCheckResult(
                                        isPaid = true,
                                        paymentId = paymentId,
                                        status = status,
                                        amountReceived = amount
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Also check the QR entity itself
            val qrRequest = Request.Builder()
                .url("https://api.razorpay.com/v1/payments/qr_codes/$qrId")
                .header("Authorization", authHeader)
                .get()
                .build()

            httpClient.newCall(qrRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val paymentsReceived = json.optInt("payments_count_received", 0)
                    val amountReceived = json.optLong("payments_amount_received", 0L)
                    val status = json.optString("status", "active").lowercase()

                    if (paymentsReceived > 0 || (status == "closed" && amountReceived > 0)) {
                        return@withContext Result.success(
                            PaymentCheckResult(
                                isPaid = true,
                                paymentId = "PAY_QR_${qrId.takeLast(8)}",
                                status = status,
                                amountReceived = amountReceived
                            )
                        )
                    }
                }
            }

            Result.success(
                PaymentCheckResult(
                    isPaid = false,
                    paymentId = null,
                    status = "pending",
                    amountReceived = 0L
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error polling QR payment status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Directly sends HTTP POST command to local ESP32 IP over Wi-Fi / AP network.
     * E.g. http://192.168.4.1/dispense or http://192.168.1.50/api/dispense
     */
    suspend fun sendHttpDispenseToEsp32(
        esp32Ip: String,
        slotId: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanIp = esp32Ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val targetIp = if (cleanIp.isBlank()) "192.168.4.1" else cleanIp

        val urlsToTry = listOf(
            "http://$targetIp/dispense",
            "http://$targetIp/api/dispense",
            "http://$targetIp/dispense?slot=$slotId"
        )

        val payload = JSONObject().apply {
            put("slot", slotId)
            put("action", "DISPENSE")
            put("status", "PAID")
            put("timestamp", System.currentTimeMillis())
        }.toString()

        var lastError: Exception? = null

        for (url in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful || response.code in 200..299) {
                        Log.i(TAG, "ESP32 HTTP Dispense Success ($url): $body")
                        return@withContext Result.success(body.ifBlank { "DISPATCH_OK" })
                    }
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "ESP32 endpoint $url unreachable: ${e.message}")
            }
        }

        Result.failure(lastError ?: Exception("ESP32 ($targetIp) did not acknowledge HTTP POST request."))
    }
}
