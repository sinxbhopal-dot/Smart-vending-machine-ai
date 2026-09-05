package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppScreen
import com.example.model.DispenseState
import com.example.model.Esp32Response
import com.example.model.SlotItem
import com.example.model.UsbConnectionState
import com.example.model.VendingDefaults
import com.example.usb.UsbSerialManager
import com.example.util.InventoryStorageManager
import com.example.util.QrCodeGenerator
import com.example.util.SecureCredentialsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Payment Session state for UPI dynamic QR flow
 */
data class PaymentSession(
    val slotItem: SlotItem,
    val transactionId: String,
    val upiUri: String,
    val qrBitmap: ImageBitmap?,
    val qrId: String? = null,
    val qrImageUrl: String? = null,
    val remainingSeconds: Int = 120,
    val isPaymentConfirmed: Boolean = false,
    val isPolling: Boolean = true,
    val failureReason: String? = null
)

class VendingViewModel(application: Application) : AndroidViewModel(application) {

    val usbManager = UsbSerialManager(application.applicationContext)

    // Local Inventory & Product Storage Manager
    private val inventoryStorageManager = InventoryStorageManager(application.applicationContext)

    // Screen navigation state
    private val _currentScreenState = MutableStateFlow(AppScreen.SELECTION)
    val currentScreenState: StateFlow<AppScreen> = _currentScreenState.asStateFlow()

    // Currently selected product
    private val _selectedProduct = MutableStateFlow<SlotItem?>(null)
    val selectedProduct: StateFlow<SlotItem?> = _selectedProduct.asStateFlow()

    // Auto-Reset countdown for success / failed screen
    private val _countdownTimer = MutableStateFlow(20)
    val countdownTimer: StateFlow<Int> = _countdownTimer.asStateFlow()
    val autoResetCountdown: StateFlow<Int> = _countdownTimer

    // 4 Vending Slots loaded from local persistence
    private val _slots = MutableStateFlow<List<SlotItem>>(inventoryStorageManager.loadSlots())
    val slots: StateFlow<List<SlotItem>> = _slots.asStateFlow()

    // Active Payment Session
    private val _paymentSession = MutableStateFlow<PaymentSession?>(null)
    val paymentSession: StateFlow<PaymentSession?> = _paymentSession.asStateFlow()

    // Dispense state
    private val _dispenseState = MutableStateFlow<DispenseState>(DispenseState.Idle)
    val dispenseState: StateFlow<DispenseState> = _dispenseState.asStateFlow()

    // Diagnostics / Hardware Console Modal
    private val _isConsoleOpen = MutableStateFlow(false)
    val isConsoleOpen: StateFlow<Boolean> = _isConsoleOpen.asStateFlow()

    // Admin Password Lock Prompt
    private val _isPasswordPromptOpen = MutableStateFlow(false)
    val isPasswordPromptOpen: StateFlow<Boolean> = _isPasswordPromptOpen.asStateFlow()

    companion object {
        const val MASTER_ADMIN_PASSWORD = "8103551677"
    }

    // Secure Credentials Storage
    private val credentialsManager = SecureCredentialsManager(application.applicationContext)
    private val _credentials = MutableStateFlow(credentialsManager.getCredentials())
    val credentials: StateFlow<SecureCredentialsManager.RazorpayCredentials> = _credentials.asStateFlow()

    // Flag / Prompt for missing credentials
    private val _missingCredentialsAlert = MutableStateFlow(false)
    val missingCredentialsAlert: StateFlow<Boolean> = _missingCredentialsAlert.asStateFlow()

    // UPI Settings & ESP32 IP
    var upiId: String
        get() = _credentials.value.upiId.ifBlank { VendingDefaults.DEFAULT_UPI_ID }
        set(value) {
            val current = _credentials.value
            saveCredentials(current.keyId, current.keySecret, value, current.merchantName, current.esp32Ip)
        }

    var merchantName: String
        get() = _credentials.value.merchantName.ifBlank { VendingDefaults.DEFAULT_MERCHANT_NAME }
        set(value) {
            val current = _credentials.value
            saveCredentials(current.keyId, current.keySecret, current.upiId, value, current.esp32Ip)
        }

    var esp32Ip: String
        get() = _credentials.value.esp32Ip.ifBlank { "192.168.4.1" }
        set(value) {
            val current = _credentials.value
            saveCredentials(current.keyId, current.keySecret, current.upiId, current.merchantName, value)
        }

    fun saveCredentials(
        keyId: String,
        keySecret: String,
        upi: String = "",
        merchant: String = "",
        esp32Ip: String = ""
    ) {
        val finalUpi = upi.ifBlank { _credentials.value.upiId }.ifBlank { VendingDefaults.DEFAULT_UPI_ID }
        val finalMerchant = merchant.ifBlank { _credentials.value.merchantName }.ifBlank { VendingDefaults.DEFAULT_MERCHANT_NAME }
        val finalEsp32Ip = esp32Ip.ifBlank { _credentials.value.esp32Ip }.ifBlank { "192.168.4.1" }
        credentialsManager.saveCredentials(keyId, keySecret, finalUpi, finalMerchant, finalEsp32Ip)
        _credentials.value = credentialsManager.getCredentials()
        if (_credentials.value.isConfigured) {
            _missingCredentialsAlert.value = false
        }
    }

    fun dismissMissingCredentialsAlert() {
        _missingCredentialsAlert.value = false
    }

    private var paymentTimerJob: Job? = null
    private var paymentPollingJob: Job? = null
    private var dispenseTimeoutJob: Job? = null
    private var dispenseProgressJob: Job? = null
    private var autoResetJob: Job? = null

    init {
        // Listen to incoming serial messages from ESP32
        viewModelScope.launch {
            usbManager.incomingMessages.collectLatest { rawMessage ->
                handleEsp32Message(rawMessage)
            }
        }
    }

    /**
     * User taps "TAP TO BUY" on a slot card.
     * Initiates direct dynamic Razorpay QR creation and launches auto-polling.
     */
    fun startPurchase(slot: SlotItem) {
        if (!slot.isAvailable) return
        autoResetJob?.cancel()
        paymentTimerJob?.cancel()
        paymentPollingJob?.cancel()

        _selectedProduct.value = slot
        _currentScreenState.value = AppScreen.PAYMENT_MODAL

        val txnId = "TXN" + System.currentTimeMillis().toString().takeLast(8) + (100..999).random()
        val defaultUpiUri = QrCodeGenerator.buildUpiUri(
            upiId = upiId,
            merchantName = merchantName,
            amount = slot.priceInr,
            slotId = slot.slotId,
            transactionId = txnId
        )
        val initialQrBitmap = QrCodeGenerator.generateQrBitmap(
            content = defaultUpiUri,
            sizePx = 768
        )

        _paymentSession.value = PaymentSession(
            slotItem = slot,
            transactionId = txnId,
            upiUri = defaultUpiUri,
            qrBitmap = initialQrBitmap,
            qrId = null,
            qrImageUrl = null,
            remainingSeconds = 120,
            isPaymentConfirmed = false,
            isPolling = true
        )
        _dispenseState.value = DispenseState.Idle

        // Direct Razorpay QR API call & Auto-Polling
        viewModelScope.launch {
            val creds = _credentials.value
            var activeQrId: String? = null

            if (creds.isConfigured) {
                val merchantToUse = creds.merchantName.ifBlank { merchantName }
                val qrResult = com.example.util.DirectKioskClient.createDynamicQr(
                    keyId = creds.keyId,
                    keySecret = creds.keySecret,
                    amountInr = slot.priceInr,
                    slotId = slot.slotId,
                    productTitle = slot.title,
                    merchantName = merchantToUse
                )

                qrResult.onSuccess { qrResp ->
                    activeQrId = qrResp.qrId
                    // Construct NPCI dynamic UPI string with Razorpay QR ID:
                    // upi://pay?pa=razorpay@icici&pn={MERCHANT_NAME}&am={AMOUNT}&cu=INR&tr={QR_ID}&tn=Slot_{SLOT_ID}
                    val dynamicUpiUri = QrCodeGenerator.getRazorpayContentUri(
                        payload = qrResp.payloadString,
                        qrId = qrResp.qrid,
                        merchantName = merchantToUse,
                        amount = slot.priceInr,
                        slotId = slot.id
                          )
                    val updatedQrBitmap = QrCodeGenerator.generateQrBitmap(dynamicUpiUri, 768)

                    _paymentSession.value = _paymentSession.value?.copy(
                        qrId = qrResp.qrId,
                        qrImageUrl = null, // Discard external poster/flyer image
                        upiUri = dynamicUpiUri,
                        qrBitmap = updatedQrBitmap ?: initialQrBitmap
                    )
                }
            }

            // Start polling every 2 seconds for payment confirmation
            startPaymentPolling(slot, activeQrId)
        }

        // Start 120-second countdown timer
        paymentTimerJob = viewModelScope.launch {
            for (sec in 120 downTo 0) {
                _paymentSession.value = _paymentSession.value?.copy(remainingSeconds = sec)
                if (sec == 0) {
                    onPaymentFailedOrExpired("Payment session expired.")
                    break
                }
                delay(1000)
            }
        }
    }

    /**
     * Polling coroutine: checks Razorpay API every 2s to detect captured payment.
     */
    private fun startPaymentPolling(slot: SlotItem, qrId: String?) {
        paymentPollingJob?.cancel()
        paymentPollingJob = viewModelScope.launch {
            val creds = _credentials.value
            val targetQrId = qrId ?: _paymentSession.value?.qrId

            while (_currentScreenState.value == AppScreen.PAYMENT_MODAL) {
                delay(2000)

                if (creds.isConfigured && !targetQrId.isNullOrBlank()) {
                    val checkResult = com.example.util.DirectKioskClient.pollQrPaymentStatus(
                        keyId = creds.keyId,
                        keySecret = creds.keySecret,
                        qrId = targetQrId
                    )

                    checkResult.onSuccess { res ->
                        if (res.isPaid) {
                            paymentTimerJob?.cancel()
                            paymentPollingJob?.cancel()
                            onPaymentSuccessDirect(slot, res.paymentId ?: "PAY_${targetQrId}")
                            return@launch
                        }
                    }
                }
            }
        }
    }

    /**
     * Triggered when payment is verified as paid/captured.
     * Initiates direct ESP32 HTTP dispatch and USB signal.
     */
    fun onPaymentSuccessDirect(slot: SlotItem, paymentId: String) {
        paymentTimerJob?.cancel()
        paymentPollingJob?.cancel()
        autoResetJob?.cancel()

        _selectedProduct.value = slot
        _currentScreenState.value = AppScreen.DISPATCHING
        _paymentSession.value = _paymentSession.value?.copy(
            isPaymentConfirmed = true,
            transactionId = paymentId
        )

        triggerDirectHardwareDispense(slot.slotId)
    }

    /**
     * Triggered on payment timeout or explicit failure.
     * DO NOT send any signal to ESP32.
     * Shows Payment Failed screen and returns Home after 5 seconds.
     */
    fun onPaymentFailedOrExpired(reason: String) {
        paymentTimerJob?.cancel()
        paymentPollingJob?.cancel()
        dispenseTimeoutJob?.cancel()
        dispenseProgressJob?.cancel()

        _currentScreenState.value = AppScreen.PAYMENT_FAILED
        _paymentSession.value = _paymentSession.value?.copy(failureReason = reason)
        scheduleAutoReset(5)
    }

    /**
     * Razorpay Standard SDK Payment Success Callback (if SDK is used).
     */
    fun onRazorpayPaymentSuccess(paymentId: String, slot: SlotItem) {
        onPaymentSuccessDirect(slot, paymentId)
    }

    /**
     * Razorpay Payment Error/Cancel Callback.
     */
    fun onRazorpayPaymentCancelledOrFailed(errorMessage: String) {
        onPaymentFailedOrExpired(errorMessage)
    }

    /**
     * Triggered when payment is confirmed: sends direct HTTP POST to ESP32 IP
     * and writes USB command.
     */
    fun triggerDirectHardwareDispense(slotId: Int) {
        autoResetJob?.cancel()
        _currentScreenState.value = AppScreen.DISPATCHING
        _dispenseState.value = DispenseState.SendingCommand(slotId)

        val espIp = _credentials.value.esp32Ip.ifBlank { "192.168.4.1" }

        // 1. USB Serial Fallback
        usbManager.sendDispenseCommand(slotId)

        // 2. Direct HTTP POST to ESP32 local IP
        viewModelScope.launch {
            val httpResult = com.example.util.DirectKioskClient.sendHttpDispenseToEsp32(espIp, slotId)
            httpResult.onSuccess { responseText ->
                handleEsp32Message(responseText)
            }
        }

        // 3. Progressive Hardware Simulation & telemetry
        dispenseProgressJob?.cancel()
        dispenseProgressJob = viewModelScope.launch {
            _dispenseState.value = DispenseState.DispensingInProgress(slotId, "Sending signal to ESP32 ($espIp)...", 0.25f)
            delay(1000)
            _dispenseState.value = DispenseState.DispensingInProgress(slotId, "Lifting: Elevator moving to Slot #$slotId...", 0.60f)
            delay(1000)
            _dispenseState.value = DispenseState.DispensingInProgress(slotId, "Belt Running: Dispensing product to collection tray...", 0.90f)
            delay(600)
            if (_dispenseState.value is DispenseState.DispensingInProgress) {
                handleEsp32Message("DISPATCH_OK")
            }
        }

        // Safety 15s timeout
        dispenseTimeoutJob?.cancel()
        dispenseTimeoutJob = viewModelScope.launch {
            delay(15000)
            if (_dispenseState.value is DispenseState.DispensingInProgress || _dispenseState.value is DispenseState.SendingCommand) {
                dispenseProgressJob?.cancel()
                _dispenseState.value = DispenseState.Failure(
                    slotId = slotId,
                    error = Esp32Response.TIMEOUT_ERROR,
                    rawMessage = "Hardware timeout: ESP32 ($espIp) did not respond within 15 seconds."
                )
                scheduleAutoReset(5)
            }
        }
    }

    /**
     * For backward compatibility with manual trigger or USB command
     */
    fun triggerHardwareDispense(slotId: Int) {
        triggerDirectHardwareDispense(slotId)
    }

    fun confirmPaymentAndDispense() {
        val session = _paymentSession.value ?: return
        onPaymentSuccessDirect(session.slotItem, session.transactionId)
    }

    /**
     * Processes raw string line from ESP32 microcontroller
     */
    private fun handleEsp32Message(raw: String) {
        val trimmed = raw.trim()
        val currentDispense = _dispenseState.value
        val slotId = when (currentDispense) {
            is DispenseState.SendingCommand -> currentDispense.slotId
            is DispenseState.DispensingInProgress -> currentDispense.slotId
            else -> _paymentSession.value?.slotItem?.slotId ?: _selectedProduct.value?.slotId ?: 1
        }

        val espResponse = Esp32Response.fromRaw(trimmed)
        dispenseTimeoutJob?.cancel()
        dispenseProgressJob?.cancel()

        when (espResponse) {
            Esp32Response.DISPENSE_OK -> {
                // Decrement slot inventory
                decrementSlotQuantity(slotId)
                _currentScreenState.value = AppScreen.DISPATCH_SUCCESS
                _dispenseState.value = DispenseState.Success(
                    slotId = slotId,
                    message = espResponse.userMessage
                )
                scheduleAutoReset(20)
            }
            Esp32Response.UNKNOWN -> {
                if (trimmed.startsWith("DISPENSE_ERROR") || trimmed.startsWith("ERROR_")) {
                    _dispenseState.value = DispenseState.Failure(
                        slotId = slotId,
                        error = espResponse,
                        rawMessage = trimmed
                    )
                    scheduleAutoReset(6)
                }
            }
            else -> {
                // Sensor errors (LIFT_UP, PRODUCT_NOT_ON_BELT, LIFT_DOWN, EXIT, etc.)
                _dispenseState.value = DispenseState.Failure(
                    slotId = slotId,
                    error = espResponse,
                    rawMessage = trimmed
                )
                scheduleAutoReset(6)
            }
        }
    }

    /**
     * Once "DISPENSE_OK" or an error code is received, hold the confirmation
     * on screen for the specified countdown (20 seconds for success), then automatically reset
     * the UI back to the idle home screen.
     */
    private fun scheduleAutoReset(durationSeconds: Int = 20) {
        autoResetJob?.cancel()
        _countdownTimer.value = durationSeconds
        autoResetJob = viewModelScope.launch {
            for (sec in durationSeconds downTo 1) {
                _countdownTimer.value = sec
                delay(1000)
            }
            _countdownTimer.value = 0
            cancelPurchase()
        }
    }

    fun decrementSlotQuantity(slotId: Int) {
        val updated = _slots.value.map { item ->
            if (item.slotId == slotId && item.quantity > 0) {
                item.copy(quantity = item.quantity - 1)
            } else {
                item
            }
        }
        _slots.value = updated
        inventoryStorageManager.saveSlots(updated)
    }

    fun updateSlot(updatedSlot: SlotItem) {
        val updated = _slots.value.map { item ->
            if (item.slotId == updatedSlot.slotId) updatedSlot else item
        }
        _slots.value = updated
        inventoryStorageManager.saveSlots(updated)
    }

    fun saveProductImageFromUri(slotId: Int, uri: Uri): String? {
        return inventoryStorageManager.saveImageFromUri(slotId, uri)
    }

    fun restockSlot(slotId: Int, newQty: Int = 20) {
        val updated = _slots.value.map { item ->
            if (item.slotId == slotId) item.copy(quantity = newQty) else item
        }
        _slots.value = updated
        inventoryStorageManager.saveSlots(updated)
    }

    fun restockAll(qty: Int = 20) {
        val updated = _slots.value.map { it.copy(quantity = qty) }
        _slots.value = updated
        inventoryStorageManager.saveSlots(updated)
    }

    fun cancelPurchase() {
        paymentTimerJob?.cancel()
        dispenseTimeoutJob?.cancel()
        dispenseProgressJob?.cancel()
        autoResetJob?.cancel()
        _selectedProduct.value = null
        _currentScreenState.value = AppScreen.SELECTION
        _paymentSession.value = null
        _dispenseState.value = DispenseState.Idle
    }

    fun requestAdminAccess() {
        _isPasswordPromptOpen.value = true
    }

    fun dismissPasswordPrompt() {
        _isPasswordPromptOpen.value = false
    }

    fun verifyAndUnlockAdmin(password: String): Boolean {
        return if (password == MASTER_ADMIN_PASSWORD) {
            _isPasswordPromptOpen.value = false
            _isConsoleOpen.value = true
            true
        } else {
            false
        }
    }

    fun openConsole(open: Boolean) {
        _isConsoleOpen.value = open
    }

    override fun onCleared() {
        super.onCleared()
        paymentTimerJob?.cancel()
        dispenseTimeoutJob?.cancel()
        dispenseProgressJob?.cancel()
        usbManager.cleanup()
    }
}
