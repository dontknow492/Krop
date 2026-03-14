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
import kotlin.math.abs

class RectangleTool(
    private var color: Color,
    private val getOpacity: () -> Float,      // Dynamic getter
    private val getStrokeWidth: () -> Float,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    // Wrapped in Compose State to trigger Canvas redraws
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
        if (start != null) {
            current = position
        }
    }

    override fun onPointerUp(position: Offset) {

        val s = start ?: return
        current = position

        if (abs(position.x - s.x) > 0.002f && abs(position.y - s.y) > 0.002f) {

            commit(
                Annotation.BoundingBox(
                    xMin = minOf(s.x, position.x),
                    yMin = minOf(s.y, position.y),
                    xMax = maxOf(s.x, position.x),
                    yMax = maxOf(s.y, position.y),
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

    override fun drawPreview(drawScope: DrawScope) {

        val s = start ?: return
        val e = current ?: return

        val sCanvas = s.toCanvas(drawScope.size)
        val eCanvas = e.toCanvas(drawScope.size)

        drawScope.drawRect(
            color = color.copy(alpha = getOpacity()),
            topLeft = Offset(minOf(sCanvas.x, eCanvas.x), minOf(sCanvas.y, eCanvas.y)),
            size = Size(
                abs(eCanvas.x - sCanvas.x),
                abs(eCanvas.y - sCanvas.y)
            ),
            style = Stroke(
                width = getStrokeWidth(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}