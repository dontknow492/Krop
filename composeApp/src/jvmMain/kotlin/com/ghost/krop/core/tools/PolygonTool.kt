package com.ghost.krop.core.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ghost.krop.models.Annotation
import kotlin.math.sqrt

class PolygonTool(
    private var color: Color,
    private val getOpacity: () -> Float,
    private val getStrokeWidth: () -> Float,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    private val path = Path()

    private val points = mutableStateListOf<Offset>()
    private var preview by mutableStateOf<Offset?>(null)

    // normalized threshold (~1% of image)
    private val CLOSE_THRESHOLD = 0.01f

    private val CLOSE_RADIUS_PX = 20f

    private var lastCanvasSize: Size? = null

    override fun setColor(color: Color) {
        this.color = color
    }

    override fun onPointerDown(position: Offset) {
        preview = position
    }

    override fun onPointerMove(position: Offset) {
        preview = position
    }

    override fun onPointerUp(position: Offset) {

        if (points.isNotEmpty()) {

            val firstPoint = points.first()
            val canvasSize = lastCanvasSize ?: return

            val normalizedThreshold =
                CLOSE_RADIUS_PX / minOf(canvasSize.width, canvasSize.height)

            val distance = calculateDistance(position, firstPoint)

            if (distance < normalizedThreshold && points.size >= 3) {

                commit(
                    Annotation.Polygon(
                        points = points.toList(),
                        color = color
                    )
                )

                points.clear()
                preview = null
                return
            }
        }

        points.add(position)
        preview = null
    }

    override fun onCancel() {
        points.clear()
        preview = null
    }

    private fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    override fun drawPreview(drawScope: DrawScope) {


        if (points.isEmpty() && preview == null) return

        val canvasSize = drawScope.size
        lastCanvasSize = drawScope.size

        path.reset()

        if (points.isNotEmpty()) {

            val first = points.first().toCanvas(canvasSize)
            path.moveTo(first.x, first.y)

            points.drop(1).forEach {
                val p = it.toCanvas(canvasSize)
                path.lineTo(p.x, p.y)
            }

            preview?.let {

                val distance = calculateDistance(it, points.first())

                val target = if (distance < CLOSE_THRESHOLD && points.size >= 3)
                    points.first()
                else
                    it

                val canvasTarget = target.toCanvas(canvasSize)

                path.lineTo(canvasTarget.x, canvasTarget.y)
            }
        }

        drawScope.drawPath(
            path = path,
            color = color.copy(alpha = getOpacity()),
            style = Stroke(
                width = getStrokeWidth(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )

        // Draw closing hit area
        if (points.isNotEmpty()) {

            val first = points.first().toCanvas(canvasSize)

            drawScope.drawCircle(
                color = Color.Red,
                radius = 12f + getStrokeWidth() * 1.5f,
                center = first,
                alpha = 0.5f
            )
        }

        // Draw vertices
        points.forEach {

            val canvasPoint = it.toCanvas(canvasSize)

            drawScope.drawCircle(
                color,
                radius = getStrokeWidth() * 1.5f,
                center = canvasPoint,
                alpha = getOpacity() + 0.2f
            )
        }
    }
}