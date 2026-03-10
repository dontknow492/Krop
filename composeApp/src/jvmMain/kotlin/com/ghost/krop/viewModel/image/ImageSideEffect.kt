package com.ghost.krop.viewModel.image

import java.nio.file.Path

sealed interface ImageSideEffect {
    data class ShowError(val message: String) : ImageSideEffect
    data class ShowToast(val message: String) : ImageSideEffect
    data class ImageSelected(val path: Path?) : ImageSideEffect
}