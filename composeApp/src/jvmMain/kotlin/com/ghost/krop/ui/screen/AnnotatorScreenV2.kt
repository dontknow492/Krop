package com.ghost.krop.ui.screen


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghost.krop.core.tools.CanvasTool
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.ui.components.*
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.annotator.CanvasUiState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotatorScreenV2(
    modifier: Modifier = Modifier,
    uiState: CanvasUiState,
    annotations: List<Annotation>,
    canUndo: Boolean,
    canRedo: Boolean,
    activeTool: CanvasTool?,
    onEvent: (CanvasEvent) -> Unit
) {
    // Local state for UI feedback
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showErrorDetails by remember { mutableStateOf(false) }

    // Log state changes for debugging
    LaunchedEffect(uiState.isLoading, uiState.isSaving, uiState.error) {
        when {
            uiState.isLoading -> Napier.d("⏳ AnnotatorScreen: Loading image...")
            uiState.isSaving -> Napier.d("💾 AnnotatorScreen: Saving annotations...")
            uiState.error != null -> Napier.e("❌ AnnotatorScreen error: ${uiState.error}")
        }
    }

    // Show temporary save confirmation
    LaunchedEffect(uiState.lastSaved) {
        if (uiState.lastSaved > 0 && !uiState.isSaving) {
            showSaveConfirmation = true
            delay(2000)
            showSaveConfirmation = false
        }
    }

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
            CanvasInteractionArea(
                uiState = uiState,
                activeTool = activeTool,
                annotations = annotations,
                onEvent = onEvent
            )

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

            // Status Indicators (Top Left)
            StatusIndicators(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                uiState = uiState,
                annotationsCount = annotations.size,
                showSaveConfirmation = showSaveConfirmation
            )

            // Loading Indicator (Center)
            if (uiState.isLoading) {
                LoadingOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    message = "Loading image..."
                )
            }

            // Saving Indicator (Bottom Right)
            if (uiState.isSaving) {
                SavingIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    title = "Saving annotations..."
                )
            }

            // Error Display
            if (uiState.error != null && showErrorDetails) {
                ErrorDisplay(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    error = uiState.error,
                    onDismiss = { showErrorDetails = false },
                    onRetry = { /* Retry last operation */ }
                )
            }
        }
    }
}

@Composable
private fun CanvasInteractionArea(
    uiState: CanvasUiState,
    annotations: List<Annotation>,
    activeTool: CanvasTool?,
    onEvent: (CanvasEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerHoverIcon(getCursorForMode(uiState.mode))
            .pointerInput(uiState.mode, activeTool, uiState.scale, uiState.offset) {

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
                        this.detectTransformGestures { centroid, pan, zoom, rotation ->
                            onEvent(CanvasEvent.Zoom(zoom))
                            onEvent(CanvasEvent.Pan(pan / uiState.scale))
                            Napier.d("🖱️ Pan: $pan, Zoom: $zoom")
                        }
                    }

                    /* ------------------ DRAWING TOOLS ------------------ */
                    is CanvasMode.Draw -> {
                        awaitEachGesture {
                            val downEvent = awaitFirstDown(requireUnconsumed = false)
                            val canvasPos = mapToCanvas(downEvent.position)

                            Napier.d("✏️ Draw started at canvas: (${canvasPos.x}, ${canvasPos.y})")
                            activeTool?.onPointerDown(canvasPos)

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()

                                if (change != null) {
                                    val currentCanvasPos = mapToCanvas(change.position)

                                    if (change.pressed) {
                                        activeTool?.onPointerMove(currentCanvasPos)
                                    } else {
                                        Napier.d("✏️ Draw completed")
                                        activeTool?.onPointerUp(currentCanvasPos)
                                    }

                                    change.consume()
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }

                    /* ------------------ SELECTION MODE ------------------ */
                    CanvasMode.Edit -> {
                        detectTapGestures(
                            onTap = { offset ->
                                val canvasPos = mapToCanvas(offset)
                                Napier.d("🔍 Tap at canvas: (${canvasPos.x}, ${canvasPos.y})")
                                // TODO: Implement selection logic
                            }
                        )
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
}

@Composable
private fun StatusIndicators(
    modifier: Modifier = Modifier,
    uiState: CanvasUiState,
    annotationsCount: Int,
    showSaveConfirmation: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Image info
            uiState.selectedImage?.let { path ->
                StatusRow(
                    icon = Icons.Default.Image,
                    text = path.fileName.toString(),
                    maxLength = 30
                )
            }

            // Annotation count
            StatusRow(
                icon = Icons.Default.Label,
                text = "$annotationsCount annotation${if (annotationsCount != 1) "s" else ""}"
            )

            // Unsaved changes indicator
            if (uiState.hasUnsavedChanges) {
                StatusRow(
                    icon = Icons.Default.Edit,
                    text = "Unsaved changes",
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
            }

            // Last saved time
            if (uiState.lastSaved > 0) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(uiState.lastSaved))
                StatusRow(
                    icon = Icons.Default.Save,
                    text = "Last saved: $time"
                )
            }

            // Save confirmation (temporary)
            AnimatedVisibility(visible = showSaveConfirmation) {
                StatusRow(
                    icon = Icons.Default.CheckCircle,
                    text = "Saved!",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLength: Int = Int.MAX_VALUE
) {
    val displayText = if (text.length > maxLength) {
        text.substring(0, maxLength) + "..."
    } else {
        text
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingOverlay(
    modifier: Modifier = Modifier,
    message: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorDisplay(
    modifier: Modifier = Modifier,
    error: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

// Update existing HUDOverlay to show more info


@Composable
private fun EmptyWorkspaceState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No Image Selected",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "Select an image from the left panel to start annotating",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
