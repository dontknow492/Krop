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
import io.github.aakira.napier.Napier

@Composable
fun AnnotationCanvas(
    annotations: List<Annotation>,
    uiState: CanvasUiState,
    activeTool: CanvasTool?,
    modifier: Modifier = Modifier,
) {
    Napier.v("Annotation canvas: ${annotations}", tag = "Annotation canvas")

    Canvas(modifier = modifier.fillMaxSize()) {

        val stroke = uiState.strokeWidth / uiState.scale

        val w = size.width
        val h = size.height

        /* ----------------------------- */
        /* Draw Finalized Annotations */
        /* ----------------------------- */

        annotations.forEach { annotation ->

            when (annotation) {

                is Annotation.BoundingBox -> {

                    Napier.v("Drawing bounding box: ${annotation}", tag = "Annotation canvas")

                    val left = minOf(annotation.xMin, annotation.xMax) * w
                    val right = maxOf(annotation.xMin, annotation.xMax) * w
                    val top = minOf(annotation.yMin, annotation.yMax) * h
                    val bottom = maxOf(annotation.yMin, annotation.yMax) * h

                    drawRect(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Polygon -> {

                    if (annotation.points.size >= 2) {

                        val path = Path().apply {

                            val first = annotation.points.first()
                            moveTo(first.x * w, first.y * h)

                            annotation.points.drop(1).forEach { point ->
                                lineTo(point.x * w, point.y * h)
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
                        radius = annotation.radius * minOf(w, h),
                        center = Offset(
                            annotation.center.x * w,
                            annotation.center.y * h
                        ),
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Oval -> {

                    val xMin = annotation.xMin * w
                    val yMin = annotation.yMin * h
                    val xMax = annotation.xMax * w
                    val yMax = annotation.yMax * h

                    drawOval(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
                        topLeft = Offset(xMin, yMin),
                        size = Size(xMax - xMin, yMax - yMin),
                        style = Stroke(width = stroke)
                    )
                }

                is Annotation.Line -> {

                    drawLine(
                        color = annotation.color.copy(alpha = uiState.annotationOpacity),
                        start = Offset(
                            annotation.start.x * w,
                            annotation.start.y * h
                        ),
                        end = Offset(
                            annotation.end.x * w,
                            annotation.end.y * h
                        ),
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