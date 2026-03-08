package com.ghost.krop.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ghost.krop.ui.components.ColorPickerButton
import com.ghost.krop.ui.components.ImageThumbnail
import com.ghost.krop.viewModel.*
import com.ghost.krop.viewModel.Annotation
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

@Composable
fun AnnotatorScreen(
    modifier: Modifier = Modifier,
    viewModel: AnnotatorViewModel = koinViewModel(),
) {
    // Observe ViewModel States
    val uiState by viewModel.uiState.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

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
                    // Re-bind pointerInput whenever the tool mode changes
                    .pointerInput(uiState.mode) {
                        when (uiState.mode) {
                            // 1. Pan & Zoom Mode
                            is CanvasMode.Pan -> {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    viewModel.onEvent(CanvasEvent.Zoom(zoom))
                                    // Adjust pan speed by scale to keep it consistent
                                    viewModel.onEvent(CanvasEvent.Pan(pan / uiState.scale))
                                }
                            }
                            // 2. Drawing Mode
                            is CanvasMode.Tool -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        viewModel.onEvent(CanvasEvent.StartDraw(offset))
                                    },
                                    onDrag = { change, _ ->
                                        viewModel.onEvent(CanvasEvent.UpdateDraw(change.position))
                                    },
                                    onDragEnd = {
                                        viewModel.onEvent(CanvasEvent.EndDraw)
                                    },
                                    onDragCancel = {
                                        viewModel.onEvent(CanvasEvent.EndDraw)
                                    }
                                )
                            }

                            else -> {} // Handle Resize/Edit later
                        }
                    }
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when {
                            it.key == Key.NumPad1 -> {
                                viewModel.onEvent(CanvasEvent.ChangeMode(CanvasMode.RectangleTool))
                                true
                            }

                            it.key == Key.NumPad2 -> {
                                viewModel.onEvent(CanvasEvent.ChangeMode(CanvasMode.PolygonTool))
                                true
                            }

                            it.key == Key.NumPad3 -> {
                                viewModel.onEvent(CanvasEvent.ChangeMode(CanvasMode.PenTool))
                                true
                            }

                            it.key == Key.P -> {
                                viewModel.onEvent(CanvasEvent.ChangeMode(CanvasMode.Pan))
                                true
                            }

                            it.isCtrlPressed && it.key == Key.Z -> {
                                viewModel.onEvent(CanvasEvent.Undo)
                                true
                            }

                            it.isCtrlPressed && it.key == Key.Y -> {
                                viewModel.onEvent(CanvasEvent.Redo)
                                true
                            }

                            else -> false
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
                onEvent = viewModel::onEvent
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
    modifier: Modifier = Modifier,
) {

    Canvas(modifier = modifier.fillMaxSize()) {

        // Keep stroke width visually consistent regardless of zoom
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

                            annotation.points
                                .drop(1)
                                .forEach { point ->
                                    lineTo(point.x, point.y)
                                }

                            close() // polygons are closed
                        }

                        drawPath(
                            path = path,
                            color = annotation.color,
                            style = Stroke(width = stroke)
                        )
                    }
                }

                is Annotation.Polyline -> {

                    if (annotation.points.size >= 2) {

                        val path = Path().apply {

                            val first = annotation.points.first()
                            moveTo(first.x, first.y)

                            annotation.points
                                .drop(1)
                                .forEach { point ->
                                    lineTo(point.x, point.y)
                                }
                        }

                        drawPath(
                            path = path,
                            color = annotation.color,
                            style = Stroke(width = stroke)
                        )
                    }
                }
            }
        }

        /* ----------------------------- */
        /* Active Drawing Feedback */
        /* ----------------------------- */

        val start = uiState.startDrag
        val current = uiState.currentDrag

        if (start != null && current != null) {

            when (uiState.mode) {

                CanvasMode.RectangleTool -> {

                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(
                            minOf(start.x, current.x),
                            minOf(start.y, current.y)
                        ),
                        size = Size(
                            abs(current.x - start.x),
                            abs(current.y - start.y)
                        ),
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(
                                    10f / uiState.scale,
                                    10f / uiState.scale
                                )
                            )
                        )
                    )
                }

                CanvasMode.PolygonTool -> {

                    drawLine(
                        color = Color.Yellow,
                        start = start,
                        end = current,
                        strokeWidth = stroke
                    )
                }

                CanvasMode.PenTool -> {

                    drawLine(
                        color = Color.Cyan,
                        start = start,
                        end = current,
                        strokeWidth = stroke
                    )
                }

                else -> {}
            }
        }
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

    var toolMenuExpanded by remember { mutableStateOf(false) }

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
                description = "Pan (P)",
                isSelected = currentMode is CanvasMode.Pan,
                onClick = { onEvent(CanvasEvent.ChangeMode(CanvasMode.Pan)) }
            )

            VerticalDivider(
                modifier = Modifier
                    .size(width = 1.dp, height = 32.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )

            /* ---------------- Draw Tool Section ---------------- */

            Box {

                ModeButton(
                    icon = when (currentMode) {
                        CanvasMode.RectangleTool -> Icons.Default.CropSquare
                        CanvasMode.PolygonTool -> Icons.Default.Polyline
                        CanvasMode.PenTool -> Icons.Default.Edit
                        else -> Icons.Default.CropSquare
                    },
                    description = "Drawing Tools",
                    isSelected = currentMode !is CanvasMode.Pan,
                    onClick = { toolMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = toolMenuExpanded,
                    onDismissRequest = { toolMenuExpanded = false }
                ) {

                    DropdownMenuItem(
                        text = { Text("Rectangle (1)") },
                        leadingIcon = { Icon(Icons.Default.CropSquare, null) },
                        onClick = {
                            onEvent(CanvasEvent.ChangeMode(CanvasMode.RectangleTool))
                            toolMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Polygon (2)") },
                        leadingIcon = { Icon(Icons.Default.Polyline, null) },
                        onClick = {
                            onEvent(CanvasEvent.ChangeMode(CanvasMode.PolygonTool))
                            toolMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Pen (3)") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            onEvent(CanvasEvent.ChangeMode(CanvasMode.PenTool))
                            toolMenuExpanded = false
                        }
                    )
                }
            }

            /* ---------------- Color Picker ---------------- */

            Box {
                ColorPickerButton(
                    color = selectedColor,
                    onColorSelected = { onEvent(CanvasEvent.ChangeColor(it)) }
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .size(width = 1.dp, height = 32.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )

            /* ---------------- Zoom Section ---------------- */

            IconButton(onClick = { onEvent(CanvasEvent.ZoomIn) }) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In (+)")
            }

            IconButton(onClick = { onEvent(CanvasEvent.ZoomOut) }) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out (-)")
            }

            IconButton(onClick = { onEvent(CanvasEvent.ResetZoom) }) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset Zoom")
            }

            VerticalDivider(
                modifier = Modifier
                    .size(width = 1.dp, height = 32.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )

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

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (mode) {
                        is CanvasMode.Tool -> MaterialTheme.colorScheme.primary // Green for drawing
                        is CanvasMode.Pan -> MaterialTheme.colorScheme.secondary // Blue for panning
                        else -> MaterialTheme.colorScheme.tertiary // Default for other modes
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = when (mode) {
                    CanvasMode.RectangleTool -> "DRAWING"
                    CanvasMode.Pan -> "PANNING"
                    else -> "EDITING"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
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