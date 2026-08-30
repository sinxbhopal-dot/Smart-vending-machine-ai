package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple

/**
 * Primary navigation and kiosk flow screens
 */
enum class AppScreen {
    SELECTION,
    PAYMENT_MODAL,
    DISPATCHING,
    DISPATCH_SUCCESS,
    PAYMENT_FAILED
}

/**
 * Represents a vending machine slot with product details and stock level.
 */
data class SlotItem(
    val slotId: Int,
    val title: String,
    val category: String,
    val subDescription: String,
    val priceInr: Double,
    val quantity: Int,
    val accentColor: Color,
    val iconName: String,
    val imageUrl: String? = null,
    val calories: String = "140 kcal",
    val volumeOrWeight: String = "250 ml"
) {
    val isAvailable: Boolean get() = quantity > 0
    val formattedPrice: String get() = "₹%.2f".format(priceInr)
    val priceInPaise: Long get() = (priceInr * 100).toLong()
}

/**
 * Serial Log Entry for USB monitor
 */
data class SerialLogEntry(
    val timestamp: String,
    val direction: LogDirection,
    val message: String
)

enum class LogDirection {
    TX, // Transmitted from Android to ESP32
    RX, // Received from ESP32 to Android
    SYS // Internal System event
}

/**
 * Connection states for the ESP32 USB-OTG port
 */
sealed class UsbConnectionState {
    object Disconnected : UsbConnectionState()
    object Connecting : UsbConnectionState()
    data class Connected(val deviceName: String, val portName: String, val baudRate: Int = 115200) : UsbConnectionState()
    data class Simulated(val baudRate: Int = 115200) : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}

/**
 * Possible parsed responses from ESP32
 */
enum class Esp32Response(val rawCommand: String, val userMessage: String, val isSuccess: Boolean) {
    DISPENSE_OK("DISPENSE_OK", "Product dispensed successfully! Please collect it from the tray.", true),
    DISPENSE_ERROR_LIFT_UP("DISPENSE_ERROR_LIFT_UP", "Elevator lift failed to reach slot level. Hardware check required.", false),
    DISPENSE_ERROR_PRODUCT_NOT_ON_BELT("DISPENSE_ERROR_PRODUCT_NOT_ON_BELT", "Product was not detected on conveyor belt. Slot might be jammed.", false),
    DISPENSE_ERROR_LIFT_DOWN("DISPENSE_ERROR_LIFT_DOWN", "Elevator lift jammed while descending to collection tray.", false),
    DISPENSE_ERROR_EXIT("DISPENSE_ERROR_EXIT", "Exit sensor failed or dispensing door obstructed.", false),
    ERROR_INVALID_ID("ERROR_INVALID_ID", "ESP32 rejected slot ID: Out of valid range (1-4).", false),
    PORT_ERROR("PORT_ERROR", "USB Serial port connection error. Please verify USB OTG cable connection.", false),
    TIMEOUT_ERROR("TIMEOUT", "Hardware timeout: ESP32 did not acknowledge dispense within 15s.", false),
    UNKNOWN("UNKNOWN", "Unrecognized response received from microcontroller.", false);

    companion object {
        fun fromRaw(raw: String): Esp32Response {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    val json = org.json.JSONObject(trimmed)
                    val status = json.optString("status", "").uppercase()
                    if (status == "DISPATCH_OK" || status == "DISPENSE_OK" || status == "SUCCESS" || status == "OK") {
                        return DISPENSE_OK
                    } else if (status.contains("LIFT_UP")) {
                        return DISPENSE_ERROR_LIFT_UP
                    } else if (status.contains("BELT")) {
                        return DISPENSE_ERROR_PRODUCT_NOT_ON_BELT
                    } else if (status.contains("LIFT_DOWN")) {
                        return DISPENSE_ERROR_LIFT_DOWN
                    } else if (status.contains("EXIT")) {
                        return DISPENSE_ERROR_EXIT
                    } else if (status.contains("ERROR") || status == "FAIL") {
                        return UNKNOWN
                    }
                } catch (_: Exception) {}
            }
            if (trimmed.equals("DISPATCH_OK", ignoreCase = true)) {
                return DISPENSE_OK
            }
            return entries.find { it.rawCommand.equals(trimmed, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Dispense cycle lifecycle states
 */
sealed class DispenseState {
    object Idle : DispenseState()
    data class SendingCommand(val slotId: Int) : DispenseState()
    data class DispensingInProgress(val slotId: Int, val stepDescription: String, val progress: Float) : DispenseState()
    data class Success(val slotId: Int, val message: String) : DispenseState()
    data class Failure(val slotId: Int, val error: Esp32Response, val rawMessage: String) : DispenseState()
}

/**
 * Default 4-slot inventory for the Smart Vending Machine
 */
object VendingDefaults {
    const val DEFAULT_UPI_ID = "smartkiosk@upi"
    const val DEFAULT_MERCHANT_NAME = "Smart Vending Kiosk #01"

    val initialSlots = listOf(
        SlotItem(
            slotId = 1,
            title = "Cyber Energy Surge",
            category = "ENERGY & TAURINE",
            subDescription = "High Voltage Cold Energy Drink with Ginseng & B-Vitamins",
            priceInr = 60.00,
            quantity = 19,
            accentColor = NeonCyan,
            iconName = "bolt",
            calories = "110 kcal",
            volumeOrWeight = "330 ml"
        ),
        SlotItem(
            slotId = 2,
            title = "Neon Whey Crisp Bar",
            category = "HIGH PROTEIN BITES",
            subDescription = "20g Whey Protein Bar with Dark Chocolate & Crunchy Crisps",
            priceInr = 50.00,
            quantity = 14,
            accentColor = NeonGreen,
            iconName = "fitness",
            calories = "220 kcal",
            volumeOrWeight = "65 g"
        ),
        SlotItem(
            slotId = 3,
            title = "Glacier Alkaline H2O",
            category = "HYDRATION & MINERALS",
            subDescription = "Zero-Calorie Pure Glacier Sparkling Water with Electrolytes",
            priceInr = 30.00,
            quantity = 25,
            accentColor = NeonPurple,
            iconName = "water_drop",
            calories = "0 kcal",
            volumeOrWeight = "500 ml"
        ),
        SlotItem(
            slotId = 4,
            title = "Quantum Almond Mix",
            category = "ROASTED SUPERFOOD",
            subDescription = "Himalayan Pink Salt Roasted Almonds, Cashews & Berries",
            priceInr = 80.00,
            quantity = 8,
            accentColor = NeonOrange,
            iconName = "cookie",
            calories = "190 kcal",
            volumeOrWeight = "50 g"
        )
    )
}
