package com.ghost.krop.core

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

class CircleTool(
    private val color: Color,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    private var start by mutableStateOf<Offset?>(null)
    private var current by mutableStateOf<Offset?>(null)

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
            color = color,
            radius = radius,
            center = s,
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}