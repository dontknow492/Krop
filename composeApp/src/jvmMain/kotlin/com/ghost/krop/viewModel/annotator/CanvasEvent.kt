package com.ghost.krop.viewModel.annotator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ghost.krop.models.Annotation
import com.ghost.krop.models.CanvasMode
import java.nio.file.Path

sealed interface CanvasEvent {
    //
    data class SelectImage(val path: Path?) : CanvasEvent

    // Viewport
    data class Pan(val delta: Offset) : CanvasEvent
    data class Zoom(val scale: Float) : CanvasEvent
    object ZoomIn : CanvasEvent
    object ZoomOut : CanvasEvent
    object ResetZoom : CanvasEvent
    data class ChangeMode(val mode: CanvasMode) : CanvasEvent // Added this crucial event

    // Management
    data class AddAnnotation(val annotation: Annotation) : CanvasEvent
    data class RemoveAnnotation(val id: String) : CanvasEvent
    data class UpdateAnnotation(val annotation: Annotation) : CanvasEvent
    data object ClearCanvas : CanvasEvent

    //style
    data class ChangeStrokeWidth(val width: Float) : CanvasEvent
    data class ChangeOpacity(val opacity: Float) : CanvasEvent
    data object ToggleLabelVisibility : CanvasEvent
    data class ChangeLabelFontSize(val fontSize: Dp) : CanvasEvent


    // Navigation
    data object NextImage : CanvasEvent
    data object PreviousImage : CanvasEvent

    // History
    data object Undo : CanvasEvent
    data object Redo : CanvasEvent

    // color
    data class ChangeColor(val color: Color) : CanvasEvent
}