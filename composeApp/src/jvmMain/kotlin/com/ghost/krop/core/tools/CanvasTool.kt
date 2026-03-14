package com.ghost.krop.core.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

interface CanvasTool {
    fun onPointerDown(position: Offset)
    fun onPointerMove(position: Offset)
    fun onPointerUp(position: Offset)
    fun onCancel()
    fun drawPreview(drawScope: DrawScope)
    fun setColor(color: Color)
    fun Offset.toCanvas(size: Size): Offset {
        return Offset(
            x * size.width,
            y * size.height
        )
    }
}

