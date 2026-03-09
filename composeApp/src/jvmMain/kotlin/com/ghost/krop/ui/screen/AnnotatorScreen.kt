package com.ghost.krop.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Crosshair
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Default
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghost.krop.core.CanvasTool
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.models.ShortcutRegistry
import com.ghost.krop.ui.components.ColorPickerButton
import com.ghost.krop.ui.components.ImageThumbnail
import com.ghost.krop.viewModel.CanvasEvent
import com.ghost.krop.viewModel.CanvasUiState

@Composable
fun AnnotatorScreen(
    modifier: Modifier = Modifier,
    uiState: CanvasUiState,
    annotations: List<Annotation>,
    canUndo: Boolean,
    canRedo: Boolean,
    activeTool: CanvasTool?,
    onEvent: (CanvasEvent) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (uiState.selectedImage == null) {
            EmptyWorkspaceState()
        } else {
            // Interactive Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerHoverIcon(getCursorForMode(uiState.mode))
                    // Re-bind pointerInput whenever the tool mode changes
                    .pointerInput(uiState.mode, activeTool) {

                        // Helper function to map screen coordinates to the transformed canvas space
                        fun mapToCanvas(screenPos: Offset): Offset {
                            return Offset(
                                x = (screenPos.x / uiState.scale) - uiState.offset.x,
                                y = (screenPos.y / uiState.scale) - uiState.offset.y
                            )
                        }

                        when (uiState.mode) {
                            /* ------------------ PAN / ZOOM ------------------ */
                            CanvasMode.Pan -> {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    onEvent(CanvasEvent.Zoom(zoom))
                                    // Note: Pan usually needs to be divided by scale to stay consistent
                                    onEvent(CanvasEvent.Pan(pan / uiState.scale))
                                }
                            }

                            /* ------------------ DRAWING TOOLS (Shapes & Paths) ------------------ */
                            is CanvasMode.Draw -> {
                                awaitEachGesture {
                                    // 1. Capture the initial touch down
                                    val downEvent = awaitFirstDown(requireUnconsumed = false)

                                    // Map the screen touch to the actual canvas coordinates
                                    val canvasPos = mapToCanvas(downEvent.position)
                                    activeTool?.onPointerDown(canvasPos)

                                    // 2. Track pointer lifecycle
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()

                                        if (change != null) {
                                            val currentCanvasPos = mapToCanvas(change.position)

                                            if (change.pressed) {
                                                // Moving or holding
                                                activeTool?.onPointerMove(currentCanvasPos)
                                            } else {
                                                // Released
                                                activeTool?.onPointerUp(currentCanvasPos)
                                            }

                                            // Consume the event so parent containers don't try to scroll or intercept
                                            change.consume()
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }

                            /* ------------------ SELECTION / EDITING ------------------ */
                            is CanvasMode.Edit -> {
                                // You could use detectTapGestures here to select different annotations
                            }

                            else -> {}
                        }
                    }
            ) {
                // Background Grid
                DotGridBackground()

                // Transformed Layer (Image + Annotations)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = uiState.scale,
                            scaleY = uiState.scale,
                            translationX = uiState.offset.x * uiState.scale,
                            translationY = uiState.offset.y * uiState.scale,
                            transformOrigin = TransformOrigin(0f, 0f)
                        )
                ) {
                    // Image Layer
                    ImageThumbnail(
                        path = uiState.selectedImage!!,
                        contentScale = ContentScale.Fit,
                        contentDescription = "Workspace Image",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Annotation Layer
                    AnnotationCanvas(
                        annotations = annotations,
                        uiState = uiState,
                        activeTool = activeTool,
                        modifier = Modifier.fillMaxSize()
                    )

                }
            }

            // UI Overlay: Floating Toolbar (Bottom Center)
            AnnotatorToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                currentMode = uiState.mode,
                selectedColor = uiState.color,
                canUndo = canUndo,
                canRedo = canRedo,
                onEvent = onEvent
            )

            // UI Overlay: Heads Up Display (Top Right)
            HUDOverlay(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                scale = uiState.scale,
                mode = uiState.mode
            )
        }
    }
}

@Composable
private fun AnnotationCanvas(
    annotations: List<Annotation>,
    uiState: CanvasUiState,
    activeTool: CanvasTool?,
    modifier: Modifier = Modifier,
) {

    Canvas(modifier = modifier.fillMaxSize()) {

        val stroke = 3f / uiState.scale

        /* ----------------------------- */
        /* Draw Finalized Annotations */
        /* ----------------------------- */

        annotations.forEach { annotation ->

            when (annotation) {

                is Annotation.BoundingBox -> {
                    drawRect(
                        color = annotation.color,
                        topLeft = Offset(annotation.xMin, annotation.yMin),
                        size = Size(
                            annotation.xMax - annotation.xMin,
                            annotation.yMax - annotation.yMin
                        ),
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Polygon -> {

                    if (annotation.points.size >= 2) {

                        val path = Path().apply {

                            val first = annotation.points.first()
                            moveTo(first.x, first.y)

                            annotation.points.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }

                            close()
                        }

                        drawPath(
                            path = path,
                            color = annotation.color,
                            style = Stroke(width = stroke)
                        )
                    }
                }

                is Annotation.Circle -> {
                    drawCircle(
                        color = annotation.color,
                        radius = annotation.radius,
                        center = annotation.center,
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Oval -> {
                    drawOval(
                        color = annotation.color,
                        topLeft = Offset(annotation.xMin, annotation.yMin),
                        size = Size(annotation.xMax - annotation.xMin, annotation.yMax - annotation.yMin),
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Line -> {
                    drawLine(
                        color = annotation.color,
                        start = annotation.start,
                        end = annotation.end,
                        strokeWidth = stroke
                    )
                }
            }
        }

        /* ----------------------------- */
        /* Active Drawing Feedback */
        /* ----------------------------- */

        activeTool?.drawPreview(this)
    }
}

@Composable
private fun AnnotatorToolbar(
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


/**
 * Clean wrapper for DropdownMenuItem
 */
@Composable
private fun ToolMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, null) },
        onClick = onClick
    )
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

@Composable
private fun HUDOverlay(
    modifier: Modifier = Modifier,
    scale: Float,
    mode: CanvasMode
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // --- Scale Indicator ---
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }

        // --- Mode Indicator ---
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (mode) {
                        is CanvasMode.Draw -> MaterialTheme.colorScheme.primary
                        is CanvasMode.Pan -> MaterialTheme.colorScheme.secondary
                        is CanvasMode.Edit -> MaterialTheme.colorScheme.tertiary
                        else -> Color.Gray
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = when (mode) {
                    is CanvasMode.Pan -> "PANNING"
                    is CanvasMode.Edit -> "EDITING"
                    is CanvasMode.Resize -> "RESIZING"
                    is CanvasMode.Draw -> {
                        // Get the specific name of the drawing tool
                        getModeName(mode).uppercase()
                    }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

/**
 * Helper to get a human-readable name for the current mode
 */
private fun getModeName(mode: CanvasMode): String {
    return when (mode) {
        is CanvasMode.Draw.Shape.Rectangle -> "Rectangle"
        is CanvasMode.Draw.Shape.Circle -> "Circle"
        is CanvasMode.Draw.Shape.Oval -> "Oval"
        is CanvasMode.Draw.Path.Polygon -> "Polygon"
        is CanvasMode.Draw.Path.Line -> "Line"
        CanvasMode.Pan -> "Pan"
        is CanvasMode.Edit -> "Edit"
        is CanvasMode.Resize -> "Resize"
    }
}

@Composable
private fun EmptyWorkspaceState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ImageNotSupported,
            contentDescription = "No image selected",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "Select an image from the gallery to begin labeling",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun DotGridBackground() {
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 20.dp.toPx()
        val dotRadius = 1.dp.toPx()

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}


fun getCursorForMode(mode: CanvasMode): PointerIcon {
    return when (mode) {
        // Drawing tools usually use a Crosshair for precision
        is CanvasMode.Draw -> Crosshair

        // Panning uses the Hand icon
        is CanvasMode.Pan -> Hand

        // Editing/Selection uses the standard Arrow
        is CanvasMode.Edit -> Default

        // Resizing uses specific directional arrows (system dependent)
        is CanvasMode.Resize -> Hand // Or a specific resize icon
    }
}