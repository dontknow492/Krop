package com.ghost.krop.viewModel.settings

sealed interface SettingsEffect {

    data class ShowError(val message: String) : SettingsEffect

    object ReloadImages : SettingsEffect

}