package com.ghost.krop.core

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
import com.ghost.krop.ui.theme.Seed
import kotlin.math.abs

class RectangleTool(
    private val color: Color,
    private val commit: (Annotation) -> Unit
) : CanvasTool {

    // Wrapped in Compose State to trigger Canvas redraws
    private var start by mutableStateOf<Offset?>(null)
    private var current by mutableStateOf<Offset?>(null)

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
        val e = current ?: return

        // Only commit if they actually dragged a measurable distance
        if (abs(e.x - s.x) > 5f && abs(e.y - s.y) > 5f) {
            val box = Annotation.BoundingBox(
                xMin = minOf(s.x, e.x),
                yMin = minOf(s.y, e.y),
                xMax = maxOf(s.x, e.x),
                yMax = maxOf(s.y, e.y),
                color = color
            )
            commit(box)
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

        drawScope.drawRect(
            color = Seed,
            topLeft = Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
            size = Size(abs(e.x - s.x), abs(e.y - s.y)),
            style = Stroke(
                width = 4f,
                // 20f represents the dash length, 10f represents the gap length
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}