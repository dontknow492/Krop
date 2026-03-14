package com.ghost.krop.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.ghost.krop.core.tools.CanvasTool
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.ui.screen.getCursorForMode
import com.ghost.krop.viewModel.annotator.CanvasEvent
import com.ghost.krop.viewModel.annotator.CanvasUiState
import io.github.aakira.napier.Napier

@Composable
fun CanvasInteractionAreaDemo(
    uiState: CanvasUiState,
    annotations: List<Annotation>,
    activeTool: CanvasTool?,
    onEvent: (CanvasEvent) -> Unit
) {
    val containerSize = uiState.viewportSize

    val imageSize = uiState.imageSize


    // Calculate where the image actually sits inside the container
    val imageRect = remember(containerSize, imageSize) {
        if (containerSize == IntSize.Zero || imageSize == IntSize.Zero) null
        else calculateImageRect(
            containerSize = containerSize,
            imageSize = imageSize,
            contentScale = ContentScale.Fit
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { onEvent(CanvasEvent.ViewportResized(it)) }
            .pointerHoverIcon(getCursorForMode(uiState.mode))
    ) {

        // Background Grid
        DotGridBackground()
        // Transformed Layer (Image + Annotations)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = uiState.scale
                    scaleY = uiState.scale
                    translationX = uiState.offset.x * uiState.scale
                    translationY = uiState.offset.y * uiState.scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }

//
        ) {
            // Image Layer
            ImageThumbnail(
                path = uiState.selectedImage!!,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onImageLoaded = {
                    onEvent(CanvasEvent.ImageLoaded(it))
                }
            )

            // demo overlay
            imageRect?.let { rect ->

                Box(
                    Modifier
                        .graphicsLayer {
                            translationX = rect.left
                            translationY = rect.top
                        }
                        .size(
                            with(LocalDensity.current) { rect.width.toDp() },
                            with(LocalDensity.current) { rect.height.toDp() }
                        )
                        .clipToBounds()
                        .pointerInput(uiState.mode, imageRect, activeTool) {

                            fun toNormalized(local: Offset): Offset {

                                val x = (local.x / rect.width).coerceIn(0f, 1f)
                                val y = (local.y / rect.height).coerceIn(0f, 1f)

                                return Offset(x, y)
                            }

                            when (uiState.mode) {

                                CanvasMode.Pan -> {
                                    detectTransformGestures { centroid, pan, zoom, _ ->

                                        if (zoom != 1f) {
                                            onEvent(CanvasEvent.ZoomAt(zoom, centroid))
                                        }

                                        if (pan != Offset.Zero) {
                                            onEvent(CanvasEvent.Pan(pan / uiState.scale))
                                        }
                                    }
                                }

                                is CanvasMode.Draw -> {

                                    awaitEachGesture {

                                        val down = awaitFirstDown(requireUnconsumed = false)

                                        val start = toNormalized(down.position)

                                        activeTool?.onPointerDown(start)

                                        var pointerId = down.id

                                        while (true) {

                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: continue

                                            val pos = toNormalized(change.position)

                                            if (change.pressed) {
                                                activeTool?.onPointerMove(pos)
                                                change.consume()
                                            } else {
                                                activeTool?.onPointerUp(pos)
                                                change.consume()
                                                break
                                            }
                                        }
                                    }
                                }

                                CanvasMode.Edit -> {

                                    detectTapGestures { pos ->

                                        val normalized = toNormalized(pos)

                                        Napier.d("🔍 Tap normalized: $normalized")

                                        // TODO selection logic
                                    }
                                }

                                else -> {}
                            }
                        }
                ) {

                    AnnotationCanvas(
                        annotations = annotations,
                        uiState = uiState,
                        activeTool = activeTool,
                        modifier = Modifier.fillMaxSize()
                    )

                }
            }
        }
    }
}


fun calculateImageRect(
    containerSize: IntSize,
    imageSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment = Alignment.Center
): Rect {

    val scaleFactor = contentScale.computeScaleFactor(
        srcSize = Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
        dstSize = Size(containerSize.width.toFloat(), containerSize.height.toFloat())
    )

    val scaledWidth = imageSize.width * scaleFactor.scaleX
    val scaledHeight = imageSize.height * scaleFactor.scaleY

    val offset = alignment.align(
        size = IntSize(scaledWidth.toInt(), scaledHeight.toInt()),
        space = containerSize,
        layoutDirection = LayoutDirection.Ltr
    )

    return Rect(
        offset = Offset(offset.x.toFloat(), offset.y.toFloat()),
        size = Size(scaledWidth, scaledHeight)
    )
}