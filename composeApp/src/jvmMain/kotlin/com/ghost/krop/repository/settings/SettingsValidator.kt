package com.ghost.krop.repository.settings

object SettingsValidator {

    fun validateAll(settings: AppSettings): AppSettings {
        return settings.copy(
            // 1. Global Window & UI State
            windowWidth = settings.windowWidth.coerceIn(800, 7680), // Min: 800px, Max: 8K
            windowHeight = settings.windowHeight.coerceIn(600, 4320),

            // 2. Session State (delegating to a separate function for clarity)
            sessionState = validateSession(settings.sessionState)
        )
    }

    fun validateSession(settings: SessionState): SessionState {
        return settings.copy(
            // Recursion shouldn't go too deep to prevent stack/memory issues
            maxRecursionDepth = settings.maxRecursionDepth.coerceIn(0, 10),

            // UI sizing constraints
            sidePanelWidthDp = settings.sidePanelWidthDp.coerceIn(200f, 800f),


            // Pass through logic for state fields
            lastDirectory = if (settings.lastDirectory?.exists() == true) settings.lastDirectory else null, // If the directory doesn't exist, reset to null
            lastFocusedImage = if (settings.lastFocusedImage != null && settings.lastFocusedImage.toFile()
                    .exists()
            ) settings.lastFocusedImage else null, // If the file doesn't exist, reset to null

            isSidebarVisible = settings.isSidebarVisible,
            settingPanelWidth = settings.sidePanelWidthDp.coerceIn(200f, 800f),
            recursiveLoad = settings.recursiveLoad,
            includeHiddenFiles = settings.includeHiddenFiles,
            galleryViewMode = settings.galleryViewMode
        )
    }
}