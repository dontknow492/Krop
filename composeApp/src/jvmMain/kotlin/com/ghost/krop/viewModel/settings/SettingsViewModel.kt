package com.ghost.krop.viewModel.settings

import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.krop.repository.settings.AppSettings
import com.ghost.krop.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


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