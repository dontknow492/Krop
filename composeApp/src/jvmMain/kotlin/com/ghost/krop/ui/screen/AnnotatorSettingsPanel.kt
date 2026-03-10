package com.ghost.krop.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.annotator.CanvasUiState


@Composable
fun AnnotationSettingsDialogButton(
    modifier: Modifier = Modifier,
    uiState: CanvasUiState,
    onEvent: (CanvasEvent) -> Unit,
    buttonContent: @Composable RowScope.() -> Unit = {
        Icon(Icons.Default.Settings, contentDescription = null)
//        Spacer(modifier = Modifier.width(8.dp))
//        Text("Annotation Settings")
    }
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showDialog = true },
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = buttonContent
        )
    }

    if (showDialog) {
        AnnotationSettingsDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { showDialog = false }
        )
    }
}


@Composable
fun AnnotationSettingsDialog(
    uiState: CanvasUiState,
    onEvent: (CanvasEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Annotation Settings",
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // Settings content
                AnnotationSettingsContent(
                    uiState = uiState,
                    onEvent = onEvent
                )

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}


@Composable
fun AnnotationSettingsContent(
    uiState: CanvasUiState,
    onEvent: (CanvasEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ----------------------------------------------------
        // BOUNDING BOX
        // ----------------------------------------------------
        SettingSection(title = "Bounding Box")

        ToggleSetting(
            label = "Show Bounding Boxes",
            checked = uiState.showAnnotationLabel, // Note: Consider adding separate state
            onCheckedChange = {
                onEvent(CanvasEvent.ToggleLabelVisibility)
            }
        )

        SliderSetting(
            label = "Opacity",
            value = uiState.annotationOpacity,
            valueRange = 0f..1f,
            onValueChange = {
                onEvent(CanvasEvent.ChangeOpacity(it))
            }
        )

        SliderSetting(
            label = "Thickness",
            value = uiState.strokeWidth,
            valueRange = 1f..10f,
            onValueChange = {
                onEvent(CanvasEvent.ChangeStrokeWidth(it))
            }
        )

        // ----------------------------------------------------
        // LABELS
        // ----------------------------------------------------
        SettingSection(title = "Labels")

        ToggleSetting(
            label = "Show Labels",
            checked = uiState.showAnnotationLabel,
            onCheckedChange = {
                onEvent(CanvasEvent.ToggleLabelVisibility)
            }
        )

        SliderSetting(
            label = "Font Size",
            value = uiState.labelFontSize.value,
            valueRange = 8f..40f,
            onValueChange = {
                onEvent(CanvasEvent.ChangeLabelFontSize(it.dp))
            }
        )
    }
}

@Composable
private fun SettingSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ToggleSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            label,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text("%.2f".format(value))
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

