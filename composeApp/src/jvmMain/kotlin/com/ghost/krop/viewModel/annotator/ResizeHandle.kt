package com.ghost.krop.viewModel.annotator

import kotlinx.serialization.Serializable

@Serializable
enum class ResizeHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}