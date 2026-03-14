package com.ghost.krop.repository.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.WindowPlacement
import com.ghost.krop.models.CanvasMode
import com.ghost.krop.models.ThemeMode
import com.ghost.krop.ui.components.ImageCardType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class SettingsRepositoryOld(
    private val manager: SettingsManager,
    scope: CoroutineScope
) {

    private val _settings = MutableStateFlow(manager.loadSettings())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        _settings
            .drop(1)
            .debounce(300.seconds) // 5 minutes
            .onEach { updatedSettings ->
                manager.saveSettings(updatedSettings)
            }
            .launchIn(scope)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update { current ->
            SettingsValidator.validateAll(transform(current))
        }
    }

    fun updateSessionSettings(transform: (SessionState) -> SessionState) {
        _settings.update {
            it.copy(sessionState = transform(it.sessionState))
        }
    }

    suspend fun flush() {
        manager.saveSettings(_settings.value)
    }
}


// Sealed class for settings events (for logging/monitoring)
sealed class SettingsEvent {
    data class SettingsLoaded(val source: String) : SettingsEvent()
    data class SettingsSaved(val changes: Map<String, Any?>) : SettingsEvent()
    data class SettingUpdated(val key: String, val oldValue: Any?, val newValue: Any?) : SettingsEvent()
    data class SettingsReset(val previousSettings: AppSettings) : SettingsEvent()
    data class SettingsError(val operation: String, val error: Throwable) : SettingsEvent()
    object AutoSaveTriggered : SettingsEvent()
}

// Main Settings Repository
class SettingsRepository(
    private val settingsManager: SettingsManager,
    private val scope: CoroutineScope,
    private val autoSaveDelay: Duration = 2.seconds,
    private val logger: SettingsLogger = SettingsLogger()
) {
    // State flows
    private val _settings = MutableStateFlow(settingsManager.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _lastSaved = MutableStateFlow(System.currentTimeMillis())
    val lastSaved: StateFlow<Long> = _lastSaved.asStateFlow()

    // Events for monitoring
    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 50)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    // Auto-save job
    private var autoSaveJob: Job? = null

    init {
        // Log initial load
        _events.tryEmit(SettingsEvent.SettingsLoaded("disk"))
        logger.logEvent(SettingsEvent.SettingsLoaded("disk"))

        // Start auto-save monitoring
        scope.launch {
            _hasUnsavedChanges
                .filter { it }
                .collect {
                    scheduleAutoSave()
                }
        }
    }

    /**
     * Update settings with partial updates
     */
    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val currentSettings = _settings.value
        val newSettings = update(currentSettings)

        if (newSettings != currentSettings) {
            // Track changes for logging
            val changes = detectChanges(currentSettings, newSettings)

            // Update state
            _settings.value = newSettings
            _hasUnsavedChanges.value = true

            // Log changes
            changes.forEach { (key, values) ->
                val event = SettingsEvent.SettingUpdated(
                    key = key,
                    oldValue = values.first,
                    newValue = values.second
                )
                _events.tryEmit(event)
                logger.logEvent(event)
            }
        }
    }

    /**
     * Update a single setting
     */
    fun <T> updateSetting(key: String, value: T, update: (AppSettings, T) -> AppSettings) {
        updateSettings { update(it, value) }
    }

    /**
     * Convenience methods for common settings
     */
    fun setThemeMode(mode: ThemeMode) {
        updateSettings { it.copy(themeMode = mode) }
    }

    fun setWindowSize(width: Dp, height: Dp) {
        updateSettings {
            it.copy(
                windowWidth = width,
                windowHeight = height
            )
        }
    }

    fun setWindowPosition(x: Dp, y: Dp) {
        updateSettings {
            it.copy(
                positionX = x,
                positionY = y
            )
        }
    }

    fun setWindowPlacement(placement: WindowPlacement) {
        updateSettings { it.copy(placement = placement) }
    }

    fun setLastFocusedImage(path: Path?) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    lastFocusedImage = path
                )
            )
        }
    }

    fun setImagePanelExpanded(expanded: Boolean) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    imagePanelExpanded = expanded
                )
            )
        }
    }

    fun setImagePanelWidth(width: Dp) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    imagePanelWidth = width
                )
            )
        }
    }

    fun setInspectorPanelExpanded(expanded: Boolean) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    inspectorPanelExpanded = expanded
                )
            )
        }
    }

    fun setInspectorPanelWidth(width: Dp) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    inspectorPanelWidth = width
                )
            )
        }
    }

    fun setGalleryViewMode(mode: ImageCardType) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    galleryViewMode = mode
                )
            )
        }
    }

    fun setRecursiveLoad(recursive: Boolean) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    recursiveLoad = recursive
                )
            )
        }
    }

    fun setMaxRecursionDepth(depth: Int) {
        updateSettings {
            it.copy(
                sessionState = it.sessionState.copy(
                    maxRecursionDepth = depth.coerceIn(1, 10)
                )
            )
        }
    }

    // Annotator settings
    fun setTool(tool: CanvasMode) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    tool = tool
                )
            )
        }
    }

    fun setBoundingBoxColor(color: Color) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    boundingBoxColor = color
                )
            )
        }
    }

    fun setBoundingBoxOpacity(opacity: Float) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    boundingBoxOpacity = opacity.coerceIn(0f, 1f)
                )
            )
        }
    }

    fun setAnnotationSearchQuery(query: String) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    searchQuery = query
                )
            )
        }
    }

    fun expandAllInspectorBox(enabled: Boolean) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    expandAllInspectorBox = enabled
                )
            )
        }
    }

    fun setBoundingBoxThickness(thickness: Float) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    boundingBoxThickness = thickness.coerceIn(1f, 10f)
                )
            )
        }
    }

    fun setShowBoundingBoxes(show: Boolean) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    showBoundingBoxes = show
                )
            )
        }
    }

    fun setShowLabels(show: Boolean) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    showLabels = show
                )
            )
        }
    }

    fun setLabelFontSize(size: Dp) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    labelFontSize = size
                )
            )
        }
    }

    fun setLabelColor(color: Color) {
        updateSettings {
            it.copy(
                annotatorSettings = it.annotatorSettings.copy(
                    labelColor = color
                )
            )
        }
    }

    /**
     * Save settings immediately
     */
    fun saveNow(): Result<Unit> {
        return if (_hasUnsavedChanges.value) {
            _isSaving.value = true
            val result = settingsManager.saveSettings(_settings.value)

            if (result.isSuccess) {
                _hasUnsavedChanges.value = false
                _lastSaved.value = System.currentTimeMillis()

                val changes = detectChanges(settingsManager.loadSettings(), _settings.value)
                _events.tryEmit(SettingsEvent.SettingsSaved(changes))
                logger.logEvent(SettingsEvent.SettingsSaved(changes))
            } else {
                result.exceptionOrNull()?.let {
                    _events.tryEmit(SettingsEvent.SettingsError("save", it))
                    logger.logEvent(SettingsEvent.SettingsError("save", it))
                }
            }

            _isSaving.value = false
            result
        } else {
            Result.success(Unit) // Nothing to save
        }
    }

    /**
     * Reload settings from disk
     */
    fun reloadFromDisk(): AppSettings {
        val loadedSettings = settingsManager.loadSettings()
        _settings.value = loadedSettings
        _hasUnsavedChanges.value = false
        _lastSaved.value = System.currentTimeMillis()

        _events.tryEmit(SettingsEvent.SettingsLoaded("disk"))
        logger.logEvent(SettingsEvent.SettingsLoaded("disk"))

        return loadedSettings
    }

    /**
     * Reset to default settings
     */
    fun resetToDefaults(): AppSettings {
        val previousSettings = _settings.value
        val defaultSettings = settingsManager.resetToDefaults()

        _settings.value = defaultSettings
        _hasUnsavedChanges.value = false
        _lastSaved.value = System.currentTimeMillis()

        _events.tryEmit(SettingsEvent.SettingsReset(previousSettings))
        logger.logEvent(SettingsEvent.SettingsReset(previousSettings))

        return defaultSettings
    }

    /**
     * Schedule auto-save
     */
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()

        autoSaveJob = scope.launch {
            delay(autoSaveDelay)

            if (_hasUnsavedChanges.value) {
                _events.tryEmit(SettingsEvent.AutoSaveTriggered)
                logger.logEvent(SettingsEvent.AutoSaveTriggered)
                saveNow()
            }
        }
    }

    /**
     * Detect changes between two settings objects
     */
    private fun detectChanges(old: AppSettings, new: AppSettings): Map<String, Pair<Any?, Any?>> {
        val changes = mutableMapOf<String, Pair<Any?, Any?>>()

        if (old.themeMode != new.themeMode) {
            changes["themeMode"] = old.themeMode to new.themeMode
        }
        if (old.windowWidth != new.windowWidth) {
            changes["windowWidth"] = old.windowWidth to new.windowWidth
        }
        if (old.windowHeight != new.windowHeight) {
            changes["windowHeight"] = old.windowHeight to new.windowHeight
        }
        if (old.positionX != new.positionX) {
            changes["positionX"] = old.positionX to new.positionX
        }
        if (old.positionY != new.positionY) {
            changes["positionY"] = old.positionY to new.positionY
        }
        if (old.placement != new.placement) {
            changes["placement"] = old.placement to new.placement
        }

        // Session state changes
        if (old.sessionState.lastFocusedImage != new.sessionState.lastFocusedImage) {
            changes["lastFocusedImage"] = old.sessionState.lastFocusedImage to new.sessionState.lastFocusedImage
        }
        if (old.sessionState.imagePanelExpanded != new.sessionState.imagePanelExpanded) {
            changes["imagePanelExpanded"] = old.sessionState.imagePanelExpanded to new.sessionState.imagePanelExpanded
        }
        if (old.sessionState.imagePanelWidth != new.sessionState.imagePanelWidth) {
            changes["imagePanelWidth"] = old.sessionState.imagePanelWidth to new.sessionState.imagePanelWidth
        }
        if (old.sessionState.inspectorPanelExpanded != new.sessionState.inspectorPanelExpanded) {
            changes["inspectorPanelExpanded"] =
                old.sessionState.inspectorPanelExpanded to new.sessionState.inspectorPanelExpanded
        }
        if (old.sessionState.inspectorPanelWidth != new.sessionState.inspectorPanelWidth) {
            changes["inspectorPanelWidth"] =
                old.sessionState.inspectorPanelWidth to new.sessionState.inspectorPanelWidth
        }
        if (old.sessionState.recursiveLoad != new.sessionState.recursiveLoad) {
            changes["recursiveLoad"] = old.sessionState.recursiveLoad to new.sessionState.recursiveLoad
        }
        if (old.sessionState.maxRecursionDepth != new.sessionState.maxRecursionDepth) {
            changes["maxRecursionDepth"] = old.sessionState.maxRecursionDepth to new.sessionState.maxRecursionDepth
        }
        if (old.sessionState.includeHiddenFiles != new.sessionState.includeHiddenFiles) {
            changes["includeHiddenFiles"] = old.sessionState.includeHiddenFiles to new.sessionState.includeHiddenFiles
        }
        if (old.sessionState.galleryViewMode != new.sessionState.galleryViewMode) {
            changes["galleryViewMode"] = old.sessionState.galleryViewMode to new.sessionState.galleryViewMode
        }

        // Annotator settings changes
        if (old.annotatorSettings.tool != new.annotatorSettings.tool) {
            changes["annotator.tool"] = old.annotatorSettings.tool to new.annotatorSettings.tool
        }
        if (old.annotatorSettings.searchQuery != new.annotatorSettings.searchQuery) {
            changes["annotator.searchQuery"] = old.annotatorSettings.searchQuery to new.annotatorSettings.searchQuery
        }
        if (old.annotatorSettings.expandAllInspectorBox != new.annotatorSettings.expandAllInspectorBox) {
            changes["annotator.expandAllInspectorBox"] =
                old.annotatorSettings.expandAllInspectorBox to new.annotatorSettings.expandAllInspectorBox
        }

        if (old.annotatorSettings.showBoundingBoxes != new.annotatorSettings.showBoundingBoxes) {
            changes["annotator.showBoundingBoxes"] =
                old.annotatorSettings.showBoundingBoxes to new.annotatorSettings.showBoundingBoxes
        }
        if (old.annotatorSettings.boundingBoxColor != new.annotatorSettings.boundingBoxColor) {
            changes["annotator.boundingBoxColor"] =
                old.annotatorSettings.boundingBoxColor to new.annotatorSettings.boundingBoxColor
        }
        if (old.annotatorSettings.boundingBoxOpacity != new.annotatorSettings.boundingBoxOpacity) {
            changes["annotator.boundingBoxOpacity"] =
                old.annotatorSettings.boundingBoxOpacity to new.annotatorSettings.boundingBoxOpacity
        }
        if (old.annotatorSettings.boundingBoxThickness != new.annotatorSettings.boundingBoxThickness) {
            changes["annotator.boundingBoxThickness"] =
                old.annotatorSettings.boundingBoxThickness to new.annotatorSettings.boundingBoxThickness
        }
        if (old.annotatorSettings.showLabels != new.annotatorSettings.showLabels) {
            changes["annotator.showLabels"] = old.annotatorSettings.showLabels to new.annotatorSettings.showLabels
        }
        if (old.annotatorSettings.labelFontSize != new.annotatorSettings.labelFontSize) {
            changes["annotator.labelFontSize"] =
                old.annotatorSettings.labelFontSize to new.annotatorSettings.labelFontSize
        }
        if (old.annotatorSettings.labelColor != new.annotatorSettings.labelColor) {
            changes["annotator.labelColor"] = old.annotatorSettings.labelColor to new.annotatorSettings.labelColor
        }

        return changes
    }

    /**
     * Clean up on close
     */
    fun close() {
        autoSaveJob?.cancel()
        saveNow() // Final save
    }
}

// Logger for settings events
class SettingsLogger {
    fun logEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SettingsLoaded -> {
                Napier.i("⚙️ Settings loaded from ${event.source}")
            }

            is SettingsEvent.SettingsSaved -> {
                Napier.i("💾 Settings saved with ${event.changes.size} changes: ${event.changes.keys}")
            }

            is SettingsEvent.SettingUpdated -> {
                Napier.d("📝 Setting '${event.key}' changed: ${event.oldValue} → ${event.newValue}")
            }

            is SettingsEvent.SettingsReset -> {
                Napier.w("🔄 Settings reset to defaults")
            }

            is SettingsEvent.AutoSaveTriggered -> {
                Napier.d("⏰ Auto-save triggered")
            }

            is SettingsEvent.SettingsError -> {
                Napier.e("❌ Settings error during ${event.operation}", event.error)
            }
        }
    }
}
