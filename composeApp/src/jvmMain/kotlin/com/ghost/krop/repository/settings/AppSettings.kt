package com.ghost.krop.repository.settings

import com.ghost.krop.models.ThemeMode
import com.ghost.krop.ui.components.ImageCardType
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path

@Serializable
data class AppSettings(
    // Global App State
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val windowWidth: Int = 1280,
    val windowHeight: Int = 800,
    val sessionState: SessionState = SessionState()

)

@Serializable
data class SessionState(
    @Serializable(with = FileSerializer::class)
    val lastDirectory: File? = null, // Default to Home
    val lastFocusedImage: Path? = null,
    val isSidebarVisible: Boolean = false,
    val recursiveLoad: Boolean = false,
    val maxRecursionDepth: Int = 2,
    val includeHiddenFiles: Boolean = false,
    val settingPanelWidth: Float = 400f,
    val galleryViewMode: ImageCardType = ImageCardType.POSTER,
    val sidePanelWidthDp: Float = 320f
)