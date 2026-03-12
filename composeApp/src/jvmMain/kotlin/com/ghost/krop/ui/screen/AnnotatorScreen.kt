package com.ghost.krop.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Crosshair
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Default
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ghost.krop.core.tools.CanvasTool
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.ui.components.*
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.annotator.CanvasUiState

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


        if (uiState.isSaving) {
            SavingIndicator(
                modifier = Modifier.align(Alignment.Center),
                title = "Saving...",
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