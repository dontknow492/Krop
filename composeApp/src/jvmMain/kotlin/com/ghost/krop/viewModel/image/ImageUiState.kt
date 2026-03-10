package com.ghost.krop.viewModel.image

import com.ghost.krop.models.DirectorySettings
import com.ghost.krop.repository.LoadFiles
import com.ghost.krop.ui.components.ImageCardType
import java.nio.file.Path

data class ImageUiState(
    val isLoading: Boolean = false,
    val currentDir: LoadFiles? = null,
    val images: List<Path> = emptyList(),
    val searchQuery: String = "",
    val sortType: ImageSort = ImageSort.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val error: String? = null,
    val selectedImage: Path? = null,
    val viewMode: ImageCardType = ImageCardType.POSTER,
    val directorySettings: DirectorySettings = DirectorySettings()
)