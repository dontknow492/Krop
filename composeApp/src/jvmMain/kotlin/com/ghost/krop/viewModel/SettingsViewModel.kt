package com.ghost.krop.viewModel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.krop.models.ThemeMode
import com.ghost.krop.repository.LoadFiles
import com.ghost.krop.repository.settings.AppSettings
import com.ghost.krop.repository.settings.SettingsRepository
import com.ghost.krop.ui.components.ImageCardType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

sealed interface SettingsEffect {

    data class ShowError(val message: String) : SettingsEffect

    object ReloadImages : SettingsEffect

}


class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _effects = MutableSharedFlow<SettingsEffect>()
    val effects: SharedFlow<SettingsEffect> = _effects

    fun onEvent(event: SettingsEvent) {
        when (event) {

            // -----------------------------
            // GLOBAL SETTINGS
            // -----------------------------

            is SettingsEvent.SetThemeMode -> {
                settingsRepository.updateSettings {
                    it.copy(themeMode = event.mode)
                }
            }

            is SettingsEvent.SetWindowSize -> {
                settingsRepository.updateSettings {
                    it.copy(windowWidth = event.width, windowHeight = event.height)
                }
            }

            is SettingsEvent.SetPosition -> {
                settingsRepository.updateSettings {
                    it.copy(positionX = event.x, positionY = event.y)
                }
            }

            is SettingsEvent.SetWindowPlacement -> {
                settingsRepository.updateSettings {
                    it.copy(placement = event.placement)
                }
            }

            // -----------------------------
            // SESSION SETTINGS
            // -----------------------------

            is SettingsEvent.SetLastDirectory -> {
                settingsRepository.updateSettings {
                    it.copy(sessionState = it.sessionState.copy(files = event.files))
                }

                sendEffect(SettingsEffect.ReloadImages)
            }

            is SettingsEvent.SetLastFocusedImage -> {
                settingsRepository.updateSettings {
                    it.copy(sessionState = it.sessionState.copy(lastFocusedImage = event.path))
                }
            }

            is SettingsEvent.SetRecursiveLoad -> {
                settingsRepository.updateSettings {
                    it.copy(sessionState = it.sessionState.copy(recursiveLoad = event.enabled))
                }

                sendEffect(SettingsEffect.ReloadImages)
            }

            is SettingsEvent.SetMaxRecursionDepth -> {
                settingsRepository.updateSettings {
                    it.copy(sessionState = it.sessionState.copy(maxRecursionDepth = event.depth))
                }
            }

            is SettingsEvent.SetIncludeHiddenFiles -> {
                settingsRepository.updateSettings {
                    it.copy(sessionState = it.sessionState.copy(includeHiddenFiles = event.include))
                }
            }

            is SettingsEvent.SetGalleryViewMode -> {
                settingsRepository.updateSettings {
                    it.copy(sessionState = it.sessionState.copy(galleryViewMode = event.mode))
                }
            }

            // -----------------------------
            // PANEL RESIZE
            // -----------------------------

            is SettingsEvent.ResizeImagePanels -> {
                settingsRepository.updateSettings {
                    it.copy(
                        sessionState = it.sessionState.copy(
                            imagePanelWidth =
                                (it.sessionState.imagePanelWidth + event.delta)
                                    .coerceIn(200.dp, 800.dp)
                        )
                    )
                }
            }

            is SettingsEvent.ResizeInspectorPanels -> {
                settingsRepository.updateSettings {
                    it.copy(
                        sessionState = it.sessionState.copy(
                            inspectorPanelWidth =
                                (it.sessionState.inspectorPanelWidth + event.delta)
                                    .coerceIn(200.dp, 800.dp)
                        )
                    )
                }
            }

            // -----------------------------
            // TOGGLES
            // -----------------------------

            SettingsEvent.ToggleInspectorPanelExpanded -> {
                settingsRepository.updateSettings {
                    it.copy(
                        sessionState = it.sessionState.copy(
                            inspectorPanelExpanded =
                                !it.sessionState.inspectorPanelExpanded
                        )
                    )
                }
            }

            SettingsEvent.ToggleImagePanelExpanded -> {
                settingsRepository.updateSettings {
                    it.copy(
                        sessionState = it.sessionState.copy(
                            imagePanelExpanded =
                                !it.sessionState.imagePanelExpanded
                        )
                    )
                }
            }
        }
    }

    private fun sendEffect(effect: SettingsEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    suspend fun flushSettings() {
        settingsRepository.flush()
    }
}