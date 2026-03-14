package com.ghost.krop.viewModel.annotator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ghost.krop.models.CanvasMode
import java.nio.file.Path

data class CanvasUiState(
    val selectedImage: Path? = null,
    val imageSize: IntSize = IntSize.Zero,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val viewportSize: IntSize = IntSize.Zero,
    val mode: CanvasMode = CanvasMode.Draw.Shape.Rectangle,
    val startDrag: Offset? = null,
    val currentDrag: Offset? = null,
    val color: Color = Color.Green,
    val strokeWidth: Float = 3f,
    val annotationOpacity: Float = 1f,
    val showAnnotationLabel: Boolean = true,
    val labelFontSize: Dp = 14.dp,

    val searchQuery: String = "",
    val expandAllInspectorBox: Boolean = false,

    // New fields from repository
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val lastSaved: Long = 0L,
    val error: String? = null
)