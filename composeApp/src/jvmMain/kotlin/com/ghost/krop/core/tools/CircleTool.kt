package com.ghost.krop.core.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ghost.krop.models.Annotation
import kotlin.math.hypot

//private val showLabel: Boolean,
//    private val label: String,
//    private val labelColor: Color,
//    private val boundingBoxOpacity: Float,
//    private val boundingBoxStrokeWidth: Float,

class CircleTool(
    private var color: Color,
    private val getOpacity: () -> Float,      // Dynamic getter
    private val getStrokeWidth: () -> Float,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    private var start by mutableStateOf<Offset?>(null)
    private var current by mutableStateOf<Offset?>(null)

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

        // hypot calculates the exact straight-line distance (radius)
        val radius = hypot(e.x - s.x, e.y - s.y)

        if (radius > 5f) { // Prevent accidental microscopic taps
            commit(Annotation.Circle(center = s, radius = radius, color = color))
        }

        start = null
        current = null
    }

    override fun onCancel() {
        start = null
        current = null
    }

    override fun drawPreview(drawScope: DrawScope) {
        val s = start ?: return
        val e = current ?: return
        val radius = hypot(e.x - s.x, e.y - s.y)

        drawScope.drawCircle(
            color = color.copy(alpha = getOpacity()), // Live opacity
            radius = radius,
            center = s,
            style = Stroke(
                width = getStrokeWidth(), // Live stroke
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}