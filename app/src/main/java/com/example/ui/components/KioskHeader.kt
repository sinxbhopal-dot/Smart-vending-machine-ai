package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UsbConnectionState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KioskHeader(
    connectionState: UsbConnectionState = UsbConnectionState.Disconnected,
    onOpenConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Live Digital Clock
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFmt.format(now).uppercase()
            currentDateString = dateFmt.format(now).uppercase()
            delay(1000)
        }
    }

    // Outer Column: Sabse pehle Title, phir neeche Time & Settings
Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("kiosk_header"),
    verticalArrangement = Arrangement.spacedBy(6.dp)
) {
    // 1. TOP: Icon + "SMART VENDING KIOSK" (Puri width milegi, text nahi tutega)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DeveloperBoard,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.kiosk_title),
            color = TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            softWrap = false
        )
    }

    // 2. BOTTOM: Time (Left me) aur Setting Icon (Right me)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Live Digital Time & Date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = NeonCyan.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = currentTimeString.ifEmpty { "--:--:-- --" },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentDateString.ifEmpty { "FETCHING DATE..." },
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Hardware Console / Settings Icon Button
        IconButton(
            onClick = onOpenConsole,
            modifier = Modifier
                .size(36.dp)
                .testTag("admin_console_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Hardware Console",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
}
