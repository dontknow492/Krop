package com.ghost.krop.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.krop.models.DirectorySettings
import com.ghost.krop.repository.ImageRepository
import com.ghost.krop.repository.LoadFiles
import com.ghost.krop.ui.components.ImageCardType
import com.ghost.krop.utils.openFileInExplorer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
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
}

enum class ImageSort {
    NAME, SIZE, PATH, DATE
}

enum class SortDirection {
    ASCENDING, DESCENDING
}


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

sealed interface ImageSideEffect {
    data class ShowError(val message: String) : ImageSideEffect
    data class ShowToast(val message: String) : ImageSideEffect
    data class ImageSelected(val path: Path?) : ImageSideEffect
}

class ImageViewModel(
    private val imageRepository: ImageRepository,
) : ViewModel() {

    // --- Private Mutable States ---
    private val _rawImages = MutableStateFlow<List<Path>>(emptyList())
    private val _currentDir = MutableStateFlow<LoadFiles?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectedImage = MutableStateFlow<Path?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(ImageSort.NAME)
    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    private val _viewMode = MutableStateFlow(ImageCardType.POSTER)
    private val _directorySettings = MutableStateFlow(DirectorySettings())

    private val _sideEffect = Channel<ImageSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var imageLoadJob: Job? = null

    // --- Optimized Processing Pipeline ---
    // This flow handles filtering and sorting entirely on a background thread
    @OptIn(FlowPreview::class)
    private val _processedImages = combine(
        _rawImages,
        _searchQuery.debounce(300), // Debounce only applies to the heavy filtering, not UI typing
        _sortType,
        _sortDirection
    ) { raw, query, sortType, sortDirection ->

        // Execute heavy filtering and sorting on IO thread to prevent UI jank
        withContext(Dispatchers.IO) {
            val filtered = if (query.isBlank()) {
                raw
            } else {
                raw.filter { it.fileName.toString().contains(query, ignoreCase = true) }
            }

            val sorted = when (sortType) {
                ImageSort.NAME -> filtered.sortedBy { it.fileName.toString().lowercase() }
                ImageSort.SIZE -> filtered.sortedBy {
                    try {
                        it.toFile().length()
                    } catch (_: Exception) {
                        0L
                    }
                }

                ImageSort.PATH -> filtered.sortedBy { it.toString().lowercase() }
                ImageSort.DATE -> filtered.sortedBy {
                    try {
                        it.toFile().lastModified()
                    } catch (_: Exception) {
                        0L
                    }
                }
            }

            if (sortDirection == SortDirection.DESCENDING) sorted.asReversed() else sorted
        }
    }.flowOn(Dispatchers.Default)

    // --- UI State Aggregation ---
    // Grouping flows to avoid the >5 combine limit limit while remaining type-safe
    private val viewStateFlow =
        combine(_viewMode, _currentDir, _directorySettings, _searchQuery) { mode, dir, settings, query ->
            listOf(mode, dir, settings, query) // Temporary holder
        }

    private val filterStateFlow = combine(_sortType, _sortDirection) { type, dir ->
        type to dir
    }

    private val dataStateFlow =
        combine(_processedImages, _isLoading, _error, _selectedImage) { images, loading, err, selected ->
            listOf(images, loading, err, selected)
        }

    val uiState = combine(viewStateFlow, filterStateFlow, dataStateFlow) { viewArr, filterPair, dataArr ->
        ImageUiState(
            viewMode = viewArr[0] as ImageCardType,
            currentDir = viewArr[1] as LoadFiles?,
            directorySettings = viewArr[2] as DirectorySettings,
            searchQuery = viewArr[3] as String,
            sortType = filterPair.first,
            sortDirection = filterPair.second,
            images = dataArr[0] as List<Path>,
            isLoading = dataArr[1] as Boolean,
            error = dataArr[2] as String?,
            selectedImage = dataArr[3] as Path?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ImageUiState()
    )

    init {
        // Automatically trigger reload if directory or settings change
        combine(_currentDir.filterNotNull(), _directorySettings) { dir, settings ->
            dir to settings
        }.onEach { (dir, settings) ->
            loadImagesFromRepository(dir, settings)
        }.launchIn(viewModelScope)

        _selectedImage
            .filterNotNull()
            .onEach { image ->
                _sideEffect.send(ImageSideEffect.ImageSelected(image))
            }
            .launchIn(viewModelScope)
    }

    // --- Intent Dispatcher ---
    fun onEvent(event: ImageEvent) {
        when (event) {
            is ImageEvent.LoadImages -> _currentDir.update {
                LoadFiles(
                    folders = listOf(event.directory),
                    files = emptyList()
                )
            }

            is ImageEvent.SelectImage -> _selectedImage.update { event.path }
            is ImageEvent.Search -> _searchQuery.update { event.query }
            is ImageEvent.ChangeViewMode -> _viewMode.value = event.viewMode
            is ImageEvent.DirectorySettingChange -> _directorySettings.update { event.setting }
            is ImageEvent.DeleteImage -> deleteImage(event.path)
            is ImageEvent.DeleteImages -> deleteImages(event.paths)
            is ImageEvent.Sort -> {
                _sortType.update { event.sort }
                _sortDirection.update { event.direction }
            }

            is ImageEvent.ClearImages -> {
                clearWorkspace()
                viewModelScope.launch { _sideEffect.send(ImageSideEffect.ShowToast("Workspace cleared")) }
            }

            is ImageEvent.OpenInExplorer -> {
                viewModelScope.launch {
                    try {
                        openFileInExplorer(event.path)
                    } catch (e: Exception) {
                        val errorMsg = "Failed to open in explorer: ${e.message}"
                        Napier.e(errorMsg, e)
                        _sideEffect.send(ImageSideEffect.ShowError(errorMsg))
                    }
                }
            }

            is ImageEvent.LoadFiles -> _currentDir.update {
                LoadFiles(
                    folders = event.folders,
                    files = event.files
                )
            }
        }
    }

    // --- Core Logic ---

    private fun loadImagesFromRepository(files: LoadFiles, settings: DirectorySettings) {
        imageLoadJob?.cancel() // Cancel ongoing load if directory changes rapidly
        imageLoadJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            _rawImages.value = emptyList() // Clear UI immediately

            try {
                imageRepository.getImages(
                    request = files,
                    recursive = settings.isRecursive,
                    maxDepth = settings.maxDepth
                ).collect { chunk ->
                    // Safely accumulate images as they stream in
                    _rawImages.update { currentList -> currentList + chunk }
                }
            } catch (e: Exception) {
                val errorMsg = "Failed to load dataset: ${e.message}"
                Napier.e(errorMsg, e)
                _error.value = errorMsg
                _sideEffect.send(ImageSideEffect.ShowError(errorMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearWorkspace() {
        imageLoadJob?.cancel()
        _rawImages.value = emptyList()
        _currentDir.value = null
        _selectedImage.value = null
        _error.value = null
    }

    private fun deleteImage(path: Path) {
        _rawImages.update { list -> list.filterNot { it == path } }
        if (_selectedImage.value == path) _selectedImage.value = null
        Napier.v { "Removed image from workspace: $path" }
    }

    private fun deleteImages(paths: List<Path>) {
        val pathSet = paths.toSet() // O(1) lookups for filtering
        _rawImages.update { list -> list.filterNot { it in pathSet } }
        if (_selectedImage.value in pathSet) _selectedImage.value = null
        Napier.v { "Removed ${paths.size} images from workspace" }
    }
}


