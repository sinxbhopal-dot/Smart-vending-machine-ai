package com.example.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.model.Esp32Response
import com.example.model.LogDirection
import com.example.model.SerialLogEntry
import com.example.model.UsbConnectionState
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Manages USB OTG Serial communication with ESP32 at 115200 Baud.
 * Supports both Physical Hardware connection via usb-serial-for-android
 * and a realistic Virtual Hardware Simulator for testing and preview.
 */
class UsbSerialManager(private val context: Context) : SerialInputOutputManager.Listener {

    private val tag = "UsbSerialManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val ioExecutor = Executors.newSingleThreadExecutor()

    // State flows
    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.Disconnected)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    private val _serialLogs = MutableStateFlow<List<SerialLogEntry>>(emptyList())
    val serialLogs: StateFlow<List<SerialLogEntry>> = _serialLogs.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    // Line assembly buffer for incoming serial stream
    private val lineBuffer = StringBuilder()

    // Simulation toggle
    private var isSimulatedMode = false
    private var simulatedFaultResponse: Esp32Response? = null

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
        const val BAUD_RATE = 115200
        const val DATA_BITS = 8
        const val STOP_BITS = UsbSerialPort.STOPBITS_1
        const val PARITY = UsbSerialPort.PARITY_NONE
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let { connectToDevice(it) }
                        } else {
                            logSys("USB Permission denied for device: ${device?.deviceName}")
                            _connectionState.value = UsbConnectionState.Error("USB permission denied by user")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    logSys("USB Device Attached event received")
                    autoConnect()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    logSys("USB Device Detached")
                    disconnect()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        // Attempt immediate auto-connect or default to simulation if no physical device
        autoConnect()
    }

    /**
     * Tries to discover and connect to a physical ESP32 / USB Serial device.
     */
    fun autoConnect() {
        if (isSimulatedMode) {
            enableSimulationMode(true)
            return
        }

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            logSys("No physical USB Serial device found. Activating Virtual ESP32 Bridge.")
            enableSimulationMode(true)
            return
        }

        val driver = availableDrivers[0]
        val device = driver.device

        if (!usbManager.hasPermission(device)) {
            logSys("Requesting USB permission for ${device.deviceName} (VID: 0x${Integer.toHexString(device.vendorId)})")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                flags
            )
            usbManager.requestPermission(device, permissionIntent)
            _connectionState.value = UsbConnectionState.Connecting
            return
        }

        connectToDevice(device)
    }

    private fun connectToDevice(device: UsbDevice) {
        scope.launch {
            try {
                _connectionState.value = UsbConnectionState.Connecting
                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                val driver = availableDrivers.find { it.device.deviceId == device.deviceId }
                    ?: availableDrivers.firstOrNull()

                if (driver == null) {
                    _connectionState.value = UsbConnectionState.Error("No compatible USB Serial driver found")
                    return@launch
                }

                val connection = usbManager.openDevice(driver.device)
                if (connection == null) {
                    _connectionState.value = UsbConnectionState.Error("Failed to open USB device connection")
                    return@launch
                }

                val port = driver.ports[0]
                port.open(connection)
                port.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)

                // Configure DTR/RTS for ESP32 UART bridge stability
                try {
                    port.dtr = true
                    port.rts = false
                } catch (e: Exception) {
                    Log.w(tag, "DTR/RTS set skipped: ${e.message}")
                }

                usbSerialPort = port
                isSimulatedMode = false

                ioManager = SerialInputOutputManager(port, this@UsbSerialManager).apply {
                    ioExecutor.submit(this)
                }

                val devName = device.productName ?: "ESP32 Controller (0x${Integer.toHexString(device.vendorId)})"
                _connectionState.value = UsbConnectionState.Connected(
                    deviceName = devName,
                    portName = "Port #0",
                    baudRate = BAUD_RATE
                )
                logSys("Connected to physical hardware: $devName at $BAUD_RATE 8-N-1")
            } catch (e: Exception) {
                Log.e(tag, "USB Connect failed", e)
                _connectionState.value = UsbConnectionState.Error("Connection error: ${e.message}")
                logSys("Hardware connection failed: ${e.message}. Falling back to simulator.")
                enableSimulationMode(true)
            }
        }
    }

    /**
     * Toggles Simulation Mode for virtual testing.
     */
    fun enableSimulationMode(enabled: Boolean) {
        isSimulatedMode = enabled
        if (enabled) {
            disconnectPhysical()
            _connectionState.value = UsbConnectionState.Simulated(BAUD_RATE)
            logSys("Virtual ESP32 Controller activated (115200 Baud)")
        } else {
            autoConnect()
        }
    }

    /**
     * Injects a specific fault response for simulation testing.
     * Set to null for standard DISPENSE_OK.
     */
    fun setSimulatedFault(fault: Esp32Response?) {
        simulatedFaultResponse = fault
        logSys("Simulated ESP32 response set to: ${fault?.rawCommand ?: "DISPENSE_OK"}")
    }

    /**
     * Sends the hardware dispense command (e.g. "DISPENSE_1\n")
     */
    fun sendDispenseCommand(slotId: Int): Boolean {
        val command = "DISPENSE_$slotId\n"
        return sendRaw(command)
    }

    /**
     * Sends a raw string command over the USB Serial port.
     */
    fun sendRaw(command: String): Boolean {
        val formatted = if (command.endsWith("\n")) command else "$command\n"
        logTx(formatted.trim())

        if (isSimulatedMode || _connectionState.value is UsbConnectionState.Simulated) {
            simulateEsp32Interaction(formatted.trim())
            return true
        }

        val port = usbSerialPort ?: return false
        return try {
            val bytes = formatted.toByteArray(Charsets.UTF_8)
            port.write(bytes, 2000)
            true
        } catch (e: IOException) {
            Log.e(tag, "USB Write Error", e)
            logSys("Write error: ${e.message}")
            false
        }
    }

    /**
     * Simulates ESP32 internal mechanical cycles and responds asynchronously.
     */
    private fun simulateEsp32Interaction(command: String) {
        scope.launch {
            val slotId: Int? = if (command.startsWith("DISPENSE_")) {
                command.substringAfter("DISPENSE_").trim().toIntOrNull()
            } else if (command.startsWith("{") && command.contains("DISPENSE")) {
                try {
                    val json = org.json.JSONObject(command)
                    json.optInt("slot", 1)
                } catch (_: Exception) { null }
            } else null

            if (slotId != null) {
                if (slotId !in 1..4) {
                    delay(300)
                    handleIncomingLine("ERROR_INVALID_ID")
                    return@launch
                }

                // Simulate realistic elevator lift and belt sensor movement delay
                delay(600)
                logSys("[V-ESP32] Stepper lift moving to Slot #$slotId position...")
                delay(1200)
                logSys("[V-ESP32] Actuating slot release coil & IR belt sensor...")
                delay(1000)

                val response = simulatedFaultResponse ?: Esp32Response.DISPENSE_OK
                handleIncomingLine(response.rawCommand)
            } else if (command.equals("PING", ignoreCase = true)) {
                delay(200)
                handleIncomingLine("PONG_ESP32_OK")
            } else if (command.equals("STATUS", ignoreCase = true)) {
                delay(300)
                handleIncomingLine("STATUS_READY_SLOTS_1234")
            } else {
                delay(200)
                handleIncomingLine("ACK_$command")
            }
        }
    }

    override fun onNewData(data: ByteArray?) {
        if (data == null || data.isEmpty()) return
        val text = String(data, Charsets.UTF_8)
        synchronized(lineBuffer) {
            for (ch in text) {
                if (ch == '\n' || ch == '\r') {
                    if (lineBuffer.isNotEmpty()) {
                        val line = lineBuffer.toString().trim()
                        lineBuffer.setLength(0)
                        if (line.isNotEmpty()) {
                            handleIncomingLine(line)
                        }
                    }
                } else {
                    lineBuffer.append(ch)
                }
            }
        }
    }

    override fun onRunError(e: Exception?) {
        Log.e(tag, "Serial I/O Error: ${e?.message}", e)
        logSys("Serial I/O Error: ${e?.message}")
        disconnect()
    }

    private fun handleIncomingLine(line: String) {
        logRx(line)
        scope.launch {
            _incomingMessages.emit(line)
        }
    }

    fun disconnect() {
        disconnectPhysical()
        _connectionState.value = UsbConnectionState.Disconnected
        logSys("USB disconnected")
    }

    private fun disconnectPhysical() {
        try {
            ioManager?.listener = null
            ioManager?.stop()
            ioManager = null
            usbSerialPort?.close()
            usbSerialPort = null
        } catch (e: Exception) {
            Log.w(tag, "Error during physical disconnect: ${e.message}")
        }
    }

    fun clearLogs() {
        _serialLogs.value = emptyList()
    }

    private fun logTx(msg: String) {
        appendLog(LogDirection.TX, msg)
    }

    private fun logRx(msg: String) {
        appendLog(LogDirection.RX, msg)
    }

    private fun logSys(msg: String) {
        appendLog(LogDirection.SYS, msg)
    }

    private fun appendLog(dir: LogDirection, msg: String) {
        val entry = SerialLogEntry(
            timestamp = timeFormat.format(Date()),
            direction = dir,
            message = msg
        )
        val updated = (_serialLogs.value + entry).takeLast(100)
        _serialLogs.value = updated
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Ignored if already unregistered
        }
        disconnectPhysical()
    }
}
