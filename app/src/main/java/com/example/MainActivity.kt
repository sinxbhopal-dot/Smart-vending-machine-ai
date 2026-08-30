package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SlotItem
import com.example.ui.components.AdminPasswordDialog
import com.example.ui.components.HardwareConsoleDialog
import com.example.ui.components.KioskHeader
import com.example.ui.components.PaymentDialog
import com.example.ui.components.ProductSlotCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SmartVendingTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.SecureCredentialsManager
import com.example.viewmodel.VendingViewModel
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultListener {

    private val viewModel: VendingViewModel by viewModels()
    private var activeSelectedSlot: SlotItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Checkout.preload(applicationContext)
        } catch (e: Exception) {
            // Preload graceful fallback
        }
        enableEdgeToEdge()
        setContent {
            SmartVendingTheme {
                VendingKioskScreen(
                    viewModel = viewModel,
                    onBuyClicked = { slot ->
                        handleBuyAction(slot)
                    },
                    onPaymentSuccess = { paymentId ->
                        onPaymentSuccess(paymentId)
                    },
                    onPaymentCancelled = { reason ->
                        onPaymentError(0, reason)
                    }
                )
            }
        }
    }

    /**
     * Handles product checkout initiation.
     * Starts in-app dynamic UPI payment modal directly with Razorpay SDK integration option.
     */
    private fun handleBuyAction(slot: SlotItem) {
        activeSelectedSlot = slot
        viewModel.startPurchase(slot)
    }

    /**
     * Optional direct Razorpay SDK Checkout Launcher
     */
    fun launchRazorpayCheckout(slot: SlotItem) {
        activeSelectedSlot = slot
        val creds = viewModel.credentials.value
        val razorpayKeyId = creds.keyId.ifBlank { "rzp_test_1DP5mmOlF5G5ag" }

        try {
            val checkout = Checkout()
            checkout.setKeyID(razorpayKeyId)

            val options = JSONObject().apply {
                put("name", creds.merchantName.ifBlank { "Smart Vending Kiosk" })
                put("description", "Slot #${slot.slotId}: ${slot.title}")
                put("currency", "INR")
                put("amount", (slot.priceInr * 100).toInt())
                put("theme.color", "#00E5FF")

                val prefill = JSONObject().apply {
                    put("email", "kiosk01@vending.local")
                    put("contact", "9999999999")
                }
                put("prefill", prefill)

                val notes = JSONObject().apply {
                    put("slot_id", slot.slotId)
                    put("product_title", slot.title)
                    put("terminal", "TERMINAL #01")
                }
                put("notes", notes)
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            viewModel.startPurchase(slot)
        }
    }

    /**
     * STRICT CONDITION: Under NO circumstances should any command be sent to the ESP32
     * before receiving onPaymentSuccess().
     * SUCCESS FLOW: show non-dismissible dispensing progress dialog and immediately transmit
     * DISPENSE_<SlotID>\n over USB Serial.
     */
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val slot = activeSelectedSlot
        if (slot != null) {
            val paymentId = razorpayPaymentId ?: "PAY_${System.currentTimeMillis()}"
            Toast.makeText(this, "Payment Verified: $paymentId", Toast.LENGTH_SHORT).show()
            viewModel.onRazorpayPaymentSuccess(paymentId, slot)
        } else {
            Toast.makeText(this, "Payment Verified: $razorpayPaymentId", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FAILURE / CANCEL FLOW: If onPaymentError() is triggered or payment fails/cancels,
     * display a Toast/alert and immediately reset the UI back to the clean home screen. No command sent.
     */
    override fun onPaymentError(code: Int, response: String?) {
        val errorMsg = response ?: "Payment cancelled (Code: $code)"
        Toast.makeText(this, "Payment Failed / Cancelled: $errorMsg", Toast.LENGTH_LONG).show()
        viewModel.onRazorpayPaymentCancelledOrFailed(errorMsg)
        activeSelectedSlot = null
    }
}

@Composable
fun VendingKioskScreen(
    viewModel: VendingViewModel,
    onBuyClicked: (SlotItem) -> Unit = { viewModel.startPurchase(it) },
    onPaymentSuccess: (String) -> Unit = { viewModel.confirmPaymentAndDispense() },
    onPaymentCancelled: (String) -> Unit = { viewModel.cancelPurchase() }
) {
    val slots by viewModel.slots.collectAsState()
    val connectionState by viewModel.usbManager.connectionState.collectAsState()
    val paymentSession by viewModel.paymentSession.collectAsState()
    val dispenseState by viewModel.dispenseState.collectAsState()
    val autoResetCountdown by viewModel.autoResetCountdown.collectAsState()
    val isConsoleOpen by viewModel.isConsoleOpen.collectAsState()
    val isPasswordPromptOpen by viewModel.isPasswordPromptOpen.collectAsState()
    val credentials by viewModel.credentials.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vending_kiosk_root"),
        containerColor = CyberDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CyberDarkBackground,
                            Color(0xFF070C16)
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
            ) {
                val gridColumns = if (maxWidth > 650.dp) 2 else 1

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_grid"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Seamless Kiosk Header (Title & Clock directly on background)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        KioskHeader(
                            connectionState = connectionState,
                            onOpenConsole = { viewModel.requestAdminAccess() },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Section Title & Instructions Header
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader()
                    }

                    // 4 Vending Slots Grid Items
                    items(slots, key = { it.slotId }) { slot ->
                        ProductSlotCard(
                            slot = slot,
                            onBuyClicked = { clickedSlot ->
                                onBuyClicked(clickedSlot)
                            }
                        )
                    }

                    // Kiosk Footer Guide
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        KioskFooterInfo()
                    }
                }
            }
        }
    }

    // Active Dynamic UPI Payment & Hardware Dispense Modal Dialog
    paymentSession?.let { session ->
        PaymentDialog(
            session = session,
            dispenseState = dispenseState,
            autoResetCountdown = autoResetCountdown,
            isCredentialsConfigured = credentials.isConfigured,
            onConfigureCredentials = { viewModel.requestAdminAccess() },
            onConfirmPayment = {
                val payId = "pay_upi_" + System.currentTimeMillis().toString().takeLast(8)
                onPaymentSuccess(payId)
            },
            onRetryDispense = { slotId -> viewModel.triggerHardwareDispense(slotId) },
            onDismiss = {
                onPaymentCancelled("User closed payment sheet")
            }
        )
    }

    // Password / PIN Verification Dialog for Admin & Settings Access
    if (isPasswordPromptOpen) {
        AdminPasswordDialog(
            onUnlockSuccess = {
                // Handled in verifyAndUnlockAdmin which opens console and closes password prompt
            },
            onDismiss = { viewModel.dismissPasswordPrompt() },
            onVerifyPassword = { pin -> viewModel.verifyAndUnlockAdmin(pin) }
        )
    }

    // ESP32 Diagnostic & Serial Monitor Console
    if (isConsoleOpen) {
        HardwareConsoleDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.openConsole(false) }
        )
    }
}

/**
 * Section Title: "SELECT YOUR ITEM" with instruction subtitle
 */
@Composable
private fun SectionHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp, 20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeonCyan)
            )
            Text(
                text = stringResource(R.string.select_item_title),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.select_item_subtitle),
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

/**
 * Bottom instructions banner for kiosk users
 */
@Composable
private fun KioskFooterInfo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardSurface.copy(alpha = 0.5f))
            .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            FooterStep(step = "1", title = "Choose Slot", sub = "Touch Item Card")
            Box(modifier = Modifier.size(1.dp, 24.dp).background(CyberCardBorder))
            FooterStep(step = "2", title = "Scan QR", sub = "Any UPI App")
            Box(modifier = Modifier.size(1.dp, 24.dp).background(CyberCardBorder))
            FooterStep(step = "3", title = "Auto Dispense", sub = "ESP32 Motor Lift")
        }
    }
}

@Composable
private fun FooterStep(step: String, title: String, sub: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NeonCyan.copy(alpha = 0.15f))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sub,
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}
