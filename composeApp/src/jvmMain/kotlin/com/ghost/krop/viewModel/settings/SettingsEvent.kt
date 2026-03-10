package com.ghost.krop.viewModel.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.WindowPlacement
import com.ghost.krop.models.ThemeMode
import com.ghost.krop.repository.LoadFiles
import com.ghost.krop.ui.components.ImageCardType
import java.nio.file.Path

sealed interface SettingsEvent {

    // Global
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent
    data class SetWindowSize(val width: Dp, val height: Dp) : SettingsEvent
    data class SetPosition(val x: Dp, val y: Dp) : SettingsEvent
    data class SetWindowPlacement(val placement: WindowPlacement) : SettingsEvent

    // Session
    data class SetLastDirectory(val files: LoadFiles) : SettingsEvent
    data class SetLastFocusedImage(val path: Path?) : SettingsEvent

    data class SetRecursiveLoad(val enabled: Boolean) : SettingsEvent
    data class SetMaxRecursionDepth(val depth: Int) : SettingsEvent
    data class SetIncludeHiddenFiles(val include: Boolean) : SettingsEvent

    data class SetGalleryViewMode(val mode: ImageCardType) : SettingsEvent

    data class ResizeImagePanels(val delta: Dp) : SettingsEvent
    data class ResizeInspectorPanels(val delta: Dp) : SettingsEvent

    object ToggleInspectorPanelExpanded : SettingsEvent
    object ToggleImagePanelExpanded : SettingsEvent
}