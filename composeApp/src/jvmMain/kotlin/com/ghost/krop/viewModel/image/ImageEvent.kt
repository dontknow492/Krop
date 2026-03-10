package com.ghost.krop.viewModel.image

import com.ghost.krop.models.DirectorySettings
import com.ghost.krop.ui.components.ImageCardType
import java.nio.file.Path

// 1. Events & States
sealed interface ImageEvent {
    data class LoadImages(val directory: Path) : ImageEvent
    data object ClearImages : ImageEvent
    data class SelectImage(val path: Path) : ImageEvent
    data class Search(val query: String) : ImageEvent
    data class Sort(val sort: ImageSort, val direction: SortDirection) : ImageEvent
    data class ChangeViewMode(val viewMode: ImageCardType) : ImageEvent
    data class DeleteImage(val path: Path) : ImageEvent
    data class DeleteImages(val paths: List<Path>) : ImageEvent
    data class OpenInExplorer(val path: Path) : ImageEvent
    data class DirectorySettingChange(val setting: DirectorySettings) : ImageEvent
    data class LoadFiles(val files: List<Path>, val folders: List<Path>) : ImageEvent
    data object NextImage : ImageEvent
    data object PreviousImage : ImageEvent
}