package com.example.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object QrCodeGenerator {

    /**
     * Builds a standard BHIM/NPCI compliant UPI Intent URI
     * e.g. upi://pay?pa=merchant@upi&pn=Smart+Vending+Kiosk&am=60.00&cu=INR&tn=Slot1_TXN12345
     */
    fun buildUpiUri(
        upiId: String,
        merchantName: String,
        amount: Double,
        slotId: Int,
        transactionId: String,
        currency: String = "INR"
    ): String {
        val encodedName = URLEncoder.encode(merchantName, StandardCharsets.UTF_8.name())
        val note = URLEncoder.encode("Slot #$slotId Item Dispense", StandardCharsets.UTF_8.name())
        val formattedAmount = "%.2f".format(amount)
        return "upi://pay?pa=$upiId&pn=$encodedName&am=$formattedAmount&cu=$currency&tn=$note&tr=$transactionId"
    }

    /**
     * Constructs a dynamic Razorpay NPCI UPI intent URI for direct on-device QR generation:
     * upi://pay?pa=razorpay@icici&pn={MERCHANT_NAME}&am={AMOUNT}&cu=INR&tr={QR_ID}&tn=Slot_{SLOT_ID}
     */
    fun getRazorpayContentUri(payload: String?, qrId: String?, merchantName: String, amount: Double, slotId: Int): String {
    // Agar Razorpay API se official payload link mila hai, to direct wahi use karein
    if (!payload.isNullOrBlank()) {
        return payload
    }
    // Fallback: Agar payload nahi hai tabhi manual link banayein
    val encodedName = URLEncoder.encode(merchantName.ifBlank { "Smart Vending Kiosk" }, StandardCharsets.UTF_8.name())
    val formattedAmount = "%.2f".format(amount)
    val note = URLEncoder.encode("Slot ${slotId}", StandardCharsets.UTF_8.name())
    return "upi://pay?pa=razorpay@icici&pn=${encodedName}&am=${formattedAmount}&cu=INR&tr=${qrId ?: ""}&tn=${note}"
    }

    /**
     * Generates a high-contrast ImageBitmap representing the standalone square QR code for Compose rendering.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 768,
        darkColor: Int = AndroidColor.BLACK,
        lightColor: Int = AndroidColor.WHITE
    ): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name(),
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
                EncodeHintType.MARGIN to 0
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
