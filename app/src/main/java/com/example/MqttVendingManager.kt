package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MqttVendingManager(private val context: Context) {
    private val TAG = "MqttVending"
    private val brokerUrl = "ssl://broker.hivemq.com:8883"
    private val aesSecretKey = "Shubh@Vending#AES256SecureKey"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("VendingMachinePrefs", Context.MODE_PRIVATE)

    private var mqttClient: MqttClient? = null

    var onSlotUpdated: ((slot: String, name: String, price: Double, stock: Int, imageUrl: String) -> Unit)? = null
    var onHardwareCommand: ((command: String) -> Unit)? = null

    fun getMachineId(): String = prefs.getString("KEY_MACHINE_ID", "VM_BHOPAL_01") ?: "VM_BHOPAL_01"
    fun getMachineName(): String = prefs.getString("KEY_MACHINE_NAME", "Bhopal Kiosk 1") ?: "Bhopal Kiosk 1"

    fun updateMachineConfig(newId: String, newName: String) {
        val cleanId = newId.trim().uppercase().replace("[^A-Z0-9_-]".toRegex(), "_")
        prefs.edit()
            .putString("KEY_MACHINE_ID", cleanId)
            .putString("KEY_MACHINE_NAME", newName.trim())
            .apply()
        restart()
    }

    fun start() {
        connectToBroker()
    }

    fun restart() {
        Thread {
            try {
                if (mqttClient?.isConnected == true) {
                    mqttClient?.disconnect()
                }
                connectToBroker()
            } catch (e: Exception) {
                Log.e(TAG, "Restart error: ${e.message}")
            }
        }.start()
    }

    private fun connectToBroker() {
        Thread {
            try {
                val currentId = getMachineId()
                val commandTopic = "vending_machine_ctrl/$currentId"
                val clientId = "Vending_" + currentId + "_" + (System.currentTimeMillis() % 10000)

                mqttClient = MqttClient(brokerUrl, clientId, null)

                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 20
                }

                mqttClient?.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        mqttClient?.subscribe(commandTopic, 1)
                        sendTelemetry(online = true, elevatorPos = "Floor 0", dropDetected = false, weightGrams = 0)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val encryptedData = String(message?.payload ?: byteArrayOf())
                        handleIncoming(encryptedData)
                    }

                    override fun connectionLost(cause: Throwable?) {}
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                mqttClient?.connect(options)
            } catch (e: Exception) {
                Log.e(TAG, "Connection Error: ${e.message}")
            }
        }.start()
    }

    private fun handleIncoming(encryptedText: String) {
        try {
            val decryptedJsonStr = decryptAES(encryptedText, aesSecretKey) ?: return
            val json = JSONObject(decryptedJsonStr)
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

    fun sendTelemetry(online: Boolean, elevatorPos: String, dropDetected: Boolean, weightGrams: Int) {
        val payload = JSONObject().apply {
            put("type", "TELEMETRY")
            put("machineId", getMachineId())
            put("machineName", getMachineName())
            put("online", online)
            put("elevatorPos", elevatorPos)
            put("dropDetected", dropDetected)
            put("weightGrams", weightGrams)
            put("timestamp", System.currentTimeMillis())
        }
        publishEncrypted(payload.toString())
    }

    private fun publishEncrypted(jsonStr: String) {
        try {
            val encrypted = encryptAES(jsonStr, aesSecretKey)
            if (encrypted != null && mqttClient?.isConnected == true) {
                val msg = MqttMessage(encrypted.toByteArray())
                msg.qos = 1
                mqttClient?.publish("vending_telemetry/" + getMachineId(), msg)
            }
        } catch (e: Exception) {}
    }

    private fun decryptAES(data: String, keyStr: String): String? {
        return try {
            val keyBytes = MessageDigest.getInstance("MD5").digest(keyStr.toByteArray(Charsets.UTF_8))
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decoded = Base64.decode(data, Base64.DEFAULT)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) { null }
    }

    private fun encryptAES(data: String, keyStr: String): String? {
        return try {
            val keyBytes = MessageDigest.getInstance("MD5").digest(keyStr.toByteArray(Charsets.UTF_8))
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encBytes, Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }
}
