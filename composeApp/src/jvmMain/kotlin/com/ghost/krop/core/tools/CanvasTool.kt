package com.ghost.krop.core.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

interface CanvasTool {
    fun onPointerDown(position: Offset)
    fun onPointerMove(position: Offset)
    fun onPointerUp(position: Offset)
    fun onCancel()
    fun drawPreview(drawScope: DrawScope)
    fun setColor(color: Color)
}

