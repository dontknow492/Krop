package com.ghost.krop.repository.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.ghost.krop.models.ThemeMode
import com.ghost.krop.repository.LoadFiles
import com.ghost.krop.ui.components.ImageCardType
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class AppSettings(
    // Global App State
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    @Serializable(with = DpSerializer::class)
    val windowWidth: Dp = 1280.dp,
    @Serializable(with = DpSerializer::class)
    val windowHeight: Dp = 800.dp,

    @Serializable(with = DpSerializer::class)
    val positionX: Dp = 0.dp,
    @Serializable(with = DpSerializer::class)
    val positionY: Dp = 0.dp,

    val placement : WindowPlacement = WindowPlacement.Floating,

    val sessionState: SessionState = SessionState()

)

@Serializable
data class SessionState(
    val files: LoadFiles? = null, // Not serialized, only used in-memory during a session

     // Default to Home
    @Serializable(with = PathSerializer::class)
    val lastFocusedImage: Path? = null,
    val imagePanelExpanded: Boolean = false,
    @Serializable(with = DpSerializer::class)
    val imagePanelWidth: Dp = 400.dp,


    // inspector side panel
    val inspectorPanelExpanded: Boolean = false,
    @Serializable(with = DpSerializer::class)
    val inspectorPanelWidth: Dp = 400.dp,


    val recursiveLoad: Boolean = false,
    val maxRecursionDepth: Int = 2,
    val includeHiddenFiles: Boolean = false,

    val galleryViewMode: ImageCardType = ImageCardType.POSTER,
)