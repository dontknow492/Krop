package com.ghost.krop.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope

interface CanvasTool {
    fun onPointerDown(position: Offset)
    fun onPointerMove(position: Offset)
    fun onPointerUp(position: Offset)
    fun onCancel()
    fun drawPreview(drawScope: DrawScope)
}

