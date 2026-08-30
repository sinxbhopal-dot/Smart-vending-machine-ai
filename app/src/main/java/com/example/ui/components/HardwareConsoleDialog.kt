package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Esp32Response
import com.example.model.LogDirection
import com.example.model.SerialLogEntry
import com.example.model.UsbConnectionState
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkBackground
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.VendingViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HardwareConsoleDialog(
    viewModel: VendingViewModel,
    onDismiss: () -> Unit
) {
    val connectionState by viewModel.usbManager.connectionState.collectAsState()
    val serialLogs by viewModel.usbManager.serialLogs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var customCommandText by remember { mutableStateOf("") }
    var selectedSimulatedFault by remember { mutableStateOf<Esp32Response?>(null) }

    val logListState = rememberLazyListState()
    LaunchedEffect(serialLogs.size) {
        if (serialLogs.isNotEmpty() && selectedTab == 0) {
            logListState.animateScrollToItem(serialLogs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(680.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("hardware_console_dialog"),
            color = CyberDarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Bar
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
                            imageVector = Icons.Default.DeveloperBoard,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "ADMIN & HARDWARE CONSOLE",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(CyberCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberCardSurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("INVENTORY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("ESP32 HARDWARE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("RAZORPAY KEYS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // Product & Inventory Editing View
                        ProductEditSection(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    2 -> {
                        // Razorpay API Credentials Settings
                        RazorpayConfigSection(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        // Hardware & Serial Console View
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                        // Connection Switch & Status Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCardSurface)
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "COMMUNICATION MODE",
                                        color = TextTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (connectionState) {
                                            is UsbConnectionState.Connected -> "PHYSICAL USB-OTG (115200 Baud)"
                                            is UsbConnectionState.Simulated -> "VIRTUAL HARDWARE SIMULATOR"
                                            is UsbConnectionState.Connecting -> "CONNECTING USB..."
                                            else -> "DISCONNECTED"
                                        },
                                        color = if (connectionState is UsbConnectionState.Connected) NeonGreen else NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (connectionState is UsbConnectionState.Simulated) "Simulated" else "Physical",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Switch(
                                        checked = connectionState is UsbConnectionState.Simulated,
                                        onCheckedChange = { isSim ->
                                            viewModel.usbManager.enableSimulationMode(isSim)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonCyan,
                                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Diagnostic Quick Commands
                        Text(
                            text = "MANUAL DISPENSE COMMANDS (TX: 115200 Baud)",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..4).forEach { slot ->
                                OutlinedButton(
                                    onClick = { viewModel.usbManager.sendDispenseCommand(slot) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f))),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(text = "DISPENSE_$slot", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.usbManager.sendRaw("PING\n") },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.5f))),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(text = "PING", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            OutlinedButton(
                                onClick = { viewModel.usbManager.sendRaw("STATUS\n") },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonOrange.copy(alpha = 0.5f))),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(text = "STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sensor Fault Injector for Simulator Testing
                        Text(
                            text = "SIMULATE HARDWARE FAULTS (TESTING):",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSimulatedFault == null,
                                onClick = {
                                    selectedSimulatedFault = null
                                    viewModel.usbManager.setSimulatedFault(null)
                                },
                                label = { Text("Normal (DISPENSE_OK)", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonGreen
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedFault == Esp32Response.DISPENSE_ERROR_LIFT_UP,
                                onClick = {
                                    selectedSimulatedFault = Esp32Response.DISPENSE_ERROR_LIFT_UP
                                    viewModel.usbManager.setSimulatedFault(Esp32Response.DISPENSE_ERROR_LIFT_UP)
                                },
                                label = { Text("Fault: LIFT_UP", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCoral.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCoral
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedFault == Esp32Response.DISPENSE_ERROR_PRODUCT_NOT_ON_BELT,
                                onClick = {
                                    selectedSimulatedFault = Esp32Response.DISPENSE_ERROR_PRODUCT_NOT_ON_BELT
                                    viewModel.usbManager.setSimulatedFault(Esp32Response.DISPENSE_ERROR_PRODUCT_NOT_ON_BELT)
                                },
                                label = { Text("Fault: NOT_ON_BELT", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCoral.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCoral
                                )
                            )
                            FilterChip(
                                selected = selectedSimulatedFault == Esp32Response.DISPENSE_ERROR_LIFT_DOWN,
                                onClick = {
                                    selectedSimulatedFault = Esp32Response.DISPENSE_ERROR_LIFT_DOWN
                                    viewModel.usbManager.setSimulatedFault(Esp32Response.DISPENSE_ERROR_LIFT_DOWN)
                                },
                                label = { Text("Fault: LIFT_DOWN", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCoral.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCoral
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Raw Serial Monitor Console
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SERIAL TERMINAL LOGS (${serialLogs.size})",
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.restockAll(20) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Inventory, contentDescription = "Restock All", tint = NeonPurple, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.usbManager.clearLogs() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Terminal Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B14))
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (serialLogs.isEmpty()) {
                                Text(
                                    text = "No serial activity logged yet.\nReady to send commands to ESP32 @ 115200 baud.",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(
                                    state = logListState,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(serialLogs) { entry ->
                                        SerialLogItem(entry = entry)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Command Input Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customCommandText,
                                onValueChange = { customCommandText = it },
                                placeholder = { Text("Type custom command...", fontSize = 11.sp, color = TextTertiary) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CyberCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            )

                            Button(
                                onClick = {
                                    if (customCommandText.isNotBlank()) {
                                        viewModel.usbManager.sendRaw(customCommandText.trim())
                                        customCommandText = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF0B111E)),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SerialLogItem(entry: SerialLogEntry) {
    val (dirColor, dirLabel) = when (entry.direction) {
        LogDirection.TX -> Pair(NeonCyan, "TX ->")
        LogDirection.RX -> Pair(NeonGreen, "<- RX")
        LogDirection.SYS -> Pair(NeonOrange, "SYS")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.timestamp,
            color = TextTertiary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = dirLabel,
            color = dirColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = entry.message,
            color = if (entry.message.contains("ERROR")) NeonCoral else TextPrimary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
