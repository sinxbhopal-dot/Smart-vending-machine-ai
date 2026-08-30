package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.SlotItem
import com.example.model.VendingDefaults
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Manages local persistence for Vending Machine Products & Inventory stock levels.
 * Automatically saves state changes to SharedPreferences and handles local storage of picked product images.
 */
class InventoryStorageManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "vending_inventory_prefs"
        private const val KEY_SLOTS_DATA = "slots_inventory_json"
    }

    /**
     * Loads the saved slots from local storage or returns the default 4 slots if uninitialized.
     */
    fun loadSlots(): List<SlotItem> {
        val jsonString = prefs.getString(KEY_SLOTS_DATA, null)
        if (jsonString.isNullOrBlank()) {
            val defaults = VendingDefaults.initialSlots
            saveSlots(defaults)
            return defaults
        }

        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<SlotItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val slotId = obj.optInt("slotId", i + 1)
                val defaultSlot = VendingDefaults.initialSlots.find { it.slotId == slotId }
                    ?: VendingDefaults.initialSlots.getOrElse(i) { VendingDefaults.initialSlots[0] }

                val colorInt = obj.optInt("accentColor", defaultSlot.accentColor.toArgb())

                list.add(
                    SlotItem(
                        slotId = slotId,
                        title = obj.optString("title", defaultSlot.title),
                        category = obj.optString("category", defaultSlot.category),
                        subDescription = obj.optString("subDescription", defaultSlot.subDescription),
                        priceInr = obj.optDouble("priceInr", defaultSlot.priceInr),
                        quantity = obj.optInt("quantity", defaultSlot.quantity),
                        accentColor = Color(colorInt),
                        iconName = obj.optString("iconName", defaultSlot.iconName),
                        imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl") && obj.getString("imageUrl").isNotBlank()) {
                            obj.getString("imageUrl")
                        } else null,
                        calories = obj.optString("calories", defaultSlot.calories),
                        volumeOrWeight = obj.optString("volumeOrWeight", defaultSlot.volumeOrWeight)
                    )
                )
            }
            if (list.size >= 4) list else VendingDefaults.initialSlots
        } catch (e: Exception) {
            e.printStackTrace()
            VendingDefaults.initialSlots
        }
    }

    /**
     * Persists the updated slots list to SharedPreferences.
     */
    fun saveSlots(slots: List<SlotItem>) {
        try {
            val jsonArray = JSONArray()
            for (slot in slots) {
                val obj = JSONObject().apply {
                    put("slotId", slot.slotId)
                    put("title", slot.title)
                    put("category", slot.category)
                    put("subDescription", slot.subDescription)
                    put("priceInr", slot.priceInr)
                    put("quantity", slot.quantity)
                    put("accentColor", slot.accentColor.toArgb())
                    put("iconName", slot.iconName)
                    put("imageUrl", slot.imageUrl ?: "")
                    put("calories", slot.calories)
                    put("volumeOrWeight", slot.volumeOrWeight)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_SLOTS_DATA, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Copies a user-picked image from an external content URI into the app's internal files directory,
     * ensuring permanent access without transient URI permission expirations.
     */
    fun saveImageFromUri(slotId: Int, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val destFile = File(context.filesDir, "slot_${slotId}_image_${System.currentTimeMillis()}.jpg")
            
            // Clean up previous images for this slot
            val oldFiles = context.filesDir.listFiles { _, name -> name.startsWith("slot_${slotId}_image_") }
            oldFiles?.forEach { it.delete() }

            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
