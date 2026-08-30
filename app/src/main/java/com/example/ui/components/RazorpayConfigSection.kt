package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.SecureCredentialsManager
import com.example.viewmodel.VendingViewModel

/**
 * Razorpay API Credentials Configuration Section for Admin Panel.
 * Dynamically updates and securely encrypts Key ID and Key Secret.
 */
@Composable
fun RazorpayConfigSection(
    viewModel: VendingViewModel,
    modifier: Modifier = Modifier
) {
    val currentCredentials by viewModel.credentials.collectAsState()

    var keyIdInput by remember(currentCredentials.keyId) {
        mutableStateOf(currentCredentials.keyId)
    }
    var keySecretInput by remember(currentCredentials.keySecret) {
        mutableStateOf(currentCredentials.keySecret)
    }
    var upiIdInput by remember(currentCredentials.upiId) {
        mutableStateOf(currentCredentials.upiId)
    }
    var merchantNameInput by remember(currentCredentials.merchantName) {
        mutableStateOf(currentCredentials.merchantName)
    }
    var esp32IpInput by remember(currentCredentials.esp32Ip) {
        mutableStateOf(currentCredentials.esp32Ip)
    }

    var showSecret by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCardSurface)
            .border(
                1.dp,
                if (currentCredentials.isConfigured) NeonCyan.copy(alpha = 0.4f) else NeonOrange.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
            .testTag("razorpay_admin_config_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section Header & Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (currentCredentials.isConfigured) NeonCyan else NeonOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "RAZORPAY API CREDENTIALS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Security Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (currentCredentials.isConfigured) NeonGreen.copy(alpha = 0.15f) else NeonCoral.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (currentCredentials.isConfigured) NeonGreen.copy(alpha = 0.5f) else NeonCoral.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (currentCredentials.isConfigured) "KEYSTORE ENCRYPTED" else "KEYS REQUIRED",
                        color = if (currentCredentials.isConfigured) NeonGreen else NeonCoral,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Warning Notice if credentials are not configured
            if (!currentCredentials.isConfigured) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonOrange.copy(alpha = 0.1f))
                        .border(1.dp, NeonOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Please configure Razorpay API Keys in Admin Panel to enable live dynamic checkout & QR code generation.",
                            color = NeonOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Razorpay Key ID input
            OutlinedTextField(
                value = keyIdInput,
                onValueChange = {
                    keyIdInput = it
                    saveSuccessMessage = false
                },
                label = { Text("Razorpay Key ID (e.g. rzp_live_... / rzp_test_...)", fontSize = 11.sp) },
                placeholder = { Text("rzp_test_XXXXXXXXXXXXXX", fontSize = 11.sp, color = TextTertiary) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("razorpay_key_id_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            )

            // Razorpay Key Secret input
            OutlinedTextField(
                value = keySecretInput,
                onValueChange = {
                    keySecretInput = it
                    saveSuccessMessage = false
                },
                label = { Text("Razorpay Key Secret", fontSize = 11.sp) },
                placeholder = { Text("••••••••••••••••••••••••", fontSize = 11.sp, color = TextTertiary) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showSecret) "Hide Secret" else "Show Secret",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("razorpay_key_secret_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            )

            // Optional UPI Virtual Payment Address (VPA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = {
                        upiIdInput = it
                        saveSuccessMessage = false
                    },
                    label = { Text("UPI VPA (QR Handle)", fontSize = 11.sp) },
                    placeholder = { Text("vendingkiosk@upi", fontSize = 11.sp, color = TextTertiary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )

                OutlinedTextField(
                    value = merchantNameInput,
                    onValueChange = {
                        merchantNameInput = it
                        saveSuccessMessage = false
                    },
                    label = { Text("Merchant Display Name", fontSize = 11.sp) },
                    placeholder = { Text("Smart Vending Kiosk", fontSize = 11.sp, color = TextTertiary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )
            }

            // ESP32 Microcontroller IP Configuration
            OutlinedTextField(
                value = esp32IpInput,
                onValueChange = {
                    esp32IpInput = it
                    saveSuccessMessage = false
                },
                label = { Text("ESP32 Local IP Address", fontSize = 11.sp) },
                placeholder = { Text("192.168.4.1 or 192.168.1.50", fontSize = 11.sp, color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )

            // Save / Update Action Button & Success Toast Feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (saveSuccessMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Settings securely saved in KeyStore!",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        viewModel.saveCredentials(
                            keyId = keyIdInput.trim(),
                            keySecret = keySecretInput.trim(),
                            upi = upiIdInput.trim(),
                            merchant = merchantNameInput.trim(),
                            esp32Ip = esp32IpInput.trim()
                        )
                        saveSuccessMessage = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF0B111E)
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("save_razorpay_credentials_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVE & ENCRYPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
