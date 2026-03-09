package com.ghost.krop.repository.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class SettingsRepository(
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