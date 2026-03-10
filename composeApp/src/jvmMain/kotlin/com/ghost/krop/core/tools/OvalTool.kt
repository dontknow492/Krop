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

class OvalTool(
    private var color: Color,
    private val getOpacity: () -> Float,      // Dynamic getter
    private val getStrokeWidth: () -> Float,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    private var start by mutableStateOf<Offset?>(null)
    private var current by mutableStateOf<Offset?>(null)

    override fun onPointerDown(position: Offset) {
        start = position
        current = position
    }

    override fun setColor(color: Color) {
        this.color = color
    }

    override fun onPointerMove(position: Offset) {
        if (start != null) current = position
    }

    override fun onPointerUp(position: Offset) {
        val s = start ?: return
        val e = current ?: return

        if (abs(e.x - s.x) > 5f && abs(e.y - s.y) > 5f) {
            commit(
                Annotation.Oval(
                    xMin = minOf(s.x, e.x),
                    yMin = minOf(s.y, e.y),
                    xMax = maxOf(s.x, e.x),
                    yMax = maxOf(s.y, e.y),
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

        drawScope.drawOval(
            color = color.copy(alpha = getOpacity()),
            topLeft = Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
            size = Size(abs(e.x - s.x), abs(e.y - s.y)),
            style = Stroke(
                width = getStrokeWidth(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}