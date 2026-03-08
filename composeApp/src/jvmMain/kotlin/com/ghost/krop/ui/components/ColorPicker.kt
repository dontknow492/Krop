package com.ghost.krop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun ColorPickerButton(
    color: Color,
    modifier: Modifier = Modifier,
    onColorSelected: (Color) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    // Sleek, circular button with a subtle border so white/black colors are always visible
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .clickable { showDialog = true }
    )

    if (showDialog) {
        ColorPickerDialog(
            initialColor = color,
            onDismiss = { showDialog = false },
            onColorSelected = {
                onColorSelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableStateOf(initialColor) }

    // Crucial: Initialize the picker to the starting color
    LaunchedEffect(initialColor) {
        controller.selectByColor(
            initialColor, true
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp), // Modern M3 corner rounding
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Pick Color",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Cleaner than multiple Spacers
            ) {
                // Main Hue/Saturation Picker
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    controller = controller,
                    onColorChanged = { envelope ->
                        selectedColor = envelope.color
                    }
                )

                // Alpha (Transparency) Slider
                Slider(
                    title = "Opacity",
                    slider = {
                        AlphaSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(35.dp),
                            controller = controller,
                            initialColor = selectedColor,
                            borderRadius = 8.dp,
                            wheelRadius = 14.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Brightness Slider
                Slider(
                    title = "Brightness",
                    slider = {
                        BrightnessSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(35.dp),
                            controller = controller,
                            initialColor = selectedColor,
                            borderRadius = 8.dp,
                            wheelRadius = 14.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Color Preview Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorSelected(selectedColor) }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.error // Use error color for destructive/cancel actions
                )
            }
        }
    )
}

@Composable
private fun Slider(
    modifier: Modifier = Modifier,
    title: String,
    slider: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        slider()
    }
}