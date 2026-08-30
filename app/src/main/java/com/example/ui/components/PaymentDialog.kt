package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DispenseState
import com.example.model.Esp32Response
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.PaymentSession

@Composable
fun PaymentDialog(
    session: PaymentSession,
    dispenseState: DispenseState,
    autoResetCountdown: Int = 20,
    isCredentialsConfigured: Boolean = true,
    onConfigureCredentials: () -> Unit = {},
    onConfirmPayment: () -> Unit,
    onRetryDispense: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            // Prevent accidental dismissal while dispensing is active
            if (dispenseState is DispenseState.Idle || dispenseState is DispenseState.Success || dispenseState is DispenseState.Failure) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, session.slotItem.accentColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("payment_modal_dialog"),
            color = CyberDarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Slot Badge & Dismiss Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(session.slotItem.accentColor.copy(alpha = 0.15f))
                            .border(1.dp, session.slotItem.accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SLOT #${session.slotItem.slotId} • ${session.slotItem.title.uppercase()}",
                            color = session.slotItem.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (dispenseState !is DispenseState.DispensingInProgress && dispenseState !is DispenseState.SendingCommand) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberCardElevated)
                                .testTag("close_payment_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Missing Credentials Alert Prompt
                if (!isCredentialsConfigured) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonOrange.copy(alpha = 0.12f))
                            .border(1.dp, NeonOrange.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Please configure Razorpay API Keys in Admin Panel",
                                    color = NeonOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "To accept live customer payments and dynamic UPI checkout, input your Key ID & Secret.",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onConfigureCredentials()
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(NeonOrange)
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Configure", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Interactive Content Flow: UPI QR -> Dispensing Telemetry -> Result Feedback -> Failure
                AnimatedContent(
                    targetState = dispenseState,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "dispense_flow_transition"
                ) { state ->
                    when (state) {
                        is DispenseState.Idle -> {
                            if (session.failureReason != null) {
                                PaymentFailedView(
                                    reason = session.failureReason,
                                    countdownSeconds = autoResetCountdown,
                                    onDismiss = onDismiss
                                )
                            } else {
                                UpiQrView(
                                    session = session
                                )
                            }
                        }
                        is DispenseState.SendingCommand,
                        is DispenseState.DispensingInProgress -> {
                            HardwareDispensingView(
                                slotId = session.slotItem.slotId,
                                itemTitle = session.slotItem.title,
                                dispenseState = state
                            )
                        }
                        is DispenseState.Success -> {
                            DispenseSuccessView(
                                slotId = session.slotItem.slotId,
                                itemTitle = session.slotItem.title,
                                message = state.message,
                                countdownSeconds = autoResetCountdown,
                                onDone = onDismiss
                            )
                        }
                        is DispenseState.Failure -> {
                            DispenseFailureView(
                                slotId = session.slotItem.slotId,
                                itemTitle = session.slotItem.title,
                                error = state.error,
                                rawMessage = state.rawMessage,
                                onRetry = { onRetryDispense(session.slotItem.slotId) },
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dynamic UPI QR Payment Screen (Fully Automated - No Manual Buttons)
 */
@Composable
private fun UpiQrView(
    session: PaymentSession
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rotate")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SCAN & PAY WITH UPI",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Use GPay, PhonePe, Paytm, CRED, or any BHIM UPI App",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // High-Contrast Standalone Square QR Code Card with Glowing Border
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.5.dp, NeonCyan.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (session.qrBitmap != null) {
                Image(
                    bitmap = session.qrBitmap,
                    contentDescription = "Standalone UPI Payment QR Code",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Price & Countdown Timer Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberCardSurface)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL AMOUNT",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = session.slotItem.formattedPrice,
                    color = NeonGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Countdown timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (session.remainingSeconds < 30) NeonCoral else NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                val mins = session.remainingSeconds / 60
                val secs = session.remainingSeconds % 60
                Text(
                    text = "%02d:%02d".format(mins, secs),
                    color = if (session.remainingSeconds < 30) NeonCoral else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auto-Detection / Polling Live Status Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NeonCyan.copy(alpha = 0.08f))
                .border(1.dp, NeonCyan.copy(alpha = pulseAlpha), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
                Column {
                    Text(
                        text = "WAITING FOR PAYMENT...",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (session.qrId != null) "Auto-verifying Razorpay QR (${session.qrId})" else "Auto-verifying transaction every 2 seconds",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction ID tag
        Text(
            text = "TXN: ${session.transactionId} • SLOT #${session.slotItem.slotId}",
            color = TextTertiary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Payment Failed / Expired Screen (Auto-returns after 5 seconds)
 */
@Composable
private fun PaymentFailedView(
    reason: String,
    countdownSeconds: Int = 5,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(NeonCoral.copy(alpha = 0.15f))
                .border(2.dp, NeonCoral, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Payment Failed",
                tint = NeonCoral,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PAYMENT FAILED / EXPIRED",
            color = NeonCoral,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = reason.ifBlank { "The payment session expired or was not completed. No dispense signal sent." },
            color = TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberCardSurface)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Returning to Home in ${countdownSeconds}s...",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Real-Time Hardware Dispense Animation & Sensor Monitoring View
 */
@Composable
private fun HardwareDispensingView(
    slotId: Int,
    itemTitle: String,
    dispenseState: DispenseState
) {
    val progress = when (dispenseState) {
        is DispenseState.DispensingInProgress -> dispenseState.progress
        is DispenseState.SendingCommand -> 0.1f
        else -> 0.5f
    }
    val stepText = when (dispenseState) {
        is DispenseState.DispensingInProgress -> dispenseState.stepDescription
        is DispenseState.SendingCommand -> "Sending DISPENSE_$slotId command to ESP32 over USB-OTG..."
        else -> "Hardware cycle in progress..."
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing spinning telemetry indicator
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(CyberCardSurface)
                .border(2.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(76.dp),
                color = NeonCyan,
                trackColor = CyberCardElevated,
                strokeWidth = 4.dp
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier
                    .size(32.dp)
                    .rotate(angle)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "DISPENSING IN PROGRESS",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Slot #$slotId • $itemTitle",
            color = NeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = NeonCyan,
            trackColor = CyberCardElevated
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sensor telemetry status box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberCardSurface)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ESP32 SENSOR FEEDBACK",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stepText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Success View (ESP32 returned "DISPATCH_OK" / "DISPENSE_OK")
 */
@Composable
private fun DispenseSuccessView(
    slotId: Int,
    itemTitle: String,
    message: String,
    countdownSeconds: Int = 20,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.15f))
                .border(2.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = NeonGreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DISPATCH SUCCESSFUL!",
            color = NeonGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Please collect your item from the pickup tray below.",
            color = TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Product summary box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberCardSurface)
                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "DISPENSED ITEM", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = itemTitle, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "SLOT #$slotId", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 20-Second Auto-Reset Timer Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberDarkBackground)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Returning to Home in:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "${countdownSeconds}s",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF0B111E)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("dispense_done_button")
        ) {
            Text(text = "COLLECT & FINISH", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

/**
 * Sensor Failure / Hardware Error View with precise ESP32 diagnostic breakdown
 */
@Composable
private fun DispenseFailureView(
    slotId: Int,
    itemTitle: String,
    error: Esp32Response,
    rawMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NeonCoral.copy(alpha = 0.15f))
                .border(2.dp, NeonCoral, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Hardware Error",
                tint = NeonCoral,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "HARDWARE SENSOR ALERT",
            color = NeonCoral,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = error.userMessage,
            color = TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic details card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberCardSurface)
                .border(1.dp, NeonCoral.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ESP32 RESPONSE:", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = error.rawCommand,
                        color = NeonCoral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Raw: $rawMessage",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCardBorder))),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("dismiss_error_button")
            ) {
                Text(text = "CLOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0B111E)),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("retry_dispense_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(text = "RETRY", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
