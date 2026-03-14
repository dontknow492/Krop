package com.ghost.krop.core.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ghost.krop.models.Annotation
import kotlin.math.hypot
import kotlin.math.min

class CircleTool(
    private var color: Color,
    private val getOpacity: () -> Float,
    private val getStrokeWidth: () -> Float,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    private var start by mutableStateOf<Offset?>(null)
    private var current by mutableStateOf<Offset?>(null)

    // ~0.5% of image
    private val MIN_RADIUS = 0.005f

    override fun setColor(color: Color) {
        this.color = color
    }

    override fun onPointerDown(position: Offset) {
        start = position
        current = position
    }

    override fun onPointerMove(position: Offset) {
        if (start != null) current = position
    }

    override fun onPointerUp(position: Offset) {

        val s = start ?: return
        val e = current ?: return

        val radius = hypot(e.x - s.x, e.y - s.y)

        if (radius > MIN_RADIUS) {
            commit(
                Annotation.Circle(
                    center = s,
                    radius = radius,
                    color = color
                )
            )
        }

        start = null
        current = null
    }

    override fun onCancel() {
        start = null
        current = null
    }


    private fun normalizedRadiusToCanvas(radius: Float, size: Size): Float {
        // Use smaller dimension to keep circle round
        return radius * min(size.width, size.height)
    }

    override fun drawPreview(drawScope: DrawScope) {

        val s = start ?: return
        val e = current ?: return

        val canvasSize = drawScope.size

        val radiusNormalized = hypot(e.x - s.x, e.y - s.y)

        val centerCanvas = s.toCanvas(canvasSize)
        val radiusCanvas = normalizedRadiusToCanvas(radiusNormalized, canvasSize)

        drawScope.drawCircle(
            color = color.copy(alpha = getOpacity()),
            radius = radiusCanvas,
            center = centerCanvas,
            style = Stroke(
                width = getStrokeWidth(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}