package com.ghost.krop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ghost.krop.core.ShortcutRegistry
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.viewModel.annotator.CanvasEvent

@Composable
fun AnnotatorToolbar(
    modifier: Modifier = Modifier,
    currentMode: CanvasMode,
    selectedColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    onEvent: (CanvasEvent) -> Unit
) {
    var shapeMenuExpanded by remember { mutableStateOf(false) }
    var pathMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /* ---------------- Move Section ---------------- */
            ModeButton(
                icon = Icons.Default.PanTool,
                description = "Pan ${ShortcutRegistry.getLabelForMode(CanvasMode.Pan) ?: ""}",
                isSelected = currentMode is CanvasMode.Pan,
                onClick = { onEvent(CanvasEvent.ChangeMode(CanvasMode.Pan)) }
            )

            VerticalDividerLine()

            /* ---------------- Shape Tools ---------------- */
            Box {
                ModeButton(
                    icon = getShapeIcon(currentMode),
                    description = "Shape Tools",
                    isSelected = currentMode is CanvasMode.Draw.Shape,
                    onClick = { shapeMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = shapeMenuExpanded,
                    onDismissRequest = { shapeMenuExpanded = false }
                ) {
                    ToolMenuItem(
                        text = "Rectangle",
                        mode = CanvasMode.Draw.Shape.Rectangle,
                        icon = Icons.Default.CropSquare
                    ) {
                        onEvent(CanvasEvent.ChangeMode(CanvasMode.Draw.Shape.Rectangle))
                        shapeMenuExpanded = false
                    }

                    ToolMenuItem(
                        text = "Circle",
                        mode = CanvasMode.Draw.Shape.Circle,
                        icon = Icons.Default.RadioButtonUnchecked
                    ) {
                        onEvent(CanvasEvent.ChangeMode(CanvasMode.Draw.Shape.Circle))
                        shapeMenuExpanded = false
                    }

                    ToolMenuItem(
                        text = "Oval",
                        mode = CanvasMode.Draw.Shape.Oval,
                        icon = Icons.Default.LensBlur
                    ) {
                        onEvent(CanvasEvent.ChangeMode(CanvasMode.Draw.Shape.Oval))
                        shapeMenuExpanded = false
                    }
                }
            }

            /* ---------------- Path Tools ---------------- */
            Box {
                ModeButton(
                    icon = getPathIcon(currentMode),
                    description = "Path Tools",
                    isSelected = currentMode is CanvasMode.Draw.Path,
                    onClick = { pathMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = pathMenuExpanded,
                    onDismissRequest = { pathMenuExpanded = false }
                ) {
                    ToolMenuItem(
                        text = "Polygon",
                        mode = CanvasMode.Draw.Path.Polygon,
                        icon = Icons.Default.Polyline
                    ) {
                        onEvent(CanvasEvent.ChangeMode(CanvasMode.Draw.Path.Polygon))
                        pathMenuExpanded = false
                    }

                    ToolMenuItem(
                        text = "Line",
                        mode = CanvasMode.Draw.Path.Line,
                        icon = Icons.Default.HorizontalRule
                    ) {
                        onEvent(CanvasEvent.ChangeMode(CanvasMode.Draw.Path.Line))
                        pathMenuExpanded = false
                    }
                }
            }

            VerticalDividerLine()

            /* ---------------- Color Picker ---------------- */
            ColorPickerButton(
                color = selectedColor,
                onColorSelected = { onEvent(CanvasEvent.ChangeColor(it)) }
            )

            VerticalDividerLine()

            /* ---------------- Zoom Section ---------------- */
            IconButton(onClick = { onEvent(CanvasEvent.ZoomIn) }) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In (+)")
            }

            IconButton(onClick = { onEvent(CanvasEvent.ZoomOut) }) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out (-)")
            }

            IconButton(onClick = { onEvent(CanvasEvent.ResetZoom) }) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset Zoom (0)")
            }

            VerticalDividerLine()

            /* ---------------- History Section ---------------- */
            IconButton(
                onClick = { onEvent(CanvasEvent.Undo) },
                enabled = canUndo
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo (Ctrl+Z)")
            }

            IconButton(
                onClick = { onEvent(CanvasEvent.Redo) },
                enabled = canRedo
            ) {
                Icon(Icons.Default.Redo, contentDescription = "Redo (Ctrl+Y)")
            }

            IconButton(onClick = { onEvent(CanvasEvent.ClearCanvas) }) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear All",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/* -----------------------------------------------------------
   Helper Composables & Functions
   ----------------------------------------------------------- */

@Composable
private fun ToolMenuItem(
    text: String,
    mode: CanvasMode,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val shortcutLabel = ShortcutRegistry.getLabelForMode(mode) ?: ""
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, modifier = Modifier.weight(1f))
                if (shortcutLabel.isNotEmpty()) {
                    Text(
                        text = shortcutLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

@Composable
private fun VerticalDividerLine() {
    VerticalDivider(
        modifier = Modifier
            .size(width = 1.dp, height = 32.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    )
}

private fun getShapeIcon(mode: CanvasMode): ImageVector {
    return when (mode) {
        is CanvasMode.Draw.Shape.Circle -> Icons.Default.RadioButtonUnchecked
        is CanvasMode.Draw.Shape.Oval -> Icons.Default.LensBlur
        else -> Icons.Default.CropSquare // Default to Rectangle icon
    }
}

private fun getPathIcon(mode: CanvasMode): ImageVector {
    return when (mode) {
        is CanvasMode.Draw.Path.Line -> Icons.Default.HorizontalRule
        else -> Icons.Default.Polyline // Default to Polygon icon
    }
}


@Composable
private fun ModeButton(
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(48.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (isSelected) 0.dp else 4.dp
        )
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}