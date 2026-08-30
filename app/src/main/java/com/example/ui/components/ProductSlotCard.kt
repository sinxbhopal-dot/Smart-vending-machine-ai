package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.SlotItem
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductSlotCard(
    slot: SlotItem,
    onBuyClicked: (SlotItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "card_scale")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag("slot_card_${slot.slotId}")
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (slot.isAvailable) 1.2.dp else 1.dp,
                brush = if (slot.isAvailable) {
                    Brush.verticalGradient(
                        listOf(slot.accentColor.copy(alpha = 0.6f), CyberCardBorder)
                    )
                } else {
                    Brush.verticalGradient(listOf(CyberCardBorder, CyberCardBorder.copy(alpha = 0.3f)))
                },
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Bar: Slot Badge + Quantity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slot Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(slot.accentColor.copy(alpha = 0.15f))
                        .border(1.dp, slot.accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SLOT #${slot.slotId}",
                        color = slot.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Quantity Badge
                val (qtyBg, qtyBorder, qtyColor, qtyText) = if (slot.isAvailable) {
                    Quadruple(
                        NeonGreen.copy(alpha = 0.12f),
                        NeonGreen.copy(alpha = 0.4f),
                        NeonGreen,
                        "QTY: ${slot.quantity}"
                    )
                } else {
                    Quadruple(
                        NeonCoral.copy(alpha = 0.18f),
                        NeonCoral.copy(alpha = 0.6f),
                        NeonCoral,
                        "OUT OF STOCK"
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(qtyBg)
                        .border(1.dp, qtyBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag(if (slot.isAvailable) "slot_qty_badge_${slot.slotId}" else "slot_out_of_stock_badge_${slot.slotId}")
                ) {
                    Text(
                        text = qtyText,
                        color = qtyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Visual: Product Icon with Neon Halo & specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Graphic Artwork Container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(slot.accentColor.copy(alpha = 0.25f), CyberCardElevated)
                            )
                        )
                        .border(1.dp, slot.accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!slot.imageUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = slot.imageUrl,
                            contentDescription = slot.title,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            loading = {
                                val iconVector = getProductIcon(slot.iconName)
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = slot.title,
                                    tint = slot.accentColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            error = {
                                val iconVector = getProductIcon(slot.iconName)
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = slot.title,
                                    tint = slot.accentColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        )
                    } else {
                        val iconVector = getProductIcon(slot.iconName)
                        Icon(
                            imageVector = iconVector,
                            contentDescription = slot.title,
                            tint = slot.accentColor,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Title, Category & Specs
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.category,
                        color = slot.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = slot.title,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slot.subDescription,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specs chips (Calories & Size)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecChip(label = slot.volumeOrWeight)
                SpecChip(label = slot.calories)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Bar: Price & "TAP TO BUY" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRICE",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = slot.formattedPrice,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action CTA Button
                Button(
                    onClick = { onBuyClicked(slot) },
                    enabled = slot.isAvailable,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = slot.accentColor,
                        contentColor = Color(0xFF0B111E),
                        disabledContainerColor = CyberCardElevated,
                        disabledContentColor = TextTertiary
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("buy_button_${slot.slotId}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (slot.isAvailable) "TAP TO BUY" else "OUT OF STOCK",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        if (slot.isAvailable) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberCardElevated)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getProductIcon(name: String): ImageVector {
    return when (name) {
        "bolt" -> Icons.Default.Bolt
        "fitness" -> Icons.Default.FitnessCenter
        "water_drop" -> Icons.Default.WaterDrop
        "cookie" -> Icons.Default.Cookie
        else -> Icons.Default.LocalDrink
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
