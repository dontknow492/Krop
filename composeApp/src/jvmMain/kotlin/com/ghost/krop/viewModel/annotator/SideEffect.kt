package com.ghost.krop.viewModel.annotator

sealed interface SideEffect {
    data object ShowNextImage : SideEffect
    data object ShowPreviousImage : SideEffect
    data class ShowToast(val message: String) : SideEffect
    data class ShowError(val message: String) : SideEffect
}