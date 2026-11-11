package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MqttVendingManager(private val context: Context) {
    private val TAG = "MqttVending"
    private val brokerUrl = "tcp://broker.hivemq.com:1883"
    private val aesPassphrase = "Shubh@Vending#AES256SecureKey"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vending_prefs", Context.MODE_PRIVATE)

    private var mqttClient: MqttClient? = null

    var onSlotUpdated: ((slot: String, name: String, price: Double, stock: Int, imageUrl: String?) -> Unit)? = null
    var onHardwareCommand: ((command: String) -> Unit)? = null

    fun getMachineId(): String = prefs.getString("KEY_MACHINE_ID", "VM_BHOPAL_01") ?: "VM_BHOPAL_01"
    fun getMachineName(): String = prefs.getString("KEY_MACHINE_NAME", "Main Kiosk") ?: "Main Kiosk"

    fun updateMachineId(newId: String, newName: String) {
        val cleanId = newId.trim().uppercase().replace("[^A-Z0-9_-]".toRegex(), "_")
        prefs.edit()
            .putString("KEY_MACHINE_ID", cleanId)
            .putString("KEY_MACHINE_NAME", newName.trim())
            .apply()

        // Restart MQTT connection with new Machine ID
        Thread {
            disconnect()
            Thread.sleep(500)
            connectToBroker()
        }.start()
    }

    fun start() {
        connectToBroker()
    }

    fun disconnect() {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

    private fun connectToBroker() {
        Thread {
            try {
                val currentId = getMachineId()
                val topicSub = "vending_machine_ctrl/$currentId"
                val clientId = "VendingApp_" + currentId + "_" + System.currentTimeMillis()

                mqttClient = MqttClient(brokerUrl, clientId, null)
                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 15
                    keepAliveInterval = 30
                }

                mqttClient?.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "Connected to HiveMQ! Subscribing to $topicSub")
                        mqttClient?.subscribe(topicSub, 1)
                        // Send initial telemetry immediately on connect
                        sendLiveTelemetry(online = true, elevatorPos = "Floor 0", dropDetected = false, weightGrams = 0)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val payloadStr = message?.toString() ?: return
                        Log.d(TAG, "Received raw MQTT: $payloadStr")
                        handleIncomingEncrypted(payloadStr)
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.w(TAG, "Connection lost: ${cause?.message}")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                mqttClient?.connect(options)
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}")
            }
        }.start()
    }

    private fun handleIncomingEncrypted(cipherText: String) {
        try {
            val decryptedJson = decryptCryptoJS(cipherText, aesPassphrase)
            Log.d(TAG, "Decrypted: $decryptedJson")
            val json = JSONObject(decryptedJson)

            when (json.optString("action")) {
                "UPDATE_SLOT" -> {
                    onSlotUpdated?.invoke(
                        json.optString("slot"),
                        json.optString("name"),
                        json.optDouble("price", 0.0),
                        json.optInt("stock", 0),
                        json.optString("imageUrl", "")
                    )
                }
                "HARDWARE_CMD" -> {
                    onHardwareCommand?.invoke(json.optString("command"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }

    // Sends telemetry back to Web Dashboard
    fun sendLiveTelemetry(online: Boolean, elevatorPos: String, dropDetected: Boolean, weightGrams: Int) {
        val payload = JSONObject().apply {
            put("type", "TELEMETRY")
            put("online", online)
            put("elevatorPos", elevatorPos)
            put("dropDetected", dropDetected)
            put("weightGrams", weightGrams)
            put("timestamp", System.currentTimeMillis())
        }
        publishEncryptedPayload("vending_telemetry/${getMachineId()}", payload.toString())
    }

    private fun publishEncryptedPayload(topic: String, plainText: String) {
        Thread {
            try {
                if (mqttClient?.isConnected == true) {
                    val encrypted = encryptCryptoJS(plainText, aesPassphrase)
                    val msg = MqttMessage(encrypted.toByteArray(StandardCharsets.UTF_8)).apply { qos = 1 }
                    mqttClient?.publish(topic, msg)
                    Log.d(TAG, "Telemetry published to $topic")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Publish error: ${e.message}")
            }
        }.start()
    }

    // --- CRYPTOJS-COMPATIBLE AES DECRYPTION ---
    private fun decryptCryptoJS(cipherText: String, passphrase: String): String {
        val cipherBytes = Base64.getDecoder().decode(cipherText)
        val salt = ByteArray(8)
        System.arraycopy(cipherBytes, 8, salt, 0, 8)
        val actualCipherText = ByteArray(cipherBytes.size - 16)
        System.arraycopy(cipherBytes, 16, actualCipherText, 0, actualCipherText.size)

        val keyAndIv = evpKDF(passphrase.toByteArray(StandardCharsets.UTF_8), 32, 16, salt)
        val key = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(keyAndIv, 0, key, 0, 32)
        System.arraycopy(keyAndIv, 32, iv, 0, 16)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val decrypted = cipher.doFinal(actualCipherText)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    // --- CRYPTOJS-COMPATIBLE AES ENCRYPTION ---
    private fun encryptCryptoJS(plainText: String, passphrase: String): String {
        val salt = ByteArray(8)
        java.security.SecureRandom().nextBytes(salt)

        val keyAndIv = evpKDF(passphrase.toByteArray(StandardCharsets.UTF_8), 32, 16, salt)
        val key = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(keyAndIv, 0, key, 0, 32)
        System.arraycopy(keyAndIv, 32, iv, 0, 16)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // Prefix with OpenSSL header: 'Salted__' (8 bytes) + Salt (8 bytes)
        val prefix = "Salted__".toByteArray(StandardCharsets.US_ASCII)
        val finalPayload = ByteArray(prefix.size + salt.size + encryptedBytes.size)
        System.arraycopy(prefix, 0, finalPayload, 0, prefix.size)
        System.arraycopy(salt, 0, finalPayload, prefix.size, salt.size)
        System.arraycopy(encryptedBytes, 0, finalPayload, prefix.size + salt.size, encryptedBytes.size)

        return Base64.getEncoder().encodeToString(finalPayload)
    }

    private fun evpKDF(password: ByteArray, keySize: Int, ivSize: Int, salt: ByteArray): ByteArray {
        val totalSize = keySize + ivSize
        val result = ByteArray(totalSize)
        val md5 = MessageDigest.getInstance("MD5")
        var currentHash = ByteArray(0)
        var generated = 0

        while (generated < totalSize) {
            md5.reset()
            if (currentHash.isNotEmpty()) md5.update(currentHash)
            md5.update(password)
            md5.update(salt)
            currentHash = md5.digest()

            val toCopy = minOf(currentHash.size, totalSize - generated)
            System.arraycopy(currentHash, 0, result, generated, toCopy)
            generated += toCopy
        }
        return result
    }
}
