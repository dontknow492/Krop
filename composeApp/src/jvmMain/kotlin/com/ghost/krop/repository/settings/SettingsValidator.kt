package com.ghost.krop.repository.settings

import androidx.compose.ui.unit.dp
import com.ghost.krop.repository.LoadFiles
import java.nio.file.Files

object SettingsValidator {

    fun validateAll(settings: AppSettings): AppSettings {
        return settings.copy(
            // 1. Global Window & UI State
            windowWidth = settings.windowWidth.coerceIn(400.dp, 7680.dp), // Min: 800px, Max: 8K
            windowHeight = settings.windowHeight.coerceIn(600.dp, 4320.dp),

            positionX = settings.positionX.coerceIn(0.dp, 7680.dp),
            positionY = settings.positionY.coerceIn(0.dp, 4320.dp),

            // 2. Session State (delegating to a separate function for clarity)
            sessionState = validateSession(settings.sessionState),
            // 3. Annotator Settings (delegating to a separate function for clarity)
            annotatorSettings = validateAnnotatorSettings(settings.annotatorSettings)
        )
    }

    fun validateSession(settings: SessionState): SessionState {
        return settings.copy(

            // -----------------------------
            // File system validation
            // -----------------------------
            files = validateLoadFiles(settings.files),

            lastFocusedImage = settings.lastFocusedImage?.takeIf {
                Files.exists(it) && Files.isRegularFile(it)
            },

            // -----------------------------
            // Recursion safety
            // -----------------------------
            maxRecursionDepth = settings.maxRecursionDepth.coerceIn(0, 10),

            // -----------------------------
            // Image panel UI constraints
            // -----------------------------
            imagePanelWidth = settings.imagePanelWidth
                .value
                .coerceIn(200f, 800f)
                .dp,

            imagePanelExpanded = settings.imagePanelExpanded,

            // -----------------------------
            // Inspector panel UI constraints
            // -----------------------------
            inspectorPanelWidth = settings.inspectorPanelWidth
                .value
                .coerceIn(200f, 800f)
                .dp,

            inspectorPanelExpanded = settings.inspectorPanelExpanded,

            // -----------------------------
            // Gallery behavior
            // -----------------------------
            recursiveLoad = settings.recursiveLoad,
            includeHiddenFiles = settings.includeHiddenFiles,
            galleryViewMode = settings.galleryViewMode
        )
    }


    fun validateAnnotatorSettings(settings: AnnotatorSettings): AnnotatorSettings {
        return settings.copy(
            boundingBoxOpacity = settings.boundingBoxOpacity.coerceIn(0f, 1f),
            boundingBoxThickness = settings.boundingBoxThickness.coerceIn(1f, 10f),
            labelFontSize = settings.labelFontSize.coerceIn(8.dp, 72.dp),
            showBoundingBoxes = settings.showBoundingBoxes,
            boundingBoxColor = settings.boundingBoxColor,
            showLabels = settings.showLabels,
            labelColor = settings.labelColor
        )
    }


    fun validateLoadFiles(loadFiles: LoadFiles?): LoadFiles? {
        if (loadFiles == null) return null

        val validFiles = loadFiles.files.filter { path ->
            Files.exists(path) && Files.isRegularFile(path)
        }

        val validFolders = loadFiles.folders.filter { path ->
            Files.exists(path) && Files.isDirectory(path)
        }

        if (validFiles.isEmpty() && validFolders.isEmpty()) {
            return null
        }

        return LoadFiles(
            files = validFiles,
            folders = validFolders
        )
    }
}