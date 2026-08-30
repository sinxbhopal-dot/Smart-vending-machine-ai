package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.VendingViewModel

/**
 * Product & Inventory Management View for Admin Console.
 * Allows editing product title, category, description, price, available stock level,
 * and picking custom product images directly from device storage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditSection(
    viewModel: VendingViewModel,
    modifier: Modifier = Modifier
) {
    val slots by viewModel.slots.collectAsState()
    var selectedSlotIndex by remember { mutableIntStateOf(0) }

    val currentSlot = slots.getOrNull(selectedSlotIndex) ?: slots.firstOrNull()

    if (currentSlot == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No slots found", color = TextSecondary)
        }
        return
    }

    // Editable form state tied to currentSlot
    var titleInput by remember(currentSlot.slotId, currentSlot.title) { mutableStateOf(currentSlot.title) }
    var categoryInput by remember(currentSlot.slotId, currentSlot.category) { mutableStateOf(currentSlot.category) }
    var descInput by remember(currentSlot.slotId, currentSlot.subDescription) { mutableStateOf(currentSlot.subDescription) }
    var priceInput by remember(currentSlot.slotId, currentSlot.priceInr) { mutableStateOf(currentSlot.priceInr.toString()) }
    var quantityInput by remember(currentSlot.slotId, currentSlot.quantity) { mutableIntStateOf(currentSlot.quantity) }
    var imageUrlInput by remember(currentSlot.slotId, currentSlot.imageUrl) { mutableStateOf(currentSlot.imageUrl) }
    var caloriesInput by remember(currentSlot.slotId, currentSlot.calories) { mutableStateOf(currentSlot.calories) }
    var volumeInput by remember(currentSlot.slotId, currentSlot.volumeOrWeight) { mutableStateOf(currentSlot.volumeOrWeight) }

    var saveSuccessMessage by remember { mutableStateOf(false) }

    // Android Gallery / Photo Picker Contract
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = viewModel.saveProductImageFromUri(currentSlot.slotId, uri)
            if (savedPath != null) {
                imageUrlInput = savedPath
                saveSuccessMessage = false
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Slot Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECT SLOT TO EDIT",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            // Restock all button
            OutlinedButton(
                onClick = {
                    viewModel.restockAll(20)
                    quantityInput = 20
                    saveSuccessMessage = true
                },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NeonPurple.copy(alpha = 0.6f))
                ),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(imageVector = Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restock All (20)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 4 Slot selection chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slots.forEachIndexed { index, slot ->
                val isSelected = selectedSlotIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) slot.accentColor.copy(alpha = 0.2f) else CyberCardSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) slot.accentColor else CyberCardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedSlotIndex = index
                            saveSuccessMessage = false
                        }
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                        .testTag("slot_select_tab_${slot.slotId}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SLOT #${slot.slotId}",
                            color = if (isSelected) slot.accentColor else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (slot.quantity > 0) "Qty: ${slot.quantity}" else "OUT OF STOCK",
                            color = if (slot.quantity > 0) NeonGreen else NeonCoral,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Product Details Form Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberCardSurface)
                .border(1.dp, currentSlot.accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section Title with Slot Badge
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
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = currentSlot.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "EDITING SLOT #${currentSlot.slotId}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (quantityInput > 0) NeonGreen.copy(alpha = 0.15f) else NeonCoral.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                if (quantityInput > 0) NeonGreen.copy(alpha = 0.5f) else NeonCoral.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (quantityInput > 0) "STOCK: $quantityInput" else "OUT OF STOCK",
                            color = if (quantityInput > 0) NeonGreen else NeonCoral,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Image Picker Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image Preview Thumbnail
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCardSurface)
                                .border(1.dp, currentSlot.accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!imageUrlInput.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = imageUrlInput,
                                    contentDescription = "Selected product image",
                                    modifier = Modifier.size(64.dp),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = currentSlot.accentColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    },
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = NeonCoral,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = currentSlot.accentColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Image Picker CTA Buttons
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Product Artwork",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (!imageUrlInput.isNullOrBlank()) "Custom photo loaded from storage" else "Using default cyber vector icon",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.7f))
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("choose_image_button_${currentSlot.slotId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick from Gallery", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                if (!imageUrlInput.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            imageUrlInput = null
                                            saveSuccessMessage = false
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(NeonCoral.copy(alpha = 0.5f))
                                        ),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Reset", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Title / Product Name Input
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                        saveSuccessMessage = false
                    },
                    label = { Text("Product Name / Title", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_product_title_input"),
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
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )

                // Category & SubDescription
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = {
                            categoryInput = it
                            saveSuccessMessage = false
                        },
                        label = { Text("Category Tag", fontSize = 11.sp) },
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

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = {
                            priceInput = it
                            saveSuccessMessage = false
                        },
                        label = { Text("Price (₹ INR)", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_product_price_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Description
                OutlinedTextField(
                    value = descInput,
                    onValueChange = {
                        descInput = it
                        saveSuccessMessage = false
                    },
                    label = { Text("Product Description / Subtitle", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
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

                // Stock & Inventory Stepper Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "AVAILABLE INVENTORY / STOCK LEVEL",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Minus Button
                            IconButton(
                                onClick = {
                                    if (quantityInput > 0) {
                                        quantityInput -= 1
                                        saveSuccessMessage = false
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyberCardSurface)
                                    .border(1.dp, NeonCoral.copy(alpha = 0.5f), CircleShape)
                                    .testTag("decrement_stock_button")
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Stock", tint = NeonCoral, modifier = Modifier.size(18.dp))
                            }

                            // Numeric Stock Display & Manual Edit
                            OutlinedTextField(
                                value = quantityInput.toString(),
                                onValueChange = {
                                    val parsed = it.toIntOrNull()
                                    if (parsed != null && parsed >= 0) {
                                        quantityInput = parsed
                                        saveSuccessMessage = false
                                    } else if (it.isEmpty()) {
                                        quantityInput = 0
                                        saveSuccessMessage = false
                                    }
                                },
                                modifier = Modifier
                                    .width(100.dp)
                                    .testTag("edit_stock_count_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CyberCardBorder,
                                    focusedTextColor = if (quantityInput > 0) NeonGreen else NeonCoral,
                                    unfocusedTextColor = if (quantityInput > 0) NeonGreen else NeonCoral
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )

                            // Plus Button
                            IconButton(
                                onClick = {
                                    quantityInput += 1
                                    saveSuccessMessage = false
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyberCardSurface)
                                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape)
                                    .testTag("increment_stock_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Stock", tint = NeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Preset Shortcuts
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = quantityInput == 0,
                                onClick = {
                                    quantityInput = 0
                                    saveSuccessMessage = false
                                },
                                label = { Text("Set 0 (Out of Stock)", fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCoral.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCoral
                                )
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    quantityInput += 5
                                    saveSuccessMessage = false
                                },
                                label = { Text("+5", fontSize = 9.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    quantityInput += 10
                                    saveSuccessMessage = false
                                },
                                label = { Text("+10", fontSize = 9.sp) }
                            )
                            FilterChip(
                                selected = quantityInput == 20,
                                onClick = {
                                    quantityInput = 20
                                    saveSuccessMessage = false
                                },
                                label = { Text("Full Restock (20)", fontSize = 9.sp) }
                            )
                        }
                    }
                }

                // Calories & Volume Specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = volumeInput,
                        onValueChange = {
                            volumeInput = it
                            saveSuccessMessage = false
                        },
                        label = { Text("Volume / Weight", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )

                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = {
                            caloriesInput = it
                            saveSuccessMessage = false
                        },
                        label = { Text("Calories", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }

                // Save Action & Success Confirmation
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
                                text = "Slot #${currentSlot.slotId} changes saved!",
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
                            val parsedPrice = priceInput.toDoubleOrNull() ?: currentSlot.priceInr
                            val updatedSlot = currentSlot.copy(
                                title = titleInput.trim(),
                                category = categoryInput.trim(),
                                subDescription = descInput.trim(),
                                priceInr = parsedPrice,
                                quantity = quantityInput,
                                imageUrl = imageUrlInput,
                                calories = caloriesInput.trim(),
                                volumeOrWeight = volumeInput.trim()
                            )
                            viewModel.updateSlot(updatedSlot)
                            saveSuccessMessage = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentSlot.accentColor,
                            contentColor = Color(0xFF0B111E)
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("save_product_changes_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVE CHANGES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
