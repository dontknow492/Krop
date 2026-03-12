package com.ghost.krop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ghost.krop.core.tools.CanvasTool
import com.ghost.krop.models.Annotation
import com.ghost.krop.viewModel.annotator.CanvasUiState

@Composable
fun AnnotationCanvas(
    annotations: List<Annotation>,
    uiState: CanvasUiState,
    activeTool: CanvasTool?,
    modifier: Modifier = Modifier,
) {

    Canvas(modifier = modifier.fillMaxSize()) {

        val stroke = uiState.strokeWidth

        /* ----------------------------- */
        /* Draw Finalized Annotations */
        /* ----------------------------- */

        annotations.forEach { annotation ->

            when (annotation) {

                is Annotation.BoundingBox -> {
                    drawRect(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
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
                            color = annotation.color.copy(alpha = uiState.annotationOpacity),
                            style = Stroke(width = stroke)
                        )
                    }
                }

                is Annotation.Circle -> {
                    drawCircle(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
                        radius = annotation.radius,
                        center = annotation.center,
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Oval -> {
                    drawOval(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
                        topLeft = Offset(annotation.xMin, annotation.yMin),
                        size = Size(annotation.xMax - annotation.xMin, annotation.yMax - annotation.yMin),
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Line -> {
                    drawLine(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
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