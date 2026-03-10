package com.ghost.krop.core.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ghost.krop.models.Annotation
import kotlin.math.hypot

class LineTool(
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

        val distance = hypot(e.x - s.x, e.y - s.y)

        if (distance > 5f) {
            commit(Annotation.Line(start = s, end = e, color = color))
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

        drawScope.drawLine(
            color = color.copy(alpha = getOpacity()),
            start = s,
            end = e,
            strokeWidth = getStrokeWidth(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
        )
    }
}