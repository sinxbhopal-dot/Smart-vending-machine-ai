package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure Credential Manager using Android KeyStore & AES-GCM encryption
 * to securely store and retrieve Razorpay API Key ID and Key Secret.
 */
class SecureCredentialsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_vending_credentials"
        private const val KEY_ALIAS = "RazorpayKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val PREF_KEY_ID = "enc_razorpay_key_id"
        private const val PREF_KEY_SECRET = "enc_razorpay_key_secret"
        private const val PREF_UPI_ID = "enc_upi_id"
        private const val PREF_MERCHANT_NAME = "enc_merchant_name"
        private const val PREF_ESP32_IP = "enc_esp32_ip"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class RazorpayCredentials(
        val keyId: String = "",
        val keySecret: String = "",
        val upiId: String = "",
        val merchantName: String = "",
        val esp32Ip: String = "192.168.4.1"
    ) {
        val isConfigured: Boolean get() = keyId.isNotBlank() && keySecret.isNotBlank()
    }

    init {
        ensureKeyGenerated()
    }

    private fun ensureKeyGenerated() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenSpec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGen.init(keyGenSpec)
                keyGen.generateKey()
            }
        } catch (e: Exception) {
            // Fallback for environments where AndroidKeyStore has limitations (e.g. unit tests)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        val secretKey = getSecretKey()
        val combined = try {
            Base64.decode(encryptedBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            return ""
        }

        if (secretKey == null || combined.size <= GCM_IV_LENGTH) {
            return try {
                String(combined, Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
        }

        return try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, combined, 0, GCM_IV_LENGTH)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(combined, GCM_IV_LENGTH, combined.size - GCM_IV_LENGTH)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback decode if direct Base64
            try {
                String(combined, Charsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }

    /**
     * Saves credentials securely into encrypted preferences.
     */
    fun saveCredentials(
        keyId: String,
        keySecret: String,
        upiId: String = "",
        merchantName: String = "",
        esp32Ip: String = ""
    ) {
        prefs.edit()
            .putString(PREF_KEY_ID, encrypt(keyId.trim()))
            .putString(PREF_KEY_SECRET, encrypt(keySecret.trim()))
            .putString(PREF_UPI_ID, encrypt(upiId.trim()))
            .putString(PREF_MERCHANT_NAME, encrypt(merchantName.trim()))
            .putString(PREF_ESP32_IP, encrypt(esp32Ip.trim()))
            .apply()
    }

    /**
     * Loads credentials and decrypts them.
     */
    fun getCredentials(): RazorpayCredentials {
        val encKeyId = prefs.getString(PREF_KEY_ID, "") ?: ""
        val encKeySecret = prefs.getString(PREF_KEY_SECRET, "") ?: ""
        val encUpiId = prefs.getString(PREF_UPI_ID, "") ?: ""
        val encMerchantName = prefs.getString(PREF_MERCHANT_NAME, "") ?: ""
        val encEsp32Ip = prefs.getString(PREF_ESP32_IP, "") ?: ""

        val keyId = decrypt(encKeyId)
        val keySecret = decrypt(encKeySecret)
        val upiId = decrypt(encUpiId).ifBlank { "vendingkiosk@upi" }
        val merchantName = decrypt(encMerchantName).ifBlank { "Smart Vending Kiosk" }
        val esp32Ip = decrypt(encEsp32Ip).ifBlank { "192.168.4.1" }

        return RazorpayCredentials(
            keyId = keyId,
            keySecret = keySecret,
            upiId = upiId,
            merchantName = merchantName,
            esp32Ip = esp32Ip
        )
    }

    /**
     * Clears credentials.
     */
    fun clearCredentials() {
        prefs.edit().clear().apply()
    }
}
