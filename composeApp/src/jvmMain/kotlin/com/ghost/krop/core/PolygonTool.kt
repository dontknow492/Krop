package com.ghost.krop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ghost.krop.models.Annotation
import kotlin.math.sqrt

class PolygonTool(
    private val color: Color,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    private val points = mutableStateListOf<Offset>()
    private var preview by mutableStateOf<Offset?>(null)

    // How close the finger needs to be to the first point to auto-close the polygon
    private val CLOSE_THRESHOLD = 50f

    override fun onPointerDown(position: Offset) {
        // Start showing the preview where the finger touches
        preview = position
    }

    override fun onPointerMove(position: Offset) {
        // Update the preview as the finger drags around
        preview = position
    }

    override fun onPointerUp(position: Offset) {
        // If we already have points, check if we are closing the polygon
        if (points.isNotEmpty()) {
            val firstPoint = points.first()
            val distance = calculateDistance(position, firstPoint)

            // If the user releases the touch near the start point, close it!
            if (distance < CLOSE_THRESHOLD && points.size >= 2) {
                commit(
                    Annotation.Polygon(
                        points = points.toList(),
                        color = color
                    )
                )
                // Reset tool for the next drawing
                points.clear()
                preview = null
                return
            }
        }

        // Otherwise, the user is just adding a normal vertex. Lock it in.
        points.add(position)
        preview = null
    }

    override fun onCancel() {
        points.clear()
        preview = null
    }

    // Simple Pythagorean theorem to find distance between two Offsets
    private fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    override fun drawPreview(drawScope: DrawScope) {
        if (points.isEmpty() && preview == null) return

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }

                // Draw a line to the finger while dragging
                preview?.let { lineTo(it.x, it.y) }
            }
        }

        // Draw the connecting lines
        drawScope.drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )

        // UX ENHANCEMENT: Highlight the starting point so the user knows where to click to close it
        if (points.isNotEmpty()) {
            drawScope.drawCircle(
                color = Color.Red, // Stand out color
                radius = CLOSE_THRESHOLD / 2f, // Visual indicator of the hit area
                center = points.first(),
                alpha = 0.5f // Make it semi-transparent
            )
        }

        // Draw normal committed vertices
        points.forEach {
            drawScope.drawCircle(color, 8f, it)
        }

        // Draw the active dragging vertex
        preview?.let {
            drawScope.drawCircle(color, 8f, it)
        }
    }
}